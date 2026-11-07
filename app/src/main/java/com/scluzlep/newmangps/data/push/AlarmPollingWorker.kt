package com.scluzlep.newmangps.data.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import java.util.concurrent.TimeUnit

class AlarmPollingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = NewsmyRepository()

    override suspend fun doWork(): Result {
        val user = inputData.getString("USER") ?: return Result.failure()
        if (!AlarmNotificationManager.canNotify(context)) return Result.success()
        val prefs = context.getSharedPreferences("alarm_polling", Context.MODE_PRIVATE)
        val key = user.hashCode().toString()
        val now = System.currentTimeMillis() / 1000
        val start = prefs.getLong("cursor_$key", now).coerceAtLeast(now - 86400)
        val seenKey = "seen_$key"
        val seen = prefs.getStringSet(seenKey, emptySet()).orEmpty().toMutableSet()
        var offset = 0
        while (offset < 10_000) {
            val page = repository.pollAlarmQueue(user, (start - 60).coerceAtLeast(0), now, offset)
                .getOrElse { return Result.retry() }
            for (alarm in page.alarms.sortedBy { it.timeSeconds }) {
                if (alarm.timeSeconds < start || !seen.add(alarm.id)) continue
                val shown = AlarmNotificationManager.showAlarmNotification(
                    context = context,
                    id = alarm.id.hashCode(),
                    title = "纽曼卫士报警通知 [${alarm.teid}]",
                    content = alarm.content
                )
                if (!shown) return Result.retry()
            }
            if (!page.hasMore) break
            offset += 200
        }
        if (offset >= 10_000) return Result.retry()
        prefs.edit()
            .putLong("cursor_$key", now)
            .putStringSet(seenKey, seen.toList().takeLast(300).toSet())
            .apply()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, user: String) {
            val prefs = context.getSharedPreferences("alarm_polling", Context.MODE_PRIVATE)
            val key = user.hashCode().toString()
            val previous = prefs.getString("active_work", null)
            if (previous != null && previous != key) {
                WorkManager.getInstance(context).cancelUniqueWork("alarm_$previous")
            }
            if (!prefs.contains("cursor_$key")) {
                prefs.edit().putLong("cursor_$key", System.currentTimeMillis() / 1000).apply()
            }
            prefs.edit().putString("active_work", key).apply()
            val request = PeriodicWorkRequestBuilder<AlarmPollingWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf("USER" to user))
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "alarm_$key",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

