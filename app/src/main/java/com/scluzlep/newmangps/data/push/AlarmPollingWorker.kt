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
        val server = inputData.getString("SERVER") ?: return Result.failure()
        repository.restoreServer(server)
        val teids = inputData.getStringArray("TEIDS")?.filter(String::isNotBlank).orEmpty()
        if (teids.isEmpty()) return Result.failure()
        if (!context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                .getBoolean("push_alarm", true)) return Result.success()
        if (!AlarmNotificationManager.canNotify(context)) return Result.success()
        val prefs = context.getSharedPreferences("alarm_polling", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis() / 1000
        for (teid in teids) {
            val key = "${server.hashCode()}_${user.hashCode()}_${teid.hashCode()}"
            val start = prefs.getLong("cursor_$key", prefs.getLong("cursor_${user.hashCode()}", now - 900))
                .coerceIn(now - 86400, now)
            val seenKey = "seen_$key"
            val seen = prefs.getStringSet(seenKey, emptySet()).orEmpty().toMutableSet()
            var offset = 0
            while (offset < 10_000) {
                val page = repository.pollAlarmQueue(user, teid, (start - 60).coerceAtLeast(0), now, offset)
                    .getOrElse { return Result.retry() }
                for (alarm in page.alarms.sortedBy { it.timeSeconds }) {
                    if (!seen.add(alarm.id)) continue
                    val shown = AlarmNotificationManager.showAlarmNotification(
                        context = context,
                        id = alarm.id.hashCode(),
                        title = "纽曼卫士报警通知 [${alarm.teid}]",
                        content = alarm.content,
                        teid = alarm.teid,
                        server = server,
                        user = user
                    )
                    if (!shown) return Result.retry()
                    prefs.edit().putStringSet(seenKey, seen.toSet()).apply()
                }
                if (!page.hasMore) break
                offset += 200
            }
            if (offset >= 10_000) return Result.retry()
            prefs.edit()
                .putLong("cursor_$key", now)
                .putStringSet(seenKey, seen.toList().takeLast(300).toSet())
                .apply()
        }
        return Result.success()
    }

    companion object {
        fun cancel(context: Context, user: String) {
            val prefs = context.getSharedPreferences("alarm_polling", Context.MODE_PRIVATE)
            val key = user.hashCode().toString()
            WorkManager.getInstance(context).cancelUniqueWork("alarm_$key")
            prefs.edit().remove("active_work").apply()
        }

        fun schedule(context: Context, user: String, teids: List<String>, server: String) {
            val prefs = context.getSharedPreferences("alarm_polling", Context.MODE_PRIVATE)
            val key = user.hashCode().toString()
            val previous = prefs.getString("active_work", null)
            if (previous != null && previous != key) {
                WorkManager.getInstance(context).cancelUniqueWork("alarm_$previous")
            }
            if (!prefs.contains("cursor_$key")) {
                prefs.edit().putLong("cursor_$key", System.currentTimeMillis() / 1000).apply()
            }
            val started = System.currentTimeMillis() / 1000
            val editor = prefs.edit()
            teids.forEach { teid ->
                val deviceKey = "${server.hashCode()}_${user.hashCode()}_${teid.hashCode()}"
                if (!prefs.contains("cursor_$deviceKey")) editor.putLong("cursor_$deviceKey", started)
            }
            editor.apply()
            prefs.edit().putString("active_work", key).apply()
            val request = PeriodicWorkRequestBuilder<AlarmPollingWorker>(15, TimeUnit.MINUTES)
                .setInputData(workDataOf("USER" to user, "TEIDS" to teids.toTypedArray(), "SERVER" to server))
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

