// RealBinanceApi.kt - 완전한 새 버전

package com.example.ver20.dao.binance

import android.util.Log
import com.example.ver20.dao.dataclass.AccountInfo
import com.example.ver20.dao.dataclass.AccountResponse
import com.example.ver20.dao.dataclass.ApiKeyData
import com.example.ver20.dao.dataclass.BalanceInfo
import com.example.ver20.dao.dataclass.BalanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ===========================================
// Spot API 인터페이스
// ===========================================

interface RealBinanceApi {
    @GET("api/v3/account")
    suspend fun getAccountInfo(
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceAccountResponse>

    @GET("api/v3/time")
    suspend fun getServerTime(): Response<ServerTimeResponse>

    @GET("api/v3/ticker/price")
    suspend fun getAllPrices(): Response<List<PriceResponse>>

    @GET("api/v3/ticker/price")
    suspend fun getPrice(@Query("symbol") symbol: String): Response<PriceResponse>
}

// ===========================================
// Futures API 인터페이스
// ===========================================

interface BinanceFuturesApi {
    @GET("fapi/v2/account")
    suspend fun getFuturesAccountInfo(
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceFuturesAccountResponse>

    @GET("fapi/v1/time")
    suspend fun getFuturesServerTime(): Response<ServerTimeResponse>
}

// ===========================================
// 기본 응답 데이터 클래스
// ===========================================

data class ServerTimeResponse(
    val serverTime: Long
)


// ===========================================
// Spot API 응답 데이터 클래스
// ===========================================

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
    val balances: List<BinanceBalance>,
    val permissions: List<String>
)

data class BinanceBalance(
    val asset: String,
    val free: String,
    val locked: String
)

// ===========================================
// Futures API 응답 데이터 클래스
// ===========================================

data class BinanceFuturesAccountResponse(
    val feeTier: Int,
    val canTrade: Boolean,
    val canDeposit: Boolean,
    val canWithdraw: Boolean,
    val updateTime: Long,
    val totalWalletBalance: String,
    val totalUnrealizedProfit: String,
    val totalMarginBalance: String,
    val availableBalance: String,
    val assets: List<BinanceFuturesAsset>,
    val positions: List<BinanceFuturesPosition>
)

data class BinanceFuturesAsset(
    val asset: String,
    val walletBalance: String,
    val unrealizedProfit: String,
    val marginBalance: String,
    val availableBalance: String
)

data class BinanceFuturesPosition(
    val symbol: String,
    val initialMargin: String,
    val maintMargin: String,
    val unrealizedProfit: String,
    val leverage: String,
    val isolated: Boolean,
    val entryPrice: String,
    val positionSide: String,
    val positionAmt: String,
    val updateTime: Long
)

// ===========================================
// 내부 데이터 클래스
// ===========================================

data class FuturesAccountInfo(
    val totalWalletBalance: Double,
    val totalUnrealizedProfit: Double,
    val totalMarginBalance: Double,
    val availableBalance: Double,
    val assets: List<FuturesAssetInfo>,
    val positions: List<FuturesPositionInfo>,
    val canTrade: Boolean,
    val canWithdraw: Boolean,
    val canDeposit: Boolean
)

data class FuturesAssetInfo(
    val asset: String,
    val walletBalance: Double,
    val unrealizedProfit: Double,
    val marginBalance: Double,
    val availableBalance: Double
)

data class FuturesPositionInfo(
    val symbol: String,
    val positionAmt: Double,
    val entryPrice: Double,
    val unrealizedProfit: Double,
    val leverage: String,
    val positionSide: String,
    val isolated: Boolean
)

// ===========================================
// 통합 바이낸스 서비스
// ===========================================

class RealBinanceService {
    companion object {
        private const val TAG = "RealBinanceService"
        private const val MAINNET_URL = "https://api.binance.com/"
        private const val TESTNET_URL = "https://testnet.binance.vision/"
        private const val FUTURES_MAINNET_URL = "https://fapi.binance.com/"
        private const val FUTURES_TESTNET_URL = "https://testnet.binancefuture.com/"
    }

