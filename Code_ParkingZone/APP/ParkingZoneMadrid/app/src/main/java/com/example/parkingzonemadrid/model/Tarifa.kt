package com.example.parkingzonemadrid.model

data class Tarifa(
    // val(solo get) var -> get y set automáticos
    val id_tarifa: Int,
    var color: TipoColor,
    var precio: Float
)