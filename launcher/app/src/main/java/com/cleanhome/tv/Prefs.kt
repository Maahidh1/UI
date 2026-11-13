package com.cleanhome.tv

import android.content.Context

/** Stores which apps are hidden and which are pinned to the front. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("clean_home", Context.MODE_PRIVATE)

    var hidden: Set<String>
        get() = sp.getStringSet(KEY_HIDDEN, emptySet())!!.toSet()
        set(value) = sp.edit().putStringSet(KEY_HIDDEN, value).apply()

    /** Pinned package names, front-most first. */
    var pinned: List<String>
        get() = sp.getString(KEY_PINNED, "")!!.split(SEP).filter { it.isNotEmpty() }
        set(value) = sp.edit().putString(KEY_PINNED, value.joinToString(SEP)).apply()

    fun hide(pkg: String) {
        hidden = hidden + pkg
        pinned = pinned - pkg
    }

    fun moveToFront(pkg: String) {
        pinned = listOf(pkg) + (pinned - pkg)
    }

    private companion object {
        const val KEY_HIDDEN = "hidden"
        const val KEY_PINNED = "pinned"
        const val SEP = "|"
    }
}
