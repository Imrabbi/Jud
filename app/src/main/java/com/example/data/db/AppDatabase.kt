package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ReminderCategory
import com.example.data.model.ReminderEntity

class Converters {
    @TypeConverter
    fun fromCategory(category: ReminderCategory?): String {
        return category?.name ?: ReminderCategory.REMINDER.name
    }

    @TypeConverter
    fun toCategory(value: String?): ReminderCategory {
        return try {
            if (value != null) ReminderCategory.valueOf(value) else ReminderCategory.REMINDER
        } catch (_: Exception) {
            ReminderCategory.REMINDER
        }
    }
}

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "jarvis_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
