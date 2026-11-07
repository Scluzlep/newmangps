package com.scluzlep.newmangps.data.push

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.scluzlep.newmangps.data.repository.NewsmyRepository

class AlarmPollingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = NewsmyRepository()

    override suspend fun doWork(): Result {
        val imei = inputData.getString("IMEI") ?: return Result.failure()

        val alarmsResult = repository.pollAlarmQueue(imei)
        alarmsResult.getOrNull()?.let { alarms ->
            for (alarm in alarms) {
                AlarmNotificationManager.showAlarmNotification(
                    context = context,
                    id = alarm.id.hashCode(),
                    title = "纽曼卫士报警通知 [${alarm.teid}]",
                    content = alarm.content
                )
            }
        }

        return Result.success()
    }
}

