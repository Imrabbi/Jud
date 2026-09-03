package com.example.data.repository

import com.example.data.db.ReminderDao
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

class ReminderRepository(private val reminderDao: ReminderDao) {
    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()
    val pendingReminders: Flow<List<ReminderEntity>> = reminderDao.getPendingReminders()

    suspend fun insert(reminder: ReminderEntity): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun update(reminder: ReminderEntity) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun toggleCompleted(id: Long, isCompleted: Boolean) {
        reminderDao.setCompleted(id, isCompleted)
    }

    suspend fun delete(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteById(id: Long) {
        reminderDao.deleteById(id)
    }
}
