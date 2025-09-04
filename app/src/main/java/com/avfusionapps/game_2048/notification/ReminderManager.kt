package com.avfusionapps.game_2048.notification

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleReminders() {
        workManager.cancelAllWorkByTag(REMINDER_WORK_TAG)

        scheduleReminderForDay(1)
        scheduleReminderForDay(3)
        scheduleReminderForDay(5)
        scheduleReminderForDay(10)
    }

    private fun scheduleReminderForDay(day: Int) {
        val data = workDataOf(GameReminderWorker.REMINDER_DAY_KEY to day)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val reminderWork = OneTimeWorkRequestBuilder<GameReminderWorker>()
            .setInitialDelay(day.toLong(), TimeUnit.DAYS)
            .setConstraints(constraints)
            .setInputData(data)
            .addTag(REMINDER_WORK_TAG)
            .build()

        workManager.enqueue(reminderWork)
    }

    fun cancelReminders() {
        workManager.cancelAllWorkByTag(REMINDER_WORK_TAG)
    }

    companion object {
        private const val REMINDER_WORK_TAG = "game_reminder_work"
    }
}