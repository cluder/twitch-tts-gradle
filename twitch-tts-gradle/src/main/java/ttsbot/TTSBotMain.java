package ttsbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ttsbot.twitch.TwitchBot;
import ttsbot.ui.SwingUI;

/**
 * Main class which starts the twitch bot and UI.
 */
public class TTSBotMain {
	private final static Logger log = LoggerFactory.getLogger(TTSBotMain.class);

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
		TTSBotMain bot = new TTSBotMain();
		bot.start();
	}
}
