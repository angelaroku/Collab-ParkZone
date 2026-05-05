package com.example.parkingzonemadrid.model

import kotlin.text.isNotBlank

data class Zona(
    // val(solo get) var -> get y set automáticos
    //public final  por defecto
    val id_zona: Int,
    var gis_x: String,
    var gis_y: String,
    var color: TipoColor,
    var direcciones:  MutableList<Direccion>,
    var horarios:  MutableList<Horario>,
    var tarifas:  MutableList<Tarifa>

)