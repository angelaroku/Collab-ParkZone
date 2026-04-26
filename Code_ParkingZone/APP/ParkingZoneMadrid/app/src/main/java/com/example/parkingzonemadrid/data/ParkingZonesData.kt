package com.example.parkingzonemadrid.data

import android.content.Context
import com.example.parkingzonemadrid.data.model.ParkingType
import com.example.parkingzonemadrid.data.model.StreetZone
import com.example.parkingzonemadrid.data.model.ZoneType
import kotlin.math.abs
import kotlin.math.pow

/**
 * Lee el CSV oficial `calles_SER_2026.csv` (fuente: Ayuntamiento de Madrid) y lo
 * convierte en una lista de [StreetZone] lista para pintar en el mapa.
 *
 * Cada fila del CSV es UNA PLAZA individual. Aquí las agrupamos por
 * (distrito, calle) y calculamos:
 *  - el tipo de zona (Verde, Azul, Mixta, ...) en base a los colores presentes,
 *  - el tipo de aparcamiento (Línea, Batería, Mixto) en base a la columna
 *    `bateria_linea`.
 *
 * Estructura del CSV (separador `;`):
 *  0 gis_x · 1 gis_y · 2 cod_distrito · 3 distrito · 4 cod_barrio · 5 num_barrio
 *  6 barrio · 7 calle · 8 numero_finca · 9 color · 10 bateria_linea · 11 numero_plazas
 */
object ParkingZonesData {

    private const val CSV_ASSET_NAME = "calles_SER_2026.csv"
    private const val MAX_ZONES_TO_RENDER = 1500

    @Volatile
    private var cachedZones: List<StreetZone>? = null

    fun getStreetZones(context: Context): List<StreetZone> {
        cachedZones?.let { return it }
        val parsed = parseCsvAndAggregateByStreet(context)
        cachedZones = parsed
        return parsed
    }

