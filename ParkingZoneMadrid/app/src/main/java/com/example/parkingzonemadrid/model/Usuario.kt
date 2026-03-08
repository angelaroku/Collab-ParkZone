package com.example.parkingzonemadrid.model

data class Usuario(
    val id_usuario: Int,
    val nom_usuario: String,
    val correo: String,
    val password: String,
    val favorito: Array<Favorito>

) {
    //Fun equals y hashCode: Autogenerado por el array de favoritos

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Usuario

        if (id_usuario != other.id_usuario) return false
        if (nom_usuario != other.nom_usuario) return false
        if (correo != other.correo) return false
        if (password != other.password) return false
        if (!favorito.contentEquals(other.favorito)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id_usuario
        result = 31 * result + nom_usuario.hashCode()
        result = 31 * result + correo.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + favorito.contentHashCode()
        return result
    }
}
