package com.example.parkingzonemadrid.map

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.ParkingZonesData
import com.example.parkingzonemadrid.data.model.StreetZone
import com.example.parkingzonemadrid.data.model.ZoneType
import com.example.parkingzonemadrid.data.repository.ParkingLocalRepository
import com.example.parkingzonemadrid.utils.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Pantalla principal: mapa OSM con todas las calles del SER de Madrid.
 *
 * - Cada calle aparece como un pin con la letra "P" y el color de su zona
 *   (verde / azul / morado para mixta), igual que en parking-madrid.es.
 * - Al pulsar el pin, se abre un popup con tarifa, horario y nº de plazas.
 * - El toolbar permite filtrar por tipo de zona o por favoritos.
 */
class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: ParkingLocalRepository
    private lateinit var infoWindow: StreetInfoWindow

    private var allZones: List<StreetZone> = emptyList()
    private var favoriteZoneIds: Set<Int> = emptySet()
    private var currentEmail: String? = null
    private var currentFilter: ZoneFilter = ZoneFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(40.4168, -3.7038)) // Madrid centro

        prefsManager = PreferencesManager(this)
        repository = ParkingLocalRepository(applicationContext)
        currentEmail = prefsManager.getUser()?.email

        infoWindow = StreetInfoWindow(mapView) { zone ->
            toggleFavorite(zone)
        }

        loadMapData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val newFilter = when (item.itemId) {
            R.id.filter_all -> ZoneFilter.ALL
            R.id.filter_verde -> ZoneFilter.VERDE
            R.id.filter_azul -> ZoneFilter.AZUL
            R.id.filter_mixta -> ZoneFilter.MIXTA
            R.id.filter_favoritos -> ZoneFilter.FAVORITOS
            else -> return super.onOptionsItemSelected(item)
        }
        item.isChecked = true
        currentFilter = newFilter
        renderStreetZones(applyFilter(allZones))
        return true
    }

    private fun loadMapData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val email = currentEmail
            if (email != null) {
                favoriteZoneIds = repository.getFavoriteZoneIds(email)
            }
            val zones = ParkingZonesData.getStreetZones(applicationContext)
            withContext(Dispatchers.Main) {
                allZones = zones
                renderStreetZones(applyFilter(zones))
            }
        }
    }

    private fun applyFilter(zones: List<StreetZone>): List<StreetZone> {
        return when (currentFilter) {
            ZoneFilter.ALL -> zones
            ZoneFilter.VERDE -> zones.filter { it.zoneType == ZoneType.VERDE }
            ZoneFilter.AZUL -> zones.filter { it.zoneType == ZoneType.AZUL }
            ZoneFilter.MIXTA -> zones.filter { it.zoneType == ZoneType.MIXTA }
            ZoneFilter.FAVORITOS -> zones.filter { favoriteZoneIds.contains(it.zoneId) }
        }
    }

    private fun renderStreetZones(zones: List<StreetZone>) {
        mapView.overlays.removeAll { it is Marker }
        infoWindow.close()

        val activity = this
        zones.forEach { zone ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(zone.latitude, zone.longitude)
            marker.title = zone.streetName
            marker.icon = ContextCompat.getDrawable(activity, iconResFor(zone.zoneType))
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.infoWindow = infoWindow
            marker.setOnMarkerClickListener { clickedMarker, map ->
                infoWindow.close()
                infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
                clickedMarker.showInfoWindow()
                map.controller.animateTo(clickedMarker.position)
                true
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    private fun iconResFor(zoneType: ZoneType): Int = when (zoneType) {
        ZoneType.VERDE -> R.drawable.pin_verde
        ZoneType.AZUL -> R.drawable.pin_azul
        ZoneType.MIXTA -> R.drawable.pin_mixta
        else -> R.drawable.pin_otro
    }

    private fun toggleFavorite(zone: StreetZone) {
        val email = currentEmail
        if (email == null) {
            Toast.makeText(this, "Inicia sesión para guardar favoritos", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(email, zone.zoneId)
            favoriteZoneIds = repository.getFavoriteZoneIds(email)
            withContext(Dispatchers.Main) {
                val nowFavorite = favoriteZoneIds.contains(zone.zoneId)
                Toast.makeText(
                    this@MapActivity,
                    if (nowFavorite) "Guardado en favoritos" else "Eliminado de favoritos",
                    Toast.LENGTH_SHORT
                ).show()
                // Refrescar el popup para que la estrella se actualice.
                infoWindow.bind(zone, nowFavorite)
                if (currentFilter == ZoneFilter.FAVORITOS) {
                    renderStreetZones(applyFilter(allZones))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        Configuration.getInstance()
            .save(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
    }

    private enum class ZoneFilter { ALL, VERDE, AZUL, MIXTA, FAVORITOS }

    private companion object {
        const val OSM_PREFS = "osm_prefs"
    }
}
