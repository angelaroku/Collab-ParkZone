package com.example.parkingzonemadrid.data.mapa

/**
 * Representa una calle del Servicio de Estacionamiento Regulado (SER) de Madrid,
 * agregada a partir de todas las plazas individuales del CSV oficial.
 *
 * - Si la calle solo tiene plazas verdes  -> ZoneType.VERDE
 * - Si la calle solo tiene plazas azules  -> ZoneType.AZUL
 * - Si tiene plazas verdes y azules       -> ZoneType.MIXTA (no aparece como tal en el CSV,
 *   pero surge al agregar plazas con colores distintos en una misma calle)
 *
 * Aparcamiento: el CSV solo distingue "Línea" o "Batería" por plaza individual.
 * Una misma calle puede tener filas con valores distintos, así que guardamos los
 * dos flags [hasLinea] y [hasBateria] para poder filtrar por presencia.
 */
data class StreetZone(
    val zoneId: Int,
    val district: String,
    val streetName: String,
    val zoneType: ZoneType,
    val parkingType: ParkingType,
    val latitude: Double,
    val longitude: Double,
    val totalPlazas: Int,
    val plazasVerde: Int = 0,
    val plazasAzul: Int = 0,
    val plazasOtras: Int = 0,
    val hasLinea: Boolean = false,
    val hasBateria: Boolean = false
)

enum class ZoneType(
    val displayName: String,
    val shortLabel: String
) {
    VERDE("Verde", "Verde"),
    AZUL("Azul", "Azul"),
    MIXTA("Mixta (Azul + Verde)", "Mixta"),
    NARANJA("Naranja", "Naranja"),
    ROJA("Roja", "Roja"),
    ALTA_ROTACION("Alta Rotación", "Alta Rotación"),
    OTRA("Otra", "Otra");

    /**
     * Tarifa orientativa publicada por parking-madrid.es / Ayto. Madrid.
     * Las cifras son aproximadas (variables según vehículo y zona); deben verificarse
     * contra la web oficial del Ayuntamiento.
     */
    val tarifaResumen: String
        get() = when (this) {
            VERDE -> "Residentes: 0,20 €/día (con autorización)\n" +
                "No residentes: 1,55-4,55 €/h (máx. 1h)"
            AZUL -> "0,75-4,15 €/h (máx. 2h)"
            MIXTA -> "Azul: 0,75-4,15 €/h\nVerde: 1,55-4,55 €/h (no residentes)"
            NARANJA -> "Tarifa especial ayuntamiento"
            ROJA -> "Uso restringido"
            ALTA_ROTACION -> "Alta rotación (máx. 1h)"
            OTRA -> "Consultar tarifa"
        }

    val horario: String
        get() = "L-V: 9:00-21:00 · Sáb: 9:00-15:00"
}

/**
 * Tipo de aparcamiento mayoritario de la calle (resumen visual).
 * Para filtrar conviene usar [StreetZone.hasLinea] / [StreetZone.hasBateria], porque
 * el CSV no marca "Mixto" como categoría: aparece al sumar filas distintas.
 */
enum class ParkingType(val displayName: String) {
    LINEA("Línea"),
    BATERIA("Batería"),
    MIXTO("Mixto"),
    OTRA("Otra")
}
