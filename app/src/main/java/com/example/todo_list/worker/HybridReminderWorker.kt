package com.example.todo_list.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.todo_list.Data.TaskDatabase
import com.example.todo_list.helper.AlarmHelper
import com.example.todo_list.helper.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Bu Worker 15 dakikada bir çalışır ve:
 * 1. Yaklaşan görevleri kontrol eder
 * 2. Alarmları yeniden kurar (force stop sonrası için)
 * 3. Zamanı gelen görevler için bildirim gösterir
 */
class HybridReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("HybridWorker", "Worker başladı - alarmlar kontrol ediliyor")

            val db = TaskDatabase.getInstance(applicationContext)
            val tasks = db.taskDao().getAllOnce()
            val now = System.currentTimeMillis()

            for (task in tasks) {
                if (task.reminderDate != null && task.reminderTime != null) {
                    val reminderMillis = combineDateAndTime(task.reminderDate, task.reminderTime)

                    // Zamanı gelmiş görevler için bildirim göster
                    if (reminderMillis in (now - 60000)..now) {
                        // Son 1 dakika içinde geçmişse göster
                        NotificationHelper.showNotification(
                            applicationContext,
                            task.id,
                            task.title,
                            task.description
                        )
                        Log.d("HybridWorker", "Geç bildirim gösterildi: ${task.title}")
                    }

                    // Gelecekteki görevler için alarm kur (force stop sonrası restore)
                    else if (reminderMillis > now) {
                        AlarmHelper.setReminder(
                            applicationContext,
                            task.id,
                            reminderMillis,
                            task.title,
                            task.description
                        )
                        Log.d("HybridWorker", "Alarm restore edildi: ${task.title}")
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("HybridWorker", "Worker hatası", e)
            Result.retry()
        }
    }

    private fun combineDateAndTime(dateMillis: Long, timeString: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }
        val parts = timeString.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        calendar.set(Calendar.MINUTE, parts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        private const val WORK_NAME = "hybrid_reminder_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Pil azken bile çalış
                .build()

            val workRequest = PeriodicWorkRequestBuilder<HybridReminderWorker>(
                15, TimeUnit.MINUTES // Minimum 15 dakika
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // İlk çalışma 1 dakika sonra
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Mevcut varsa değiştirme
                workRequest
            )

            Log.d("HybridWorker", "Worker zamanlandı - 15 dakikada bir çalışacak")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("HybridWorker", "Worker iptal edildi")
        }
    }
}