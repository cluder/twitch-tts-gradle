package ttsbot.tts;

import java.util.Collection;

import com.google.cloud.texttospeech.v1.SsmlVoiceGender;

public interface TTSProvider {
	public enum TTSFeature {
		GENDER;
	}

	public static final int DEFAULT_VOLUME = 0;
	public static final int DEFAULT_PITCH = 0;

	public String getName();

	/**
	 * @return true if the service is available.
	 */
	public boolean isAvailable();

	/**
	 * Sets sane default values for voice and language
	 */
	public void setDefault();

	boolean isSupported(TTSFeature f);

	public float getVolume();

	/**
	 * Sets the volume gain in decibel.
	 * 
	 * @param value -80 / +6
	 * @return
	 */
	public boolean setVolume(float value);

	public String getLang();

	public boolean setLang(String value);

	public Collection<String> getKnownLanguages();

	public boolean setGender(String value);

	public String getGender();

	public void syntesizeAndPlay(String value) throws Exception;

	public void syntesizeAndPlay(String text, String langOverride, SsmlVoiceGender genderOverride) throws Exception;

	public String translate(String src, String dst, String txt);

	public boolean isKnownLanguage(String lang);

	public boolean isKnownVoice(String lang);

	public Collection<String> listSupportedVoices(String lang);

	public String getVoice();

	public void setVoice(String item);
}
