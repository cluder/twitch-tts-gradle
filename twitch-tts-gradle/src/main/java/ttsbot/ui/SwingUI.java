package ttsbot.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ttsbot.tts.GoogleTTS;
import ttsbot.twitch.TwitchBot;
import ttsbot.util.Settings;

/**
 * Swing UI.<br>
 */
public class SwingUI extends JFrame {
	private final static Logger log = LoggerFactory.getLogger(SwingUI.class);

	TwitchBot bot;

	Thread pircBotThread;

	private static final String NOT_CONNECTED = "not connected";
	private static final String CONNECTED = "connected";
	private static final long serialVersionUID = 1L;
	private JTextField textFieldChannel;
	private JTextField textFieldStatus;

	private JButton btnConnect;
	private JSpinner spinnerVolume;
	private JSpinner spinnerPitch;
	private JSpinner spinnerSpeakrate;
	private JComboBox<String> comboBoxGender;
	private JComboBox<String> comboBoxLanguage;

	/**
	 * Set default values.
	 */
	public void init() {
		final String channel = Settings.get().get(Settings.CHANNEL);
		textFieldChannel.setText(channel);

		textFieldStatus.setEnabled(false);
		setConnectionState();

		spinnerPitch.setValue(GoogleTTS.DEFAULT_PITCH);
		spinnerVolume.setValue(GoogleTTS.DEFAULT_VOLUME);
		spinnerSpeakrate.setValue(1);

		for (int i = 0; i < comboBoxLanguage.getItemCount(); i++) {
			final String itemAt = comboBoxLanguage.getItemAt(i);
			final String botLang = bot.getTts().getLang();
			if (itemAt.equals(botLang)) {
				comboBoxLanguage.setSelectedItem(itemAt);
			}
		}

	}

	private void setConnectionState() {
		if (bot != null) {
			if (bot.isConnected()) {
				textFieldStatus.setText(CONNECTED);
				btnConnect.setEnabled(false);
			} else {
				textFieldStatus.setText(NOT_CONNECTED);
				btnConnect.setEnabled(true);
			}
		}
	}

