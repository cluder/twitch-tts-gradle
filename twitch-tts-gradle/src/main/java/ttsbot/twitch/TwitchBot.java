package ttsbot.twitch;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.delay.StaticDelay;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ListenerExceptionEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PingEvent;
import org.pircbotx.hooks.events.UserListEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import ttsbot.tts.GoogleTTS;
import ttsbot.util.Settings;
import ttsbot.util.Utils;

public class TwitchBot extends ListenerAdapter {
	// irc settings
	PircBotX pircBot;
	private String channel;

	GoogleTTS tts;

	Random r;

	public TwitchBot() throws UnsupportedEncodingException {

		r = new Random();
		tts = new GoogleTTS();

		channel = Settings.get().get(Settings.CHANNEL);

		Configuration config = new Configuration.Builder() //
				.setName("TwitchChatBot") //
				.addServer("irc.chat.twitch.tv", 6667) //
				.setServerPassword(Settings.get().get(Settings.OAUTH_TOKEN)) //
				.addListener(this) //
				.addAutoJoinChannel("#" + channel) //
				.addCapHandler(new EnableCapHandler("twitch.tv/membership")) // enable join/part
				.addCapHandler(new EnableCapHandler("twitch.tv/tags")) // enable badges
				.addCapHandler(new EnableCapHandler("twitch.tv/commands")) // enable clearchat,usernotice etc.
				.setAutoReconnect(true) //
				.setAutoReconnectDelay(new StaticDelay(2000)) //
				.setEncoding(StandardCharsets.UTF_8) //
				.buildConfiguration();

		pircBot = new PircBotX(config);

	}

	/**
	 * starts the chat bot and initializes the chatters-list.
	 */
	public void start() throws Exception {
		pircBot.startBot();
	}

	/**
	 * The command will always be the first word of the message.
	 */
	private String getCommandFromMessage(String message) {
		String[] msgParts = message.split(" ");
		return msgParts[0];
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
	 * PircBotx will return the exact message sent and not the raw line
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

	protected void sendMsg(String msg) {
		pircBot.sendIRC().message("#" + channel, msg);
	}

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

		if (command.equals("!pitch")) {
			String parsed = msgWithoutCommand.trim();
			int parseInt = org.pircbotx.Utils.tryParseInt(parsed, GoogleTTS.DEFAULT_PITCH);
			final boolean result = tts.setPitch(parseInt);
			if (!result) {
				sendMsg("pitch value has to be between -20 and +20");
			}
		}

		if (command.equals("!volume")) {
			try {
				String parsed = msgWithoutCommand.trim();
				int parseInt = org.pircbotx.Utils.tryParseInt(parsed, GoogleTTS.DEFAULT_VOLUME);
				int newVolume = Utils.constrainToRange(parseInt, 0, 400);
				tts.setVolume(newVolume);

				sendMsg("Volume set to " + newVolume + "%");
			} catch (Exception e) {
				e.printStackTrace();
				sendMsg("Exception:" + e.getStackTrace()[0] + " - " + e.getMessage());
			}
		}

		if (command.equals("!speak") || command.equals("!s")) {
			// tts
			try {
				tts.syntesizeAndPlay(msgWithoutCommand);
			} catch (Exception e) {
				e.printStackTrace();
				sendMsg("Exception " + e.getStackTrace()[0] + " - " + e.getMessage());
			}
		}

		if (command.equals("!lang")) {
			boolean success = tts.setLang(msgWithoutCommand);
			if (success) {
				sendMsg("Language set to:" + msgWithoutCommand);
			} else {
				sendMsg("Language not known, try:" + tts.getKnownLanguages());
			}
		}

		if (command.equals("!gender")) {
			boolean success = tts.setGender(msgWithoutCommand);
			if (success) {
				sendMsg("Gender set to:" + msgWithoutCommand);
			}
		}

		if (command.equals("!speakrate")) {
			double parsedDouble = Double.parseDouble(msgWithoutCommand);
			double newRate = Utils.constrainToRange(parsedDouble, 0.25, 4);
			boolean success = tts.setSpeakingRate(newRate);
			if (success) {
				sendMsg("Speak rate set to:" + msgWithoutCommand);
			}
		}
	}

	private boolean isIgnoredUser(String username) {
		if (username == null) {
			return false;
		}

		if (username.toLowerCase().equals("nightbot")) {
			return true;
		}

		return false;
	}

	@Override
	public void onUserList(UserListEvent event) throws Exception {
		super.onUserList(event);
	}

	@Override
	public void onListenerException(ListenerExceptionEvent event) throws Exception {

	}

	private String lowerTrimmed(String message) {
		if (message == null) {
			return message;
		}
		return message.toLowerCase().trim();
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

}
