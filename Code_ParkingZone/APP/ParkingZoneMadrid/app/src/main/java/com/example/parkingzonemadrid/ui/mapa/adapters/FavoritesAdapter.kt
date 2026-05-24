package com.example.parkingzonemadrid.ui.mapa.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.mapa.StreetZone
import com.example.parkingzonemadrid.data.mapa.ZoneType

/**
 * Adapter para la lista de favoritos del cajón lateral.
 *
 * - Click en el item: centra el mapa sobre la calle y abre su popup.
 * - Click en la papelera: elimina el favorito del usuario actual.
 */
class FavoritesAdapter(
    private val onItemClick: (StreetZone) -> Unit,
    private val onRemoveClick: (StreetZone) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavoriteVH>() {

    private val items = mutableListOf<StreetZone>()

    fun submit(newItems: List<StreetZone>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteVH(view)
    }

    override fun onBindViewHolder(holder: FavoriteVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FavoriteVH(view: View) : RecyclerView.ViewHolder(view) {
        private val colorTag: View = view.findViewById(R.id.favColorTag)
        private val streetName: TextView = view.findViewById(R.id.favStreetName)
        private val subtitle: TextView = view.findViewById(R.id.favSubtitle)
        private val remove: ImageButton = view.findViewById(R.id.favRemove)

        fun bind(zone: StreetZone) {
            streetName.text = zone.streetName
            subtitle.text = "${zone.district} · ${zone.zoneType.shortLabel} · " +
                "${zone.parkingType.displayName} · ${zone.totalPlazas} plazas"

            val colorRes = when (zone.zoneType) {
                ZoneType.VERDE -> R.color.zona_verde
                ZoneType.AZUL -> R.color.zona_azul
                ZoneType.MIXTA -> R.color.zona_mixta
                else -> R.color.zona_otro
            }
            colorTag.setBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))

            itemView.setOnClickListener { onItemClick(zone) }
            remove.setOnClickListener { onRemoveClick(zone) }
        }
    }
}

