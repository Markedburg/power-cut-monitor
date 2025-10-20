package com.powercuts.powercutmonitor.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.powercuts.powercutmonitor.data.db.dao.EventDao
import com.powercuts.powercutmonitor.data.db.entities.PowerEvent
import com.powercuts.powercutmonitor.util.Constants

/**
 * Room database for Power Cuts Log
 */
@Database(
    entities = [PowerEvent::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun eventDao(): EventDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For MVP, allow destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

