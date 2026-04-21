package com.example.parkingzonemadrid.model

data class Horario(
    // val(solo get) var -> get y set automáticos
    val id_horario:Int,
    var hora:String,
    var dias_semana:String,
    var detalles: String
) {
    //uso de companion object para autogenerar los id's
    companion object{
        private var contador = 0

        fun generarId(): Int{
            contador ++
            return contador
        }
    }
}
