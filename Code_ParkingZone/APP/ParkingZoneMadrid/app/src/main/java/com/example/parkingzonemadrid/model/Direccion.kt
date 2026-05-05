package com.example.parkingzonemadrid.model

data class Direccion(
    // val(solo get) var -> get y set automáticos
    val id_direccion:Int,
    var tipo_via: String,
    var codigo_postal: Int,
    var calle: String,
    var num_finca: String,
    var cod_distrito:Int,
    var distrito: String,
    var cod_barrio: Int,
    var num_barrio: Int,
    var barrio: String,
    var bateria_linea: TipoAparcamiento

)
