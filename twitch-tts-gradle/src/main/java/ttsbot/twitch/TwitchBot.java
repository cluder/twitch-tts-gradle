package ttsbot.twitch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ListenerExceptionEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PingEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ttsbot.tts.GoogleTTS;
import ttsbot.ui.SwingUI;
import ttsbot.util.Settings;
import ttsbot.util.Utils;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;

/**
 * Chat bot, implements PircBotx's {@link ListenerAdapter}.
 */
public class TwitchBot extends ListenerAdapter {
	private final static Logger log = LoggerFactory.getLogger(TwitchBot.class);

	PircBotX pircBot;
	GoogleTTS tts;
	private String channel;
	SwingUI ui;
	private MediaPlayer mediaPlayer = null;

	public TwitchBot() throws UnsupportedEncodingException {

		final Settings settings = Settings.get();
		if (settings == null) {
			log.error("settings null");
		}
		tts = new GoogleTTS();

		channel = settings.get(Settings.CHANNEL);
		final String serverPassword = settings.get(Settings.OAUTH_TOKEN);

		Configuration config = new Configuration.Builder() //
				.setName("TwitchChatBot") //
				.addServer("irc.chat.twitch.tv", 6667) //
				.setServerPassword(serverPassword) //
				.addListener(this) //
				.addAutoJoinChannel("#" + channel) //
				.addCapHandler(new EnableCapHandler("twitch.tv/membership")) // enable join/part
				.addCapHandler(new EnableCapHandler("twitch.tv/tags")) // enable badges
				.addCapHandler(new EnableCapHandler("twitch.tv/commands")) // enable clearchat,usernotice etc.
				.setAutoReconnect(true) //
				.setAutoReconnectDelay(new StaticDelay(2000)) // pircbotx 2.1
//				.setAutoReconnectDelay(2000) // pircbotx 2.2
				.setEncoding(StandardCharsets.UTF_8) //
				.buildConfiguration();

		pircBot = new PircBotX(config);

		try {
			mediaPlayer = new MediaPlayerFactory().mediaPlayers().newMediaPlayer();
		} catch (Throwable e) {
			log.error("could not create media player", e);
		}
	}

	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	@Override
	public void onNotice(NoticeEvent event) throws Exception {
		super.onNotice(event);
	}

	/**
	 * We MUST respond to this or else we will get kicked
	 */
	@Override
	public void onPing(PingEvent event) throws Exception {
		pircBot.sendRaw().rawLineNow(String.format("PONG %s\r\n", event.getPingValue()));
	}

	/**
	 * Called for every message in the chat.<br>
	 * Calls {@link #parseMessage} to check for commands.
	 */
	@Override
	public void onGenericMessage(GenericMessageEvent event) throws Exception {
		String message = event.getMessage();
		String command = getCommandFromMessage(message);

		final User user = event.getUser();
		final String nick = user == null ? "" : user.getLogin();

		parseMessage(nick, false, channel, message, command);

	}

	@Override
	public void onMessage(org.pircbotx.hooks.events.MessageEvent event) throws Exception {
		super.onMessage(event);
	}

	@Override
	public void onUserList(UserListEvent event) throws Exception {
		super.onUserList(event);
	}

	@Override
	public void onListenerException(ListenerExceptionEvent event) throws Exception {

	}

	/**
	 * Helper to send a message to the current channel.
	 */
	protected void sendMsg(String msg) {
		pircBot.sendIRC().message("#" + channel, msg);
	}

	/**
	 * Returns the command, from a message. The command will always be the first
	 * word of the message.
	 */
	private String getCommandFromMessage(String message) {
		String[] msgParts = message.split(" ");
		return msgParts[0];
	}

