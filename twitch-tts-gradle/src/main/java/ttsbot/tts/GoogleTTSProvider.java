package ttsbot.tts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.ListVoicesRequest;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings.Builder;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import ttsbot.util.Utils;

/**
 * Helper for Google TTS Api Calls.<br>
 */
public class GoogleTTSProvider implements CredentialsProvider, TTSProvider {
	private final static Logger log = LoggerFactory.getLogger(GoogleTTSProvider.class);
	public static final String DEFAULT_LANG = "de";
	public static final String DEFAULT_VOICE = "de-DE-Wavenet-A";

	// TTS settings
	private double speakingRate = 0;
	private double pitch = DEFAULT_PITCH;
	private float volume = DEFAULT_VOLUME;
	private String lang = DEFAULT_LANG;
	private SsmlVoiceGender gender = SsmlVoiceGender.MALE;
	private String preferredVoice = "";

	// google API credentials
	GoogleCredentials credentials;

	protected List<String> knownLanguages = Lists.newArrayList(//
			"de", //
			"en-GB", //
			"es", //
			"fr", //
			"it", //
			"nl", //
			"ru", //
			"tr", //
			"da", //
			"cs", //
			"el", //
			"en-AU", //
			"en-IN", //
			"en-US", //
			"fi", //
			"fil", //
			"hi", //
			"hu", //
			"id", //
			"ja", //
			"ko", //
			"nb", //
			"pl", //
			"pt", //
			"sk", //
			"sv", //
			"uk", //
			"vi" //
	);
	protected Set<TTSFeature> knownFeatures = EnumSet.allOf(TTSFeature.class);

	public GoogleTTSProvider() throws IOException {
		super();
		try {
			this.credentials = loadCredentials("google-credentials.json");
		} catch (IOException e) {
			log.warn(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public String getName() {
		return "Google TTS";
	}

	@Override
	public void setDefault() {
		lang = DEFAULT_LANG;
		preferredVoice = DEFAULT_VOICE;

	}

	public List<String> getKnownLanguages() {
		return knownLanguages;
	}

	public boolean isKnownLanguage(String dst) {
		for (String lang : knownLanguages) {
			if (lang.startsWith(dst)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isKnownVoice(String lang) {
		Collection<String> listSupportedVoices = listSupportedVoices(getLang());
		if (listSupportedVoices.contains(lang)) {
			return true;
		}
		return false;
	}

	@Override
	public void setVoice(String value) {
		preferredVoice = value;
	}

	@Override
	public String getVoice() {
		return preferredVoice;
	}

	@Override
	public boolean isSupported(TTSFeature f) {
		return knownFeatures.contains(f);
	}

	public String getLang() {
		return lang;
	}

	public float getVolume() {
		return volume;
	}

	/**
	 * Returns google api credentials for {@link CredentialsProvider}
	 */
	@Override
	public Credentials getCredentials() throws IOException {
		return credentials;
	}

	public boolean setVolume(float volume) {
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
		return setLang(value, false);
	}

	public boolean setLang(String value, boolean force) {
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
		if (known || force) {
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
					.setName(preferredVoice) //
					.build();

			// Select the type of audio file you want returned
			AudioConfig audioConfig = AudioConfig.newBuilder()
					.setAudioEncoding(com.google.cloud.texttospeech.v1.AudioEncoding.LINEAR16)//
					// .setVolumeGainDb(volume) volume is handled by the player
					.setSpeakingRate(speakingRate) // 0.25 - 4
					.setPitch(pitch) // --20 - +20
					.build();

			// Perform the text-to-speech request on the text input with the selected voice
			// parameters and audio file type
			SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, deVoiceMale, audioConfig);

			// Get the audio contents from the response
			ByteString audioContents = response.getAudioContent();

			// play wav
			Utils.playWAV(audioContents, volume);
		} catch (Throwable e) {
			log.warn(e.getMessage(), e);
		}
	}

	public String getGender() {
		return gender.toString().toLowerCase();
	}

	public double getSpeakingRate() {
		return speakingRate;
	}

	public double getPitch() {
		return pitch;
	}

	@Override
	public List<String> listSupportedVoices(String lang) {
		List<String> voiceNames = new ArrayList<>();
		try {
			List<Voice> listAllSupportedVoices = listAllSupportedVoices(lang);
			listAllSupportedVoices.stream().forEach(v -> voiceNames.add(v.getName()));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return voiceNames;
	}

	/**
	 * Returns all voices matching the given language.
	 */
	public List<Voice> listAllSupportedVoices(String lang) throws Exception {
		List<Voice> voicesForLanguage = new ArrayList<>();

		final Builder b = TextToSpeechSettings.newBuilder();
		b.setCredentialsProvider(this);

		try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(b.build())) {
			// Builds the text to speech list voices request

			ListVoicesRequest request = ListVoicesRequest.getDefaultInstance();

			// Performs the list voices request
			ListVoicesResponse response = textToSpeechClient.listVoices(request);
			List<Voice> voices = response.getVoicesList();

			for (Voice voice : voices) {
				// Display the voice's name. Example: tpc-vocoded
				System.out.format("Name:%s Gender:%s Hz:%s\n", voice.getName(), voice.getSsmlGender(),
						voice.getNaturalSampleRateHertz());
				if (lang == null || voice.getName().startsWith(lang)) {
					voicesForLanguage.add(voice);
				}
				// Display the supported language codes for this voice. Example: "en-us"
//				List<ByteString> languageCodes = voice.getLanguageCodesList().asByteStringList();
//				for (ByteString languageCode : languageCodes) {
//					System.out.format("Supported Language: %s\n", languageCode.toStringUtf8());
//				}

			}
		}
		return voicesForLanguage;
	}

	public String translate(String src, String dst, String message) {
		TranslateOptions build;
		try {

			build = TranslateOptions.newBuilder() //
					.setCredentials(getCredentials()) //
					.build();
			final Translate service = build.getService();
			List<Language> languages = service.listSupportedLanguages();
			Language srcLang = null, dstLang = null;
			for (Language language : languages) {
				if (src.equals(language.getCode())) {
					srcLang = language;
				}
				if (dst.equals(language.getCode())) {
					dstLang = language;
				}
			}
			if (srcLang == null) {
				return src + " not supported";
			}
			if (dstLang == null) {
				return dst + " not supported";
			}

			final Translation translated = service.translate(message, TranslateOption.sourceLanguage(src),
					TranslateOption.targetLanguage(dst));
			final String translatedText = StringEscapeUtils.unescapeHtml4(translated.getTranslatedText());

			return translatedText;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		return "";
	}

	public static void main(String[] args) throws IOException {
		GoogleTTSProvider tts = new GoogleTTSProvider();
		Set<String> codes = new TreeSet<>();
		try {

			for (Voice v : tts.listAllSupportedVoices(null)) {
				codes.add(v.getName().substring(0, 6));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (String s : codes) {
			System.out.println(s);
			log.info(s);
		}
	}
}
