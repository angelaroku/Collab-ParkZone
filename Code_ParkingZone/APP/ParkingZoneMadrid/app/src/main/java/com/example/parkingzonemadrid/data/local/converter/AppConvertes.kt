package com.example.parkingzonemadrid.data.local.converter

import androidx.room.TypeConverter
import com.example.parkingzonemadrid.model.TipoColor
import com.example.parkingzonemadrid.model.TipoAparcamiento

class AppConverters {

    //  Converters para TipoColor
    @TypeConverter
    fun fromTipoColor(color: TipoColor): String {
        return color.name // Guarda "AZUL", "VERDE", etc.
    }

    @TypeConverter
    fun toTipoColor(colorStr: String): TipoColor {
        return TipoColor.valueOf(colorStr) // Convierte el texto de vuelta al Enum
    }

    //  Converters para TipoAparcamiento
    @TypeConverter
    fun fromTipoAparcamiento(tipo: TipoAparcamiento): String {
        return tipo.name // Guarda "BATERIA" o "LINEA"
    }

    @TypeConverter
    fun toTipoAparcamiento(tipoStr: String): TipoAparcamiento {
        return TipoAparcamiento.valueOf(tipoStr)
    }
}