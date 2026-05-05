package com.example.parkingzonemadrid.model


data class Usuario(
    // val(solo get) var -> get y set automáticos
    //public final  por defecto
    val id_usuario: Int,
    var nom_usuario: String,
    var correo: String,
    var password: String,
    var favoritos: MutableList<Favorito>

)
