// BootReceiver.kt - 부팅 완료 시 시세포착 서비스 재시작

package com.example.ver20.dao.signal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.ver20.dao.trading.signal.MarketSignalMonitor

/**
 * 부팅 완료 및 앱 업데이트 시 시세포착 모니터링을 재시작하는 리시버
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot receiver called with action: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "시스템 부팅 완료 또는 앱 업데이트 감지, 시세포착 모니터링 재시작")
                
                try {
                    // 시세포착 모니터링 재시작
                    val marketSignalMonitor = MarketSignalMonitor(context)
                    marketSignalMonitor.startMonitoring()
                    
                    Log.d(TAG, "시세포착 모니터링 재시작 완료")
                } catch (e: Exception) {
                    Log.e(TAG, "시세포착 모니터링 재시작 실패: ${e.message}")
                }
            }
            else -> {
                Log.d(TAG, "알 수 없는 액션: ${intent.action}")
            }
        }
    }
}