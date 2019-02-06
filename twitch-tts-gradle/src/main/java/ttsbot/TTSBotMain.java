package ttsbot;

import ttsbot.twitch.TwitchBot;
import ttsbot.ui.SwingUI;

public class TTSBotMain {
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
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		TTSBotMain bot = new TTSBotMain();
		bot.start();
	}
}
