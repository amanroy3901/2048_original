package com.avfusionapps.game_2048.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avfusionapps.game_2048.MainActivity
import com.avfusionapps.game_2048.R
import com.avfusionapps.game_2048.data.GameSettingsRepository
import com.avfusionapps.game_2048.data.room.GameMoveRepository
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Shows one personalized re-engagement notification, then schedules the next step of the chain.
 * Skips (and restarts the chain) if the player has been active since it was scheduled, so it
 * never nags an engaged user. Content is drawn from real game state (saved game / best score).
 */
class GameReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "game_reminder_channel"
        const val NOTIFICATION_ID = 2048
        const val KEY_STEP = "step"
        const val KEY_SCHEDULED_AT = "scheduled_at"
        const val EXTRA_DESTINATION = "nav_destination"
        const val DEST_RESUME = "resume"
        const val DEST_PLAY = "play"
        private const val ACCENT = 0xFFFF006E.toInt() // neon pink
    }

    private data class Content(val text: String, val destination: String)

    override suspend fun doWork(): Result {
        val settings = GameSettingsRepository(applicationContext)

        // Respect the in-app toggle.
        if (!settings.remindersEnabledFlow.first()) return Result.success()

        val step = inputData.getInt(KEY_STEP, 0)
        val scheduledAt = inputData.getLong(KEY_SCHEDULED_AT, 0L)
        val lastPlayed = settings.lastPlayedAtFlow.first()

        // Active since we scheduled (or in the last ~day)? Don't nag — restart the chain instead.
        val activeRecently = lastPlayed > scheduledAt ||
            (System.currentTimeMillis() - lastPlayed) < TimeUnit.HOURS.toMillis(20)
        if (activeRecently) {
            ReminderManager(applicationContext).scheduleStep(0)
            return Result.success()
        }

        val content = buildContent(settings, step)
        createChannel()
        showNotification(content)
        logEvent("notification_shown", step)

        // Chain the next reminder.
        ReminderManager(applicationContext).scheduleStep(step + 1)
        return Result.success()
    }

    private suspend fun buildContent(settings: GameSettingsRepository, step: Int): Content {
        val name = settings.playerNameFlow.first()
        val namePrefix = if (name.isNotBlank() && name != GameSettingsRepository.DEFAULT_PLAYER_NAME) {
            "$name, "
        } else ""
        val best = settings.highScoreFlow.first()

        val lastMove = runCatching { GameMoveRepository(applicationContext).getLastMove() }.getOrNull()
        val variant = step % 3

        return when {
            lastMove != null -> {
                val tile = (lastMove.grid.flatten().maxOrNull() ?: 0).toString()
                val score = fmt(lastMove.score)
                val res = intArrayOf(R.string.notif_saved_1, R.string.notif_saved_2, R.string.notif_saved_3)[variant]
                Content(context.getString(res, namePrefix, tile, score), DEST_RESUME)
            }
            best > 0 -> {
                val res = intArrayOf(R.string.notif_best_1, R.string.notif_best_2, R.string.notif_best_3)[variant]
                Content(context.getString(res, namePrefix, fmt(best)), DEST_PLAY)
            }
            else -> {
                val res = intArrayOf(R.string.notif_new_1, R.string.notif_new_2, R.string.notif_new_3)[variant]
                Content(context.getString(res, namePrefix), DEST_PLAY)
            }
        }
    }

    private fun showNotification(content: Content) {
        val actionRes = if (content.destination == DEST_RESUME) {
            R.string.notif_action_resume
        } else {
            R.string.notif_action_play
        }
        val largeIcon = runCatching {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ACCENT)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(deepLink(content.destination))
            .addAction(0, context.getString(actionRes), deepLink(content.destination))

        if (largeIcon != null) builder.setLargeIcon(largeIcon)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, builder.build())
    }

    private fun deepLink(destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            destination.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // DEFAULT importance = a quiet nudge (no heads-up interruption).
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun logEvent(name: String, step: Int) {
        runCatching {
            Firebase.analytics.logEvent(name, android.os.Bundle().apply { putInt("step", step) })
        }
    }

    private fun fmt(n: Int): String = "%,d".format(n)
}
