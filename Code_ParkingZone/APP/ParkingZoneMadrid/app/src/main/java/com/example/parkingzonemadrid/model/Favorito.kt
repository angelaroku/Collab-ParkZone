package com.example.parkingzonemadrid.model

data class Favorito(
    // val(solo get) var -> get y set automáticos
    public final var id_favorito: Int,
    public final var nombre_favorito: String,
    //elementos que relacionan con otras clases
    // y se agregan al crearse desde la app
    public final var id_usuario: Int,
    public final var  id_direccion:Int

)
