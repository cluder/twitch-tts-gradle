package ttsbot;

import ttsbot.twitch.TwitchBot;
import ttsbot.ui.SwingUI;

public class TTSBotMain {
	SwingUI ui;

	public TTSBotMain() {
	}

	public void start() {
		try {
//			ui = new SwingUI();
//			ui.setVisible(true);

			TwitchBot bot;
			bot = new TwitchBot();
			bot.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		TTSBotMain bot = new TTSBotMain();
		bot.start();
	}
}
