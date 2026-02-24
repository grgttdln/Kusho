# Tutorial Mode: Phone-Driven Feedback (Single Source of Truth)

## Problem

Tutorial Mode has a split-brain feedback problem: the watch self-evaluates gesture correctness and displays feedback locally via `TutorialModeStateHolder.showFeedback()`, while the phone independently displays its own feedback UI. This causes:

1. Watch shows stale "wrong" avatar from previous sessions (singleton state persists)
2. Watch and phone can display different states if messages are delayed or lost
3. No mechanism to clear feedback when reopening the tutorial screen

## Solution

Mirror Learn Mode's phone-driven feedback pattern where the phone is the single authority for what the watch displays.

## Architecture Changes

### Watch Side

**TutorialModeStateHolder** - Remove feedback state:
- Remove `FeedbackData` class, `_feedbackData`/`feedbackData` flows
- Remove `showFeedback()`, `clearFeedback()` methods
- Keep `retryTrigger` (distinct phone command)

**PhoneCommunicationManager** - Add feedback event handling:
- `TutorialModeFeedbackEvent` data class (isCorrect, predictedLetter, timestamp)
- `tutorialModeFeedbackEvent` StateFlow
- `tutorialModeFeedbackDismissed` StateFlow
- Handle `/tutorial_mode_show_feedback` and `/tutorial_mode_feedback_dismissed`

**TutorialModeScreen** - Event-driven local state:
- Replace StateHolder feedback with local `showingFeedback` + `feedbackIsCorrect`
- Collect `tutorialModeFeedbackEvent` for display
- Collect `tutorialModeFeedbackDismissed` for dismissal
- Reset feedback on new letter timestamp
- Clear stale state via `resetSession()` on screen entry
- Show feedback 2s then "Waiting for teacher..." (matching Learn Mode)

### Phone Side

**WatchConnectionManager** - New send methods:
- `sendTutorialModeFeedback(isCorrect, predictedLetter)`
- `notifyTutorialModeFeedbackDismissed()`
- Message paths: `/tutorial_mode_show_feedback`, `/tutorial_mode_feedback_dismissed`

**TutorialSessionScreen** - Send feedback to watch:
- On gesture result: call `sendTutorialModeFeedback()`
- On Continue tap: call `notifyTutorialModeFeedbackDismissed()` before retry/next

## Message Flow

```
Watch: gesture_result        --> Phone  (existing)
Phone: show_feedback         --> Watch  (NEW)
Phone: feedback_dismissed    --> Watch  (NEW, before retry/next_letter)
Phone: retry / next_letter   --> Watch  (existing)
```

## Files Modified

| File | Change |
|------|--------|
| `wear/.../tutorial/TutorialModeStateHolder.kt` | Remove FeedbackData, showFeedback, clearFeedback |
| `wear/.../tutorial/TutorialModeScreen.kt` | Event-driven feedback, stale state clear, 2s+waiting pattern |
| `wear/.../service/PhoneCommunicationManager.kt` | Add feedback event/dismissed flows, handle new paths |
| `wear/.../service/WearMessageListenerService.kt` | Remove feedback_dismissed handler if present |
| `app/.../service/WatchConnectionManager.kt` | Add sendTutorialModeFeedback, notifyTutorialModeFeedbackDismissed |
| `app/.../tutorialmode/TutorialSessionScreen.kt` | Send feedback/dismissed to watch |
