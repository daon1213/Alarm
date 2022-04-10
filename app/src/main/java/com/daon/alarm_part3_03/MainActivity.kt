package com.daon.alarm_part3_03

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TODO : Step 0 initialize view
        initOnOffButton()
        initChangeAlarmTimeButton()

        // TODO : Step 1 get data from SharedPreference
        val model = fetchDataFromSharedPreferences()
        // TODO : Step 2 binding data to view
        renderView(model)
    }

    private fun initOnOffButton() {
        val onOffButton: Button = findViewById(R.id.onOffButton)
        onOffButton.setOnClickListener {
            // TODO : 데이터를 확인한다.
            val model = it.tag as? AlarmDisplayModel // 변환이 불가능한 경우 null 을 반환
            model ?: return@setOnClickListener

            // TODO : 데이터를 저장한다.
            val newModel = saveAlarmMode(model.hour,model.minute,model.onOff.not())
            renderView(newModel)

            // TODO : on / off 에 따라 작업 처리 -> on(알람을 등록), off(알람을 제거)
            if (model.onOff) {
                // 켜진 경우 -> 알림을 등록
                val calender = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, newModel.hour)
                    set(Calendar.MINUTE, newModel.minute)

                    // 이미 알람시간을 지난 경우, 다음날로 이동
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                // 알람 manager 에 등록
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(this@MainActivity, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this@MainActivity, BROADCAST_REQUEST_CODE,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT) // 만약 이미 있다면 현재걸로 업데이트

                /**
                 * 정확한 시간에 발생하고 자 하면
                 * alarmManager.setExact()
                 * 를 사용
                 */
                alarmManager.setInexactRepeating(
                    // RTC_WAKED : 절대 시간을 기준으로
                    // ELAPSED_REALTIME_WAKEUP : 사용 권장
                    AlarmManager.RTC_WAKEUP,
                    calender.timeInMillis,
                    AlarmManager.INTERVAL_DAY, // 하루있다가 시작
                    pendingIntent
                )
            } else {
                // 꺼진 경우 -> 알림을 제거
                cancelAlarm()
            }
        }
    }

    private fun initChangeAlarmTimeButton() {
        val changeAlarmTimeButton: Button = findViewById(R.id.changeAlarmTimeButton)
        changeAlarmTimeButton.setOnClickListener {
            // TODO : 현재시간을 가져온다.
            val calendar = Calendar.getInstance() // 현재 시스템에 설정되어 있는 시간 get

            // TODO : TimePickerDialog 를 사용해서 알람 시간을 사용자로부터 입력 받는다.
            TimePickerDialog(this, { picker, hour, minute ->
                // TODO : 사용자가 선택한 시간으로 데이터를 저장한다.
                val model = saveAlarmMode(hour, minute, false)

                // TODO : 선택된 시간으로 view 를 업데이트 한다.
                renderView(model)

                // TODO : 기존에 작업중인 알람이 있다면 삭제한다.
                cancelAlarm()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
                .show()
        }
    }

    private fun saveAlarmMode(
        hour: Int,
        minute: Int,
        onOff: Boolean
    ): AlarmDisplayModel {
        val model = AlarmDisplayModel(
            hour = hour,
            minute = minute,
            onOff = onOff
        )

        val sharedPreferences = getSharedPreferences(SHARED_PREPERENCES_NAME, MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(ALARM_KEY, model.makeDataForDB())
            putBoolean(ONOFF_KEY, model.onOff)
            commit()
        }

        return model
    }

    private fun fetchDataFromSharedPreferences(): AlarmDisplayModel {
        val sharedPreferences = getSharedPreferences(SHARED_PREPERENCES_NAME, MODE_PRIVATE)
        // getString 은 자바코드의 Nullable 로 선언되었다. 따라서 null 처리를 위한 추가 기능이 필요하다.
        val timeDBValue = sharedPreferences.getString(ALARM_KEY, "09:30") ?: "09:30"
        val onOffDBValue = sharedPreferences.getBoolean(ONOFF_KEY, false) // 이건 non-null
        val alarmData = timeDBValue.split(":")

        val alarmModel = AlarmDisplayModel(
            hour = alarmData.get(0).toInt(),
            minute = alarmData.get(0).toInt(),
            onOff = onOffDBValue
        )

        // TODO : onOff 의 값과 현재 알람 상태가 일치하지 않을 때 맞춰주는 추가 작업 필요
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            BROADCAST_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        if ((pendingIntent == null) and alarmModel.onOff) {
            // 알람은 등록되지 않았는데, onOff(데이터)는 켜져 있는 경우
            alarmModel.onOff = false
        } else if ((pendingIntent != null) and alarmModel.onOff.not()) {
            // 알람은 등록되어 있는데, OnOff(데이터)는 꺼져 있는 경우
            // 알람을 취소한다.
            pendingIntent.cancel()
        }
        return alarmModel
    }

    private fun renderView(model: AlarmDisplayModel) {
        findViewById<TextView>(R.id.timeTextView).apply {
            text = model.timeText
        }
        findViewById<TextView>(R.id.ampmTextView).apply {
            text = model.ampmText
        }
        findViewById<Button>(R.id.onOffButton).apply {
            text = model.onOff.toString()
            tag = model
        }
    }

    private fun cancelAlarm () {
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            BROADCAST_REQUEST_CODE,
            Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.cancel()
    }

    companion object {
        private const val SHARED_PREPERENCES_NAME = "time"
        private const val ALARM_KEY = "alarm"
        private const val ONOFF_KEY = "onOff"
        private const val BROADCAST_REQUEST_CODE = 1000
    }
}