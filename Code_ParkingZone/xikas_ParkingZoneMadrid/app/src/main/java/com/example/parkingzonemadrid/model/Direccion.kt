package com.example.parkingzonemadrid.model

data class Direccion(
    val id_direccion:Int,
    val tipo_via: String,
    val codigo_postal: Int,
    val calle: String,
    val num_finca: String,
    val cod_distrito:Int,
    val distrito: String,
    val cod_barrio: Int,
    val num_barrio: Int,
    val barrio: String,
    val bateria_linea: TipoAparcamiento

)
