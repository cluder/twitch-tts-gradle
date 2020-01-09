package ttsbot.tts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

public class AmazonPollyTTSProvider implements TTSProvider {
	protected Set<TTSFeature> knownFeatures = EnumSet.allOf(TTSFeature.class);
	public static final String DEFAULT_LANG = "de-DE";
	public static final String DEFAULT_VOICE = "Marlene";

	// TTS settings
	private double speakingRate = 0;
	private double pitch = DEFAULT_PITCH;
	private float volume = DEFAULT_VOLUME;
	private String lang = DEFAULT_LANG;
	private String gender = "neutral";
	private String preferredVoice = "";

	protected Collection<String> knownLanguages = null;

	AmazonPolly client;

	public AmazonPollyTTSProvider() {
		AmazonPollyClientBuilder clientBuilder = AmazonPollyClientBuilder.standard();
		clientBuilder.setRegion("eu-west-1");
		client = clientBuilder.build();
	}

	@Override
	public String getName() {
		return "Amazon Polly";
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
	public double getPitch() {
		return pitch;
	}

	@Override
	public boolean setPitch(int value) {
		// TODO checks
		this.pitch = value;
		return true;
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public boolean setVolume(float value) {
		// TODO checks
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
		if (knownLanguages == null) {
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
			knownLanguages = languages;
		}
		return knownLanguages;
	}

	@Override
	public boolean setGender(String value) {
		this.gender = value;
		return true;
	}

	@Override
	public String getGender() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getSpeakingRate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean setSpeakingRate(double value) {
		// TODO Auto-generated method stub
		return false;
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
			System.err.println("Exception caught: " + e);
		}

		MediaPlayer mediaPlayer = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
		mediaPlayer.media().play(tmpFile.getAbsolutePath());

	}

	@Override
	public String translate(String src, String dst, String txt) {
		return null;
	}

	@Override
	public boolean isKnownLanguage(String value) {
		for (String lang : knownLanguages) {
			if (lang.startsWith(value)) {
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
