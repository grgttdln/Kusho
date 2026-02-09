package com.example.app.ui.components.learnmode

/**
 * Data class representing the annotation data to be passed to/from the LearnerProfileAnnotationDialog.
 * This serves as a UI model that can be easily converted to/from the database entity.
 */
data class AnnotationData(
    val levelOfProgress: String? = null,
    val strengthsObserved: Set<String> = emptySet(),
    val strengthsNote: String = "",
    val challenges: Set<String> = emptySet(),
    val challengesNote: String = ""
) {
    /**
     * Check if this annotation has any data
     */
    fun hasData(): Boolean {
        return levelOfProgress != null ||
                strengthsObserved.isNotEmpty() ||
                strengthsNote.isNotBlank() ||
                challenges.isNotEmpty() ||
                challengesNote.isNotBlank()
    }

    companion object {
        /**
         * Create an empty AnnotationData
         */
        fun empty() = AnnotationData()
    }
}

