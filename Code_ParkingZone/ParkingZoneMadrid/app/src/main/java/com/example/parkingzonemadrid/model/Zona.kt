package com.example.parkingzonemadrid.model

import kotlin.text.isNotBlank

data class Zona(
    val id_zona: Int,
    val gis_x: String,
    val gis_y: String,
    val color: TipoColor,
    val direcciones: Array<Direccion>
) {





    //Fun equals y hashCode: se autogenera por el array de favoritos

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Zona

        if (id_zona != other.id_zona) return false
        if (gis_x != other.gis_x) return false
        if (gis_x != other.gis_x) return false
        if (color != other.color) return false
        if (!direcciones.contentEquals(other.direcciones)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id_zona
        result = 31 * result + gis_x.hashCode()
        result = 31 * result + gis_x.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + direcciones.contentHashCode()
        return result
    }
}
