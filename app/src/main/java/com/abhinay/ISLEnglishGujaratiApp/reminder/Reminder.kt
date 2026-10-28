package com.abhinay.ISLEnglishGujaratiApp.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.abhinay.ISLEnglishGujaratiApp.MainActivity
import com.abhinay.ISLEnglishGujaratiApp.R
import com.abhinay.ISLEnglishGujaratiApp.SanketSetuApp
import com.abhinay.ISLEnglishGujaratiApp.data.Srs
import com.abhinay.ISLEnglishGujaratiApp.data.Streak
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** A once-a-day nudge at 18:00 local time, only when there is something to review and you have not practised yet. */
object Reminder {
    private const val WORK = "daily_practice"
    private const val CHANNEL = "practice"
    private const val HOUR = 18

    fun shouldNotify(answeredToday: Int, reviewable: Int) = answeredToday == 0 && reviewable > 0

    fun delayUntilNext(now: LocalDateTime): Duration {
        var next = now.toLocalDate().atTime(HOUR, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext(LocalDateTime.now())).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) = WorkManager.getInstance(context).cancelUniqueWork(WORK)

    internal fun notify(context: Context, reviewable: Int) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Daily practice", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_mono)
            .setContentTitle("Time to practise")
            .setContentText("$reviewable signs are ready to review.")
            .setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify(1, n)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = (applicationContext as SanketSetuApp).store.state.value
        val today = LocalDate.now().toEpochDay()
        val reviewable = Srs.reviewQueue(state.cards, today).size
        if (state.reminder && Reminder.shouldNotify(Streak.answeredToday(state.activity, today), reviewable)) Reminder.notify(applicationContext, reviewable)
        return Result.success()
    }
}
