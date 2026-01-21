package com.example.app.ui.components.dashboard

import com.example.app.R

fun getWatchImageResource(watchName: String): Int {
    return when {
        watchName.contains("Watch8", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8_classic
        watchName.contains("Watch 8", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8_classic
        watchName.contains("Watch8", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8
        watchName.contains("Watch 8", ignoreCase = true) -> R.drawable.ic_galaxy_watch_8
        watchName.contains("Watch7", ignoreCase = true) -> R.drawable.ic_galaxy_watch_7
        watchName.contains("Watch 7", ignoreCase = true) -> R.drawable.ic_galaxy_watch_7
        watchName.contains("Watch6", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6_classic
        watchName.contains("Watch 6", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6_classic
        watchName.contains("Watch6", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6
        watchName.contains("Watch 6", ignoreCase = true) -> R.drawable.ic_galaxy_watch_6
        watchName.contains("Watch5", ignoreCase = true) && watchName.contains("Pro", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5_pro
        watchName.contains("Watch 5", ignoreCase = true) && watchName.contains("Pro", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5_pro
        watchName.contains("Watch5", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5
        watchName.contains("Watch 5", ignoreCase = true) -> R.drawable.ic_galaxy_watch_5
        watchName.contains("Watch4", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4_classic
        watchName.contains("Watch 4", ignoreCase = true) && watchName.contains("Classic", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4_classic
        watchName.contains("Watch4", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4
        watchName.contains("Watch 4", ignoreCase = true) -> R.drawable.ic_galaxy_watch_4
        watchName.contains("Ultra", ignoreCase = true) -> R.drawable.ic_galaxy_ultra
        else -> R.drawable.ic_watch_generic
    }
}

