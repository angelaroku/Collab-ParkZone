package com.example.parkingzonemadrid.data.parser

data class ColumnasCsvSER(
    val gis_x: String,
    val gis_y: String,
    val cod_distrito: Int,
    val distrito: String,
    val cod_barrio: Int,
    val num_barrio: Int,
    val barrio: String,
    val calle: String,
    val numero_finca: String,
    val color: String,
    val bateria_linea: String,
    val numero_plazas: String
)