	/**
	 * Create the frame.
	 */
	public SwingUI(TwitchBot bot) {
		this.bot = bot;

		pircBotThread = new Thread(new Runnable() {
			@Override
			public void run() {
				bot.connect();
			}
		}, "PircBotX Thread");

		setTitle("Twitch TTS Bot");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 454, 510);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);

		panel.setLayout(null);

		JLabel lblChannel = new JLabel("Channel");
		lblChannel.setBounds(10, 41, 46, 14);
		panel.add(lblChannel);

		textFieldChannel = new JTextField();
		textFieldChannel.setBounds(62, 38, 138, 20);
		panel.add(textFieldChannel);
		textFieldChannel.setColumns(10);

		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bot == null) {
					return;
				}
				pircBotThread.start();

				for (int i = 0; i < 10; i++) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
					}
					if (bot.isConnected()) {
						break;
					}
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						setConnectionState();
					}
				});
			}
		});
		btnConnect.setBounds(210, 37, 89, 23);
		panel.add(btnConnect);

		JLabel lblStatus = new JLabel("Status:");
		lblStatus.setBounds(10, 67, 46, 14);
		panel.add(lblStatus);

		textFieldStatus = new JTextField();
		textFieldStatus.setBounds(62, 64, 138, 20);
		panel.add(textFieldStatus);
		textFieldStatus.setColumns(10);

		JButton btnRefresh = new JButton("Refresh");
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setConnectionState();
			}
		});
		btnRefresh.setBounds(210, 63, 89, 23);
		panel.add(btnRefresh);

		panel.setLayout(null);

		JTextArea textAreaTTSInput = new JTextArea();
		textAreaTTSInput.setText("Hallo Welt");
		textAreaTTSInput.setBounds(10, 134, 418, 91);
		panel.add(textAreaTTSInput);

		JLabel lblTtsinput = new JLabel("TTS Input");
		lblTtsinput.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblTtsinput.setBounds(10, 109, 123, 14);
		panel.add(lblTtsinput);

		JButton btnSpeak = new JButton("Speak");
		btnSpeak.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					final String text = textAreaTTSInput.getText();
					if (text != null && text.isEmpty() == false) {
						bot.getTts().syntesizeAndPlay(text);
					}
				} catch (IOException e1) {
					log.error(e1.getMessage(), e);
				}
			}
		});
		btnSpeak.setBounds(10, 236, 89, 23);
		panel.add(btnSpeak);

		comboBoxLanguage = new JComboBox<>();
		comboBoxLanguage.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					bot.getTts().setLang((String) e.getItem());
				}
			}
		});
		comboBoxLanguage.setBounds(161, 304, 126, 20);
		for (String s : bot.getTts().getKnownLanguages()) {
			comboBoxLanguage.addItem(s);
		}
		panel.add(comboBoxLanguage);

		JLabel lblGender = new JLabel("Gender");
		lblGender.setBounds(10, 332, 98, 14);
		panel.add(lblGender);

		comboBoxGender = new JComboBox<>();
		comboBoxGender.addItem("Male");
		comboBoxGender.addItem("Female");
		comboBoxGender.addItem("Neutral");
		comboBoxGender.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					bot.getTts().setGender((String) e.getItem());
				}
			}
		});
		comboBoxGender.setBounds(161, 329, 123, 20);
		panel.add(comboBoxGender);

		JSeparator separator = new JSeparator();
		separator.setBounds(10, 97, 418, 14);
		panel.add(separator);

		JLabel lblSpeakrate = new JLabel("Speakrate (0.25, 4.0)");
		lblSpeakrate.setBounds(10, 360, 123, 14);
		panel.add(lblSpeakrate);

		spinnerSpeakrate = new JSpinner(new SpinnerNumberModel(1, 0.25, 4, 0.01));
		spinnerSpeakrate.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				final Number value = (Number) spinnerSpeakrate.getValue();
				bot.getTts().setSpeakingRate(value.doubleValue());

			}
		});
		spinnerSpeakrate.setBounds(161, 357, 64, 20);
		panel.add(spinnerSpeakrate);

		spinnerPitch = new JSpinner(new SpinnerNumberModel(1, -20, 20, 1));
		spinnerPitch.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				final Integer value = (Integer) spinnerPitch.getValue();
				bot.getTts().setPitch(value);
			}
		});

		spinnerPitch.setBounds(161, 385, 64, 20);
		panel.add(spinnerPitch);

		JLabel lblPitch = new JLabel("Pitch (-20, +20)");
		lblPitch.setBounds(10, 388, 108, 14);
		panel.add(lblPitch);

		JLabel lblVolume = new JLabel("Volume dB (-96, +16)");
		lblVolume.setBounds(10, 415, 123, 14);
		panel.add(lblVolume);

		spinnerVolume = new JSpinner(new SpinnerNumberModel(0, -96, 16, 0.25));
		spinnerVolume.setBounds(161, 412, 64, 20);
		spinnerVolume.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {

				final Number value = (Number) spinnerVolume.getValue();
				bot.getTts().setVolume(value.doubleValue());
			}
		});
		panel.add(spinnerVolume);

		JLabel labelTwitchSettings = new JLabel("Twitch Settings");
		labelTwitchSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
		labelTwitchSettings.setBounds(10, 11, 123, 14);
		panel.add(labelTwitchSettings);

		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 270, 418, 14);
		panel.add(separator_1);

		JLabel lblTtsSettings = new JLabel("TTS Settings");
		lblTtsSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblTtsSettings.setBounds(10, 282, 123, 14);
		panel.add(lblTtsSettings);

		JLabel lblLanguage = new JLabel("Language");
		lblLanguage.setBounds(10, 307, 89, 14);
		panel.add(lblLanguage);

		JLabel lblCommands = new JLabel("Chat Commands");
		lblCommands.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblCommands.setBounds(324, 282, 117, 14);
		panel.add(lblCommands);

		JLabel lblCmdLang = new JLabel("!lang");
		lblCmdLang.setBounds(324, 307, 77, 14);
		panel.add(lblCmdLang);

		JLabel lblgender = new JLabel("!gender");
		lblgender.setBounds(324, 332, 77, 14);
		panel.add(lblgender);

		JLabel lblCmdSpeakrate = new JLabel("!speakrate");
		lblCmdSpeakrate.setBounds(324, 360, 77, 14);
		panel.add(lblCmdSpeakrate);

		JLabel lblCmdPitch = new JLabel("!pitch");
		lblCmdPitch.setBounds(324, 388, 77, 14);
		panel.add(lblCmdPitch);

		JLabel lblCmdVolume = new JLabel("!vol");
		lblCmdVolume.setBounds(324, 415, 77, 14);
		panel.add(lblCmdVolume);

		JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(302, 290, -10, 159);
		panel.add(separator_3);

		JSeparator separator_2 = new JSeparator();
		separator_2.setOrientation(SwingConstants.VERTICAL);
		separator_2.setBounds(305, 279, 9, 161);
		panel.add(separator_2);

		init();
	}
}
