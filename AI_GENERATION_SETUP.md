# AI Activity Generation Setup Guide

## Overview
This feature allows teachers to generate complete activities with sets using AI. Teachers describe what they want in natural language, select words from their word bank, and the AI generates a structured activity with multiple sets.

## Setup Instructions

### 1. Get an OpenRouter API Key (RECOMMENDED - Better Free Tier)
1. Visit [OpenRouter](https://openrouter.ai/)
2. Sign in with your Google/GitHub account
3. Go to "Keys" section
4. Create a new API key
5. Copy the key

**Why OpenRouter?**
- More generous free tier than Gemini
- Uses `nvidia/nemotron-nano-9b-v2:free` model
- No credit card required for free tier
- Rate limits: 20 requests/minute, 200 requests/day

### 2. Add API Key to Project

Open the `local.properties` file in the project root and add:

```properties
OPENROUTER_API_KEY=your_actual_api_key_here
```

**Example:**
```properties
OPENROUTER_API_KEY=sk-or-v1-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 3. Build the Project

After adding the API key, rebuild the project:

```bash
./gradlew :app:clean :app:build
```

## How It Works

### User Flow
1. **Navigate to Your Activities** → Tap "AI Generate" button
2. **Enter Prompt** → Describe the activity (e.g., "Create a story about animals for beginners")
3. **Select Words** → Choose at least 3 words from your Word Bank (or "Select All")
4. **AI Generation** → OpenRouter API generates:
   - Activity title and description
   - Multiple sets (chapters) with words
   - Word configurations (fill-in-the-blank, identification, air writing)
5. **Review Sets** → Each generated set opens in the familiar Add Set screen for editing
6. **Create Activity** → Final activity screen is pre-filled with AI data and linked sets

### Technical Architecture

**Components:**
- `GeminiService` (kept name for compatibility) - Retrofit interface for OpenRouter API
- `GeminiRepository` - Prompt building, API calls, response validation
- `GenerateActivityViewModel` - State management for generation flow
- `GenerateActivityScreen` - Word selection and prompt input UI
- `AiGeneratedData` - Data classes for AI output

**AI Model:**
- **Provider:** OpenRouter
- **Model:** `nvidia/nemotron-nano-9b-v2:free`
- **Endpoint:** `https://openrouter.ai/api/v1/chat/completions`
- **Format:** OpenAI-compatible API

**Data Flow:**
1. Teacher inputs prompt and selects words
2. `GenerateActivityViewModel` calls `GeminiRepository.generateActivity()`
3. Repository builds prompt with JSON schema embedded in system message
4. OpenRouter API returns JSON response
5. Repository parses and validates response
6. ViewModel stores parsed data
7. Teacher steps through each set using `AddSetScreen`
8. Finally creates activity using `AddNewActivityScreen`

**Configuration Types:**
- **Fill in the Blank** - Student fills in a missing letter (letter index specified)
- **Identification** - Student identifies word from picture (image upload option)
- **Air Writing** - Student writes word in the air using gestures (best for short words)

### Validation & Retries
The system automatically validates AI output:
- Words must exist in the teacher's word bank
- Each set must have at least 3 words
- Letter indices must be valid for fill-in-the-blank
- Configuration types must be valid

If validation fails, the system automatically retries up to 2 times before showing an error.

### Error Handling
Common errors and solutions:
- **"API rate limit exceeded"** - You've hit the 20 requests/minute or 200 requests/day limit. Wait a moment and try again.
- **"Authentication failed"** - Check your OpenRouter API key in `local.properties`
- **"Payment required"** - You've exceeded the free tier. Add credits to your OpenRouter account or wait for the daily reset.
- **"No content generated"** - The AI couldn't generate valid output. Try a different prompt.

## Screens Added
- **Screen 48** - `GenerateActivityScreen` - Word selection + prompt input
- **Screen 49** - AI Set creation (reuses `AddSetScreen`)
- **Screen 50** - Word selection for additional words
- **Screen 51** - Final activity creation (reuses `AddNewActivityScreen`)

## API Details
- **Base URL:** `https://openrouter.ai/`
- **Endpoint:** `/api/v1/chat/completions`
- **Model:** `nvidia/nemotron-nano-9b-v2:free`
- **Temperature:** 0.7
- **Max Tokens:** 2048
- **Timeout:** 60 seconds

## Free Tier Limits (OpenRouter)
- 20 requests per minute
- 200 requests per day
- No credit card required
- Rate limits reset daily

## Security Notes
- API key is stored in `local.properties` (not committed to git)
- API key is compiled into BuildConfig at build time
- Never commit your actual API key to version control
