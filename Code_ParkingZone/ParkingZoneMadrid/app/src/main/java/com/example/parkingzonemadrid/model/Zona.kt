package com.example.parkingzonemadrid.model

import kotlin.text.isNotBlank


data class Zona(
    // val(solo get) var -> get y set automáticos
    public final var id_zona: Int,
    public final  var gis_x: String,
    public final var gis_y: String,
    public final var color: TipoColor,
    public final var direcciones:  MutableList<Direccion>,
    public final var horarios:  MutableList<Horario>,
    public final var tarifas:  MutableList<Tarifa>

) {
    // CRUD de Horario y Tarifa
    fun consultarTarifa(id_horarioZona: Int) : Horario? {

        val horario_consultada = horarios.find { it.id_horario == id_horarioZona }

        if (horario_consultada != null)
            return horario_consultada
        else
            return null
    }
    fun consultarHorario(id_tarifaZona: Int) :Tarifa? {

        val tarifa_consultada = tarifas.find { it.id_tarifa == id_tarifaZona }

        if (tarifa_consultada != null)
            return tarifa_consultada
        else
            return null
    }
}
