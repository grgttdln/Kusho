# Deepgram TTS Integration Setup

This app now uses Deepgram's Text-to-Speech API with the **Helena** voice for natural-sounding speech in Learn Mode.

## Setup Instructions

### 1. Get Your Deepgram API Key

1. Go to [Deepgram Console](https://console.deepgram.com/)
2. Sign up or log in to your account
3. Create a new API key
4. Copy the API key

### 2. Configure the API Key

You have two options to configure the API key:

#### Option A: Local Properties (Recommended for development)

Add the following to your `local.properties` file in the project root:

```properties
DEEPGRAM_API_KEY=your_api_key_here
```

#### Option B: Environment Variable

Set an environment variable:

```bash
export DEEPGRAM_API_KEY=your_api_key_here
```

Then sync your project in Android Studio.

### 3. Verify Helena Voice

The Helena voice should be automatically used. The default Deepgram TTS endpoint uses their best available voices, which includes Helena.

## Features

- **Natural-sounding voice**: Uses Deepgram's high-quality TTS
- **Audio caching**: Audio files are cached locally to reduce API calls
- **Offline playback**: Once audio is cached, it can be played offline
- **Random phrases**: Different encouraging phrases for each question type

## Question Types Supported

1. **Fill in the Blank**: Random phrases like "Fill in the missing letter to make the word DOG!"
2. **Write the Word**: Random phrases like "Can you write the word DOG?"
3. **Name the Picture**: Random phrases like "What is this picture? Trace and write its name!"

## Troubleshooting

### API Key Not Working
- Make sure you've added the API key to `local.properties`
- Sync the project after adding the key
- Clean and rebuild the project

### No Audio Playing
- Check your internet connection (first-time playback requires internet)
- Check logcat for DeepgramTTSManager errors
- Verify the API key has not expired

### Cache Management
Audio files are cached in the app's cache directory. To clear the cache:
```kotlin
ttsManager.clearCache()
```

## API Usage Notes

- Each TTS request counts against your Deepgram API quota
- Cached audio reduces API usage
- Audio is cached using the text content as the key
- Maximum cache size is limited by available device storage

## Security

⚠️ **Important**: Never commit your API key to version control. The key is configured through `local.properties` which should be in your `.gitignore` file.
