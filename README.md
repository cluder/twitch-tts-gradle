# twitch-tts-gradle

Twitch chat bot, using [PircBotX](https://github.com/pircbotx/pircbotx).
Text-to-speech is done through [google cloud text-to-speech](https://cloud.google.com/text-to-speech)

## Usage:
* checkout source and use gradle to build the jar ''gradle jar''
* create a google cloud account with access to the text-to-speech api and paste your key into google-credentials.json see also [Google-API-Account-Erstellung](https://github.com/cluder/twitch-tts-gradle/wiki/Google-API-Account-Erstellung)
* create a OAuth token using [Twitch Chat OAuth Password Generator](https://twitchapps.com/tmi/) and edit the oauth.properties
