package ttsbot.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import ttsbot.TTSBotMain;
import ttsbot.tts.TTSProvider;
import ttsbot.tts.TTSProvider.TTSFeature;
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
	private JTextArea textAreaTTSInput;

	private JButton btnConnect;
	private JButton btnDisconnect;
	private JSpinner spinnerVolume;
	private JComboBox<String> comboBoxTTSProvider;
	private JComboBox<String> comboBoxLanguage;
	private JComboBox<String> comboBoxVoice;
	private JComboBox<String> comboBoxGender;
	private JLabel txtVlcInfo;
	private JCheckBox chckbxMediaCommands;

	/**
	 * Set default values.
	 */
	public void init() {
		final String channel = Settings.get().get(Settings.CHANNEL);
		textFieldChannel.setText(channel);

		textFieldStatus.setEnabled(false);
		setConnectionState();
		spinnerVolume.setValue(0);

		for (int i = 0; i < comboBoxLanguage.getItemCount(); i++) {
			final String itemAt = comboBoxLanguage.getItemAt(i);
			final String botLang = bot.getTts().getLang();
			if (itemAt.equals(botLang)) {
				comboBoxLanguage.setSelectedItem(itemAt);
			}
		}

		// vlc
		if (bot.getMediaPlayer() == null) {
			txtVlcInfo.setOpaque(true);
			txtVlcInfo.setText("VLC not available - media palyback disabled");
			txtVlcInfo.setBackground(Color.red);
			chckbxMediaCommands.setEnabled(false);
		} else {
			txtVlcInfo.setText("VLC found - media palyback enabled");
			chckbxMediaCommands.setEnabled(true);
		}
		enableFeatureCombos(bot.getTts());
	}

	public boolean isMediaCommandsEnabled() {
		return chckbxMediaCommands.isSelected();
	}

	private void setConnectionState() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (bot != null) {
					if (bot.isConnected()) {
						textFieldStatus.setText(CONNECTED);
						btnConnect.setEnabled(false);
						btnDisconnect.setEnabled(true);
					} else {
						textFieldStatus.setText(NOT_CONNECTED);
						btnConnect.setEnabled(true);
						btnDisconnect.setEnabled(false);
					}
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public SwingUI(TwitchBot bot) {
		this.bot = bot;
		bot.setUi(this);

		setTitle("Twitch TTS Bot");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 454, 631);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		{
			JMenu mnuFile = new JMenu("File");
			menuBar.add(mnuFile);

			JMenuItem mntmExit = new JMenuItem("Exit");
			mntmExit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
			mnuFile.add(mntmExit);
		}

		menuBar.add(Box.createHorizontalGlue());
		menuBar.add(new JLabel("v " + TTSBotMain.VERSION));
		menuBar.add(Box.createHorizontalStrut(10));

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
		textFieldChannel.setEditable(false);

		// connect
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				pircBotThread = createPircXThread(bot);
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
				setConnectionState();
			}
		});
		btnConnect.setBounds(210, 37, 89, 23);
		panel.add(btnConnect);

		// disconnect
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bot.disconnect();

				for (int i = 0; i < 20; i++) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
					if (!bot.isConnected()) {
						break;
					}
				}
				pircBotThread.interrupt();
				setConnectionState();
			}
		});
		btnDisconnect.setBounds(305, 37, 100, 23);
		panel.add(btnDisconnect);

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

		textAreaTTSInput = new JTextArea();
		textAreaTTSInput.setText("Hallo Welt");
		textAreaTTSInput.setBounds(10, 134, 418, 54);
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
				} catch (Exception e1) {
					log.error(e1.getMessage(), e);
				}
			}
		});
		btnSpeak.setBounds(10, 199, 89, 23);
		panel.add(btnSpeak);

		comboBoxTTSProvider = new JComboBox<String>();
		comboBoxLanguage = new JComboBox<>();
		comboBoxVoice = new JComboBox<String>();
		comboBoxGender = new JComboBox<>();

		comboBoxTTSProvider.setBounds(141, 280, 144, 20);
		for (TTSProvider p : bot.getTtsProviders()) {
			comboBoxTTSProvider.addItem(p.getName());
		}
		comboBoxTTSProvider.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					// change TTS provider
					String ttsProviderName = (String) e.getItem();
					onTTSProviderChanged(bot, ttsProviderName);
				}
			}

		});
		panel.add(comboBoxTTSProvider);

		comboBoxLanguage.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					bot.getTts().setLang((String) e.getItem());
					initVoiceComboBox(bot);
				}
			}
		});
		comboBoxLanguage.setBounds(141, 309, 144, 20);
		initLanguageComboBox(bot);
		panel.add(comboBoxLanguage);

		comboBoxVoice.setBounds(141, 340, 144, 20);
		comboBoxVoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					bot.getTts().setVoice((String) e.getItem());
				}
			}
		});
		initVoiceComboBox(bot);
		panel.add(comboBoxVoice);

		JLabel lblGender = new JLabel("Gender");
		lblGender.setBounds(10, 371, 98, 14);
		panel.add(lblGender);

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
		comboBoxGender.setBounds(141, 368, 144, 20);
		panel.add(comboBoxGender);

		JSeparator separator = new JSeparator();
		separator.setBounds(10, 97, 418, 14);
		panel.add(separator);

		JLabel lblVolume = new JLabel("Volume dB (-80, +6)");
		lblVolume.setBounds(10, 403, 123, 14);
		panel.add(lblVolume);

		// values from -80 , +6 are allowed
		spinnerVolume = new JSpinner(new SpinnerNumberModel(0, -80, 6, 0.25));
		spinnerVolume.setBounds(141, 399, 64, 20);
		spinnerVolume.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				setTTSVolume(bot);
			}

		});
		panel.add(spinnerVolume);

		JLabel labelTwitchSettings = new JLabel("Twitch Settings");
		labelTwitchSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
		labelTwitchSettings.setBounds(10, 11, 105, 14);
		panel.add(labelTwitchSettings);

		JLabel labelTwitchSettingsHint = new JLabel(" (see settings.properties)");
		labelTwitchSettingsHint.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelTwitchSettingsHint.setBounds(113, 11, 150, 14);
		panel.add(labelTwitchSettingsHint);

		JSeparator separator_1 = new JSeparator();
		separator_1.setBounds(10, 233, 418, 14);
		panel.add(separator_1);

		JLabel lblTtsSettings = new JLabel("TTS Settings");
		lblTtsSettings.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblTtsSettings.setBounds(10, 247, 123, 14);
		panel.add(lblTtsSettings);

		JLabel lblLanguage = new JLabel("Language");
		lblLanguage.setBounds(10, 312, 89, 14);
		panel.add(lblLanguage);

		JLabel lblCommands = new JLabel("Chat Commands");
		lblCommands.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblCommands.setBounds(324, 247, 104, 14);
		panel.add(lblCommands);

		JLabel lblCmdLang = new JLabel("!lang");
		lblCmdLang.setBounds(324, 302, 77, 14);
		panel.add(lblCmdLang);

		JLabel lblgender = new JLabel("!gender");
		lblgender.setBounds(324, 327, 77, 14);
		panel.add(lblgender);

		JLabel lblCmdVolume = new JLabel("!vol");
		lblCmdVolume.setBounds(324, 350, 77, 14);
		panel.add(lblCmdVolume);

		JSeparator separator_3 = new JSeparator();
		separator_3.setBounds(302, 290, -10, 159);
		panel.add(separator_3);

		JSeparator separator_2 = new JSeparator();
		separator_2.setOrientation(SwingConstants.VERTICAL);
		separator_2.setBounds(305, 233, 9, 241);
		panel.add(separator_2);

		JSeparator separator_4 = new JSeparator();
		separator_4.setBounds(10, 473, 418, 14);
		panel.add(separator_4);

		JLabel lblMedia = new JLabel("Media Settings");
		lblMedia.setFont(new Font("Tahoma", Font.BOLD, 11));
		lblMedia.setBounds(10, 483, 123, 14);
		panel.add(lblMedia);

		txtVlcInfo = new JLabel();
		txtVlcInfo.setEnabled(false);
		txtVlcInfo.setBounds(10, 533, 315, 20);
		panel.add(txtVlcInfo);

		chckbxMediaCommands = new JCheckBox("Media Commands enabled");
		chckbxMediaCommands.setSelected(false);
		chckbxMediaCommands.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (bot.getMediaPlayer() == null) {
					txtVlcInfo.setBackground(Color.red);
				}

				if (chckbxMediaCommands.isSelected()) {
					txtVlcInfo.setText("VLC found - media palyback enabled");
				} else {
					txtVlcInfo.setText("VLC found - media palyback disabled");
					bot.stopMedia();
				}
			}
		});
		chckbxMediaCommands.setBounds(10, 504, 230, 23);
		panel.add(chckbxMediaCommands);

		JLabel lblTtsProvider = new JLabel("TTS Provider");
		lblTtsProvider.setBounds(10, 283, 98, 14);
		panel.add(lblTtsProvider);

		JLabel lblVoice = new JLabel("Voice");
		lblVoice.setBounds(10, 342, 60, 17);
		panel.add(lblVoice);

		JLabel lblsHalloWelt = new JLabel("!speak , !s");
		lblsHalloWelt.setBounds(324, 277, 77, 14);
		panel.add(lblsHalloWelt);

		init();
	}

	private void setTTSVolume(TwitchBot bot) {
		final Number value = (Number) spinnerVolume.getValue();
		bot.getTts().setVolume(value.floatValue());
	}

	private void initVoiceComboBox(TwitchBot bot) {
		comboBoxVoice.removeAllItems();
		for (String v : bot.getTts().listSupportedVoices(bot.getTts().getLang())) {
			comboBoxVoice.addItem(v);
		}
	}

	private void initLanguageComboBox(TwitchBot bot) {
		comboBoxLanguage.removeAllItems();

		// remove itemlistener to avoid triggering item changed
		ItemListener[] itemListeners = comboBoxLanguage.getItemListeners();
		for (ItemListener i : itemListeners) {
			comboBoxLanguage.removeItemListener(i);
		}
		for (String s : bot.getTts().getKnownLanguages()) {
			comboBoxLanguage.addItem(s);
		}

		// re-add itemlistener
		for (ItemListener i : itemListeners) {
			comboBoxLanguage.addItemListener(i);
		}
		comboBoxLanguage.setSelectedItem(bot.getTts().getLang());
	}

	private Thread createPircXThread(TwitchBot bot) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				bot.connect();
			}
		}, "PircBotX Thread");
	}

	public void updateVol(double value) {
		this.spinnerVolume.setValue(value);
	}

	public void updateLang(String value) {
		int matchedIdx = -1;
		for (int i = 0; i < comboBoxLanguage.getItemCount(); i++) {
			final String itemAt = comboBoxLanguage.getItemAt(i);
			if (itemAt.startsWith(value)) {
				matchedIdx = i;
			}
			if (itemAt.equals(value)) {
				// exact match
				matchedIdx = i;
				break;
			}
		}
		if (matchedIdx != -1) {
			this.comboBoxLanguage.setSelectedIndex(matchedIdx);
		}
	}

	public void updateGender(String gender) {
		for (int i = 0; i < comboBoxGender.getItemCount(); i++) {
			final String itemAt = comboBoxGender.getItemAt(i);
			if (itemAt.equalsIgnoreCase(gender)) {
				this.comboBoxGender.setSelectedItem(itemAt);
			}
		}
	}

	private void onTTSProviderChanged(TwitchBot bot, String ttsProviderName) {
		bot.setTTSProvider(ttsProviderName);

		bot.getTts().setDefault();

		// re-populate language dependent combo boxes
		initLanguageComboBox(bot);
		initVoiceComboBox(bot);
		setTTSVolume(bot);
		enableFeatureCombos(bot.getTts());
	}

	/**
	 * Enable / disable features based on current TTS Provider.
	 */
	private void enableFeatureCombos(TTSProvider tts) {
		comboBoxGender.setEnabled(tts.isSupported(TTSFeature.GENDER));
	}

	public void updateInput(String msgWithoutCommand) {
		textAreaTTSInput.setText(msgWithoutCommand);
	}

	public void updateTTS(TTSProvider tts) {
		comboBoxTTSProvider.setSelectedItem(tts.getName());
	}

	public void updateVoice(String msgWithoutCommand) {
		comboBoxVoice.setSelectedItem(msgWithoutCommand);
	}
}
