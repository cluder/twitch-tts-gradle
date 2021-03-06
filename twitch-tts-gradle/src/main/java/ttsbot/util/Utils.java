package ttsbot.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

/**
 * Various helper methods.
 */
public class Utils {
	private final static Logger log = LoggerFactory.getLogger(Utils.class);

	private static Semaphore lock = new Semaphore(1);

	/**
	 * Clamps the given value to the min max range.
	 */
	public static int constrainToRange(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Clamps the given value to the min max range.
	 */
	public static double constrainToRange(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Plays a .wav audiostream.
	 * 
	 * @param volChange volume change in decibel + / - (default 0)
	 */
	public static void playAudio(AudioInputStream audioInputStream) throws LineUnavailableException, IOException {
		playAudio(audioInputStream, 0);
	}

	public static void playAudio(AudioInputStream audioInputStream, float volChange)
			throws LineUnavailableException, IOException {
		try {
			lock.tryAcquire(10, TimeUnit.SECONDS);

			AudioFormat format = audioInputStream.getFormat();

			log.info(String.format("%s, bits:%s, channels:%s, samplerate:%s, framerate:%s ", //
					format.getEncoding(), format.getSampleSizeInBits(), //
					format.getChannels(), format.getSampleRate(), format.getFrameRate()));
			Line line;
			line = AudioSystem.getLine(new Line.Info(Clip.class));

			Clip clip = (Clip) line;

			clip.open(audioInputStream);

			// min / max: -80 / 6
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			volChange = Math.max(volChange, -79.9f);
			volChange = Math.min(volChange, 5.9f);

			gainControl.setValue(volChange);

			clip.addLineListener(new LineListener() {

				@Override
				public void update(LineEvent le) {
					LineEvent.Type type = le.getType();
					if (type == LineEvent.Type.OPEN) {
					} else if (type == LineEvent.Type.CLOSE) {
					} else if (type == LineEvent.Type.START) {
					} else if (type == LineEvent.Type.STOP) {
						clip.close();
						lock.release();
					}
				}
			});
			clip.start();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			lock.release();
		}
	}

	/**
	 * Loads the given .wav file and plays it.
	 */
	public static void playWAV(String pathToWav) {
		File file = new File(pathToWav);
		try (FileInputStream fileInputStream = new FileInputStream(file);
				BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
				AudioInputStream ais = AudioSystem.getAudioInputStream(bufferedInputStream)) {
			playAudio(ais);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Plays the given .wav content.
	 */
	public static void playWAV(ByteString audioContents, float volume) {
		try (InputStream newInputStream = audioContents.newInput();
				AudioInputStream ais = AudioSystem.getAudioInputStream(newInputStream)) {
			playAudio(ais, volume);

		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			log.error(e.getMessage(), e);
		}
	}
}
