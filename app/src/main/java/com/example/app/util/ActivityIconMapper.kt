package com.example.app.util

import com.example.app.R
import kotlin.math.absoluteValue

/**
 * Maps an Activity Set title to an appropriate drawable icon resource
 * based on keyword matching. Uses title-hash seeded selection so the
 * same title always returns the same icon deterministically.
 */
object ActivityIconMapper {

    // ── Category definitions ────────────────────────────────────────

    private data class IconCategory(
        val keywords: List<String>,
        val icons: List<Int>
    )

    private val categories = listOf(
        // Animals / Pets / Sea Creatures
        IconCategory(
            keywords = listOf("animal", "pet", "dog", "cat", "fish", "bird", "farm", "zoo", "ocean"),
            icons = listOf(
                R.drawable.ic_activity_10, // fish 1
                R.drawable.ic_activity_11, // fish 2
                R.drawable.ic_activity_12, // chicken
                R.drawable.ic_activity_13, // pufferfish
                R.drawable.ic_activity_16, // cat
                R.drawable.ic_activity_19, // cat
                R.drawable.ic_activity_20  // capybara
            )
        ),
        // Food / Fruits / Vegetables
        IconCategory(
            keywords = listOf("food", "fruit", "eat", "meal", "breakfast", "lunch", "dinner", "apple", "bake"),
            icons = listOf(
                R.drawable.ic_activity_1,  // apple
                R.drawable.ic_activity_2,  // orange
                R.drawable.ic_activity_3,  // cherry
                R.drawable.ic_activity_4,  // loaf of bread
                R.drawable.ic_activity_8,  // red turnip
                R.drawable.ic_activity_14, // egg
                R.drawable.ic_activity_15  // strawberry
            )
        ),
        // Nature / Flowers / Plants
        IconCategory(
            keywords = listOf("nature", "flower", "plant", "garden", "spring", "bloom"),
            icons = listOf(
                R.drawable.ic_activity_5,  // pink flower
                R.drawable.ic_activity_6,  // yellowish orange flower
                R.drawable.ic_activity_9,  // red orange flower
                R.drawable.ic_activity_21, // daisy
                R.drawable.ic_activity_22  // 4 heart-shaped petal flowers
            )
        ),
        // Shapes / Weather / Miscellaneous
        IconCategory(
            keywords = listOf("shape", "weather", "sky", "star", "space"),
            icons = listOf(
                R.drawable.ic_activity_7,  // star
                R.drawable.ic_activity_17, // star patch
                R.drawable.ic_activity_18  // cloud
            )
        )
    )

    private val fallbackIcon = R.drawable.ic_activity_7 // star

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Returns a drawable resource ID for the given Activity Set [title].
     *
     * The title is normalised to lowercase and checked against category
     * keywords. If a match is found, one icon from that category is
     * picked deterministically (using the title's hash code as a seed).
     * If no keywords match, the fallback icon is returned.
     */
    fun getIconForActivity(title: String): Int {
        val normalised = title.lowercase()

        for (category in categories) {
            if (category.keywords.any { keyword -> normalised.contains(keyword) }) {
                val index = title.hashCode().absoluteValue % category.icons.size
                return category.icons[index]
            }
        }

        return fallbackIcon
    }
}