    private fun parseCsvAndAggregateByStreet(context: Context): List<StreetZone> {
        val grouped = linkedMapOf<String, MutableStreetSummary>()

        try {
            context.assets.open(CSV_ASSET_NAME).bufferedReader().use { reader ->
                reader.readLine() // cabecera

                reader.forEachLine { rawLine ->
                    if (rawLine.isBlank()) return@forEachLine
                    val parts = rawLine.split(';')
                    if (parts.size < 12) return@forEachLine

                    val x = parts[0].toDoubleOrNull() ?: return@forEachLine
                    val y = parts[1].toDoubleOrNull() ?: return@forEachLine
                    val district = parts[3].trim().ifBlank { "SIN DISTRITO" }
                    val rawStreet = parts[7].trim().ifBlank { "SIN CALLE" }
                    val colorRaw = parts[9].trim().lowercase()
                    val parkingRaw = parts[10].trim().lowercase()
                    val plazas = parts[11].toIntOrNull() ?: 0

                    val key = "$district|$rawStreet"
                    val summary = grouped.getOrPut(key) {
                        MutableStreetSummary(district = district, rawStreet = rawStreet)
                    }
                    summary.countRows++
                    summary.sumX += x
                    summary.sumY += y

                    when {
                        "verde" in colorRaw -> summary.plazasVerde += plazas
                        "azul" in colorRaw -> summary.plazasAzul += plazas
                        "naranja" in colorRaw -> {
                            summary.plazasOtras += plazas
                            summary.hasNaranja = true
                        }
                        "rojo" in colorRaw -> {
                            summary.plazasOtras += plazas
                            summary.hasRojo = true
                        }
                        "alta" in colorRaw -> {
                            summary.plazasOtras += plazas
                            summary.hasAltaRotacion = true
                        }
                        else -> summary.plazasOtras += plazas
                    }

                    when {
                        "línea" in parkingRaw || "linea" in parkingRaw -> summary.countLinea++
                        "batería" in parkingRaw || "bateria" in parkingRaw -> summary.countBateria++
                        else -> summary.countOtherParking++
                    }
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }

        return grouped.values
            .asSequence()
            .filter { it.countRows > 0 }
            .map { summary ->
                val avgX = summary.sumX / summary.countRows
                val avgY = summary.sumY / summary.countRows
                val geo = Utm30NConverter.toWgs84(avgX, avgY)
                val stableKey = "${summary.district}_${summary.rawStreet}"
                val zoneType = resolveZoneType(summary)
                val parkingType = resolveParkingType(summary)
                val total = summary.plazasVerde + summary.plazasAzul + summary.plazasOtras

                StreetZone(
                    zoneId = abs(stableKey.hashCode()),
                    district = summary.district,
                    streetName = formatStreetName(summary.rawStreet),
                    zoneType = zoneType,
                    parkingType = parkingType,
                    latitude = geo.latitude,
                    longitude = geo.longitude,
                    totalPlazas = total,
                    plazasVerde = summary.plazasVerde,
                    plazasAzul = summary.plazasAzul,
                    plazasOtras = summary.plazasOtras
                )
            }
            .sortedByDescending { it.totalPlazas }
            .take(MAX_ZONES_TO_RENDER)
            .toList()
    }

    /**
     * El CSV trae los nombres en formato `NOMBRE, TIPO_VIA, NEXO`, por ejemplo
     * `AGUAS, CALLE, DE LAS`. Lo convertimos al orden natural en español:
     * `CALLE DE LAS AGUAS`.
     */
    private fun formatStreetName(rawStreet: String): String {
        val parts = rawStreet.split(',').map { it.trim() }.filter { it.isNotBlank() }
        return when (parts.size) {
            0 -> rawStreet
            1 -> parts[0]
            2 -> "${parts[1]} ${parts[0]}".trim()
            else -> "${parts[1]} ${parts.drop(2).joinToString(" ")} ${parts[0]}"
                .replace("  ", " ")
                .trim()
        }
    }

    private fun resolveZoneType(summary: MutableStreetSummary): ZoneType {
        val verde = summary.plazasVerde > 0
        val azul = summary.plazasAzul > 0
        return when {
            verde && azul -> ZoneType.MIXTA
            verde -> ZoneType.VERDE
            azul -> ZoneType.AZUL
            summary.hasNaranja -> ZoneType.NARANJA
            summary.hasRojo -> ZoneType.ROJA
            summary.hasAltaRotacion -> ZoneType.ALTA_ROTACION
            else -> ZoneType.OTRA
        }
    }

    private fun resolveParkingType(summary: MutableStreetSummary): ParkingType {
        val linea = summary.countLinea > 0
        val bateria = summary.countBateria > 0
        return when {
            linea && bateria -> ParkingType.MIXTO
            linea -> ParkingType.LINEA
            bateria -> ParkingType.BATERIA
            else -> ParkingType.OTRA
        }
    }
}

private class MutableStreetSummary(
    val district: String,
    val rawStreet: String,
    var countRows: Int = 0,
    var plazasVerde: Int = 0,
    var plazasAzul: Int = 0,
    var plazasOtras: Int = 0,
    var hasNaranja: Boolean = false,
    var hasRojo: Boolean = false,
    var hasAltaRotacion: Boolean = false,
    var countLinea: Int = 0,
    var countBateria: Int = 0,
    var countOtherParking: Int = 0,
    var sumX: Double = 0.0,
    var sumY: Double = 0.0
)

private data class LatLngPoint(val latitude: Double, val longitude: Double)

/**
 * Conversión aproximada UTM 30N -> WGS84 (lat/lon).
 */
private object Utm30NConverter {
    private const val A = 6378137.0
    private const val E = 0.081819190842622
    private const val K0 = 0.9996
    private const val ZONE_NUMBER = 30

    fun toWgs84(easting: Double, northing: Double): LatLngPoint {
        val e1 = (1 - kotlin.math.sqrt(1 - E * E)) / (1 + kotlin.math.sqrt(1 - E * E))
        val x = easting - 500000.0
        val y = northing

        val m = y / K0
        val mu = m / (A * (1 - (E * E) / 4.0 - 3 * E.pow(4.0) / 64.0 - 5 * E.pow(6.0) / 256.0))

        val j1 = 3 * e1 / 2 - 27 * e1.pow(3.0) / 32.0
        val j2 = 21 * e1.pow(2.0) / 16 - 55 * e1.pow(4.0) / 32.0
        val j3 = 151 * e1.pow(3.0) / 96.0
        val j4 = 1097 * e1.pow(4.0) / 512.0

        val fp = mu +
            j1 * kotlin.math.sin(2 * mu) +
            j2 * kotlin.math.sin(4 * mu) +
            j3 * kotlin.math.sin(6 * mu) +
            j4 * kotlin.math.sin(8 * mu)

        val eSq = E * E / (1 - E * E)
        val c1 = eSq * kotlin.math.cos(fp).pow(2.0)
        val t1 = kotlin.math.tan(fp).pow(2.0)
        val r1 = A * (1 - E * E) / (1 - E * E * kotlin.math.sin(fp).pow(2.0)).pow(1.5)
        val n1 = A / kotlin.math.sqrt(1 - E * E * kotlin.math.sin(fp).pow(2.0))
        val d = x / (n1 * K0)

        val q1 = n1 * kotlin.math.tan(fp) / r1
        val q2 = d * d / 2.0
        val q3 = (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * eSq) * d.pow(4.0) / 24.0
        val q4 = (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * eSq - 3 * c1 * c1) * d.pow(6.0) / 720.0

        val latRad = fp - q1 * (q2 - q3 + q4)
        val q5 = d
        val q6 = (1 + 2 * t1 + c1) * d.pow(3.0) / 6.0
        val q7 = (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * eSq + 24 * t1 * t1) * d.pow(5.0) / 120.0
        val lonRad = (q5 - q6 + q7) / kotlin.math.cos(fp)

        val lonOrigin = (ZONE_NUMBER - 1) * 6 - 180 + 3
        val lat = Math.toDegrees(latRad)
        val lon = lonOrigin + Math.toDegrees(lonRad)
        return LatLngPoint(latitude = lat, longitude = lon)
    }
}
