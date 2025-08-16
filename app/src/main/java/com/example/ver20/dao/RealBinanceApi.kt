// RealBinanceApi.kt - 실제 바이낸스 API 연동

package com.example.ver20.dao

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// ===========================================
// 바이낸스 API 인터페이스
// ===========================================

interface RealBinanceApi {
    // 계좌 정보 조회 (Spot)
    @GET("api/v3/account")
    suspend fun getAccountInfo(
        @Query("timestamp") timestamp: Long,
        @Query("signature") signature: String,
        @Header("X-MBX-APIKEY") apiKey: String
    ): Response<BinanceAccountResponse>

    // 서버 시간 조회
    @GET("api/v3/time")
    suspend fun getServerTime(): Response<ServerTimeResponse>

    // 24시간 가격 변동 통계
    @GET("api/v3/ticker/24hr")
    suspend fun get24hrTicker(): Response<List<TickerResponse>>

    // 특정 심볼 가격 조회
    @GET("api/v3/ticker/price")
    suspend fun getPrice(@Query("symbol") symbol: String): Response<PriceResponse>

    // 모든 심볼 가격 조회
    @GET("api/v3/ticker/price")
    suspend fun getAllPrices(): Response<List<PriceResponse>>
}

// ===========================================
// 응답 데이터 클래스
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

data class ServerTimeResponse(
    val serverTime: Long
)

data class TickerResponse(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val lastPrice: String,
    val volume: String,
    val count: Int
)

// ===========================================
// 실제 바이낸스 서비스
// ===========================================

class RealBinanceService {
    companion object {
        private const val TAG = "RealBinanceService"
        private const val MAINNET_URL = "https://api.binance.com/"
        private const val TESTNET_URL = "https://testnet.binance.vision/"
    }

