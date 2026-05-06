package com.example.parkingzonemadrid.ui.mapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.parkingzonemadrid.R
import com.example.parkingzonemadrid.data.ParkingZonesData
import com.example.parkingzonemadrid.data.model.StreetZone
import com.example.parkingzonemadrid.data.model.ZoneType
import com.example.parkingzonemadrid.data.repository.ParkingLocalRepository
import com.example.parkingzonemadrid.ui.mapa.adapters.FavoritesAdapter
import com.example.parkingzonemadrid.ui.mapa.components.StreetInfoWindow
import com.example.parkingzonemadrid.utils.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.URLEncoder

/**
 * Pantalla principal: mapa OSM con todas las calles del SER de Madrid.
 *
 * Mejoras incluidas:
 *  - La barra de leyenda (Verde/Azul/Mixta) actúa como filtro por color.
 *  - El menú permite filtrar por aparcamiento "con plazas en línea" o
 *    "con plazas en batería" (basado en presencia, no en tipo dominante).
 *  - El mapa solo pinta los pines visibles en el viewport actual; si haces
 *    zoom out muy lejos no carga toda Madrid de golpe (evita el efecto "a trompicones").
 *  - Las animaciones de centrado son suaves (animateTo con duración).
 *  - Soporta `MyLocationNewOverlay` para mostrar la ubicación del usuario.
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
    private lateinit var legendVerde: LinearLayout
    private lateinit var legendAzul: LinearLayout
    private lateinit var legendMixta: LinearLayout

    private var locationOverlay: MyLocationNewOverlay? = null

    private var allZones: List<StreetZone> = emptyList()
    private var favoriteZoneIds: Set<Int> = emptySet()
    private var currentEmail: String? = null
    private var currentParkingFilter: ParkingFilter = ParkingFilter.ALL
    private var currentColorFilter: ColorFilter = ColorFilter.ALL

    private val viewportRefreshHandler = Handler(Looper.getMainLooper())
    private var pendingViewportRefresh: Runnable? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocation()
            centerOnLastKnownLocation()
        } else {
            Toast.makeText(
                this,
                "Sin permiso de ubicación: el mapa se centrará en Madrid",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        drawerLayout = findViewById(R.id.drawerLayout)
        favoritesRecycler = findViewById(R.id.rvFavorites)
        favoritesEmptyView = findViewById(R.id.tvFavoritesEmpty)
        legendVerde = findViewById(R.id.legendVerde)
        legendAzul = findViewById(R.id.legendAzul)
        legendMixta = findViewById(R.id.legendMixta)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Aplica padding superior con el inset del sistema para que el toolbar
        // no quede tapado por la barra de notificaciones.
        val contentRoot = findViewById<View>(R.id.contentRoot)
        ViewCompat.setOnApplyWindowInsetsListener(contentRoot) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        // Empezamos con un zoom de barrio sobre Madrid centro;
        // si el usuario concede ubicación, recolocamos al instante.
        mapView.controller.setZoom(15.5)
        mapView.controller.setCenter(MADRID_CENTER)
        mapView.minZoomLevel = 12.0
        mapView.maxZoomLevel = 19.0

        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                scheduleViewportRefresh()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                scheduleViewportRefresh()
                return false
            }
        })

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

        legendVerde.setOnClickListener { onLegendClicked(ColorFilter.VERDE) }
        legendAzul.setOnClickListener { onLegendClicked(ColorFilter.AZUL) }
        legendMixta.setOnClickListener { onLegendClicked(ColorFilter.MIXTA) }
        refreshLegendHighlight()

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
        requestLocationOrFallback()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_my_location -> {
                requestLocationOrFallback(centerAfterPermission = true)
                return true
            }
            R.id.filter_park_all -> currentParkingFilter = ParkingFilter.ALL
            R.id.filter_park_linea -> currentParkingFilter = ParkingFilter.LINEA
            R.id.filter_park_bateria -> currentParkingFilter = ParkingFilter.BATERIA
            else -> return super.onOptionsItemSelected(item)
        }
        item.isChecked = true
        renderViewport()
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
                renderViewport()
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

    /**
     * Centrar en una zona desde la lista de favoritos: comparamos con el viewport
     * actual y solo movemos el mapa si la zona está fuera. Si está dentro, evitamos
     * el "salto" brusco que pidió revisar la tutora.
     */
    private fun focusOnZone(zone: StreetZone) {
        val point = GeoPoint(zone.latitude, zone.longitude)
        val visibleBox = mapView.boundingBox
        val centerArea = visibleBox?.let {
            // "Área de centro": el 50 % central del viewport actual.
            val latSpan = it.latNorth - it.latSouth
            val lonSpan = it.lonEast - it.lonWest
            val latPad = latSpan * 0.25
            val lonPad = lonSpan * 0.25
            doubleArrayOf(
                it.latSouth + latPad,
                it.latNorth - latPad,
                it.lonWest + lonPad,
                it.lonEast - lonPad
            )
        }

        val needsMove = centerArea == null ||
            zone.latitude !in centerArea[0]..centerArea[1] ||
            zone.longitude !in centerArea[2]..centerArea[3]

        if (needsMove) {
            val targetZoom = maxOf(mapView.zoomLevelDouble, 16.5)
            mapView.controller.animateTo(point, targetZoom, ANIMATION_MS)
        }

        // Mostramos el infowindow tras la animación, aunque no haya marker visible.
        viewportRefreshHandler.postDelayed({
            showInfoWindowFor(zone, point)
        }, if (needsMove) ANIMATION_MS + 50L else 0L)
    }

    private fun showInfoWindowFor(zone: StreetZone, point: GeoPoint) {
        val marker = mapView.overlays
            .filterIsInstance<Marker>()
            .firstOrNull { it.position.latitude == zone.latitude && it.position.longitude == zone.longitude }
        infoWindow.close()
        infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
        if (marker != null) {
            marker.showInfoWindow()
        } else {
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
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
        )
        startActivity(webIntent)
    }

    /**
     * Aplica los dos filtros (color de zona + tipo de aparcamiento por presencia).
     * Sobre LINEA / BATERIA usamos `hasLinea` / `hasBateria` para que también
     * incluyan calles "mixtas" en las que coexisten línea y batería.
     */
    private fun applyFilters(zones: List<StreetZone>): List<StreetZone> {
        val byParking = when (currentParkingFilter) {
            ParkingFilter.ALL -> zones
            ParkingFilter.LINEA -> zones.filter { it.hasLinea }
            ParkingFilter.BATERIA -> zones.filter { it.hasBateria }
        }
        return when (currentColorFilter) {
            ColorFilter.ALL -> byParking
            ColorFilter.VERDE -> byParking.filter { it.zoneType == ZoneType.VERDE }
            ColorFilter.AZUL -> byParking.filter { it.zoneType == ZoneType.AZUL }
            ColorFilter.MIXTA -> byParking.filter { it.zoneType == ZoneType.MIXTA }
        }
    }

    /**
     * Pinta SOLO las calles dentro del viewport actual para evitar el efecto
     * "a trompicones" cuando se intenta mostrar todo Madrid de golpe.
     */
    private fun renderViewport() {
        if (allZones.isEmpty()) {
            mapView.invalidate()
            return
        }
        val box = mapView.boundingBox
        val zoom = mapView.zoomLevelDouble

        // A zoom muy alejado preferimos no mostrar miles de pines.
        if (zoom < 13.0 || box == null) {
            mapView.overlays.removeAll { it is Marker }
            mapView.invalidate()
            return
        }

        val latS = box.latSouth
        val latN = box.latNorth
        val lonW = box.lonEast.coerceAtMost(box.lonWest)
        val lonE = box.lonEast.coerceAtLeast(box.lonWest)

        val visibleZones = applyFilters(allZones)
            .asSequence()
            .filter { it.latitude in latS..latN && it.longitude in lonW..lonE }
            .take(MAX_VISIBLE_MARKERS)
            .toList()

        renderStreetZones(visibleZones)
    }

    private fun scheduleViewportRefresh() {
        pendingViewportRefresh?.let { viewportRefreshHandler.removeCallbacks(it) }
        val task = Runnable { renderViewport() }
        pendingViewportRefresh = task
        viewportRefreshHandler.postDelayed(task, VIEWPORT_DEBOUNCE_MS)
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
                // Animación suave hacia el pin (1s) en lugar del salto brusco.
                map.controller.animateTo(clickedMarker.position, map.zoomLevelDouble, ANIMATION_MS)
                true
            }
            mapView.overlays.add(marker)
        }
        // El locationOverlay se pinta encima si existe.
        locationOverlay?.let { overlay ->
            mapView.overlays.remove(overlay)
            mapView.overlays.add(overlay)
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
                infoWindow.bind(zone, nowFavorite)
                refreshFavoritesList()
            }
        }
    }

    private fun onLegendClicked(filter: ColorFilter) {
        currentColorFilter = if (currentColorFilter == filter) ColorFilter.ALL else filter
        refreshLegendHighlight()
        renderViewport()
    }

    private fun refreshLegendHighlight() {
        val active = currentColorFilter
        legendVerde.alpha = if (active == ColorFilter.ALL || active == ColorFilter.VERDE) 1f else 0.4f
        legendAzul.alpha = if (active == ColorFilter.ALL || active == ColorFilter.AZUL) 1f else 0.4f
        legendMixta.alpha = if (active == ColorFilter.ALL || active == ColorFilter.MIXTA) 1f else 0.4f
    }

    /* ----------- Geolocalización ----------- */

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestLocationOrFallback(centerAfterPermission: Boolean = false) {
        if (hasLocationPermission()) {
            enableMyLocation()
            if (centerAfterPermission) centerOnLastKnownLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableMyLocation() {
        if (locationOverlay != null) return
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView).apply {
            enableMyLocation()
        }
        locationOverlay = overlay
        mapView.overlays.add(overlay)
        mapView.invalidate()
    }

    private fun centerOnLastKnownLocation() {
        if (!hasLocationPermission()) return
        val lm = getSystemService(LOCATION_SERVICE) as? LocationManager ?: return
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val location = providers.firstNotNullOfOrNull { provider ->
            try {
                lm.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            }
        }
        if (location != null) {
            val point = GeoPoint(location.latitude, location.longitude)
            mapView.controller.animateTo(point, 16.5, ANIMATION_MS)
            // Diagnóstico: imprime la posición real que está dando el sistema,
            // útil para distinguir entre "el GPS está mal" y "la app no la usa".
            Toast.makeText(
                this,
                "Tu ubicación: %.5f, %.5f (proveedor: %s)".format(
                    location.latitude, location.longitude, location.provider ?: "?"
                ),
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Aún no hay una ubicación reciente. Activa el GPS y espera unos segundos.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (hasLocationPermission()) {
            locationOverlay?.enableMyLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        locationOverlay?.disableMyLocation()
    }

    override fun onDestroy() {
        super.onDestroy()
        pendingViewportRefresh?.let { viewportRefreshHandler.removeCallbacks(it) }
        Configuration.getInstance()
            .save(applicationContext, getSharedPreferences(OSM_PREFS, MODE_PRIVATE))
    }

    private enum class ParkingFilter { ALL, LINEA, BATERIA }
    private enum class ColorFilter { ALL, VERDE, AZUL, MIXTA }

    private companion object {
        const val OSM_PREFS = "osm_prefs"
        const val ANIMATION_MS = 800L
        const val VIEWPORT_DEBOUNCE_MS = 250L
        const val MAX_VISIBLE_MARKERS = 350
        val MADRID_CENTER = GeoPoint(40.4168, -3.7038)
    }
}
