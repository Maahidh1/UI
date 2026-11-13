package com.cleanhome.tv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/** Finds launchable apps, preferring TV (leanback) entry points and banners. */
class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    fun loadApps(): List<Tile.App> {
        val byPackage = LinkedHashMap<String, Tile.App>()

        // TV apps first so their leanback intent and banner win over phone ones.
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            for (ri in query(category)) {
                val ai = ri.activityInfo
                val pkg = ai.packageName
                if (pkg == context.packageName || pkg in byPackage) continue

                val intent = Intent(Intent.ACTION_MAIN)
                    .addCategory(category)
                    .setClassName(pkg, ai.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

                byPackage[pkg] = Tile.App(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm),
                    icon = ri.loadIcon(pm),
                    launchIntent = intent,
                )
            }
        }
        return byPackage.values.sortedBy { it.label.lowercase() }
    }

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }
}
