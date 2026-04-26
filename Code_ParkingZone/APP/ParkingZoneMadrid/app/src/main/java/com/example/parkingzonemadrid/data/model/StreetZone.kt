package com.example.parkingzonemadrid.data.model

/**
 * Representa una calle del Servicio de Estacionamiento Regulado (SER) de Madrid,
 * agregada a partir de todas las plazas individuales del CSV oficial.
 *
 * - Si la calle solo tiene plazas verdes  -> ZoneType.VERDE
 * - Si la calle solo tiene plazas azules  -> ZoneType.AZUL
 * - Si tiene plazas verdes y azules       -> ZoneType.MIXTA
 *
 * El [parkingType] indica si las plazas son en línea, en batería o mixto.
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
    val plazasOtras: Int = 0
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

    /** Tarifa resumida, tal como la presenta parking-madrid.es */
    val tarifaResumen: String
        get() = when (this) {
            VERDE -> "0,25-0,45 €/h (residentes)\nNo residentes: 2,85 €/h"
            AZUL -> "2,85 €/h (máx. 2h no residentes)"
            MIXTA -> "Azul: 2,85 €/h\nVerde: 0,25-0,45 €/h residentes"
            NARANJA -> "Tarifa especial ayuntamiento"
            ROJA -> "Uso restringido"
            ALTA_ROTACION -> "Alta rotación (máx. 1h)"
            OTRA -> "Consultar tarifa"
        }

    val horario: String
        get() = "L-V: 9:00-21:00 · Sáb: 9:00-15:00"
}

/**
 * Tipo de aparcamiento mayoritario en la calle.
 *
 * - LINEA: las plazas están alineadas en paralelo a la acera.
 * - BATERIA: perpendicular o en diagonal.
 * - MIXTO: la calle tiene tramos en línea y tramos en batería.
 */
enum class ParkingType(val displayName: String) {
    LINEA("Línea"),
    BATERIA("Batería"),
    MIXTO("Mixto"),
    OTRA("Otra")
}
