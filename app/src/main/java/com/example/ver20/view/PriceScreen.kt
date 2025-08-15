package com.example.ver20.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ver20.dao.BinanceApi
import com.example.ver20.dao.CoinItem
import com.example.ver20.dao.MongoDbService
import com.example.ver20.dao.PriceResponse
import com.example.ver20.dao.UserService
import com.example.ver20.dao.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun PriceScreen(
    modifier: Modifier = Modifier,
    onShowCreateAccount: () -> Unit = {}
) {
    val context = LocalContext.current

    var coins by remember { mutableStateOf(emptyList<CoinItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var hasUserInfo by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<UserData?>(null) }

    // 사용자 정보 확인 및 즐겨찾기 로드
    LaunchedEffect(Unit) {
        val userService = UserService()
        val userData = userService.getUserFromPreferences(context)
        hasUserInfo = userData != null
        currentUser = userData

        if (hasUserInfo && userData != null) {
            // 사용자별 MongoDB에서 즐겨찾기 코인 조회
            val mongoService = MongoDbService()
            mongoService.getFavoriteCoins(userData.username) { favoriteSymbols, error ->
                if (error == null && favoriteSymbols.isNotEmpty()) {
                    println("✅ 사용자별 즐겨찾기 로드 성공! 사용자: ${userData.username}, 코인: $favoriteSymbols")
                    val favoriteCoins = favoriteSymbols.map { symbol ->
                        CoinItem(symbol, "0.00", "0.00", true, true)
                    }
                    coins = favoriteCoins
                    fetchAllPricesWithFuture(coins) { updatedCoins ->
                        coins = updatedCoins
                        isLoading = false
                    }
                } else {
                    println("❌ 사용자별 즐겨찾기 오류 또는 비어있음: $error")
                    isLoading = false
                }
            }
        } else {
            isLoading = false
        }
    }

    // 코인 추가 다이얼로그
    if (showAddDialog) {
        AddCoinDialog(
            onDismiss = { showAddDialog = false },
            onAddCoin = { symbol ->
                val fullSymbol = if (symbol.uppercase().endsWith("USDT")) {
                    symbol.uppercase()
                } else {
                    "${symbol.uppercase()}USDT"
                }

                // 중복 체크
                if (coins.any { it.symbol == fullSymbol }) {
                    Toast.makeText(context, "이미 존재하는 코인입니다: $fullSymbol", Toast.LENGTH_SHORT).show()
                    return@AddCoinDialog
                }

                // 사용자 정보 확인
                if (currentUser == null) {
                    Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                    return@AddCoinDialog
                }

                // 로딩 상태로 즉시 추가
                val loadingCoin = CoinItem(fullSymbol, "0.00", "0.00", true, true)
                coins = coins + loadingCoin
                showAddDialog = false

                // 사용자별 MongoDB에 저장
                val mongoService = MongoDbService()
                mongoService.saveFavoriteCoin(currentUser!!.username, fullSymbol) { success, message ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            Toast.makeText(
                                context,
                                "코인이 추가되었습니다: $fullSymbol",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Spot과 Future 가격 조회
                            fetchBothPrices(fullSymbol) { spotPrice, futurePrice ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    coins = coins.map {
                                        if (it.symbol == fullSymbol)
                                            it.copy(
                                                spotPrice = spotPrice,
                                                futurePrice = futurePrice,
                                                isSpotLoading = false,
                                                isFutureLoading = false
                                            )
                                        else it
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "추가 실패: $message", Toast.LENGTH_LONG).show()
                            // 실패 시 목록에서 제거
                            coins = coins.filter { it.symbol != fullSymbol }
                        }
                    }
                }
            }
        )
    }

    // 메인 화면
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단 제목과 버튼들
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "코인 가격 목록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    if (currentUser != null) {
                        Text(
                            "사용자: ${currentUser!!.username}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row {
                    // + 버튼
                    IconButton(
                        onClick = {
                            if (currentUser != null) {
                                showAddDialog = true
                            } else {
                                Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "코인 추가",
                            tint = Color(0xFF2196F3)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 새로고침 버튼
                    IconButton(
                        onClick = {
                            if (currentUser != null && coins.isNotEmpty()) {
                                coins = coins.map { it.copy(isSpotLoading = true, isFutureLoading = true) }
                                fetchAllPricesWithFuture(coins) { updatedCoins ->
                                    coins = updatedCoins
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 컨텐츠 영역
            when {
                // 유저정보 없음
                !hasUserInfo -> {
                    UserNotLoggedInContent(onShowCreateAccount = onShowCreateAccount)
                }
                // 로딩 중
                isLoading -> {
                    LoadingContent()
                }
                // 코인 목록 비어있음
                coins.isEmpty() -> {
                    EmptyCoinsContent(onAddCoin = { showAddDialog = true })
                }
                // 코인 목록 표시
                else -> {
                    CoinsListContent(
                        coins = coins,
                        currentUser = currentUser,
                        onDeleteCoin = { symbol ->
                            if (currentUser == null) {
                                Toast.makeText(context, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
                                return@CoinsListContent
                            }

                            val mongoService = MongoDbService()
                            mongoService.removeFavoriteCoin(currentUser!!.username, symbol) { success, message ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "코인이 삭제되었습니다: $symbol",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        coins = coins.filter { it.symbol != symbol }
                                    } else {
                                        Toast.makeText(context, "삭제 실패: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserNotLoggedInContent(onShowCreateAccount: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "유저 정보가 없습니다",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "좌측 상단의 유저 아이콘을 클릭하여\n프로필을 설정해주세요",
                    fontSize = 15.sp,
                    color = Color(0xFFBF360C),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onShowCreateAccount,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "유저정보설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF2196F3)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "즐겨찾기 코인을 불러오는 중...",
                color = Color.Gray
            )
        }
    }
}

@Composable
fun EmptyCoinsContent(onAddCoin: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "즐겨찾기 코인이 없습니다",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddCoin,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("코인 추가")
            }
        }
    }
}

@Composable
fun CoinsListContent(
    coins: List<CoinItem>,
    currentUser: UserData?,
    onDeleteCoin: (String) -> Unit
) {
    // 테이블 헤더
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFBBDEFB)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "코인",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                color = Color(0xFF1976D2)
            )
            Text(
                text = "Spot",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "Future",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                color = Color(0xFF9C27B0)
            )
            Text(
                text = "삭제",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = Color(0xFFF44336)
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 코인 목록
    LazyColumn {
        items(coins) { coin ->
            CoinTableRow(
                coin = coin,
                onDelete = { symbol -> onDeleteCoin(symbol) }
            )
        }
    }
}

@Composable
fun AddCoinDialog(
    onDismiss: () -> Unit,
    onAddCoin: (String) -> Unit
) {
    var customSymbol by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "새 코인 추가",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customSymbol,
                    onValueChange = { customSymbol = it.uppercase() },
                    label = { Text("심볼 (예: BTC, ETH, DOGE)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        focusedLabelColor = Color(0xFF2196F3)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("취소", color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (customSymbol.isNotEmpty()) {
                                onAddCoin(customSymbol)
                                customSymbol = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = Color.White
                        )
                    ) {
                        Text("추가")
                    }
                }
            }
        }
    }
}

@Composable
fun CoinTableRow(
    coin: CoinItem,
    onDelete: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 코인 심볼 (USDT 제거)
            Text(
                text = coin.symbol.replace("USDT", ""),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                color = Color(0xFF1976D2)
            )

            // Spot 가격
            if (coin.isSpotLoading) {
                Box(
                    modifier = Modifier.weight(2f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Text(
                    text = formatPrice(coin.spotPrice),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4CAF50)
                )
            }

            // Future 가격
            if (coin.isFutureLoading) {
                Box(
                    modifier = Modifier.weight(2f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color(0xFF9C27B0)
                    )
                }
            } else {
                Text(
                    text = formatPrice(coin.futurePrice),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF9C27B0)
                )
            }

            // 삭제 버튼
            IconButton(
                onClick = { onDelete(coin.symbol) },
                modifier = Modifier
                    .weight(1f)
                    .size(32.dp)
            ) {
                Text(
                    "×",
                    fontSize = 16.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Binance API 호출 함수들 (기존과 동일)
fun fetchAllPricesWithFuture(currentCoins: List<CoinItem>, callback: (List<CoinItem>) -> Unit) {
    val symbols = currentCoins.map { it.symbol }
    val results = mutableListOf<CoinItem>()
    var completedRequests = 0
    val totalRequests = symbols.size

    symbols.forEach { symbol ->
        fetchBothPrices(symbol) { spotPrice, futurePrice ->
            results.add(CoinItem(symbol, spotPrice, futurePrice, false, false))
            completedRequests++

            if (completedRequests == totalRequests) {
                callback(results.sortedBy { it.symbol })
            }
        }
    }
}

fun fetchBothPrices(symbol: String, callback: (String, String) -> Unit) {
    var spotPrice = "오류"
    var futurePrice = "오류"
    var completedCalls = 0

    val checkCompletion = {
        completedCalls++
        if (completedCalls == 2) {
            callback(spotPrice, futurePrice)
        }
    }

    fetchSpotPrice(symbol) { price ->
        spotPrice = price
        checkCompletion()
    }

    fetchFuturePrice(symbol) { price ->
        futurePrice = price
        checkCompletion()
    }
}

fun fetchSpotPrice(symbol: String, callback: (String) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(BinanceApi::class.java)

    api.getSpotPrice(symbol).enqueue(object : Callback<PriceResponse> {
        override fun onResponse(call: Call<PriceResponse>, response: Response<PriceResponse>) {
            if (response.isSuccessful) {
                val price = response.body()?.price ?: "0.00"
                callback(price)
            } else {
                callback("오류")
            }
        }

        override fun onFailure(call: Call<PriceResponse>, t: Throwable) {
            callback("네트워크 오류")
        }
    })
}

fun fetchFuturePrice(symbol: String, callback: (String) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://fapi.binance.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(BinanceApi::class.java)

    api.getFuturePrice(symbol).enqueue(object : Callback<PriceResponse> {
        override fun onResponse(call: Call<PriceResponse>, response: Response<PriceResponse>) {
            if (response.isSuccessful) {
                val price = response.body()?.price ?: "0.00"
                callback(price)
            } else {
                callback("오류")
            }
        }

        override fun onFailure(call: Call<PriceResponse>, t: Throwable) {
            callback("네트워크 오류")
        }
    })
}

fun formatPrice(price: String): String {
    return try {
        val priceValue = price.toDouble()
        if (priceValue >= 1000) {
            String.format("%.0f", priceValue)
        } else if (priceValue >= 1) {
            String.format("%.2f", priceValue)
        } else {
            String.format("%.4f", priceValue)
        }
    } catch (e: Exception) {
        price
    }
}