    private val mainnetRetrofit = Retrofit.Builder()
        .baseUrl(MAINNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val testnetRetrofit = Retrofit.Builder()
        .baseUrl(TESTNET_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private fun getApi(isTestnet: Boolean): RealBinanceApi {
        return if (isTestnet) {
            testnetRetrofit.create(RealBinanceApi::class.java)
        } else {
            mainnetRetrofit.create(RealBinanceApi::class.java)
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

    // 서버 시간 조회
    suspend fun getServerTime(isTestnet: Boolean): Long? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 서버 시간 조회 중...")
            val api = getApi(isTestnet)
            val response = api.getServerTime()
            
            if (response.isSuccessful && response.body() != null) {
                val serverTime = response.body()!!.serverTime
                Log.d(TAG, "✅ 서버 시간 조회 성공: $serverTime")
                serverTime
            } else {
                Log.e(TAG, "❌ 서버 시간 조회 실패: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 서버 시간 조회 예외: ${e.message}")
            null
        }
    }

    // 실제 계좌 정보 조회
    suspend fun getAccountInfo(apiKeyData: ApiKeyData): AccountResponse = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 실제 계좌 정보 조회 시작 (테스트넷: ${apiKeyData.isTestnet})")
            
            val api = getApi(apiKeyData.isTestnet)
            
            // 1단계: 서버 시간 조회
            val serverTime = getServerTime(apiKeyData.isTestnet)
            if (serverTime == null) {
                return@withContext AccountResponse(
                    success = false,
                    data = null,
                    message = "서버 시간 조회 실패"
                )
            }

            // 2단계: 서명 생성
            val timestamp = serverTime
            val queryString = "timestamp=$timestamp"
            val signature = createSignature(queryString, apiKeyData.secretKey)
            
            if (signature.isEmpty()) {
                return@withContext AccountResponse(
                    success = false,
                    data = null,
                    message = "서명 생성 실패"
                )
            }

            Log.d(TAG, "🔐 서명 생성 완료")

            // 3단계: API 호출
            val response = api.getAccountInfo(timestamp, signature, apiKeyData.apiKey)
            
            if (response.isSuccessful && response.body() != null) {
                val binanceAccount = response.body()!!
                
                // 바이낸스 응답을 내부 데이터 형식으로 변환
                val accountInfo = AccountInfo(
                    makerCommission = binanceAccount.makerCommission,
                    takerCommission = binanceAccount.takerCommission,
                    buyerCommission = binanceAccount.buyerCommission,
                    sellerCommission = binanceAccount.sellerCommission,
                    canTrade = binanceAccount.canTrade,
                    canWithdraw = binanceAccount.canWithdraw,
                    canDeposit = binanceAccount.canDeposit,
                    updateTime = binanceAccount.updateTime,
                    accountType = binanceAccount.accountType,
                    balances = binanceAccount.balances.map { balance ->
                        BalanceInfo(
                            asset = balance.asset,
                            free = balance.free,
                            locked = balance.locked
                        )
                    },
                    permissions = binanceAccount.permissions
                )

                Log.d(TAG, "✅ 실제 계좌 정보 조회 성공! 자산 개수: ${accountInfo.balances.size}")
                
                AccountResponse(
                    success = true,
                    data = accountInfo,
                    message = "계좌 정보 조회 성공"
                )
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ 계좌 정보 조회 실패: ${response.code()}, $errorBody")
                
                val errorMessage = when (response.code()) {
                    401 -> "API 키 인증 실패. API 키와 Secret 키를 확인해주세요."
                    403 -> "API 권한 부족. IP 제한이나 권한 설정을 확인해주세요."
                    429 -> "API 요청 한도 초과. 잠시 후 다시 시도해주세요."
                    else -> "API 호출 실패: ${response.code()}"
                }
                
                AccountResponse(
                    success = false,
                    data = null,
                    message = errorMessage
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 계좌 정보 조회 예외: ${e.message}")
            AccountResponse(
                success = false,
                data = null,
                message = "네트워크 오류: ${e.message}"
            )
        }
    }

    // 모든 가격 정보 조회 (USD 환산용)
    suspend fun getAllPrices(isTestnet: Boolean): Map<String, Double> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 가격 정보 조회 중...")
            val api = getApi(isTestnet)
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
}

// ===========================================
// 업데이트된 AccountService
// ===========================================

class RealAccountService {
    companion object {
        private const val TAG = "RealAccountService"
    }

    private val binanceService = RealBinanceService()

    suspend fun getAccountInfo(apiKeyData: ApiKeyData): AccountResponse {
        Log.d(TAG, "📊 실제 바이낸스 계좌 정보 조회 시작")
        
        // 실제 바이낸스 API 호출
        return binanceService.getAccountInfo(apiKeyData)
    }

    // USD 환산 가치 계산을 위한 가격 정보 조회
    suspend fun getPricesForUSDCalculation(isTestnet: Boolean): Map<String, Double> {
        return binanceService.getAllPrices(isTestnet)
    }
}

// ===========================================
// 향상된 BalanceUtils (실제 가격 사용)
// ===========================================

object EnhancedBalanceUtils {
    private const val TAG = "EnhancedBalanceUtils"

    // 실제 바이낸스 가격을 사용한 USD 환산
    suspend fun calculateRealUSDValue(
        asset: String, 
        amount: Double, 
        priceMap: Map<String, Double>
    ): Double {
        return when (asset) {
            "USDT", "BUSD", "USDC", "FDUSD" -> amount // 스테이블코인은 1:1
            else -> {
                // USDT 페어 가격 찾기
                val usdtPair = "${asset}USDT"
                val price = priceMap[usdtPair]
                
                if (price != null && price > 0) {
                    val usdValue = amount * price
                    Log.d(TAG, "💰 $asset: ${amount} * $price = $$usdValue")
                    usdValue
                } else {
                    Log.w(TAG, "⚠️ $asset 가격 정보 없음")
                    0.0
                }
            }
        }
    }

    // 자산 리스트의 총 USD 가치 계산
    suspend fun calculateTotalRealUSDValue(
        balances: List<BalanceInfo>,
        priceMap: Map<String, Double>
    ): Double {
        return balances.sumOf { balance ->
            val totalAmount = BalanceUtils.getTotalBalance(balance)
            calculateRealUSDValue(balance.asset, totalAmount, priceMap)
        }
    }

    // 개별 자산의 USD 가치 포함한 확장 정보
    data class BalanceWithUSD(
        val balance: BalanceInfo,
        val usdValue: Double,
        val price: Double?
    )

    // 자산별 USD 가치와 함께 반환
    suspend fun getBalancesWithUSDValue(
        balances: List<BalanceInfo>,
        priceMap: Map<String, Double>
    ): List<BalanceWithUSD> {
        return balances.map { balance ->
            val totalAmount = BalanceUtils.getTotalBalance(balance)
            val usdValue = calculateRealUSDValue(balance.asset, totalAmount, priceMap)
            val price = when (balance.asset) {
                "USDT", "BUSD", "USDC", "FDUSD" -> 1.0
                else -> priceMap["${balance.asset}USDT"]
            }
            
            BalanceWithUSD(balance, usdValue, price)
        }
    }
}

// ===========================================
// 통합 데이터 로드 함수
// ===========================================

suspend fun loadRealAccountData(
    accountService: RealAccountService,
    apiKeyData: ApiKeyData,
    callback: (AccountInfo?, List<BalanceInfo>, Double, String?) -> Unit
) {
    try {
        Log.d("LoadData", "🚀 실제 바이낸스 데이터 로드 시작")
        
        // 1단계: 계좌 정보 조회
        val accountResponse = accountService.getAccountInfo(apiKeyData)
        
        if (!accountResponse.success || accountResponse.data == null) {
            callback(null, emptyList(), 0.0, accountResponse.message)
            return
        }
        
        val accountInfo = accountResponse.data
        val nonZeroBalances = BalanceUtils.getNonZeroBalances(accountInfo.balances)
        
        Log.d("LoadData", "📊 0이 아닌 자산 개수: ${nonZeroBalances.size}")
        
        // 2단계: 가격 정보 조회 (USD 환산용)
        val priceMap = accountService.getPricesForUSDCalculation(apiKeyData.isTestnet)
        
        // 3단계: 총 USD 가치 계산
        val totalUSD = EnhancedBalanceUtils.calculateTotalRealUSDValue(nonZeroBalances, priceMap)
        
        Log.d("LoadData", "💰 총 자산 가치: $$totalUSD")
        
        callback(accountInfo, nonZeroBalances, totalUSD, null)
        
    } catch (e: Exception) {
        Log.e("LoadData", "❌ 데이터 로드 예외: ${e.message}")
        callback(null, emptyList(), 0.0, "데이터 로드 오류: ${e.message}")
    }
}

// 탭별 자산 분류 (실제 가격 반영)
suspend fun classifyRealBalancesByTab(
    balances: List<BalanceInfo>,
    priceMap: Map<String, Double>,
    callback: (List<BalanceInfo>, List<BalanceInfo>, List<BalanceInfo>, Double, Double, Double) -> Unit
) {
    val nonZeroBalances = BalanceUtils.getNonZeroBalances(balances)
    
    // Spot 자산 (일반 거래 자산)
    val spotAssets = listOf("BTC", "ETH", "BNB", "ADA", "DOT", "LINK", "LTC", "XRP", "DOGE", "SOL", "MATIC")
    val spotBalances = nonZeroBalances.filter { it.asset in spotAssets }
    
    // Earn 자산 (스테이킹 가능한 자산 - 최소 10달러 이상)
    val earnAssets = listOf("BNB", "ETH", "ADA", "DOT", "SOL", "MATIC")
    val earnBalances = nonZeroBalances.filter { balance ->
        if (balance.asset in earnAssets) {
            val totalAmount = BalanceUtils.getTotalBalance(balance)
            val usdValue = EnhancedBalanceUtils.calculateRealUSDValue(balance.asset, totalAmount, priceMap)
            usdValue >= 10.0 // 최소 10달러 이상만
        } else false
    }
    
    // Futures 자산 (선물 거래용 마진)
    val futuresAssets = listOf("USDT", "BUSD", "USDC", "FDUSD")
    val futuresBalances = nonZeroBalances.filter { it.asset in futuresAssets }
    
    // 각 탭별 실제 USD 총액 계산
    val spotTotalUSD = EnhancedBalanceUtils.calculateTotalRealUSDValue(spotBalances, priceMap)
    val earnTotalUSD = EnhancedBalanceUtils.calculateTotalRealUSDValue(earnBalances, priceMap)
    val futuresTotalUSD = EnhancedBalanceUtils.calculateTotalRealUSDValue(futuresBalances, priceMap)
    
    callback(spotBalances, earnBalances, futuresBalances, spotTotalUSD, earnTotalUSD, futuresTotalUSD)
}