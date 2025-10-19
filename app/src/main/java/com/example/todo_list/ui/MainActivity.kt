package com.example.todo_list.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.todo_list.databinding.ActivityMainBinding
import com.example.todo_list.helper.AlarmHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tüm izinleri kontrol et
        checkPermissions()
    }

    private fun checkPermissions() {
        // 1. Notification izni (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION
            )
        }

        // 2. Exact Alarm izni (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!AlarmHelper.canScheduleExactAlarms(this)) {
                showExactAlarmPermissionDialog()
            }
        }

        // 3. Pil Optimizasyonu (ÇOK ÖNEMLİ!)
        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Pil Optimizasyonu")
                    .setMessage("Hatırlatıcıların düzgün çalışması için pil optimizasyonunu kapatmanız önerilir.\n\n" +
                            "Bu, uygulamanın arka planda çalışmasına izin verir.")
                    .setPositiveButton("Ayarlara Git") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback - genel ayarlar sayfası
                            try {
                                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            } catch (ex: Exception) {
                                ex.printStackTrace()
                            }
                        }
                    }
                    .setNegativeButton("Daha Sonra", null)
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Tam Zamanlı Alarm İzni")
            .setMessage("Hatırlatıcıların tam zamanında çalışması için izin vermeniz gerekiyor.")
            .setPositiveButton("Ayarlara Git") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // İzin kontrolleri burada yapılabilir
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1001
    }
}