package ttsbot.tts;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

public class GoogleTTS implements CredentialsProvider {
	public static final int DEFAULT_VOLUME = -1;
	public static final int DEFAULT_PITCH = 0;
	private static List<String> knownLanguages = Lists.newArrayList("en", //
			"de", //
			"nl", //
			"fr", //
			"it", //
			"ja", //
			"ko", //
			"pt", //
			"es", //
			"sv", //
			"tr");
	private double speakingRate = 0;
	private double pitch = DEFAULT_PITCH;
	private double volume = DEFAULT_VOLUME;
	private String lang = "de-CH";
	GoogleCredentials credentials;
	private SsmlVoiceGender gender = SsmlVoiceGender.MALE;

	public GoogleTTS() {
		try {
			this.credentials = loadCredentials("google-credentials.json");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> getKnownLanguages() {
		return knownLanguages;
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
	public void setVolume(double volume) {
		this.volume = volume;
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
		String langCode = value.substring(0, 2);
		if (knownLanguages.contains(langCode)) {
			this.lang = value.trim();
			return true;
		}
		return false;
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
		final InputStream cred = ClassLoader.getSystemResourceAsStream("google-credentials.json");
		return credentials = GoogleCredentials.fromStream(cred)
				.createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
	}

	public void syntesizeAndPlay(String text) throws IOException {
		syntesizeAndPlay(text, null, null);
	}

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

			Utils.playWAV(audioContents);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
