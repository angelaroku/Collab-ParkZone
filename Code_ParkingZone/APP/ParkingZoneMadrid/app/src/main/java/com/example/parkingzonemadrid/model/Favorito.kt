package com.example.parkingzonemadrid.model

data class Favorito(
    // val(solo get) var -> get y set automáticos
    val id_favorito: Int,
    var nombre_favorito: String,
    //elementos que relacionan con otras clases
    // y se agregan al crearse un Fav desde la app
    var id_usuario: Int, var  id_direccion:Int

)
