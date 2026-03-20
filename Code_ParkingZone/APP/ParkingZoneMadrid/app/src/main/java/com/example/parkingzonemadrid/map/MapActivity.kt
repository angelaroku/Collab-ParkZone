package com.example.parkingzonemadrid.map
/*Usaremos Mapa OSMDroid para poder vissualizarlo en el centro de la aplicación
* Se han descargado sus dependencias 6.1.18*/

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.repository.ParkingLocalRepository
import com.example.parkingzonemadrid.utils.PreferencesManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var prefsManager: PreferencesManager
    private lateinit var repository: ParkingLocalRepository
    private var favoriteZoneIds: Set<Int> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(GeoPoint(40.4168, -3.7038)) // Madrid

        prefsManager = PreferencesManager(this)
        repository = ParkingLocalRepository(applicationContext)
        loadFavoritesFromRoom()
    }

    private fun loadFavoritesFromRoom() {
        val email = prefsManager.getUser()?.email ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            favoriteZoneIds = repository.getFavoriteZoneIds(email)
            // Por ahora solo se carga para cuando dibujemos polígonos en el mapa.
            // Más adelante lo usaremos para pintar zonas favoritas.
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

    private companion object {
        const val OSM_PREFS = "osm_prefs"
    }
}

