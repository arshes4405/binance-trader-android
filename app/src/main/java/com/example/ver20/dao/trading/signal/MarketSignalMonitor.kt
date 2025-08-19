// MarketSignalMonitor.kt - 시세포착 백그라운드 모니터링 (재작성)

package com.example.ver20.dao.trading.signal

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.ver20.dao.mongoDB.UserService
import com.example.ver20.view.MainActivity
import java.util.concurrent.TimeUnit

/**
 * 시세포착 백그라운드 모니터링 매니저 (간소화 버전)
 */
class MarketSignalMonitor(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val WORK_NAME = "market_signal_monitoring"
        private const val NOTIFICATION_CHANNEL_ID = "market_signals"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "MarketSignalMonitor"
    }

    init {
        createNotificationChannel()
    }

    /**
     * 시세포착 모니터링 시작
     */
    fun startMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MarketSignalWorker>(
            15, TimeUnit.MINUTES // 최소 15분 간격
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "시세포착 모니터링 시작됨")
    }

    /**
     * 시세포착 모니터링 중지
     */
    fun stopMonitoring() {
        workManager.cancelUniqueWork(WORK_NAME)
        Log.d(TAG, "시세포착 모니터링 중지됨")
    }

    /**
     * 알림 채널 생성
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "시세포착 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "시세포착 신호 알림"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 시세포착 알림 표시
     */
    fun showSignalNotification(signal: MarketSignal) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val directionText = if (signal.direction == "LONG") "매수" else "매도"
            val directionEmoji = if (signal.direction == "LONG") "📈" else "📉"

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("$directionEmoji ${signal.symbol} ${directionText} 신호")
                .setContentText("가격: ${String.format("%.2f", signal.price)} | CCI: ${String.format("%.1f", signal.cciValue)}")
                .setSmallIcon(R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID + signal.hashCode(), notification)

            Log.d(TAG, "알림 표시 완료: ${signal.symbol} ${signal.direction}")

        } catch (e: Exception) {
            Log.e(TAG, "알림 표시 실패: ${e.message}")
        }
    }
}

/**
 * WorkManager를 이용한 백그라운드 작업 (간소화 버전)
 */
class MarketSignalWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val marketSignalService = MarketSignalService()
    private val userService = UserService()
    private val signalMonitor = MarketSignalMonitor(context)

    companion object {
        private const val TAG = "MarketSignalWorker"
    }

    override fun doWork(): Result {
        return try {
            Log.d(TAG, "시세포착 작업 시작")

            // 현재 사용자 정보 가져오기
            val currentUser = userService.getUserFromPreferences(applicationContext)
            if (currentUser == null) {
                Log.d(TAG, "사용자 정보가 없어 작업 종료")
                return Result.success()
            }

            // 테스트용 더미 설정으로 작업 진행
            processTestSignalCheck(currentUser.username)

            Log.d(TAG, "시세포착 작업 완료")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "시세포착 작업 중 오류: ${e.message}")
            Result.retry()
        }
    }

    /**
     * 테스트용 시세포착 체크 (실제 API 호출 없이)
     */
    private fun processTestSignalCheck(username: String) {
        try {
            // 테스트용 CCI 설정
            val testConfig = MarketSignalConfig(
                username = username,
                signalType = "CCI",
                symbol = "BTCUSDT",
                timeframe = "1h",
                checkInterval = 60,
                cciPeriod = 20,
                cciBreakoutValue = 100.0,
                cciEntryValue = 90.0,
                seedMoney = 1000.0,
                isActive = true
            )

            // 현재 시간 기준으로 간단한 시그널 체크 시뮬레이션
            val currentMinute = System.currentTimeMillis() / 60000

            // 10분마다 한 번씩 테스트 신호 생성 (실제 환경에서는 제거)
            if (currentMinute % 10 == 0L) {
                val isLongSignal = (currentMinute % 20) == 0L

                val testSignal = MarketSignal(
                    configId = testConfig.id,
                    username = username,
                    symbol = testConfig.symbol,
                    signalType = testConfig.signalType,
                    direction = if (isLongSignal) "LONG" else "SHORT",
                    price = if (isLongSignal) 45000.0 else 44000.0,
                    volume = 1000000.0,
                    cciValue = if (isLongSignal) -95.0 else 95.0,
                    cciBreakoutValue = testConfig.cciBreakoutValue,
                    cciEntryValue = testConfig.cciEntryValue,
                    reason = if (isLongSignal)
                        "CCI가 -100 아래 이탈 후 -90 진입" else
                        "CCI가 +100 위 이탈 후 +90 진입",
                    timeframe = testConfig.timeframe
                )

                // 알림 표시
                signalMonitor.showSignalNotification(testSignal)

                // 신호 저장 (콜백 기반, 간단하게)
                saveSignalSimple(testSignal)

                Log.d(TAG, "테스트 신호 생성됨: ${testSignal.symbol} ${testSignal.direction}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "테스트 신호 체크 중 오류: ${e.message}")
        }
    }

    /**
     * 간단한 신호 저장 (콜백 처리 없이)
     */
    private fun saveSignalSimple(signal: MarketSignal) {
        try {
            marketSignalService.saveSignal(signal) { success, message ->
                if (success) {
                    Log.d(TAG, "신호 저장 성공: ${signal.symbol}")
                } else {
                    Log.e(TAG, "신호 저장 실패: $message")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "신호 저장 중 오류: ${e.message}")
        }
    }
}

/**
 * 시세포착 상태 관리 (간소화)
 */
class MarketSignalPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences("market_signal_prefs", Context.MODE_PRIVATE)

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("monitoring_enabled", enabled).apply()
    }

    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean("monitoring_enabled", false)
    }

    fun setLastCheckTime(timestamp: Long) {
        prefs.edit().putLong("last_check_time", timestamp).apply()
    }

    fun getLastCheckTime(): Long {
        return prefs.getLong("last_check_time", 0)
    }
}