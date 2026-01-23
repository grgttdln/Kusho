package com.example.app.util

/**
 * Utility object for validating words according to pedagogical rules.
 *
 * Specifically designed for early learner literacy (ages 3-7) focusing on
 * CVC (Consonant-Vowel-Consonant) pattern recognition.
 */
object WordValidator {
    private val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')

    /**
     * Validates if a word follows the CVC (Consonant-Vowel-Consonant) pattern.
     *
     * CVC pattern examples: "cat", "dog", "run", "pen", "sit"
     * Non-CVC examples: "eat" (VVC), "car" (CVC but ends with 'r' which could be vowel-like),
     *                   "the" (CVV), "and" (VCC)
     *
     * @param word The word to validate
     * @return true if the word follows CVC pattern, false otherwise
     */
    fun isCVCPattern(word: String): Boolean {
        if (word.length != 3) return false

        val chars = word.lowercase()
        return !isVowel(chars[0]) &&  // First character: Consonant
               isVowel(chars[1]) &&   // Second character: Vowel
               !isVowel(chars[2])     // Third character: Consonant
    }

    /**
     * Checks if a character is a vowel.
     *
     * @param char The character to check
     * @return true if the character is a vowel (a, e, i, o, u), false otherwise
     */
    private fun isVowel(char: Char): Boolean {
        return vowels.contains(char)
    }

    /**
     * Validates if a word is exactly three letters long.
     *
     * @param word The word to validate
     * @return true if the word is exactly 3 letters, false otherwise
     */
    fun isThreeLetters(word: String): Boolean {
        return word.length == 3
    }

    /**
     * Provides a comprehensive validation for word bank entries.
     * Returns a pair of (isValid, errorMessage).
     *
     * @param word The word to validate
     * @return Pair<Boolean, String?> where Boolean indicates validity and String provides error message if invalid
     */
    fun validateWordForBank(word: String): Pair<Boolean, String?> {
        val trimmed = word.trim()

        // Check if empty
        if (trimmed.isBlank()) {
            return false to "Word cannot be empty"
        }

        // Check if only letters
        if (!trimmed.all { it.isLetter() }) {
            return false to "Word can only contain letters"
        }

        // Check if exactly 3 letters
        if (!isThreeLetters(trimmed)) {
            return false to "Word must be exactly 3 letters long"
        }

        // Check if follows CVC pattern
        if (!isCVCPattern(trimmed)) {
            return false to "Word must follow CVC pattern (e.g., 'cat', 'dog', 'run')"
        }

        return true to null
    }

    /**
     * Gets a list of example CVC words for user guidance.
     *
     * @return List of valid CVC pattern words
     */
    fun getCVCExamples(): List<String> {
        return listOf(
            "cat", "dog", "run", "pen", "sit",
            "bat", "cup", "hop", "net", "pig",
            "bag", "bed", "hot", "pin", "sun"
        )
    }
}
