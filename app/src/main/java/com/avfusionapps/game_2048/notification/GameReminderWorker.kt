package com.avfusionapps.game_2048.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avfusionapps.game_2048.MainActivity
import com.avfusionapps.game_2048.R

class GameReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "game_reminder_channel"
        const val NOTIFICATION_ID = 1
        const val REMINDER_DAY_KEY = "reminder_day"
    }

    override suspend fun doWork(): Result {
        val reminderDay = inputData.getInt(REMINDER_DAY_KEY, 1)
        val message = when (reminderDay) {
            1 -> "Miss playing 2048? Come back for another round!"
            3 -> "It's been 3 days! Your tiles are waiting for you."
            5 -> "Challenge yourself with a new game of 2048!"
            10 -> "Final reminder: Don't forget about your 2048 skills!"
            else -> "Time for a quick game of 2048!"
        }

        createNotificationChannel()
        showNotification(message)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Game Reminders"
            val descriptionText = "Reminders to play 2048"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("2048 Neon Rush")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}