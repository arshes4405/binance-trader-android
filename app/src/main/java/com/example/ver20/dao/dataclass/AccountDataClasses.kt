// AccountDataClasses.kt - 계좌 관련 모든 데이터 클래스 및 서비스 통합

package com.example.ver20.dao.dataclass

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.delay

// ===========================================
// 데이터 클래스들
// ===========================================

// API 키 데이터
data class ApiKeyData(
    val apiKey: String,
    val secretKey: String,
    val isTestnet: Boolean = true,
    val label: String = "Main Trading"
)

// 계좌 정보
data class AccountInfo(
    val makerCommission: Int,
    val takerCommission: Int,
    val buyerCommission: Int,
    val sellerCommission: Int,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean,
    val updateTime: Long,
    val accountType: String,
    val balances: List<BalanceInfo>,
    val permissions: List<String>
)

// 잔고 정보
data class BalanceInfo(
    val asset: String,
    val free: String,
    val locked: String
)

// API 응답 래퍼
data class AccountResponse(
    val success: Boolean,
    val data: AccountInfo? = null,
    val message: String? = null
)

// ===========================================
// 서비스 클래스들
// ===========================================

// API 키 서비스 (보안 강화)
class ApiKeyService(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("api_keys", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY_PREF = "binance_api_key"
        private const val SECRET_KEY_PREF = "binance_secret_key"
        private const val IS_TESTNET_PREF = "is_testnet"
        private const val HAS_KEYS_PREF = "has_keys"
        private const val TAG = "ApiKeyService"
    }

    // API 키 저장 (Base64 인코딩)
    fun saveApiKeys(apiKey: String, secretKey: String, isTestnet: Boolean = true): Boolean {
        return try {
            Log.d(TAG, "API 키 저장 중...")

            // 입력 유효성 검사
            if (apiKey.isBlank() || secretKey.isBlank()) {
                Log.e(TAG, "❌ API 키 또는 Secret 키가 비어있음")
                return false
            }

            // Base64 인코딩으로 간단한 암호화
            val encodedApiKey = Base64.encodeToString(apiKey.toByteArray(), Base64.DEFAULT)
            val encodedSecretKey = Base64.encodeToString(secretKey.toByteArray(), Base64.DEFAULT)

            prefs.edit().apply {
                putString(API_KEY_PREF, encodedApiKey)
                putString(SECRET_KEY_PREF, encodedSecretKey)
                putBoolean(IS_TESTNET_PREF, isTestnet)
                putBoolean(HAS_KEYS_PREF, true)
                apply()
            }

            Log.d(TAG, "✅ API 키 저장 완료 (테스트넷: $isTestnet)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ API 키 저장 실패: ${e.message}")
            false
        }
    }

    // API 키 조회 (Base64 디코딩)
    fun getApiKeys(): ApiKeyData? {
        return try {
            val hasKeys = prefs.getBoolean(HAS_KEYS_PREF, false)
            if (!hasKeys) {
                Log.d(TAG, "⚠️ 저장된 API 키 없음")
                return null
            }

            val encodedApiKey = prefs.getString(API_KEY_PREF, null)
            val encodedSecretKey = prefs.getString(SECRET_KEY_PREF, null)
            val isTestnet = prefs.getBoolean(IS_TESTNET_PREF, true)

            if (encodedApiKey != null && encodedSecretKey != null) {
                val apiKey = String(Base64.decode(encodedApiKey, Base64.DEFAULT))
                val secretKey = String(Base64.decode(encodedSecretKey, Base64.DEFAULT))

                Log.d(TAG, "✅ API 키 조회 성공 (테스트넷: $isTestnet)")
                ApiKeyData(apiKey, secretKey, isTestnet)
            } else {
                Log.e(TAG, "❌ API 키 디코딩 실패")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ API 키 조회 실패: ${e.message}")
            null
        }
    }

    // API 키 삭제
    fun clearApiKeys(): Boolean {
        return try {
            prefs.edit().clear().apply()
            Log.d(TAG, "✅ API 키 삭제 완료")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ API 키 삭제 실패: ${e.message}")
            false
        }
    }

    // API 키 존재 여부 확인
    fun hasApiKeys(): Boolean {
        return prefs.getBoolean(HAS_KEYS_PREF, false) &&
                prefs.contains(API_KEY_PREF) &&
                prefs.contains(SECRET_KEY_PREF)
    }

    // 테스트넷/메인넷 모드 변경
    fun switchNetwork(isTestnet: Boolean): Boolean {
        return try {
            if (!hasApiKeys()) {
                Log.e(TAG, "❌ API 키가 없어서 네트워크 모드 변경 불가")
                return false
            }

            prefs.edit().apply {
                putBoolean(IS_TESTNET_PREF, isTestnet)
                apply()
            }

            Log.d(TAG, "✅ 네트워크 모드 변경: ${if (isTestnet) "테스트넷" else "메인넷"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ 네트워크 모드 변경 실패: ${e.message}")
            false
        }
    }

    // API 키 유효성 검사
    fun validateApiKeys(apiKey: String, secretKey: String): Boolean {
        // 바이낸스 API 키 형식 검사
        val apiKeyPattern = Regex("^[A-Za-z0-9]{64}$")
        val secretKeyPattern = Regex("^[A-Za-z0-9]{64}$")

        return apiKeyPattern.matches(apiKey) && secretKeyPattern.matches(secretKey)
    }
}

// 계좌 서비스
class AccountService {
    companion object {
        private const val TAG = "AccountService"
    }

    suspend fun getAccountInfo(apiKeyData: ApiKeyData): AccountResponse {
        return try {
            Log.d(TAG, "계좌 정보 조회 시작 (테스트넷: ${apiKeyData.isTestnet})")

            // 네트워크 지연 시뮬레이션
            delay(1500)

            // 더미 데이터 생성 (실제로는 바이낸스 API 호출)
            val dummyAccountInfo = createDummyAccountInfo(apiKeyData.isTestnet)

            Log.d(TAG, "✅ 계좌 정보 조회 성공")
            AccountResponse(
                success = true,
                data = dummyAccountInfo,
                message = "계좌 정보 조회 성공"
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 계좌 정보 조회 실패: ${e.message}")
            AccountResponse(
                success = false,
                data = null,
                message = "계좌 정보 조회 실패: ${e.message}"
            )
        }
    }

    private fun createDummyAccountInfo(isTestnet: Boolean): AccountInfo {
        val balances = if (isTestnet) {
            // 테스트넷 더미 데이터 (풍부한 자산)
            listOf(
                BalanceInfo("BTC", "0.00123456", "0.00000000"),
                BalanceInfo("ETH", "1.23456789", "0.00000000"),
                BalanceInfo("USDT", "10000.50000000", "500.00000000"),
                BalanceInfo("BNB", "50.50000000", "0.00000000"),
                BalanceInfo("ADA", "1000.00000000", "0.00000000"),
                BalanceInfo("DOT", "100.50000000", "0.00000000")
            )
        } else {
            // 메인넷 더미 데이터 (실제적인 금액)
            listOf(
                BalanceInfo("BTC", "0.00050000", "0.00000000"),
                BalanceInfo("ETH", "0.12345678", "0.00000000"),
                BalanceInfo("USDT", "100.50000000", "50.00000000"),
                BalanceInfo("BNB", "5.50000000", "0.00000000")
            )
        }

        return AccountInfo(
            makerCommission = 15,
            takerCommission = 15,
            buyerCommission = 0,
            sellerCommission = 0,
            canTrade = true,
            canWithdraw = !isTestnet, // 테스트넷에서는 출금 불가
            canDeposit = true,
            updateTime = System.currentTimeMillis(),
            accountType = "SPOT",
            balances = balances,
            permissions = listOf("SPOT")
        )
    }

    // 실제 바이낸스 API 호출 함수 (미래 구현용)
    private suspend fun callBinanceApi(apiKeyData: ApiKeyData): AccountInfo? {
        // TODO: 실제 바이낸스 API 호출 구현
        // - HMAC-SHA256 서명 생성
        // - HTTP 요청 헤더 설정
        // - API 엔드포인트 호출
        return null
    }
}

// ===========================================
// 유틸리티 함수들
// ===========================================

// 잔고 계산 유틸리티
object BalanceUtils {
    fun getTotalBalance(balance: BalanceInfo): Double {
        val free = balance.free.toDoubleOrNull() ?: 0.0
        val locked = balance.locked.toDoubleOrNull() ?: 0.0
        return free + locked
    }

    fun getNonZeroBalances(balances: List<BalanceInfo>): List<BalanceInfo> {
        return balances.filter { balance ->
            getTotalBalance(balance) > 0.0
        }
    }

    fun calculateTotalUSDValue(balances: List<BalanceInfo>): Double {
        // 실제로는 각 코인의 현재 가격을 조회해야 하지만,
        // 여기서는 USDT, BUSD만 USD 가치로 계산
        return balances.sumOf { balance ->
            val total = getTotalBalance(balance)
            when (balance.asset) {
                "USDT", "BUSD", "USDC" -> total
                else -> 0.0 // 다른 코인들은 가격 조회 API가 필요
            }
        }
    }

    fun formatBalance(amount: Double): String {
        return when {
            amount >= 1000000 -> String.format("%.2fM", amount / 1000000)
            amount >= 1000 -> String.format("%.2fK", amount / 1000)
            amount >= 1 -> String.format("%.4f", amount)
            else -> String.format("%.8f", amount)
        }
    }

    // 자산별 우선순위 정렬
    fun sortBalancesByPriority(balances: List<BalanceInfo>): List<BalanceInfo> {
        val priorityOrder = listOf("USDT", "BUSD", "USDC", "BTC", "ETH", "BNB")

        return balances.sortedWith { a, b ->
            val aPriority = priorityOrder.indexOf(a.asset)
            val bPriority = priorityOrder.indexOf(b.asset)

            when {
                aPriority != -1 && bPriority != -1 -> aPriority.compareTo(bPriority)
                aPriority != -1 -> -1
                bPriority != -1 -> 1
                else -> a.asset.compareTo(b.asset)
            }
        }
    }
}