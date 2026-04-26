package com.example.parkingzonemadrid.data.mapper

import com.example.parkingzonemadrid.data.model.ParkingType
import com.example.parkingzonemadrid.data.model.StreetZone
import com.example.parkingzonemadrid.data.model.ZoneType
import com.example.parkingzonemadrid.model.Direccion
import com.example.parkingzonemadrid.model.Horario
import com.example.parkingzonemadrid.model.Tarifa
import com.example.parkingzonemadrid.model.TipoAparcamiento
import com.example.parkingzonemadrid.model.TipoColor
import com.example.parkingzonemadrid.model.Zona

/**
 * Convierte el DTO de UI [StreetZone] (alimentado desde el CSV oficial) en el
 * modelo de dominio [Zona] que mantiene la compañera. Útil cuando, en el futuro,
 * se sustituya el CSV por una BBDD/servidor: la capa Vista seguirá hablando de
 * [Zona] y solo cambiará el origen.
 *
 * Reglas:
 *  - El color principal se elige por mayoría: si la calle es MIXTA se devuelve
 *    el color que más plazas tenga (azul ganaría en empate técnico, igual que
 *    suele ocurrir en los tramos azules del centro).
 *  - Se crea **una única** Direccion por calle (todas las plazas comparten
 *    distrito + nombre).
 *  - Se rellenan tarifas estándar SER y un horario base.
 */
object ZonaMapper {

    fun toZona(zone: StreetZone): Zona {
        val color = toTipoColor(zone.zoneType, zone.plazasVerde, zone.plazasAzul)
        val direccion = Direccion(
            id_direccion = Direccion.generarId(),
            tipo_via = inferTipoVia(zone.streetName),
            codigo_postal = 0,
            calle = zone.streetName,
            num_finca = "",
            cod_distrito = 0,
            distrito = zone.district,
            cod_barrio = 0,
            num_barrio = 0,
            barrio = "",
            bateria_linea = toTipoAparcamiento(zone.parkingType)
        )

        val tarifas = mutableListOf<Tarifa>()
        if (zone.plazasVerde > 0) {
            tarifas += Tarifa(Tarifa.generarId(), TipoColor.VERDE, 0.45f)
        }
        if (zone.plazasAzul > 0) {
            tarifas += Tarifa(Tarifa.generarId(), TipoColor.AZUL, 2.85f)
        }
        if (tarifas.isEmpty()) {
            tarifas += Tarifa(Tarifa.generarId(), color, 0f)
        }

        val horarios = mutableListOf(
            Horario(
                id_horario = Horario.generarId(),
                hora = "09:00-21:00",
                dias_semana = "L-V",
                detalles = "Servicio SER ordinario"
            ),
            Horario(
                id_horario = Horario.generarId(),
                hora = "09:00-15:00",
                dias_semana = "Sábado",
                detalles = "Servicio SER reducido"
            )
        )

        return Zona(
            id_zona = zone.zoneId,
            gis_x = zone.longitude.toString(),
            gis_y = zone.latitude.toString(),
            color = color,
            direcciones = mutableListOf(direccion),
            horarios = horarios,
            tarifas = tarifas
        )
    }

    fun toZonaList(zones: List<StreetZone>): List<Zona> = zones.map(::toZona)

    private fun toTipoColor(
        zoneType: ZoneType,
        plazasVerde: Int,
        plazasAzul: Int
    ): TipoColor = when (zoneType) {
        ZoneType.VERDE -> TipoColor.VERDE
        ZoneType.AZUL -> TipoColor.AZUL
        ZoneType.MIXTA -> if (plazasVerde >= plazasAzul) TipoColor.VERDE else TipoColor.AZUL
        ZoneType.NARANJA -> TipoColor.NARANJA
        ZoneType.ROJA -> TipoColor.ROJOBLANCO
        else -> TipoColor.AZUL
    }

    private fun toTipoAparcamiento(parkingType: ParkingType): TipoAparcamiento =
        when (parkingType) {
            ParkingType.LINEA -> TipoAparcamiento.LINEA
            ParkingType.BATERIA -> TipoAparcamiento.BATERIA
            // En el modelo de la compañera no hay MIXTO; lo mapeamos a LINEA
            // (es el más frecuente en Madrid) para no romper su clase.
            ParkingType.MIXTO -> TipoAparcamiento.LINEA
            ParkingType.OTRA -> TipoAparcamiento.LINEA
        }

    /**
     * El nombre llega ya formateado desde [com.example.parkingzonemadrid.data.ParkingZonesData]
     * con la forma `CALLE DE LAS AGUAS`. La primera palabra es siempre el tipo de vía.
     */
    private fun inferTipoVia(streetName: String): String {
        val first = streetName.trim().substringBefore(' ', missingDelimiterValue = "")
        return first.ifBlank { "CALLE" }
    }
}
