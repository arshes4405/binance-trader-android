package com.example.ver20.dao

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

// Binance API 테스트용 인터페이스
interface BinanceTestApi {
    // 계정 정보 조회 (API 키 유효성 확인용)
    @GET("api/v3/account")
    suspend fun getAccountInfo(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): Response<BinanceAccountResponse>

    // 테스트넷 계정 정보 조회
    @GET("api/v3/account")
    suspend fun getTestAccountInfo(
        @Header("X-MBX-APIKEY") apiKey: String,
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String
    ): Response<BinanceAccountResponse>
}

// 응답 데이터 클래스
data class BinanceAccountResponse(
    val makerCommission: Int,
    val takerCommission: Int,
    val buyerCommission: Int,
    val sellerCommission: Int,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean,
    val updateTime: Long,
    val accountType: String,
    val balances: List<BalanceInfo>
)

data class BalanceInfo(
    val asset: String,
    val free: String,
    val locked: String
)

// API 키 정보 데이터 클래스
data class ApiKeyInfo(
    val apiKey: String,
    val secretKey: String,
    val isTestnet: Boolean,
    val isValid: Boolean = false,
    val lastValidated: Long = 0L
)

class ApiKeyService(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("binance_api_keys", Context.MODE_PRIVATE)
    private val encryptionKey = getOrCreateEncryptionKey()
    
    companion object {
        private const val TAG = "ApiKeyService"
        private const val PREF_API_KEY = "encrypted_api_key"
        private const val PREF_SECRET_KEY = "encrypted_secret_key"
        private const val PREF_IS_TESTNET = "is_testnet"
        private const val PREF_IS_VALID = "is_valid"
        private const val PREF_LAST_VALIDATED = "last_validated"
        private const val PREF_ENCRYPTION_KEY = "encryption_key"
        
        private const val BINANCE_MAINNET_BASE_URL = "https://api.binance.com/"
        private const val BINANCE_TESTNET_BASE_URL = "https://testnet.binance.vision/"
    }
    
    // 암호화 키 생성/가져오기
    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyString = prefs.getString(PREF_ENCRYPTION_KEY, null)
        
        return if (keyString != null) {
            // 기존 키 복원
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            SecretKeySpec(keyBytes, "AES")
        } else {
            // 새 키 생성
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val secretKey = keyGenerator.generateKey()
            
            // 키 저장
            val keyString = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
            prefs.edit().putString(PREF_ENCRYPTION_KEY, keyString).apply()
            
            secretKey
        }
    }
    
    // 문자열 암호화
    private fun encrypt(text: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }
    
    // 문자열 복호화
    private fun decrypt(encryptedText: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
        val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    }
    
    // API 키 저장
    fun saveApiKeys(apiKey: String, secretKey: String, isTestnet: Boolean) {
        try {
            val encryptedApiKey = encrypt(apiKey)
            val encryptedSecretKey = encrypt(secretKey)
            
            prefs.edit().apply {
                putString(PREF_API_KEY, encryptedApiKey)
                putString(PREF_SECRET_KEY, encryptedSecretKey)
                putBoolean(PREF_IS_TESTNET, isTestnet)
                putBoolean(PREF_IS_VALID, false) // 저장 시점에는 미검증
                putLong(PREF_LAST_VALIDATED, 0L)
                apply()
            }
            
            Log.d(TAG, "API 키가 암호화되어 저장되었습니다 (테스트넷: $isTestnet)")
        } catch (e: Exception) {
            Log.e(TAG, "API 키 저장 실패: ${e.message}", e)
            throw e
        }
    }
    
    // API 키 불러오기
    fun getApiKeys(): ApiKeyInfo? {
        return try {
            val encryptedApiKey = prefs.getString(PREF_API_KEY, null)
            val encryptedSecretKey = prefs.getString(PREF_SECRET_KEY, null)
            
            if (encryptedApiKey != null && encryptedSecretKey != null) {
                val apiKey = decrypt(encryptedApiKey)
                val secretKey = decrypt(encryptedSecretKey)
                val isTestnet = prefs.getBoolean(PREF_IS_TESTNET, true)
                val isValid = prefs.getBoolean(PREF_IS_VALID, false)
                val lastValidated = prefs.getLong(PREF_LAST_VALIDATED, 0L)
                
                ApiKeyInfo(apiKey, secretKey, isTestnet, isValid, lastValidated)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "API 키 불러오기 실패: ${e.message}", e)
            null
        }
    }
    
    // API 키 존재 여부 확인
    fun hasApiKeys(): Boolean {
        return prefs.contains(PREF_API_KEY) && prefs.contains(PREF_SECRET_KEY)
    }
    
    // API 키 삭제
    fun deleteApiKeys() {
        prefs.edit().apply {
            remove(PREF_API_KEY)
            remove(PREF_SECRET_KEY)
            remove(PREF_IS_TESTNET)
            remove(PREF_IS_VALID)
            remove(PREF_LAST_VALIDATED)
            apply()
        }
        Log.d(TAG, "저장된 API 키가 삭제되었습니다")
    }
    
    // HMAC-SHA256 서명 생성
    private fun createSignature(data: String, secretKey: String): String {
        val signingKey = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(signingKey)
        val rawHmac = mac.doFinal(data.toByteArray())
        return rawHmac.joinToString("") { "%02x".format(it) }
    }
    
    // API 키 유효성 검사
    suspend fun validateApiKeys(): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                val apiKeyInfo = getApiKeys()
                    ?: return@withContext Pair(false, "저장된 API 키가 없습니다")
                
                Log.d(TAG, "API 키 유효성 검사 시작 (테스트넷: ${apiKeyInfo.isTestnet})")
                
                // Retrofit 인스턴스 생성
                val baseUrl = if (apiKeyInfo.isTestnet) {
                    BINANCE_TESTNET_BASE_URL
                } else {
                    BINANCE_MAINNET_BASE_URL
                }
                
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val api = retrofit.create(BinanceTestApi::class.java)
                
                // 타임스탬프 생성
                val timestamp = System.currentTimeMillis()
                
                // 쿼리 스트링 생성
                val queryString = "timestamp=$timestamp"
                
                // 서명 생성
                val signature = createSignature(queryString, apiKeyInfo.secretKey)
                
                Log.d(TAG, "요청 URL: $baseUrl")
                Log.d(TAG, "API Key: ${apiKeyInfo.apiKey.take(8)}...")
                Log.d(TAG, "Timestamp: $timestamp")
                Log.d(TAG, "Signature: ${signature.take(8)}...")
                
                // API 호출
                val response = if (apiKeyInfo.isTestnet) {
                    api.getTestAccountInfo(apiKeyInfo.apiKey, timestamp, signature)
                } else {
                    api.getAccountInfo(apiKeyInfo.apiKey, timestamp, signature)
                }
                
                if (response.isSuccessful) {
                    val accountInfo = response.body()
                    Log.d(TAG, "✅ API 키 유효성 검사 성공!")
                    Log.d(TAG, "계정 타입: ${accountInfo?.accountType}")
                    Log.d(TAG, "거래 가능: ${accountInfo?.canTrade}")
                    
                    // 유효성 정보 저장
                    prefs.edit().apply {
                        putBoolean(PREF_IS_VALID, true)
                        putLong(PREF_LAST_VALIDATED, System.currentTimeMillis())
                        apply()
                    }
                    
                    Pair(true, "API 키가 유효합니다. 거래 가능: ${accountInfo?.canTrade}")
                } else {
                    Log.e(TAG, "❌ API 호출 실패: ${response.code()}")
                    Log.e(TAG, "오류 내용: ${response.errorBody()?.string()}")
                    
                    val errorMessage = when (response.code()) {
                        401 -> "API 키가 유효하지 않습니다"
                        403 -> "API 키 권한이 부족합니다"
                        429 -> "요청 한도를 초과했습니다"
                        else -> "API 연결 실패 (코드: ${response.code()})"
                    }
                    
                    Pair(false, errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "API 키 검증 중 오류: ${e.message}", e)
                Pair(false, "네트워크 오류: ${e.message}")
            }
        }
    }
    
    // 마지막 검증 시간으로부터 얼마나 지났는지 확인
    fun getTimeSinceLastValidation(): Long {
        val lastValidated = prefs.getLong(PREF_LAST_VALIDATED, 0L)
        return if (lastValidated > 0) {
            System.currentTimeMillis() - lastValidated
        } else {
            Long.MAX_VALUE
        }
    }
    
    // API 키 마스킹 (보안을 위해 일부만 표시)
    fun getMaskedApiKey(): String? {
        return try {
            val apiKeyInfo = getApiKeys()
            apiKeyInfo?.let { info ->
                val key = info.apiKey
                if (key.length > 8) {
                    "${key.take(4)}****${key.takeLast(4)}"
                } else {
                    "****"
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}