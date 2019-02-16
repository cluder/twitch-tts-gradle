package ttsbot.tts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings.Builder;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import ttsbot.util.Utils;

/**
 * Helper for Google TTS Api Calls.<br>
 */
public class GoogleTTS implements CredentialsProvider {
	private final static Logger log = LoggerFactory.getLogger(GoogleTTS.class);

	public static final int DEFAULT_VOLUME = 0;
	public static final int DEFAULT_PITCH = 0;
	public static final String DEFAULT_LANG = "de";

	// known TTS languages
	private static List<String> knownLanguages = Lists.newArrayList("de", //
			"en-GB", //
			"en-AU", //
			"en-US", //
			"nl", //
			"fr", //
			"it", //
			"ja", //
			"ko", //
			"pt", //
			"es", //
			"sv", //
			"tr");

	// TTS settings
	private double speakingRate = 0;
	private double pitch = DEFAULT_PITCH;
	private double volume = DEFAULT_VOLUME;
	private String lang = DEFAULT_LANG;
	private SsmlVoiceGender gender = SsmlVoiceGender.MALE;

	// google API credentials
	GoogleCredentials credentials;

	public GoogleTTS() {
		try {
			this.credentials = loadCredentials("google-credentials.json");
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
		}
	}

	public List<String> getKnownLanguages() {
		return knownLanguages;
	}

	public String getLang() {
		return lang;
	}

	public double getVolume() {
		return volume;
	}

	/**
	 * Returns google api credentials for {@link CredentialsProvider}
	 */
	@Override
	public Credentials getCredentials() throws IOException {
		return credentials;
	}

	/**
	 * Sets the Volume in % (more or less), values 0-200 are expected.
	 */
	public boolean setVolume(double volume) {
		if (volume >= -96 && volume <= 16) {
			this.volume = volume;
			return true;
		}
		return false;
	}

	public boolean setSpeakingRate(double speakingRate) {
		this.speakingRate = speakingRate;
		if (this.speakingRate < 0.25) {
			this.speakingRate = 0;
			return false;
		}
		return true;
	}

	public boolean setPitch(int value) {
		if (value >= -20 && value <= 20) {
			pitch = value;
			return true;
		}
		return false;
	}

	public boolean setLang(String value) {
		if (value == null || value.length() < 2) {
			return false;
		}

		boolean known = false;
		for (String knownLang : knownLanguages) {
			if (knownLang.startsWith(value)) {
				known = true;
				break;
			}
		}
		if (known) {
			this.lang = value.trim();
		}
		return known;
	}

	public boolean setGender(String value) {
		if (value.equalsIgnoreCase("female")) {
			gender = SsmlVoiceGender.FEMALE;
			return true;
		} else if (value.equalsIgnoreCase("male")) {
			gender = SsmlVoiceGender.MALE;
			return true;
		} else if (value.equalsIgnoreCase("neutral")) {
			gender = SsmlVoiceGender.NEUTRAL;
			return true;
		}
		return false;
	}

	/**
	 * Creates {@link Credentials} by loading a local .json file.
	 */
	private GoogleCredentials loadCredentials(String jsonPath) throws IOException {
		File file = new File(jsonPath);
		try (FileInputStream fis = new FileInputStream(file)) {
			return credentials = GoogleCredentials.fromStream(fis)
					.createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
		}
	}

	/**
	 * see {@link #syntesizeAndPlay(String, String, SsmlVoiceGender)}
	 */
	public void syntesizeAndPlay(String text) throws IOException {
		syntesizeAndPlay(text, null, null);
	}

	/**
	 * Calls google's text-to-speech with the given text and plays the created wav
	 * directly.<br>
	 */
	public void syntesizeAndPlay(String text, String langOverride, SsmlVoiceGender genderOverride) throws IOException {
		if (volume <= -96) {
			return;
		}

		Builder builder = TextToSpeechSettings.newBuilder();
		builder.setCredentialsProvider(this);
		TextToSpeechSettings settings = builder.build();

		// Instantiates a client
		try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {

			// Set the text input to be synthesized
			SynthesisInput input = SynthesisInput.newBuilder() //
					.setText(text)//
					.build();

			// Build the voice request, select the language code ("en-US") and the ssml
			// voice gender
			// ("neutral")
			VoiceSelectionParams deVoiceMale = VoiceSelectionParams.newBuilder() //
					.setLanguageCode(langOverride != null ? langOverride : lang) //
					.setSsmlGender(genderOverride != null ? genderOverride : gender) //
					.build();

			// Select the type of audio file you want returned
			AudioConfig audioConfig = AudioConfig.newBuilder()
					.setAudioEncoding(com.google.cloud.texttospeech.v1.AudioEncoding.LINEAR16)//
					.setVolumeGainDb(volume)//
					.setSpeakingRate(speakingRate) // 0.25 - 4
					.setPitch(pitch) // --20 - +20
					.build();

			// Perform the text-to-speech request on the text input with the selected voice
			// parameters and
			// audio file type
			SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, deVoiceMale, audioConfig);

			// Get the audio contents from the response
			ByteString audioContents = response.getAudioContent();

			// play wav
			Utils.playWAV(audioContents);
		} catch (Throwable e) {
			log.warn(e.getMessage(), e);
		}
	}

	public String getGender() {
		return gender.toString().toLowerCase();
	}

	public double getSpeakrate() {
		return speakingRate;
	}

	public double getPitch() {
		return pitch;
	}

}
