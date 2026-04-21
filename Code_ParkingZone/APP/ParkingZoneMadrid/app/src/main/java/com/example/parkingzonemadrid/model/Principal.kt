package com.example.parkingzonemadrid.model

class Principal {
    var usuarios:  MutableList<Usuario> = mutableListOf()
    var zonas:  MutableList<Zona> = mutableListOf()
    var direcciones:  MutableList<Direccion> = mutableListOf()


    // metodos CRUD de Usuario
    fun crearUsuario(nom_usuario:String, correo:String, password: String){
        // id automatico
        val id_usuario = Usuario.generarId()

        val nuevo_usuario = Usuario(id_usuario, nom_usuario, correo, password, mutableListOf())
        usuarios.add(nuevo_usuario)
    }


    fun consultarUsuario(id_usuario: Int): Usuario? {
        return usuarios.find { it.id_usuario == id_usuario }
    }


    fun modificarUsuario(id_usuario_modificar: Int, nuevo_nombre: String, nuevo_correo: String, nueva_password: String): Boolean {
        val usuario = usuarios.find { it.id_usuario == id_usuario_modificar }

        return if (usuario != null) {
            usuario.nom_usuario = nuevo_nombre
            usuario.correo = nuevo_correo
            usuario.password = nueva_password
            true
        } else {
            false
        }
    }


    fun eliminarUsuario(id_usuario_eliminar: Int): Boolean {
        return usuarios.removeIf { it.id_usuario == id_usuario_eliminar }
    }



}