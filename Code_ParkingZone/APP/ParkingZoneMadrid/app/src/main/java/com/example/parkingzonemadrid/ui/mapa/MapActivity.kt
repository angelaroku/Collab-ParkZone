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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
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
import com.example.parkingzonemadrid.ui.login.LoginActivity
import com.example.parkingzonemadrid.ui.mapa.adapters.FavoritesAdapter
import com.example.parkingzonemadrid.ui.mapa.components.StreetInfoWindow
import com.example.parkingzonemadrid.utils.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.views.overlay.MapEventsOverlay
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
    private var selectedStreetZone: StreetZone? = null
    private var ignoreMapEventsUntilMs = 0L
    private var skipLocationPromptOnNextResume = false

    private val viewportRefreshHandler = Handler(Looper.getMainLooper())
    private var pendingViewportRefresh: Runnable? = null

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            refreshUserSession()
        }
    }

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

        mapView.overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                closeStreetInfo()
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }))

        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                if (System.currentTimeMillis() < ignoreMapEventsUntilMs) return false
                scheduleViewportRefresh()
                return false
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                if (System.currentTimeMillis() < ignoreMapEventsUntilMs) return false
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
        skipLocationPromptOnNextResume = true
        ensureLocationPermissionAskIfNeeded()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_map, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val loggedIn = prefsManager.getUser() != null
        menu.findItem(R.id.action_sign_in)?.isVisible = !loggedIn
        menu.findItem(R.id.action_sign_out)?.isVisible = loggedIn
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_my_location -> {
                requestLocationOrFallback(centerAfterPermission = true)
                return true
            }
            R.id.action_sign_in -> {
                loginLauncher.launch(Intent(this, LoginActivity::class.java))
                return true
            }
            R.id.action_sign_out -> {
                prefsManager.clearUser()
                currentEmail = null
                favoriteZoneIds = emptySet()
                refreshFavoritesList()
                invalidateOptionsMenu()
                Toast.makeText(this, R.string.session_closed, Toast.LENGTH_SHORT).show()
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

    private fun refreshUserSession() {
        currentEmail = prefsManager.getUser()?.correo
        lifecycleScope.launch(Dispatchers.IO) {
            favoriteZoneIds = currentEmail?.let { repository.getFavoriteZoneIds(it) } ?: emptySet()
            withContext(Dispatchers.Main) {
                refreshFavoritesList()
                invalidateOptionsMenu()
                prefsManager.getUser()?.nom_usuario?.let { name ->
                    Toast.makeText(
                        this@MapActivity,
                        getString(R.string.session_welcome, name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                invalidateOptionsMenu()
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
     * Desde favoritos: centra la calle, recarga pines del área y abre el recuadro resumen
     * (con botón Cómo llegar en Google Maps).
     */
    private fun focusOnZone(zone: StreetZone) {
        drawerLayout.closeDrawer(GravityCompat.START)
        val point = GeoPoint(zone.latitude, zone.longitude)
        pendingViewportRefresh?.let { viewportRefreshHandler.removeCallbacks(it) }
        pendingViewportRefresh = null

        selectedStreetZone = zone
        ignoreMapEventsUntilMs = System.currentTimeMillis() + ANIMATION_MS + 500L
        mapView.controller.animateTo(point, 17.0, ANIMATION_MS)

        viewportRefreshHandler.postDelayed({
            renderViewport()
            revealStreetInfo(zone, point)
        }, ANIMATION_MS + 200L)
    }

    private fun revealStreetInfo(zone: StreetZone, point: GeoPoint) {
        selectedStreetZone = zone
        infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
        val marker = findMarkerForZone(zone)
        if (marker != null) {
            marker.showInfoWindow()
            return
        }
        val temp = Marker(mapView).apply {
            position = point
            relatedObject = zone
            icon = ContextCompat.getDrawable(this@MapActivity, iconResFor(zone.zoneType))
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            infoWindow = this@MapActivity.infoWindow
            setOnMarkerClickListener { clicked, map ->
                openStreetInfo(zone, clicked, map)
                true
            }
        }
        mapView.overlays.add(temp)
        mapView.invalidate()
        temp.showInfoWindow()
    }

    private fun findMarkerForZone(zone: StreetZone): Marker? =
        mapView.overlays.filterIsInstance<Marker>()
            .firstOrNull { (it.relatedObject as? StreetZone)?.zoneId == zone.zoneId }

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
        val preserveZone = selectedStreetZone
        mapView.overlays.removeAll { it is Marker }

        if (preserveZone == null || zones.none { it.zoneId == preserveZone.zoneId }) {
            closeStreetInfo()
        }

        val activity = this
        var markerToReopen: Marker? = null
        zones.forEach { zone ->
            val marker = Marker(mapView)
            marker.position = GeoPoint(zone.latitude, zone.longitude)
            marker.title = zone.streetName
            marker.icon = ContextCompat.getDrawable(activity, iconResFor(zone.zoneType))
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.relatedObject = zone
            marker.infoWindow = infoWindow
            marker.setOnMarkerClickListener { clickedMarker, map ->
                openStreetInfo(zone, clickedMarker, map)
                true
            }
            mapView.overlays.add(marker)
            if (preserveZone?.zoneId == zone.zoneId) {
                markerToReopen = marker
            }
        }

        preserveZone?.let { zone ->
            markerToReopen?.let { marker ->
                infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
                marker.showInfoWindow()
            }
        }

        locationOverlay?.let { overlay ->
            mapView.overlays.remove(overlay)
            mapView.overlays.add(overlay)
        }
        mapView.invalidate()
    }

    private fun openStreetInfo(zone: StreetZone, marker: Marker, map: MapView) {
        pendingViewportRefresh?.let { viewportRefreshHandler.removeCallbacks(it) }
        pendingViewportRefresh = null

        selectedStreetZone = zone
        infoWindow.bind(zone, favoriteZoneIds.contains(zone.zoneId))
        marker.showInfoWindow()

        ignoreMapEventsUntilMs = System.currentTimeMillis() + ANIMATION_MS + 400L
        map.controller.animateTo(marker.position, map.zoomLevelDouble, ANIMATION_MS)
    }

    private fun closeStreetInfo() {
        selectedStreetZone = null
        infoWindow.close()
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
            Toast.makeText(this, R.string.favorites_need_login, Toast.LENGTH_SHORT).show()
            loginLauncher.launch(Intent(this, LoginActivity::class.java))
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
                if (selectedStreetZone?.zoneId == zone.zoneId) {
                    selectedStreetZone = zone
                }
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

    /**
     * Si no hay permiso de ubicación, muestra un diálogo y lanza el sistema de permisos.
     * Se llama al abrir el mapa y en cada [onResume] mientras siga sin concederse.
     */
    private fun ensureLocationPermissionAskIfNeeded(centerAfterGrant: Boolean = false) {
        if (hasLocationPermission()) {
            enableMyLocation()
            if (centerAfterGrant) centerOnLastKnownLocation()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.location_permission_title)
            .setMessage(R.string.location_permission_message)
            .setPositiveButton(R.string.location_permission_allow) { _, _ ->
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_LONG).show()
            }
            .setCancelable(true)
            .show()
    }

    private fun requestLocationOrFallback(centerAfterPermission: Boolean = false) {
        if (hasLocationPermission()) {
            enableMyLocation()
            if (centerAfterPermission) centerOnLastKnownLocation()
        } else {
            ensureLocationPermissionAskIfNeeded(centerAfterGrant = centerAfterPermission)
        }
    }

    @Suppress("DEPRECATION") // osmdroid 6.1.18 aún expone setDirectionArrow como deprecado pero sin API sustituta estable
    private fun enableMyLocation() {
        if (locationOverlay != null) return
        val d = resources.displayMetrics.density
        val dotPx = (48 * d).toInt().coerceAtLeast(1)
        val arrowW = (32 * d).toInt().coerceAtLeast(1)
        val arrowH = (40 * d).toInt().coerceAtLeast(1)

        val personBmp = ContextCompat.getDrawable(this, R.drawable.ic_my_location_dot)!!
            .toBitmap(dotPx, dotPx)
        val directionBmp = ContextCompat.getDrawable(this, R.drawable.ic_my_location_direction)!!
            .toBitmap(arrowW, arrowH)

        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView).apply {
            // Sustituye el pin/gráficos por defecto (triángulo blanco poco visible)
            // por un círculo azul con borde y una flecha con contorno.
            setPersonIcon(personBmp)
            setDirectionArrow(personBmp, directionBmp)
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
        if (skipLocationPromptOnNextResume) {
            skipLocationPromptOnNextResume = false
            if (hasLocationPermission()) {
                locationOverlay?.enableMyLocation()
            }
            return
        }
        if (hasLocationPermission()) {
            locationOverlay?.enableMyLocation()
        } else {
            ensureLocationPermissionAskIfNeeded()
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
