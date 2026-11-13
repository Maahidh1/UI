package com.cleanhome.tv

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TileAdapter(
    private val spanCount: Int,
    private val onClick: (Tile) -> Unit,
    private val onLongClick: (Tile) -> Unit,
) : RecyclerView.Adapter<TileAdapter.Holder>() {

    var tiles: List<Tile> = emptyList()
        set(value) {
            field = value
            @Suppress("NotifyDataSetChanged") // small list, rebuilt wholesale
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = tiles[position].key.hashCode().toLong()

    override fun getItemCount() = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return Holder(view, parent)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(tiles[position])

    inner class Holder(view: View, private val parent: ViewGroup) : RecyclerView.ViewHolder(view) {
        private val card: View = view.findViewById(R.id.card)
        private val banner: ImageView = view.findViewById(R.id.banner)
        private val icon: ImageView = view.findViewById(R.id.icon)
        private val label: TextView = view.findViewById(R.id.label)

        fun bind(tile: Tile) {
            // 16:9 cards sized to the column width.
            val available = parent.width - parent.paddingLeft - parent.paddingRight
            if (available > 0) {
                val cardWidth = available / spanCount - itemView.paddingLeft - itemView.paddingRight
                card.layoutParams = card.layoutParams.apply { height = cardWidth * 9 / 16 }
            }

            label.text = tile.label
            card.contentDescription = tile.label

            val ctx = itemView.context
            when (tile) {
                is Tile.App -> if (tile.banner != null) {
                    banner.setImageDrawable(tile.banner)
                    icon.setImageDrawable(null)
                } else {
                    banner.setImageDrawable(null)
                    icon.setImageDrawable(tile.icon)
                }
                is Tile.Settings -> {
                    banner.setImageDrawable(null)
                    icon.setImageDrawable(ctx.getDrawable(android.R.drawable.ic_menu_preferences))
                }
                is Tile.ShowHidden -> {
                    banner.setImageDrawable(null)
                    icon.setImageDrawable(ctx.getDrawable(android.R.drawable.ic_menu_view))
                }
            }

            card.setOnClickListener { onClick(tile) }
            card.setOnLongClickListener { onLongClick(tile); true }
            card.setOnFocusChangeListener { _, focused ->
                val scale = if (focused) 1.08f else 1f
                card.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
                label.setTextColor(ctx.getColor(if (focused) R.color.text_primary else R.color.text_secondary))
            }
        }
    }
}
