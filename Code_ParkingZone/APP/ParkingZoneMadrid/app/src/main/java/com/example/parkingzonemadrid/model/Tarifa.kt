package com.example.parkingzonemadrid.model

data class Tarifa(
    // val(solo get) var -> get y set automáticos
    val id_tarifa: Int,
    var color: TipoColor,
    var precio: Float
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
