package com.example.ver20.view

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ver20.dao.ApiKeyService
import com.example.ver20.dao.BalanceInfo

// 계좌 조회 결과 데이터 클래스
data class AccountResult(
    val success: Boolean,
    val balances: List<BalanceInfo>?,
    val accountInfo: Map<String, Any>?,
    val errorMessage: String?
)

class AccountService {

    companion object {
        private const val TAG = "AccountService"
    }

    // Spot 계좌 정보 조회 (샘플 데이터)
    suspend fun getSpotAccountInfo(apiKeyService: ApiKeyService): AccountResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiKeyInfo = apiKeyService.getApiKeys()
                    ?: return@withContext AccountResult(false, null, null, "저장된 API 키가 없습니다")

                Log.d(TAG, "Spot 계좌 정보 조회 시작 (테스트넷: ${apiKeyInfo.isTestnet})")

                // 테스트넷인 경우 샘플 데이터 반환
                if (apiKeyInfo.isTestnet) {
                    Log.d(TAG, "테스트넷 샘플 데이터 반환")
                    val sampleBalances = listOf(
                        BalanceInfo("BTC", "1.00000000", "0.00000000"),
                        BalanceInfo("USDT", "10000.00000000", "0.00000000"),
                        BalanceInfo("ETH", "10.00000000", "0.00000000"),
                        BalanceInfo("BNB", "100.00000000", "0.00000000")
                    )
                    val sampleAccountInfo = mapOf(
                        "accountType" to "SPOT",
                        "canTrade" to true,
                        "canWithdraw" to true,
                        "canDeposit" to true,
                        "updateTime" to System.currentTimeMillis()
                    )
                    return@withContext AccountResult(true, sampleBalances, sampleAccountInfo, null)
                }

                // 메인넷인 경우 실제 API 호출 (나중에 구현)
                AccountResult(false, null, null, "메인넷 API 연동은 아직 구현되지 않았습니다")

            } catch (e: Exception) {
                Log.e(TAG, "Spot 계좌 정보 조회 중 오류: ${e.message}", e)
                AccountResult(false, null, null, "네트워크 오류: ${e.message}")
            }
        }
    }

    // Future 계좌 정보 조회 (샘플 데이터)
    suspend fun getFutureAccountInfo(apiKeyService: ApiKeyService): AccountResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiKeyInfo = apiKeyService.getApiKeys()
                    ?: return@withContext AccountResult(false, null, null, "저장된 API 키가 없습니다")

                Log.d(TAG, "Future 계좌 정보 조회 시작 (테스트넷: ${apiKeyInfo.isTestnet})")

                // 테스트넷인 경우 샘플 데이터 반환
                if (apiKeyInfo.isTestnet) {
                    Log.d(TAG, "테스트넷 Future 샘플 데이터 반환")
                    val sampleBalances = listOf(
                        BalanceInfo("USDT", "1000.00000000", "0.00000000"),
                        BalanceInfo("BTC", "0.01000000", "0.005000000")
                    )
                    val sampleAccountInfo = mapOf(
                        "accountType" to "FUTURE",
                        "canTrade" to true,
                        "canWithdraw" to true,
                        "canDeposit" to true,
                        "totalWalletBalance" to "1000.00",
                        "totalMarginBalance" to "1000.00",
                        "totalUnrealizedProfit" to "0.00",
                        "availableBalance" to "1000.00"
                    )
                    return@withContext AccountResult(true, sampleBalances, sampleAccountInfo, null)
                }

                // 메인넷인 경우 실제 API 호출 (나중에 구현)
                AccountResult(false, null, null, "메인넷 Future API 연동은 아직 구현되지 않았습니다")

            } catch (e: Exception) {
                Log.e(TAG, "Future 계좌 정보 조회 중 오류: ${e.message}", e)
                AccountResult(false, null, null, "네트워크 오류: ${e.message}")
            }
        }
    }
}