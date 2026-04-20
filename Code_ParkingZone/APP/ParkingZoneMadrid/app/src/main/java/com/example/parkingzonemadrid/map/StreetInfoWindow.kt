package com.example.parkingzonemadrid.map

import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.model.StreetZone
import com.example.parkingzonemadrid.data.model.ZoneType
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Popup estilo parking-madrid.es que aparece al pulsar una calle del mapa.
 * Muestra nombre, plazas, tipo de zona, distrito, tarifa y horario.
 *
 * Cuando el usuario pulsa la estrella, avisa a [onFavoriteClicked] para que
 * [MapActivity] persista el cambio en Room.
 */
class StreetInfoWindow(
    mapView: MapView,
    private val onFavoriteClicked: (StreetZone) -> Unit
) : InfoWindow(R.layout.infowindow_street, mapView) {

    private var currentZone: StreetZone? = null
    private var isFavorite: Boolean = false

    fun bind(zone: StreetZone, favorite: Boolean) {
        currentZone = zone
        isFavorite = favorite

        val context = mView.context
        val header = mView.findViewById<LinearLayout>(R.id.iwHeader)
        val streetNameView = mView.findViewById<TextView>(R.id.iwStreetName)
        val plazasView = mView.findViewById<TextView>(R.id.iwPlazas)
        val zonaLabel = mView.findViewById<TextView>(R.id.iwZonaLabel)
        val distritoView = mView.findViewById<TextView>(R.id.iwDistrito)
        val tarifaView = mView.findViewById<TextView>(R.id.iwTarifa)
        val horarioView = mView.findViewById<TextView>(R.id.iwHorario)
        val favoriteBtn = mView.findViewById<ImageButton>(R.id.iwFavorite)

        streetNameView.text = zone.streetName
        plazasView.text = zone.totalPlazas.toString()
        zonaLabel.text = zone.zoneType.shortLabel.uppercase()
        distritoView.text = zone.district

        tarifaView.text = buildTarifaText(zone)
        horarioView.text = zone.zoneType.horario

        val (headerColor, accentColor) = resolveColors(context, zone.zoneType)
        header.setBackgroundColor(headerColor)
        zonaLabel.setTextColor(accentColor)
        tarifaView.setTextColor(accentColor)

        favoriteBtn.setImageResource(
            if (isFavorite) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
        )

        favoriteBtn.setOnClickListener {
            currentZone?.let { onFavoriteClicked(it) }
        }
    }

    private fun buildTarifaText(zone: StreetZone): String {
        return when (zone.zoneType) {
            ZoneType.MIXTA -> buildString {
                if (zone.plazasAzul > 0) {
                    append("• Azul: ${zone.plazasAzul} plazas · 2,85 €/h\n")
                }
                if (zone.plazasVerde > 0) {
                    append("• Verde: ${zone.plazasVerde} plazas · 0,25-0,45 €/h")
                }
            }.trim()
            else -> zone.zoneType.tarifaResumen
        }
    }

    private fun resolveColors(
        context: android.content.Context,
        zoneType: ZoneType
    ): Pair<Int, Int> {
        val strong = when (zoneType) {
            ZoneType.VERDE -> R.color.zona_verde
            ZoneType.AZUL -> R.color.zona_azul
            ZoneType.MIXTA -> R.color.zona_mixta
            else -> R.color.zona_otro
        }
        val color = ContextCompat.getColor(context, strong)
        return color to color
    }

    override fun onOpen(item: Any?) {
        // El contenido ya se vincula desde MapActivity mediante bind()
    }

    override fun onClose() {
        currentZone = null
    }
}
