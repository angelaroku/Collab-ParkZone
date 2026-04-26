package com.example.parkingzonemadrid.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.ParkingZonesData
import com.example.parkingzonemadrid.data.model.ParkingType
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
import java.net.URLEncoder

/**
 * Pantalla principal: mapa OSM con todas las calles del SER de Madrid.
 *
 * - Cada calle aparece como un pin con la letra "P" y el color de su zona
 *   (verde / azul / morado para mixta), igual que en parking-madrid.es.
 * - Al pulsar el pin, se abre un popup con tarifa, horario y nº de plazas.
 * - El toolbar permite filtrar por tipo de plaza (línea / batería / mixto).
 */
class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: ParkingLocalRepository
    private lateinit var infoWindow: StreetInfoWindow
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var favoritesRecycler: RecyclerView
    private lateinit var favoritesEmptyView: TextView
    private lateinit var favoritesAdapter: FavoritesAdapter

    private var allZones: List<StreetZone> = emptyList()
    private var favoriteZoneIds: Set<Int> = emptySet()
    private var currentEmail: String? = null
    private var currentParkingFilter: ParkingFilter = ParkingFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        drawerLayout = findViewById(R.id.drawerLayout)
        favoritesRecycler = findViewById(R.id.rvFavorites)
        favoritesEmptyView = findViewById(R.id.tvFavoritesEmpty)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(40.4168, -3.7038)) // Madrid centro

        prefsManager = PreferencesManager(this)
        repository = ParkingLocalRepository(applicationContext)
        currentEmail = prefsManager.getUser()?.correo

        infoWindow = StreetInfoWindow(
            mapView,
            onFavoriteClicked = { zone -> toggleFavorite(zone) },
            onNavigateClicked = { zone -> openInGoogleMaps(zone) }
        )

        favoritesAdapter = FavoritesAdapter(
            onItemClick = { zone ->
                drawerLayout.closeDrawer(GravityCompat.START)
                focusOnZone(zone)
            },
            onRemoveClick = { zone -> toggleFavorite(zone) }
        )
        favoritesRecycler.layoutManager = LinearLayoutManager(this)
        favoritesRecycler.adapter = favoritesAdapter

        // Back: si el cajón está abierto lo cierra; si no, comportamiento normal.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        loadMapData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_park_all -> currentParkingFilter = ParkingFilter.ALL
            R.id.filter_park_linea -> currentParkingFilter = ParkingFilter.LINEA
            R.id.filter_park_bateria -> currentParkingFilter = ParkingFilter.BATERIA
            R.id.filter_park_mixto -> currentParkingFilter = ParkingFilter.MIXTO
            else -> return super.onOptionsItemSelected(item)
        }
        item.isChecked = true
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
                refreshFavoritesList()
            }
        }
    }

    private fun refreshFavoritesList() {
        val favorites = allZones.filter { favoriteZoneIds.contains(it.zoneId) }
        favoritesAdapter.submit(favorites)
        if (favorites.isEmpty()) {
            favoritesEmptyView.visibility = View.VISIBLE
            favoritesRecycler.visibility = View.GONE
        } else {
            favoritesEmptyView.visibility = View.GONE
            favoritesRecycler.visibility = View.VISIBLE
        }
    }

    private fun focusOnZone(zone: StreetZone) {
        val point = GeoPoint(zone.latitude, zone.longitude)
        mapView.controller.setZoom(17.0)
        mapView.controller.animateTo(point)

        val marker = mapView.overlays
            .filterIsInstance<Marker>()
            .firstOrNull { it.position.latitude == zone.latitude && it.position.longitude == zone.longitude }

        infoWindow.close()
        infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
        marker?.showInfoWindow() ?: run {
            // Si no estaba en pantalla, lo añadimos puntualmente para mostrar el popup.
            val temp = Marker(mapView).apply {
                position = point
                icon = ContextCompat.getDrawable(this@MapActivity, iconResFor(zone.zoneType))
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                infoWindow = this@MapActivity.infoWindow
            }
            mapView.overlays.add(temp)
            mapView.invalidate()
            temp.showInfoWindow()
        }
    }

    private fun openInGoogleMaps(zone: StreetZone) {
        val query = URLEncoder.encode("${zone.streetName}, ${zone.district}, Madrid", "UTF-8")
        // Intent oficial de búsqueda de Google Maps con coordenadas de respaldo.
        val gmmIntentUri = Uri.parse(
            "geo:${zone.latitude},${zone.longitude}?q=$query"
        )
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
            return
        }
        // Fallback: abrir en navegador.
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
        )
        startActivity(webIntent)
    }

    private fun applyFilter(zones: List<StreetZone>): List<StreetZone> {
        return when (currentParkingFilter) {
            ParkingFilter.ALL -> zones
            ParkingFilter.LINEA -> zones.filter { it.parkingType == ParkingType.LINEA }
            ParkingFilter.BATERIA -> zones.filter { it.parkingType == ParkingType.BATERIA }
            ParkingFilter.MIXTO -> zones.filter { it.parkingType == ParkingType.MIXTO }
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
                refreshFavoritesList()
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

    private enum class ParkingFilter { ALL, LINEA, BATERIA, MIXTO }

    private companion object {
        const val OSM_PREFS = "osm_prefs"
    }
}
