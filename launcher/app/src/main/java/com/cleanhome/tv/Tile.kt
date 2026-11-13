package com.cleanhome.tv

import android.content.Intent
import android.graphics.drawable.Drawable

/** One tile on the home screen: an installed app, or a built-in action. */
sealed class Tile {
    abstract val key: String
    abstract val label: String

    data class App(
        val packageName: String,
        override val label: String,
        val banner: Drawable?,
        val icon: Drawable?,
        val launchIntent: Intent,
    ) : Tile() {
        override val key get() = packageName
    }

    /** Opens system Settings so the user never needs the stock launcher. */
    data class Settings(override val label: String) : Tile() {
        override val key get() = "action:settings"
    }

    /** Brings hidden apps back. Only shown when something is hidden. */
    data class ShowHidden(override val label: String) : Tile() {
        override val key get() = "action:show_hidden"
    }
}
