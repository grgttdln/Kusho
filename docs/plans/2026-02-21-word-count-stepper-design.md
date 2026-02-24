# Word Count Stepper for WordBankGenerationModal

## Date: 2026-02-21

## Summary

Add a word count stepper component to `WordBankGenerationModal.kt` allowing users to specify how many CVC words Kuu should generate. UI-only scope — no ViewModel or backend wiring.

## Parameters

New parameters on `WordBankGenerationModal`:

```kotlin
wordCount: Int,                     // current count (default 5)
onWordCountChanged: (Int) -> Unit,  // callback when count changes
```

Constraints: min 1, max 20, step 1. Default: 5.

## Visual Spec

| Element | Spec |
|---------|------|
| Label | "How many words should Kuu generate?" — 16sp, Medium weight, `#0B0B0B` |
| Minus/Plus buttons | 48x48dp circles, `#49A9FF` background, white `-`/`+` icon, 24sp bold |
| Minus disabled state | `#B0D9FF` background when `wordCount == 1` |
| Plus disabled state | `#B0D9FF` background when `wordCount == 20` |
| Count display | `wordCount` as 28sp Bold `#0B0B0B`, "words" below as 14sp `#888888` |
| Preset chips | Rounded pills (24dp radius), 12dp horizontal padding. Selected: `#49A9FF` fill + white text. Unselected: `#F0F0F0` fill + `#666666` text |
| Spacing | 16dp between label and stepper row, 12dp between stepper and chips, 32dp below chips to generate button |
| Touch targets | All buttons minimum 44dp (buttons are 48dp, chips have 44dp min height) |
| Chip spacing | 8dp gap between chips |

## Layout Placement

```
[Title: "Let Kuu Generate CVC Words!"]
  ↓ 24dp
[Label: "What kind of CVC words..."]
[OutlinedTextField]
  ↓ 24dp
[Label: "How many words should Kuu generate?"]
  ↓ 16dp
[ (-) ]   [ 5 words ]   [ (+) ]
  ↓ 12dp
[ 5 ] [ 10 ] [ 15 ] [ 20 ]
  ↓ 32dp
[Generate CVC Words button]
[Error message]
```

## Interaction

- `-` decrements by 1 (clamped to min 1)
- `+` increments by 1 (clamped to max 20)
- Preset chip sets count to that value exactly
- Chip matching current `wordCount` shown as selected (blue fill)
- All controls disabled when `isLoading == true`
- Soft shadows on stepper buttons for tactile feel

## Scope

- UI only: add stepper to `WordBankGenerationModal.kt`
- Add `wordCount` / `onWordCountChanged` parameters
- Update call site in `WordBankScreen.kt` with local state
- Update preview composables
