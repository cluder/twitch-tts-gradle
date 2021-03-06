package ttsbot;

import java.nio.file.Paths;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ttsbot.twitch.TwitchBot;
import ttsbot.ui.SwingUI;

/**
 * Main class which starts the twitch bot and UI.
 */
public class TTSBotMain {
	private final static Logger log = LoggerFactory.getLogger(TTSBotMain.class);
	public static String VERSION = "3";

	SwingUI ui;

	public TTSBotMain() {
	}

	public void start() {
		try {
			TwitchBot bot;
			bot = new TwitchBot();

			ui = new SwingUI(bot);
			ui.setVisible(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public static void main(String[] args) {
		startupCheck();

		TTSBotMain bot = new TTSBotMain();
		bot.start();
	}

	/**
	 * Check if we have java 8+ 64bit.
	 */
	private static void startupCheck() {
		final String os_arch = System.getProperty("os.arch");
		final String version = System.getProperty("java.specification.version");
		final String java_version = version;
		log.info("os.arch:" + os_arch + " java version:" + java_version);

		boolean javaVersionAtLeast8 = false;
		if (!version.contains(".") && Integer.valueOf(version) >= 8) {
			javaVersionAtLeast8 = true;
		} else {
			javaVersionAtLeast8 = SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8);
		}
		log.info("java version 8+: " + javaVersionAtLeast8);

		if (os_arch.contains("64") == false && javaVersionAtLeast8) {
			JOptionPane.showMessageDialog(null, "A 64-bit Java 8 or higher installation is needed",
					"64 Bit Java needed", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		if (!Paths.get("oauth.properties").toFile().exists()) {
			JOptionPane.showMessageDialog(null, "oauth.properties not found", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
}
