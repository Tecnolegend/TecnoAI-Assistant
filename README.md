<p align="center"><img src="docs/banner.jpg" alt="Tecno AI — voice assistant for Android" width="100%"></p>

# Tecno AI

An Android voice assistant powered by Google Gemini. Say **"Hey Tecno"**, ask a question or give a command, and Tecno AI answers out loud or takes action on your phone.

## Download
Get the latest APK from **[Releases → Latest](https://github.com/Tecnolegend/TecnoAI-Assistant/releases/latest)**, open it on your phone and allow installing from this source. New versions install over the old one and keep your data.

## Features
- **"Hey Tecno" wake word** — hands-free activation that runs in the background (can be switched off in Settings)
- **Voice in, voice out** — speech recognition and natural text-to-speech replies
- **Voice enrollment** — trains on your voice during setup
- **Floating bubble & assist overlay** — call Tecno AI on top of any app, or set it as your phone's assist app
- **Device control** — open apps, call, message, set alarms and timers, flashlight, settings, and on-screen actions through the accessibility service
- **Works offline** — common commands run on the phone without internet
- **Clear errors** — tells you when your API key is wrong, the free limit is reached, or you're offline
- **Personalization & history** — learns your preferences; conversations stay on your device

## Setup
1. Get a free Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey).
2. Open Tecno AI → **Settings** → paste your key (up to 5 keys; it switches to the next one if a key hits its limit).
3. Allow the microphone, and optionally the overlay and accessibility permissions for full phone control.

## Build it yourself
```bash
cp .env.example .env    # optionally set GEMINI_API_KEY
gradle :app:assembleDebug
```
Requires JDK 17 and the Android SDK (Android Studio). Every push to GitHub is built automatically by GitHub Actions.

Optional cloud sign-in: add your Firebase `app/google-services.json` and set `GOOGLE_WEB_CLIENT_ID` in `.env`. Without them the app works fully and simply hides cloud sign-in.

## Built with
Kotlin · Jetpack Compose · Room · DataStore · Retrofit · Gemini API · GitHub Actions

## Author
Made by [Tecnolegend](https://github.com/Tecnolegend) ·
[YouTube](https://www.youtube.com/@tecnolegendmc) ·
[Instagram](https://www.instagram.com/tecnolegend103/) ·
Discord: `tecnolegend103`
