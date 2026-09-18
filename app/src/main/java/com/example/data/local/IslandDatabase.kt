package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AutomationRule
import com.example.data.model.IslandProfile
import com.example.data.model.ManagedApp
import com.example.data.model.SecurityLog

@Database(
    entities = [
        IslandProfile::class,
        ManagedApp::class,
        AutomationRule::class,
        SecurityLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IslandDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: IslandDatabase? = null

        fun getInstance(context: Context): IslandDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IslandDatabase::class.java,
                    "island_pro_max.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
