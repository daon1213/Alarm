package com.daon.alarm_part3_03

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "1000"
        private const val NOTIFICATION_ID = 100
    }

    override fun onReceive(context: Context, intent: Intent) {
        // broadcast 를 생성할 때 전달한 pendingIntent 가 온다.

        createNotificationChannel(context)
        notifyNotification(context)
    }

    private fun createNotificationChannel(context: Context) {
        // activity 내에서만 this == context : android app 의 기본 정보, API 등을 가지고 있다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 채널 생성 필요
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "기상 알람",
                NotificationManager.IMPORTANCE_HIGH // 알람의 중요도
            )

            NotificationManagerCompat
                .from(context)
                .createNotificationChannel(notificationChannel)
        }
    }

    private fun notifyNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            val build = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("알람")
                .setContentText("일어날 시간입니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // notification 생성에 필수
                .setPriority(NotificationCompat.PRIORITY_HIGH)
            notify(NOTIFICATION_ID, build.build())
        }
    }
}