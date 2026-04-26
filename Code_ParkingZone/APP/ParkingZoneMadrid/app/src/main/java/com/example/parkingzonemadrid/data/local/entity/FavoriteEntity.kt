package com.example.parkingzonemadrid.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "favorite_zones",
    primaryKeys = ["user_email", "zone_id"]
)
data class FavoriteEntity(
    @ColumnInfo(name = "user_email")
    val userEmail: String,

    @ColumnInfo(name = "zone_id")
    val zoneId: Int
)

