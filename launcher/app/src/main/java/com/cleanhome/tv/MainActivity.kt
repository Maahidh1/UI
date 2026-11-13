package com.cleanhome.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

/** A minimal TV home screen: clock, then a grid of your apps. No rows, no ads. */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var repo: AppRepository
    private lateinit var grid: RecyclerView
    private lateinit var adapter: TileAdapter

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var apps: List<Tile.App> = emptyList()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = Prefs(this)
        repo = AppRepository(this)
        adapter = TileAdapter(SPAN_COUNT, ::onTileClick, ::onTileLongClick)

        grid = findViewById(R.id.apps)
        grid.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        grid.adapter = adapter
        grid.itemAnimator = null // avoid flicker when the list is rebuilt

        registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        io.shutdown()
        super.onDestroy()
    }

    /** Pressing Home while already home jumps back to the first tile. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        grid.scrollToPosition(0)
        focusFirst()
    }

    // This is the home screen: Back has nowhere to go.
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event)

    // The Menu key opens the options for the focused tile.
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            val focused = grid.focusedChild ?: return true
            val pos = grid.getChildAdapterPosition(focused)
            if (pos != RecyclerView.NO_POSITION) onTileLongClick(adapter.tiles[pos])
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun reload() {
        io.execute {
            val loaded = repo.loadApps()
            main.post {
                if (isDestroyed) return@post
                apps = loaded
                render()
            }
        }
    }

    private fun render() {
        val hidden = prefs.hidden
        val pinned = prefs.pinned
        val visible = apps.filter { it.packageName !in hidden }
        val ordered = visible.sortedBy { app ->
            pinned.indexOf(app.packageName).let { if (it < 0) Int.MAX_VALUE else it }
        } // stable sort keeps the rest alphabetical

        val hiddenCount = apps.count { it.packageName in hidden }
        val tiles = buildList<Tile> {
            addAll(ordered)
            add(Tile.Settings(getString(R.string.settings)))
            if (hiddenCount > 0) add(Tile.ShowHidden(getString(R.string.show_hidden, hiddenCount)))
        }

        val hadFocus = grid.focusedChild != null
        adapter.tiles = tiles
        if (!hadFocus) focusFirst()
    }

    private fun focusFirst() {
        grid.post {
            grid.findViewHolderForAdapterPosition(0)?.itemView
                ?.findViewById<android.view.View>(R.id.card)?.requestFocus()
        }
    }

    private fun onTileClick(tile: Tile) {
        when (tile) {
            is Tile.App -> launch(tile.launchIntent)
            is Tile.Settings -> launch(Intent(Settings.ACTION_SETTINGS))
            is Tile.ShowHidden -> {
                prefs.hidden = emptySet()
                render()
            }
        }
    }

    private fun onTileLongClick(tile: Tile) {
        if (tile !is Tile.App) return
        val options = arrayOf(
            getString(R.string.move_to_front),
            getString(R.string.hide_app),
            getString(R.string.app_info),
        )
        AlertDialog.Builder(this)
            .setTitle(tile.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { prefs.moveToFront(tile.packageName); render(); scrollHome() }
                    1 -> { prefs.hide(tile.packageName); render() }
                    2 -> launch(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", tile.packageName, null))
                    )
                }
            }
            .show()
    }

    private fun scrollHome() {
        grid.scrollToPosition(0)
        focusFirst()
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val SPAN_COUNT = 5
    }
}
