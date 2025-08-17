// RealDataBacktestEngine.kt 수정 - 기존 파일 업데이트

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// ===========================================
// 바이낸스 K-line API 인터페이스
// ===========================================

interface BinanceKlineApi {
    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 1000,
        @Query("startTime") startTime: Long? = null,
        @Query("endTime") endTime: Long? = null
    ): retrofit2.Response<List<List<Any>>>
}

// ===========================================
// 실제 데이터 백테스트 엔진 (CCI 전용으로 수정)
// ===========================================

class RealDataBacktestEngine {
    companion object {
        private const val TAG = "RealDataBacktestEngine"
        private const val BINANCE_BASE_URL = "https://api.binance.com/"

        // 시간프레임 변환
        private fun getIntervalString(timeframe: String): String {
            return when (timeframe) {
                "1시간" -> "1h"
                "4시간" -> "4h"
                "1일" -> "1d"
                "1주" -> "1w"
                else -> "4h"
            }
        }

        // 기간별 데이터 개수 계산 (1주일 추가)
        private fun getDataLimit(period: String, timeframe: String): Int {
            return when (period) {
                "1주일" -> when (timeframe) {
                    "1시간" -> 168    // 7일 * 24시간
                    "4시간" -> 42     // 7일 * 6개 (24시간/4시간)
                    "1일" -> 7        // 7일
                    else -> 42
                }
                "3개월" -> when (timeframe) {
                    "1시간" -> 2160   // 90일 * 24시간
                    "4시간" -> 540    // 90일 * 6개
                    "1일" -> 90       // 90일
                    else -> 540
                }
                "6개월" -> when (timeframe) {
                    "1시간" -> 4320   // 180일 * 24시간
                    "4시간" -> 1080   // 180일 * 6개
                    "1일" -> 180      // 180일
                    else -> 1080
                }
                "1년" -> when (timeframe) {
                    "1시간" -> 8760   // 365일 * 24시간
                    "4시간" -> 2190   // 365일 * 6개
                    "1일" -> 365      // 365일
                    else -> 2190
                }
                "2년" -> when (timeframe) {
                    "1시간" -> 17520  // 730일 * 24시간
                    "4시간" -> 4380   // 730일 * 6개
                    "1일" -> 730      // 730일
                    else -> 4380
                }
                else -> 168 // 기본값: 1주일
            }
        }
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl(BINANCE_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(BinanceKlineApi::class.java)

    // 실제 바이낸스 데이터 가져오기 (CCI 설정 사용)
    suspend fun fetchRealPriceData(settings: CciStrategySettings): List<PriceCandle> {
        return try {
            Log.d(TAG, "📡 바이낸스 API 호출 시작")
            Log.d(TAG, "요청 정보: 심볼=${settings.symbol}, 시간프레임=${settings.timeframe}, 기간=${settings.testPeriod}")

            val interval = getIntervalString(settings.timeframe)
            val limit = getDataLimit(settings.testPeriod, settings.timeframe)

            Log.d(TAG, "변환된 정보: interval=$interval, limit=$limit")

            val allCandles = mutableListOf<PriceCandle>()
            var currentLimit = limit
            var endTime: Long? = null
            var requestCount = 0

            while (allCandles.size < limit && currentLimit > 0) {
                val requestLimit = minOf(currentLimit, 1000)
                requestCount++

                Log.d(TAG, "📞 API 요청 #$requestCount: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit")

                val response = try {
                    api.getKlines(
                        symbol = settings.symbol,
                        interval = interval,
                        limit = requestLimit,
                        endTime = endTime
                    )
                } catch (networkError: Throwable) {
                    Log.e(TAG, "❌ 네트워크 오류 발생: ${networkError.message}")
                    throw RuntimeException("바이낸스 API 네트워크 오류\n" +
                            "요청 #$requestCount 실패\n" +
                            "URL: ${BINANCE_BASE_URL}api/v3/klines\n" +
                            "파라미터: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit\n" +
                            "오류: ${networkError.message}")
                }

                if (response.isSuccessful && response.body() != null) {
                    val klines = response.body()!!
                    Log.d(TAG, "✅ API 응답 성공: ${klines.size}개 데이터 수신")

                    val newCandles = klines.mapNotNull { kline ->
                        try {
                            if (kline.size >= 6) {
                                PriceCandle(
                                    timestamp = (kline[0] as Double).toLong(),
                                    open = (kline[1] as String).toDouble(),
                                    high = (kline[2] as String).toDouble(),
                                    low = (kline[3] as String).toDouble(),
                                    close = (kline[4] as String).toDouble(),
                                    volume = (kline[5] as String).toDouble()
                                )
                            } else null
                        } catch (parseError: Exception) {
                            Log.w(TAG, "⚠️ 캔들 데이터 파싱 오류: ${parseError.message}")
                            null
                        }
                    }

                    if (newCandles.isNotEmpty()) {
                        allCandles.addAll(0, newCandles) // 앞쪽에 추가 (오래된 데이터)
                        endTime = newCandles.first().timestamp - 1
                        currentLimit -= newCandles.size
                        Log.d(TAG, "📈 누적 데이터: ${allCandles.size}개 (목표: $limit 개)")
                    } else {
                        Log.w(TAG, "⚠️ 파싱된 데이터가 없음")
                        break
                    }
                } else {
                    val errorCode = response.code()
                    val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"

                    Log.e(TAG, "❌ API 호출 실패: HTTP $errorCode")
                    Log.e(TAG, "오류 내용: $errorBody")

                    val errorMessage = when (errorCode) {
                        400 -> "잘못된 요청 (400)\n심볼명이나 시간프레임을 확인해주세요"
                        403 -> "접근 금지 (403)\nIP 제한이 있을 수 있습니다"
                        429 -> "요청 한도 초과 (429)\n잠시 후 다시 시도해주세요"
                        500 -> "서버 오류 (500)\n바이낸스 서버에 문제가 있습니다"
                        else -> "HTTP $errorCode 오류"
                    }

                    throw RuntimeException("바이낸스 API 호출 실패\n" +
                            "요청 #$requestCount 실패\n" +
                            "URL: ${BINANCE_BASE_URL}api/v3/klines\n" +
                            "파라미터: symbol=${settings.symbol}, interval=$interval, limit=$requestLimit\n" +
                            "$errorMessage\n" +
                            "상세: $errorBody")
                }

                // API 호출 간격 (Rate Limit 방지)
                kotlinx.coroutines.delay(100)
            }

            if (allCandles.isEmpty()) {
                throw RuntimeException("데이터 수집 실패\n" +
                        "총 ${requestCount}번의 API 요청을 시도했지만 유효한 데이터를 받지 못했습니다\n" +
                        "심볼: ${settings.symbol}\n" +
                        "시간프레임: ${settings.timeframe}\n" +
                        "기간: ${settings.testPeriod}")
            }

            val finalData = allCandles.takeLast(limit)
            Log.d(TAG, "✅ 데이터 수집 완료: ${finalData.size}개 (요청 횟수: $requestCount)")

            // 데이터 품질 체크 및 타임스탬프 검증
            val startDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(finalData.first().timestamp))
            val endDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(finalData.last().timestamp))
            Log.d(TAG, "📅 데이터 기간: $startDate ~ $endDate")

            // 🔍 샘플 데이터 검증 (처음 3개, 마지막 3개)
            Log.d(TAG, "🔍 데이터 샘플 검증:")
            for (i in 0 until minOf(3, finalData.size)) {
                val sample = finalData[i]
                val sampleTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(sample.timestamp))
                Log.d(TAG, "  시작 #$i: $sampleTime, 종가=${sample.close}")
            }

            for (i in maxOf(0, finalData.size - 3) until finalData.size) {
                val sample = finalData[i]
                val sampleTime = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(sample.timestamp))
                Log.d(TAG, "  끝 #$i: $sampleTime, 종가=${sample.close}")
            }

            finalData

        } catch (e: Throwable) {
            Log.e(TAG, "❌ fetchRealPriceData 실패: ${e.message}")
            throw e // 오류를 그대로 전파
        }
    }
}