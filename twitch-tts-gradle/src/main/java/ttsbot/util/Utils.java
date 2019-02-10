package ttsbot.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.protobuf.ByteString;

/**
 * Various helper methods.
 */
public class Utils {

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
	 */
	public static void playAudio(AudioInputStream audioInputStream) throws LineUnavailableException, IOException {

		Line line;
		line = AudioSystem.getLine(new Line.Info(Clip.class));

		Clip clip = (Clip) line;

		clip.open(audioInputStream);
		clip.addLineListener(new LineListener() {

			@Override
			public void update(LineEvent le) {
				LineEvent.Type type = le.getType();
				if (type == LineEvent.Type.OPEN) {
				} else if (type == LineEvent.Type.CLOSE) {
				} else if (type == LineEvent.Type.START) {
				} else if (type == LineEvent.Type.STOP) {
					clip.close();
				}
			}
		});
		clip.start();
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
			e.printStackTrace();
		}
	}

	/**
	 * Plays the given .wav content.
	 */
	public static void playWAV(ByteString audioContents) {
		try (InputStream newInputStream = audioContents.newInput();
				AudioInputStream ais = AudioSystem.getAudioInputStream(newInputStream)) {
			playAudio(ais);

		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}
}
