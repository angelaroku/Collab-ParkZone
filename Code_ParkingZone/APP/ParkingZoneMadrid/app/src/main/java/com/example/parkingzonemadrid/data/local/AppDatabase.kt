package com.example.parkingzonemadrid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.parkingzonemadrid.data.local.dao.FavoriteDao
import com.example.parkingzonemadrid.data.local.dao.UserDao
import com.example.parkingzonemadrid.data.local.entity.FavoriteEntity
import com.example.parkingzonemadrid.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "parkingzone_madrid.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

