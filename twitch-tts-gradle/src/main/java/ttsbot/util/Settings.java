package ttsbot.util;

import java.util.Properties;

public class Settings {

	private static Settings instance = null;

	public static final String CHANNEL_ID = "channel_id";
	public static String CHANNEL = "defaultChannel";
	public static String SERVER = "ircServer";
	public static String PORT = "ircPort";
	public static String BOT_NICK = "bot_nick";
	public static String OAUTH_TOKEN = "oauth_irc";
	public static String OAUTH_TOKEN_EDITOR = "oauth_editor";

	final Properties properties;

	private Settings() throws Exception {
		properties = load();
	}

	public static Settings get() {
		if (instance == null) {
			try {
				instance = new Settings();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	/**
	 * read all settings from properties files.
	 */
	public Properties load() throws Exception {
		Properties properties = new Properties();
		properties.load(ClassLoader.getSystemResourceAsStream("oauth.properties"));
		properties.load(ClassLoader.getSystemResourceAsStream("settings.properties"));
		return properties;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Returns the channel name '#channel name'
	 */
	public String getChannelName() {
		return "#" + properties.getProperty(Settings.CHANNEL);
	}
}
