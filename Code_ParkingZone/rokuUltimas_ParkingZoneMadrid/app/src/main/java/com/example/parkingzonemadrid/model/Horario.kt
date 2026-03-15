package com.example.parkingzonemadrid.model

data class Horario(
    // val(solo get) var -> get y set automáticos
    var id_horario:Int,
    var hora:String,
    var dias_semana:String,
    var detalles: String
)
