package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AppRuleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScreenDeckDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenDeckDatabase? = null

        fun getInstance(context: Context): ScreenDeckDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenDeckDatabase::class.java,
                    "screendeck_rules.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