	/**
	 * Parses the message for commands.
	 */
	private void parseMessage(String username, boolean isMod, String channel, String message, String command) {
		if (isIgnoredUser(username)) {
			return;
		}

		if (command == null) {
			command = "";
		}
		if (message == null) {
			message = "";
		}

		command = lowerTrimmed(command);
		String msgWithoutCommand = getMsgWithoutCommand(message, command);

		// pitch
		if (command.equals("!pitch")) {
			if (msgWithoutCommand.isEmpty()) {
				sendMsg("Current pitch: " + tts.getPitch());
			} else {
				String parsed = msgWithoutCommand;
				int parsedInt = org.pircbotx.Utils.tryParseInt(parsed, GoogleTTS.DEFAULT_PITCH);
				final boolean result = tts.setPitch(parsedInt);
				if (result) {
					sendMsg("pitch set to " + tts.getPitch());
					ui.updatePitch(tts.getPitch());
				} else {
					sendMsg("pitch value has to be between -20 and +20");
				}
			}
		}

		// volume
		if (command.equals("!vol")) {
			if (msgWithoutCommand.isEmpty()) {
				sendMsg("Current volume: " + tts.getVolume());
			} else {

				try {
					double value = GoogleTTS.DEFAULT_VOLUME;
					try {
						value = Double.parseDouble(msgWithoutCommand);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
					final boolean success = tts.setVolume(value);
					if (success) {
						sendMsg("Volume set to " + value + " dB");
						ui.updateVol(value);
					} else {
						sendMsg("Volume has to be between -96 and +16");
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
					sendMsg("Exception:" + e.getStackTrace()[0] + " - " + e.getMessage());
				}
			}
		}

		if (command.equals("!lang")) {
			if (msgWithoutCommand.isEmpty()) {
				sendMsg("Current language: " + tts.getLang());
			} else {
				boolean success = tts.setLang(msgWithoutCommand);
				if (success) {
					sendMsg("Language set to:" + msgWithoutCommand);
					ui.updateLang(tts.getLang());
				} else {
					sendMsg("Language not known, try:" + tts.getKnownLanguages());
				}
			}
		}

		// gender
		if (command.equals("!gender")) {
			if (msgWithoutCommand.isEmpty()) {
				sendMsg("Current gender: " + tts.getGender());
			} else {
				boolean success = tts.setGender(msgWithoutCommand);
				if (success) {
					sendMsg("Gender set to:" + msgWithoutCommand);
					ui.updateGender(tts.getGender());
				} else {
					sendMsg("Gender must be any of: female/male/neutral");
				}
			}
		}

		// speakrate
		if (command.equals("!speakrate")) {
			if (msgWithoutCommand.isEmpty()) {
				sendMsg("Current speakrate: " + tts.getSpeakrate());
			} else {
				double parsedDouble = Double.parseDouble(msgWithoutCommand);
				double newRate = Utils.constrainToRange(parsedDouble, 0.25, 4);
				boolean success = tts.setSpeakingRate(newRate);
				if (success) {
					sendMsg("Speakrate set to:" + tts.getSpeakrate());
					ui.updateSpeakrate(newRate);
				} else {
					sendMsg("Speakrate has to be between -20 and +20");
				}
			}
		}

		if (command.equals("!speak") || command.equals("!s")) {
			try {
				tts.syntesizeAndPlay(msgWithoutCommand);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				sendMsg("Exception " + e.getStackTrace()[0] + " - " + e.getMessage());
			}
		}

		if (command.equals("!tr")) {
			String src = null;
			String dst = null;
			final String[] split = msgWithoutCommand.split(" ");
			if (split.length > 2) {
				if (split[0].length() == 2) {
					src = split[0];
					msgWithoutCommand = msgWithoutCommand.substring(3);
				}
				if (split[1].length() == 2) {
					dst = split[1];
					msgWithoutCommand = msgWithoutCommand.substring(2);
				}
			}
			if (src == null || dst == null) {
				sendMsg("!tr <quelle> <ziel> <text>");
			} else {
				final String translate = tts.translate(src, dst, msgWithoutCommand);
				if (translate != null && translate.isEmpty() == false) {
					sendMsg(translate);
				}
				boolean known = tts.isKnownLanguage(dst);
				if (known) {
					try {
						tts.syntesizeAndPlay(translate, dst, null);
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}

		playMedia(command);
	}

	public void stopMedia() {
		if (mediaPlayer == null) {
			return;
		}
		mediaPlayer.controls().stop();

	}

	private void playMedia(String command) {
		if (ui.isMediaCommandsEnabled() == false) {
			return;
		}
		if (mediaPlayer == null) {
			return;
		}
		if (command.length() < 2) {
			return;
		}
		String media = command.substring(1);
		final String mediaPath = "media/";
		List<String> extensions = Arrays.asList("mp3", "mp4", "wmv", "avi", "mpg", "wav", "ogg", "gif", "png", "jpg",
				"bmp");

		for (String ext : extensions) {
			final String fileNAme = mediaPath + media + "." + ext;
			if (Files.exists(Paths.get(fileNAme))) {
				mediaPlayer.media().play(fileNAme);
				break;
			}
		}
	}

	/**
	 * starts the chat bot and initializes the chatters-list.
	 */
	public void connect() {
		try {
			pircBot.startBot();
		} catch (IOException | IrcException e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * starts the chat bot and initializes the chatters-list.
	 */
	public void disconnect() {
		pircBot.stopBotReconnect();
		pircBot.sendIRC().quitServer("leaving");
	}

	/**
	 * returns the message in lowercases and trimmed.
	 */
	private String lowerTrimmed(String message) {
		if (message == null) {
			return message;
		}
		return message.toLowerCase().trim();
	}

	/**
	 * returns true, if we should ignore this user.<br>
	 * ignored users wont trigger commands.
	 */
	private boolean isIgnoredUser(String username) {
		if (username == null) {
			return false;
		}

		if (username.toLowerCase().equals("nightbot")) {
			return true;
		}

		return false;
	}

	/**
	 * Returns the message without the leading command.
	 */
	private String getMsgWithoutCommand(String message, String cmd) {
		if (cmd == null || cmd.startsWith("!") == false) {
			return message;
		}
		if (message.length() <= cmd.length() + 1) {
			return "";
		}
		return message.substring(cmd.length()).trim();
	}

	public boolean isConnected() {
		if (pircBot == null) {
			return false;
		}
		return pircBot.isConnected();
	}

	public GoogleTTS getTts() {
		return tts;
	}

	public void setUi(SwingUI swingUI) {
		ui = swingUI;
	}

}
