package ttsbot.tts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.translate.AmazonTranslate;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;
import com.amazonaws.services.translate.model.TranslateTextResult;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class AmazonPollyTTSProvider implements TTSProvider, AWSCredentialsProvider {
	private final static Logger log = LoggerFactory.getLogger(AmazonPollyTTSProvider.class);

	protected Set<TTSFeature> knownFeatures = EnumSet.noneOf(TTSFeature.class);

	public static final String DEFAULT_LANG = "de-DE";
	public static final String DEFAULT_VOICE = "Marlene";
	final String REGION = "eu-west-1";

	// TTS settings
	private float volume = DEFAULT_VOLUME;
	private String lang = DEFAULT_LANG;
	private String preferredVoice = "";

	private Collection<String> knownLanguagesCached = null;

	AmazonPolly client;
	AWSCredentials awsCredentials;

	public AmazonPollyTTSProvider() {
		if (getCredentialFile() == null) {
			throw new RuntimeException("credentials not found");
		}
		AmazonPollyClientBuilder clientBuilder = AmazonPollyClientBuilder.standard();
		clientBuilder.setCredentials(this);
		clientBuilder.setRegion("eu-west-1");
		client = clientBuilder.build();
	}

	@Override
	public String getName() {
		return "amazon";
	}

	@Override
	public boolean isAvailable() {
		Collection<String> listSupportedVoices = listSupportedVoices(DEFAULT_LANG);
		if (listSupportedVoices.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public AWSCredentials getCredentials() {
		final Path path = getCredentialFile();

		try {
			awsCredentials = new PropertiesCredentials(path.toFile());
		} catch (IllegalArgumentException | IOException e) {
			log.error("error loading credentials", e);
		}
		return awsCredentials;
	}

	private Path getCredentialFile() {
		final String fileName = "aws-credentials.properties";
		final Path path = Paths.get(fileName);
		if (path.toFile().exists()) {
			return path;
		}
		return null;
	}

	/**
	 * AWS credentials refresh
	 */
	@Override
	public void refresh() {

	}

	@Override
	public void setDefault() {
		lang = DEFAULT_LANG;
		preferredVoice = DEFAULT_VOICE;
	}

	@Override
	public boolean isSupported(TTSFeature f) {
		return knownFeatures.contains(f);
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public boolean setVolume(float value) {
		this.volume = value;
		return true;
	}

	@Override
	public String getLang() {
		return this.lang;
	}

	@Override
	public boolean setLang(String value) {
		if (isKnownLanguage(value)) {
			this.lang = value;
			return true;
		}
		return false;
	}

	@Override
	public Collection<String> getKnownLanguages() {
		if (knownLanguagesCached == null) {
			// fetch known languages
			Set<String> languages = new TreeSet<>();
			DescribeVoicesRequest voicesRequest = new DescribeVoicesRequest();
			String nextToken;
			do {
				DescribeVoicesResult voicesResult = client.describeVoices(voicesRequest);
				nextToken = voicesResult.getNextToken();
				voicesRequest.setNextToken(nextToken);
				voicesResult.getVoices().stream().forEach(v -> languages.add(v.getLanguageCode()));
			} while (nextToken != null);
			knownLanguagesCached = languages;
		}
		return knownLanguagesCached;
	}

	@Override
	public boolean setGender(String value) {
		return false;
	}

	@Override
	public String getGender() {
		return null;
	}

	@Override
	public void syntesizeAndPlay(String value) throws Exception {
		syntesizeAndPlay(value, null, null);
	}

	@Override
	public void syntesizeAndPlay(String text, String langOverride, SsmlVoiceGender genderOverride) throws Exception {

		SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
				.withOutputFormat(OutputFormat.Mp3) //
				.withVoiceId(preferredVoice) //
				.withLanguageCode(lang) //
				.withText(text);

		File tmpFile = File.createTempFile("polly", "speech");
		try (FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
			SynthesizeSpeechResult synthesizeSpeechResult = client.synthesizeSpeech(synthesizeSpeechRequest);
			byte[] buffer = new byte[2 * 1024];
			int readBytes;

			try (InputStream in = synthesizeSpeechResult.getAudioStream()) {
				while ((readBytes = in.read(buffer)) > 0) {
					outputStream.write(buffer, 0, readBytes);
				}
			}
		} catch (Exception e) {
			log.error("Exception caught: " + e);
		}

		MediaPlayer mediaPlayer = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
		Thread.sleep(50);
		int volPercent = calcVolume(getVolume());

		mediaPlayer.audio().setVolume(volPercent);
		mediaPlayer.media().play(tmpFile.getAbsolutePath());
	}

	private int calcVolume(float volume) {
		if (volume > 0) {
			// 0 - 6 = 100 - 200
			float volChange = 100.0f / 6 * volume;
			return (int) (100 + volChange);
		}
		if (volume < 0) {
			// -70 - 0 = 0- 100
			float volChange = 100.0f / 70 * volume;
			return (int) (100 + volChange);
		}
		return 100;
	}

	@Override
	public String translate(String src, String dst, String txt) {
		try {
			AmazonTranslate translate = AmazonTranslateClient.builder().withCredentials(this).withRegion(REGION)
					.build();

			TranslateTextRequest request = new TranslateTextRequest()//
					.withText(txt).withSourceLanguageCode(src)//
					.withTargetLanguageCode(dst);
			TranslateTextResult result = translate.translateText(request);
			return result.getTranslatedText();
		} catch (Exception e) {
			log.error("error during translate", e);
		}
		return "";
	}

	@Override
	public boolean isKnownLanguage(String value) {
		for (String lang : knownLanguagesCached) {
			if (lang.equals(value)) {
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
	public Collection<String> listSupportedVoices(String lang) {
		List<String> voices = new ArrayList<>();

		DescribeVoicesRequest voicesRequest = new DescribeVoicesRequest().withLanguageCode(lang);
		String nextToken;
		do {
			DescribeVoicesResult voicesResult = client.describeVoices(voicesRequest);
			nextToken = voicesResult.getNextToken();
			voicesRequest.setNextToken(nextToken);
			voicesResult.getVoices().stream().forEach(v -> voices.add(v.getName()));
		} while (nextToken != null);

		return voices;
	}

	@Override
	public void setVoice(String value) {
		preferredVoice = value;
	}

	@Override
	public String getVoice() {
		return preferredVoice;
	}

	/**
	 * Test
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		AmazonPollyClientBuilder clientBuilder = AmazonPollyClientBuilder.standard();
		clientBuilder.setRegion("eu-west-1");
		AmazonPolly client = clientBuilder.build();

//		AmazonPollyTTSProvider provider = new AmazonPollyTTSProvider();
//
//		Collection<String> voices = provider.listSupportedVoices("de-DE");
//		for (String v : voices) {
//			System.out.println(v);
//		}

//
//		SynthesizeSpeechRequest synthesizeSpeechRequest = new SynthesizeSpeechRequest()
//				.withOutputFormat(OutputFormat.Mp3) //
//				.withVoiceId(VoiceId.Brian) //
//				.withText("Hallo mein Name ist Polly");
//
//		File tmpFile = File.createTempFile("polly", "speech");
//		try (FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
//			SynthesizeSpeechResult synthesizeSpeechResult = client.synthesizeSpeech(synthesizeSpeechRequest);
//			byte[] buffer = new byte[2 * 1024];
//			int readBytes;
//
//			try (InputStream in = synthesizeSpeechResult.getAudioStream()) {
//				while ((readBytes = in.read(buffer)) > 0) {
//					outputStream.write(buffer, 0, readBytes);
//				}
//			}
//		} catch (Exception e) {
//			System.err.println("Exception caught: " + e);
//		}
//
//		MediaPlayer mediaPlayer = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
//		mediaPlayer.media().play(tmpFile.getAbsolutePath());
//
//		Thread.sleep(10000);
//		tmpFile.delete();

	}
}
