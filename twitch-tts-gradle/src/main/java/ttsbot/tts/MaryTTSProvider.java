package ttsbot.tts;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.common.collect.Lists;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import ttsbot.util.Utils;

public class MaryTTSProvider implements TTSProvider {
	private final static Logger log = LoggerFactory.getLogger(MaryTTSProvider.class);

	public static final String DEFAULT_LANG = "de";
	public static final String DEFAULT_VOICE = "bits1-hsmm";

	private String lang = DEFAULT_LANG;
	private String preferredVoice = "";

	protected List<String> knownLanguages = Lists.newArrayList();
	protected Set<TTSFeature> knownFeatures = EnumSet.noneOf(TTSFeature.class);

	MaryInterface marytts;

	// TTS settings
	private float volume = DEFAULT_VOLUME;

	public MaryTTSProvider() throws MaryConfigurationException {
		super();
		marytts = new LocalMaryInterface();
	}

	@Override
	public String getName() {
		return "mary";
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
	public void setDefault() {
		lang = DEFAULT_LANG;
		preferredVoice = DEFAULT_VOICE;
	}

	@Override
	public Collection<String> listSupportedVoices(String lang) {
		Set<String> availableVoices = marytts.getAvailableVoices(Locale.forLanguageTag(lang));
		return availableVoices;
	}

	@Override
	public void setVoice(String value) {
		preferredVoice = value;
	}

	@Override
	public String getVoice() {
		return preferredVoice;
	}

	public List<String> getKnownLanguages() {
		if (knownLanguages.isEmpty()) {
			knownLanguages = marytts.getAvailableLocales().stream().map(l -> l.toLanguageTag())
					.collect(Collectors.toList());
		}
		return knownLanguages;
	}

	public boolean isKnownLanguage(String dst) {
		for (String lang : getKnownLanguages()) {
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
	public boolean isSupported(TTSFeature f) {
		return knownFeatures.contains(f);
	}

	@Override
	public float getVolume() {
		return volume;
	}

	@Override
	public boolean setVolume(float value) {
		volume = value;
		return true;
	}

	@Override
	public String getLang() {
		return lang;
	}

	@Override
	public boolean setLang(String value) {
		// TODO needs more checks
		lang = value;
		return true;
	}

	@Override
	public boolean setGender(String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getGender() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void syntesizeAndPlay(String value) throws Exception {
		syntesizeAndPlay(value, null, null);
	}

	@Override
	public void syntesizeAndPlay(String text, String langOverride, SsmlVoiceGender genderOverride) throws Exception {
		marytts.setLocale(Locale.forLanguageTag(lang));
		marytts.setVoice(preferredVoice);

		AudioInputStream audio = marytts.generateAudio(text);

		Utils.playAudio(audio, volume);
	}

	@Override
	public String translate(String src, String dst, String txt) {
		// TODO Auto-generated method stub
		return "not supported yet";
	}

}