    // Spot API Retrofit 인스턴스
    private val mainnetRetrofit = Retrofit.Builder()
        .baseUrl(MAINNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val testnetRetrofit = Retrofit.Builder()
        .baseUrl(TESTNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Futures API Retrofit 인스턴스
    private val futuresMainnetRetrofit = Retrofit.Builder()
        .baseUrl(FUTURES_MAINNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val futuresTestnetRetrofit = Retrofit.Builder()
        .baseUrl(FUTURES_TESTNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // API 인스턴스 생성 함수들
    private fun getSpotApi(isTestnet: Boolean): RealBinanceApi {
        return if (isTestnet) {
            testnetRetrofit.create(RealBinanceApi::class.java)
        } else {
            mainnetRetrofit.create(RealBinanceApi::class.java)
        }
    }

    private fun getFuturesApi(isTestnet: Boolean): BinanceFuturesApi {
        return if (isTestnet) {
            futuresTestnetRetrofit.create(BinanceFuturesApi::class.java)
        } else {
            futuresMainnetRetrofit.create(BinanceFuturesApi::class.java)
        }
    }

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

    // Spot 서버 시간 조회
    private suspend fun getSpotServerTime(isTestnet: Boolean): Long? {
        return try {
            val api = getSpotApi(isTestnet)
            val response = api.getServerTime()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.serverTime
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spot 서버 시간 조회 실패: ${e.message}")
            null
        }
    }

    // Futures 서버 시간 조회
    private suspend fun getFuturesServerTime(isTestnet: Boolean): Long? {
        return try {
            val api = getFuturesApi(isTestnet)
            val response = api.getFuturesServerTime()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.serverTime
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Futures 서버 시간 조회 실패: ${e.message}")
            null
        }
    }

    // Spot 계좌 정보 조회
    suspend fun getAccountInfo(apiKeyData: ApiKeyData): AccountResponse = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 Spot 계좌 정보 조회 시작")

            val api = getSpotApi(apiKeyData.isTestnet)
            val serverTime = getSpotServerTime(apiKeyData.isTestnet)

            if (serverTime == null) {
                return@withContext AccountResponse(false, null, "Spot 서버 시간 조회 실패")
            }

            val timestamp = serverTime
            val queryString = "timestamp=$timestamp"
            val signature = createSignature(queryString, apiKeyData.secretKey)

            if (signature.isEmpty()) {
                return@withContext AccountResponse(false, null, "서명 생성 실패")
            }

            val response = api.getAccountInfo(timestamp, signature, apiKeyData.apiKey)

            if (response.isSuccessful && response.body() != null) {
                val binanceAccount = response.body()!!
                val accountInfo = AccountInfo(
                    makerCommission = binanceAccount.makerCommission,
                    takerCommission = binanceAccount.takerCommission,
                    buyerCommission = binanceAccount.buyerCommission,
                    sellerCommission = binanceAccount.sellerCommission,
                    canTrade = binanceAccount.canTrade,
                    canWithdraw = binanceAccount.canWithdraw,
                    canDeposit = binanceAccount.canDeposit,
                    updateTime = binanceAccount.updateTime,
                    accountType = "SPOT",
                    balances = binanceAccount.balances.map { balance ->
                        BalanceInfo(balance.asset, balance.free, balance.locked)
                    },
                    permissions = binanceAccount.permissions
                )

                Log.d(TAG, "✅ Spot 계좌 정보 조회 성공! 자산 개수: ${accountInfo.balances.size}")
                AccountResponse(true, accountInfo, "Spot 계좌 정보 조회 성공")
            } else {
                val errorMessage = when (response.code()) {
                    401 -> "API 키 인증 실패"
                    403 -> "API 권한 부족 또는 IP 제한"
                    429 -> "API 요청 한도 초과"
                    else -> "API 호출 실패: ${response.code()}"
                }
                AccountResponse(false, null, errorMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Spot 계좌 정보 조회 예외: ${e.message}")
            AccountResponse(false, null, "네트워크 오류: ${e.message}")
        }
    }

    // Futures 계좌 정보 조회
    suspend fun getFuturesAccountInfo(apiKeyData: ApiKeyData): Pair<Boolean, FuturesAccountInfo?> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 Futures 계좌 정보 조회 시작")

            val api = getFuturesApi(apiKeyData.isTestnet)
            val serverTime = getFuturesServerTime(apiKeyData.isTestnet)

            if (serverTime == null) {
                Log.e(TAG, "❌ Futures 서버 시간 조회 실패")
                return@withContext Pair(false, null)
            }

            val timestamp = serverTime
            val queryString = "timestamp=$timestamp"
            val signature = createSignature(queryString, apiKeyData.secretKey)

            if (signature.isEmpty()) {
                Log.e(TAG, "❌ Futures 서명 생성 실패")
                return@withContext Pair(false, null)
            }

            val response = api.getFuturesAccountInfo(timestamp, signature, apiKeyData.apiKey)

            if (response.isSuccessful && response.body() != null) {
                val futuresAccount = response.body()!!

                val futuresInfo = FuturesAccountInfo(
                    totalWalletBalance = futuresAccount.totalWalletBalance.toDoubleOrNull() ?: 0.0,
                    totalUnrealizedProfit = futuresAccount.totalUnrealizedProfit.toDoubleOrNull() ?: 0.0,
                    totalMarginBalance = futuresAccount.totalMarginBalance.toDoubleOrNull() ?: 0.0,
                    availableBalance = futuresAccount.availableBalance.toDoubleOrNull() ?: 0.0,
                    canTrade = futuresAccount.canTrade,
                    canWithdraw = futuresAccount.canWithdraw,
                    canDeposit = futuresAccount.canDeposit,
                    assets = futuresAccount.assets.map { asset ->
                        FuturesAssetInfo(
                            asset = asset.asset,
                            walletBalance = asset.walletBalance.toDoubleOrNull() ?: 0.0,
                            unrealizedProfit = asset.unrealizedProfit.toDoubleOrNull() ?: 0.0,
                            marginBalance = asset.marginBalance.toDoubleOrNull() ?: 0.0,
                            availableBalance = asset.availableBalance.toDoubleOrNull() ?: 0.0
                        )
                    }.filter { it.walletBalance > 0 || it.marginBalance > 0 },
                    positions = futuresAccount.positions.map { position ->
                        FuturesPositionInfo(
                            symbol = position.symbol,
                            positionAmt = position.positionAmt.toDoubleOrNull() ?: 0.0,
                            entryPrice = position.entryPrice.toDoubleOrNull() ?: 0.0,
                            unrealizedProfit = position.unrealizedProfit.toDoubleOrNull() ?: 0.0,
                            leverage = position.leverage,
                            positionSide = position.positionSide,
                            isolated = position.isolated
                        )
                    }.filter { it.positionAmt != 0.0 }
                )

                Log.d(TAG, "✅ Futures 계좌 정보 조회 성공!")
                Log.d(TAG, "💰 총 지갑 잔고: ${futuresInfo.totalWalletBalance}")
                Log.d(TAG, "📊 자산 개수: ${futuresInfo.assets.size}")
                Log.d(TAG, "📈 포지션 개수: ${futuresInfo.positions.size}")

                Pair(true, futuresInfo)
            } else {
                Log.e(TAG, "❌ Futures 계좌 정보 조회 실패: ${response.code()}")
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Futures 계좌 정보 조회 예외: ${e.message}")
            Pair(false, null)
        }
    }

    // 가격 정보 조회
    suspend fun getAllPrices(isTestnet: Boolean): Map<String, Double> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 가격 정보 조회 중...")
            val api = getSpotApi(isTestnet)
            val response = api.getAllPrices()

            if (response.isSuccessful && response.body() != null) {
                val prices = response.body()!!.associate { priceResponse ->
                    priceResponse.symbol to (priceResponse.price.toDoubleOrNull() ?: 0.0)
                }
                Log.d(TAG, "✅ 가격 정보 조회 성공: ${prices.size}개")
                prices
            } else {
                Log.e(TAG, "❌ 가격 정보 조회 실패: ${response.code()}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 가격 정보 조회 예외: ${e.message}")
            emptyMap()
        }
    }

    // 통합 계좌 정보 조회 (Spot + Futures + 가격정보)
    suspend fun getIntegratedAccountInfo(apiKeyData: ApiKeyData): Triple<AccountResponse, FuturesAccountInfo?, Map<String, Double>> {
        Log.d(TAG, "🚀 통합 계좌 정보 조회 시작")

        val spotResponse = getAccountInfo(apiKeyData)
        val (futuresSuccess, futuresInfo) = getFuturesAccountInfo(apiKeyData)
        val priceMap = getAllPrices(apiKeyData.isTestnet)

        Log.d(TAG, "📊 통합 조회 완료 - Spot: ${spotResponse.success}, Futures: $futuresSuccess")

        return Triple(spotResponse, futuresInfo, priceMap)
    }
}

// ===========================================
// 유틸리티 함수들
// ===========================================

// Futures 자산을 BalanceInfo 형식으로 변환
fun FuturesAssetInfo.toBalanceInfo(): BalanceInfo {
    return BalanceInfo(
        asset = this.asset,
        free = this.availableBalance.toString(),
        locked = (this.walletBalance - this.availableBalance).toString()
    )
}

// 통합 자산 분류 함수
suspend fun classifyIntegratedAssets(
    spotBalances: List<BalanceInfo>,
    futuresInfo: FuturesAccountInfo?,
    priceMap: Map<String, Double>,
    callback: (List<BalanceInfo>, List<BalanceInfo>, List<BalanceInfo>, Double, Double, Double) -> Unit
) {
    val nonZeroSpotBalances = BalanceUtils.getNonZeroBalances(spotBalances)

    // Futures 자산을 BalanceInfo 형식으로 변환
    val futuresBalances = futuresInfo?.assets?.map { it.toBalanceInfo() } ?: emptyList()
    val nonZeroFuturesBalances = BalanceUtils.getNonZeroBalances(futuresBalances)

    // Spot 자산: 모든 현물 거래 자산
    val spotAssets = nonZeroSpotBalances

    // Earn 자산: 스테이킹 가능한 주요 암호화폐 (최소 10달러 이상)
    val earnCandidates = listOf("BNB", "ETH", "ADA", "DOT", "SOL", "MATIC", "AVAX", "ATOM")
    val earnAssets = nonZeroSpotBalances.filter { balance ->
        if (balance.asset in earnCandidates) {
            val totalAmount = BalanceUtils.getTotalBalance(balance)
            val usdValue = calculateSimpleUSDValue(balance.asset, totalAmount, priceMap)
            usdValue >= 10.0
        } else false
    }

    // Futures 자산: 실제 선물 계좌의 자산들
    val futuresAssets = nonZeroFuturesBalances

    // 각 탭별 실제 USD 총액 계산
    val spotTotalUSD = calculateTotalUSDValue(spotAssets, priceMap)
    val earnTotalUSD = calculateTotalUSDValue(earnAssets, priceMap)
    val futuresTotalUSD = calculateTotalUSDValue(futuresAssets, priceMap)

    Log.d("AssetClassify", "💰 자산 분류 완료 - Spot: $${spotTotalUSD}, Earn: $${earnTotalUSD}, Futures: $${futuresTotalUSD}")

    callback(spotAssets, earnAssets, futuresAssets, spotTotalUSD, earnTotalUSD, futuresTotalUSD)
}

// 간단한 USD 가치 계산
suspend fun calculateSimpleUSDValue(asset: String, amount: Double, priceMap: Map<String, Double>): Double {
    return when (asset) {
        "USDT", "BUSD", "USDC", "FDUSD" -> amount
        else -> {
            val usdtPair = "${asset}USDT"
            val price = priceMap[usdtPair] ?: 0.0
            amount * price
        }
    }
}

// 총 USD 가치 계산
suspend fun calculateTotalUSDValue(balances: List<BalanceInfo>, priceMap: Map<String, Double>): Double {
    return balances.sumOf { balance ->
        val totalAmount = BalanceUtils.getTotalBalance(balance)
        calculateSimpleUSDValue(balance.asset, totalAmount, priceMap)
    }
}