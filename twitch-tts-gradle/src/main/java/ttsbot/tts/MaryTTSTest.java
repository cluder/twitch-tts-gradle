package ttsbot.tts;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.signalproc.effects.AudioEffect;
import marytts.signalproc.effects.AudioEffects;
import marytts.util.data.audio.MaryAudioUtils;
import ttsbot.util.Utils;

public class MaryTTSTest {

	public static void main(String[] args) throws Exception {
		listEffects();

		speak("Hellooooooooooo World", "Volume(amount:1.0;)" //
		// ,"Robot(amount:100.0;)" //
		// "JetPilot" //
		// , "Chorus(delay1:466;amp1:0.54;delay2:600;amp2:-0.10;delay3:250;amp3:0.30)\n"
		// , "TractScaler(amount:1.5;)" //
		);

		listLocalesAndVoices();

//		MaryAudioUtils.playWavFile(localFile, 0, true);

	}

	private static void listLocalesAndVoices() throws MaryConfigurationException {
		MaryInterface marytts = new LocalMaryInterface();
		System.out.println("Locales:");
		Set<Locale> availableLocales = marytts.getAvailableLocales();
		availableLocales.stream().forEach(s -> System.out.println(s));

//		System.out.println("\nInput Types:");
//		Set<String> availableInputTypes = marytts.getAvailableInputTypes();
//		availableInputTypes.stream().forEach(s -> System.out.println(s));

		{
			System.out.println("\nAll Voices:");
			Set<String> voices = marytts.getAvailableVoices();
			voices.stream().forEach(s -> System.out.println(s));
		}
		{
			System.out.println("\nVoices German:");
			Set<String> voices = marytts.getAvailableVoices(Locale.GERMAN);
			voices.stream().forEach(s -> System.out.println(s));
		}
		{
			System.out.println("\nVoices English:");
			Set<String> voices = marytts.getAvailableVoices(Locale.ENGLISH);
			voices.stream().forEach(s -> System.out.println(s));
		}
	}

	private static void speak(String text, String... effects) throws MaryConfigurationException, SynthesisException,
			LineUnavailableException, IOException, InterruptedException {
		MaryInterface marytts = new LocalMaryInterface();
		String allEffects = "";
		for (String e : effects) {
			if (!allEffects.isEmpty()) {
				allEffects += "+";
			}
			allEffects += e;
		}
		marytts.setAudioEffects(allEffects);
		marytts.setLocale(Locale.US);
		AudioInputStream audio = marytts.generateAudio(text);

		Utils.playAudio(audio, 0);
		MaryAudioUtils.writeWavFile(MaryAudioUtils.getSamplesAsDoubleArray(audio), "/home/core/Music/thisIsMyText.wav",
				audio.getFormat());

		Thread.sleep(5000);
		return;
	}

	private static void listEffects() {
		int countEffects = AudioEffects.countEffects();
		System.out.println(countEffects + " effects found");
		Iterable<AudioEffect> effects = AudioEffects.getEffects();
		for (AudioEffect e : effects) {
			System.out.println();
			System.out.println(String.format("help:%s  -  example:%s", e.getHelpText(),
					e.getFullEffectWithExampleParametersAsString()));
		}

	}

}
