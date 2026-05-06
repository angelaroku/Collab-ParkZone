package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.parkingzonemadrid.model.TipoColor

@Entity(tableName = "zonas")
data class ZonaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_zona")
    val id_zona: Int = 0,

    @ColumnInfo(name = "gis_x")
    val gis_x: String,

    @ColumnInfo(name = "gis_y")
    val gis_y: String,

    @ColumnInfo(name = "color_zona")
    val color: TipoColor
)
