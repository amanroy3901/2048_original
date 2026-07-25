package com.avfusionapps.game_2048.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules the re-engagement reminder chain. A single unique WorkManager job runs at a time;
 * each firing schedules the next step, giving an escalating-then-steady cadence with an infinite
 * win-back tail (see [stepDelayDays]). The chain is (re)started whenever the player is active, so
 * reminders only ever fire after a real gap in play — never while someone is engaged.
 */
class ReminderManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /** Start (or restart) the chain from the beginning — call when the player was just active. */
    fun scheduleReminders() = scheduleStep(0)

    /** Schedule a single step of the chain; the worker calls this to queue the next one. */
    fun scheduleStep(step: Int) {
        val data = workDataOf(
            GameReminderWorker.KEY_STEP to step,
            GameReminderWorker.KEY_SCHEDULED_AT to System.currentTimeMillis()
        )
        val work = OneTimeWorkRequestBuilder<GameReminderWorker>()
            .setInitialDelay(delayToEveningAfterDays(stepDelayDays(step)), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
            )
            .setInputData(data)
            .addTag(WORK_TAG)
            .build()
        // REPLACE: only ever one reminder pending, and a fresh session cancels the stale one.
        workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, work)
    }

    fun cancelReminders() {
        workManager.cancelUniqueWork(UNIQUE_NAME)
    }

    /** Millis until ~[REMINDER_HOUR]:00 local, [days] days from now (never in the past). */
    private fun delayToEveningAfterDays(days: Long): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days.toInt())
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var delay = cal.timeInMillis - System.currentTimeMillis()
        if (delay < TimeUnit.MINUTES.toMillis(1)) delay += TimeUnit.DAYS.toMillis(1)
        return delay
    }

    companion object {
        const val WORK_TAG = "game_reminder_work"
        private const val UNIQUE_NAME = "game_reminder"
        private const val REMINDER_HOUR = 19 // 7 PM local — a good moment to nudge

        /**
         * Days from the previous step. Cumulative: +1d, +3d, +7d, +14d, then every +30d forever.
         * Balanced cadence — escalates, then a steady long-tail win-back.
         */
        fun stepDelayDays(step: Int): Long = when (step) {
            0 -> 1
            1 -> 2
            2 -> 4
            3 -> 7
            else -> 30
        }
    }
}
