package com.example.parkingzonemadrid.model

import kotlin.random.Random

data class Usuario(
    // val(solo get) var -> get y set automáticos
    //public final  por defecto
    val id_usuario: Int,
    var nom_usuario: String,
    var correo: String,
    var password: String,
    var favoritos: MutableList<Favorito>

) {
    companion object {
        fun generarId(): Int = Random.nextInt(1, 1_000_000)
    }
}
