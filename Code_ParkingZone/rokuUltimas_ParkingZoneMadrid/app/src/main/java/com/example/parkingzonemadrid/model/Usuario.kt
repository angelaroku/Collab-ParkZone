package com.example.parkingzonemadrid.model

import kotlin.Int


data class Usuario(
    // val(solo get) var -> get y set automáticos
    public final  var id_usuario: Int,
    public final  var nom_usuario: String,
    public final  var correo: String,
    public final  var password: String,
    public final  var favoritos: MutableList<Favorito>

) {
    // metodos CRUD de Favorito (desde clase Usuario)

    fun crearFavorito(nombre_favorito: String ){
        //se agrega un id el numero de elementos de la lista de favoritos del usuario
        val id_favorito = favoritos.size+1

        val favorito = Favorito(id_favorito,nombre_favorito)
        favoritos.add(favorito)
    }

    fun consultarFavorito(id_favorito_seleccionado: Int) : Favorito?{

        val favorito_seleccionado = favoritos.find { it.id_favorito == id_favorito_seleccionado }

        if (favorito_seleccionado != null)
            return favorito_seleccionado
        else
            return null

    }

    fun modificar (id_favorito: Int, nuevo_nombre_favorito: String) : Boolean {

        val favorito = favoritos.find { it.id_favorito == id_favorito }

        return if (favorito != null) {
            favorito.nombre_favorito = nuevo_nombre_favorito
            true
        } else{
            false
        }
    }

    fun eliminar(id_favorito: Int) : Boolean{
        return favoritos.removeIf { it.id_favorito == id_favorito }
    }


}
