package com.example.parkingzonemadrid.model

import kotlin.Int


data class Usuario(
    // val(solo get) var -> get y set automáticos
    //public final  por defecto
    val id_usuario: Int,
    var nom_usuario: String,
    var correo: String,
    var password: String,
    var favoritos: MutableList<Favorito>

) {
    //uso de companion object para autogenerar los id's
    companion object{
        private var contador = 0

        fun generarId(): Int{
            contador ++
            return contador
        }
    }

    // metodos CRUD de Favorito (desde clase Usuario)
    fun crearFavorito(nombre_favorito: String, id_direccion:Int){
        //se agrega un id el numero de elementos de la lista de favoritos del usuario
        val id_favorito = if ( favoritos. isEmpty()){
            1
        }else{
            favoritos.maxOf { it.id_favorito } + 1
        }

        val nuevo_favorito = Favorito(id_favorito,nombre_favorito,this.id_usuario, id_direccion)
        favoritos.add(nuevo_favorito)
    }

    fun consultarFavorito(id_favorito_consultar: Int) : Favorito?{
        return favoritos.find { it.id_favorito == id_favorito_consultar }

    }

    fun modificarFavorito(id_favorito_modificar: Int, nuevo_nombre_favorito: String) : Boolean {

        val favorito = favoritos.find { it.id_favorito == id_favorito_modificar }

        return if (favorito != null) {
            favorito.nombre_favorito = nuevo_nombre_favorito
            true
        } else{
            false
        }
    }

    fun eliminarFavorito(id_favorito_eliminar: Int) : Boolean{
        return favoritos.removeIf { it.id_favorito == id_favorito_eliminar }
    }


}
