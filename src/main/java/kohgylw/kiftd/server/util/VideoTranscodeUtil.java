package kohgylw.kiftd.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import kohgylw.kiftd.server.mapper.NodeMapper;
import kohgylw.kiftd.server.model.Node;
import kohgylw.kiftd.server.pojo.VideoTranscodeThread;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.VideoAttributes;

/**
 * 
 * <h2>视频转码工具</h2>
 * <p>
 * 该工具用于进行视频转码操作，使用Spring IOC容器管理。
 * </p>
 * 
 * @author 青阳龙野(kohgylw)
 * @version 1.0
 */
@Component
public class VideoTranscodeUtil {

	private EncodingAttributes ea;// 转码设定
	@Resource
	private FileBlockUtil fbu;
	@Resource
	private NodeMapper nm;

	private Map<String, VideoTranscodeThread> videoTranscodeThreads = new HashMap<>();

	{
		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("libmp3lame");
		audio.setBitRate(128000);
		audio.setChannels(2);
		audio.setSamplingRate(44100);
		VideoAttributes video = new VideoAttributes();
		video.setCodec("libx264");
		ea = new EncodingAttributes();
		ea.setFormat("MP4");
		ea.setVideoAttributes(video);
		ea.setAudioAttributes(audio);
	}

	/**
	 * 
	 * <h2>获取指定视频转码进度</h2>
	 * <p>
	 * 以百分制返回指定ID的视频转码进度，如若未开始则自动开始。
	 * </p>
	 * 
	 * @author 青阳龙野(kohgylw)
	 * @param fId
	 *            java.lang.String 转码视频的ID
	 * @return java.lang.String 转码进度，按百分制返回，例如“1.2”代表完成1.2%，返回null则参数不正确。
	 * @throws Exception
	 *             获取失败
	 */
	public String getTranscodeProcess(String fId) throws Exception {
		synchronized (videoTranscodeThreads) {
			VideoTranscodeThread vtt = videoTranscodeThreads.get(fId);
			Node n = nm.queryById(fId);
			File f = fbu.getFileFromBlocks(n);
			if (vtt != null) {
				if ("FIN".equals(vtt.getProgress())) {
					String md5 = DigestUtils.md5Hex(new FileInputStream(f));
					if (md5.equals(vtt.getMd5())) {
						return vtt.getProgress();
					}
				} else {
					return vtt.getProgress();
				}
			}
			String suffix = n.getFileName().substring(n.getFileName().lastIndexOf(".") + 1).toLowerCase();
			switch (suffix) {
			case "mp4":
			case "webm":
			case "mov":
			case "avi":
			case "wmv":
			case "mkv":
				break;
			default:
				throw new IllegalArgumentException();
			}
			videoTranscodeThreads.put(fId, new VideoTranscodeThread(f, ea));
			return "0.0";
		}
	}

}
