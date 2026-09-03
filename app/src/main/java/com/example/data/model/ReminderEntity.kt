package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderCategory {
    REMINDER,
    SCHEDULE,
    MEETING,
    CALL,
    TASK,
    SETTING
}

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val details: String = "",
    val timeString: String = "",
    val category: ReminderCategory = ReminderCategory.REMINDER,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
