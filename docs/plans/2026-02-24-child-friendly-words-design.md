# Child-Friendly CVC Word Generation Design

## Problem

The AI generates CVC words like "vat" and "lat" which are not age-appropriate for 4-8 year olds. The current prompt even lists "vat" and "lat" as acceptable examples for -at words.

## Fix

Improve the `generateCVCWords()` system instruction prompt in `GeminiRepository.kt` to:

1. Replace the vague "common English word that a child would recognize" with explicit kindergarten/first-grade vocabulary requirements
2. Add a list of negative examples (words to avoid)
3. Add guidance to prefer illustratable words
4. Remove "vat" and "lat" from the -at word examples in the prompt itself

## Scope

One file changed: `GeminiRepository.kt`, specifically the `generateCVCWords()` system instruction string.
