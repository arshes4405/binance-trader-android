// BinanceApiTester.kt - 바이낸스 API 연결 테스트 전용

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BinanceApiTester {
    companion object {
        private const val TAG = "BinanceApiTester"
        private const val MAINNET_URL = "https://api.binance.com"
        private const val TESTNET_URL = "https://testnet.binance.vision"
    }
    
    private val client = OkHttpClient()
    
    // HMAC SHA256 서명 생성
    private fun createSignature(data: String, secretKey: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            mac.init(secretKeySpec)
            val signature = mac.doFinal(data.toByteArray())
            signature.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 서명 생성 실패: ${e.message}")
            ""
        }
    }
    
    // 1단계: 서버 시간 확인 (인증 불필요)
    suspend fun testServerConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 서버 연결 테스트 시작...")
            
            val request = Request.Builder()
                .url("$MAINNET_URL/api/v3/time")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ 서버 연결 성공: $responseBody")
                Pair(true, "바이낸스 서버 연결 성공")
            } else {
                Log.e(TAG, "❌ 서버 연결 실패: HTTP ${response.code}")
                Pair(false, "서버 연결 실패: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 서버 연결 예외: ${e.message}")
            Pair(false, "네트워크 오류: ${e.message}")
        }
    }
    
    // 2단계: 현재 가격 조회 (Public API)
    suspend fun testPriceQuery(symbol: String = "BTCUSDT"): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 가격 조회 테스트 시작: $symbol")
            
            val request = Request.Builder()
                .url("$MAINNET_URL/api/v3/ticker/price?symbol=$symbol")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ 가격 조회 성공: $responseBody")
                Pair(true, responseBody)
            } else {
                Log.e(TAG, "❌ 가격 조회 실패: HTTP ${response.code}")
                Pair(false, "가격 조회 실패: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 가격 조회 예외: ${e.message}")
            Pair(false, "가격 조회 오류: ${e.message}")
        }
    }
    
    // 3단계: API 키 인증 테스트 (계좌 정보 조회)
    suspend fun testApiAuthentication(apiKeyData: ApiKeyData): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 API 인증 테스트 시작...")
            
            val baseUrl = if (apiKeyData.isTestnet) TESTNET_URL else MAINNET_URL
            val timestamp = System.currentTimeMillis()
            val queryString = "timestamp=$timestamp"
            val signature = createSignature(queryString, apiKeyData.secretKey)
            
            if (signature.isEmpty()) {
                return@withContext Pair(false, "서명 생성 실패")
            }
            
            val url = "$baseUrl/api/v3/account?$queryString&signature=$signature"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("X-MBX-APIKEY", apiKeyData.apiKey)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            when (response.code) {
                200 -> {
                    Log.d(TAG, "✅ API 인증 성공")
                    Log.d(TAG, "계좌 정보: $responseBody")
                    Pair(true, "API 인증 성공! 계좌에 정상 접근됨")
                }
                401 -> {
                    Log.e(TAG, "❌ API 인증 실패: 권한 없음")
                    Pair(false, "API 키 또는 Secret 키가 잘못되었습니다")
                }
                403 -> {
                    Log.e(TAG, "❌ API 접근 금지")
                    Pair(false, "API 키 권한이 부족하거나 IP 제한이 있습니다")
                }
                429 -> {
                    Log.e(TAG, "❌ API 요청 한도 초과")
                    Pair(false, "API 요청 한도를 초과했습니다. 잠시 후 다시 시도하세요")
                }
                else -> {
                    Log.e(TAG, "❌ API 호출 실패: HTTP ${response.code}")
                    Log.e(TAG, "오류 응답: $responseBody")
                    Pair(false, "API 호출 실패: $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ API 인증 예외: ${e.message}")
            Pair(false, "API 인증 오류: ${e.message}")
        }
    }
    
    // 4단계: 잔고 조회 테스트 (상세 정보)
    suspend fun testBalanceQuery(apiKeyData: ApiKeyData): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 잔고 조회 테스트 시작...")
            
            val baseUrl = if (apiKeyData.isTestnet) TESTNET_URL else MAINNET_URL
            val timestamp = System.currentTimeMillis()
            val queryString = "timestamp=$timestamp"
            val signature = createSignature(queryString, apiKeyData.secretKey)
            
            val url = "$baseUrl/api/v3/account?$queryString&signature=$signature"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("X-MBX-APIKEY", apiKeyData.apiKey)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ 잔고 조회 성공")
                
                // 잔고 정보 파싱 (간단한 버전)
                val balanceInfo = if (responseBody.contains("balances")) {
                    "계좌 잔고 정보를 성공적으로 조회했습니다"
                } else {
                    "계좌에 접근했지만 잔고 정보가 없습니다"
                }
                
                Pair(true, balanceInfo)
            } else {
                Log.e(TAG, "❌ 잔고 조회 실패: HTTP ${response.code}")
                Pair(false, "잔고 조회 실패: $responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 잔고 조회 예외: ${e.message}")
            Pair(false, "잔고 조회 오류: ${e.message}")
        }
    }
    
    // 전체 테스트 실행
    suspend fun runFullTest(apiKeyData: ApiKeyData?): List<Pair<String, Pair<Boolean, String>>> {
        val results = mutableListOf<Pair<String, Pair<Boolean, String>>>()
        
        // 1단계: 서버 연결
        results.add("서버 연결" to testServerConnection())
        
        // 2단계: 가격 조회
        results.add("가격 조회" to testPriceQuery())
        
        // API 키가 있는 경우에만 인증 테스트
        if (apiKeyData != null) {
            // 3단계: API 인증
            results.add("API 인증" to testApiAuthentication(apiKeyData))
            
            // 4단계: 잔고 조회
            results.add("잔고 조회" to testBalanceQuery(apiKeyData))
        }
        
        return results
    }
}