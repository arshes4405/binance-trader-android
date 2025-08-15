package com.example.ver20

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun PriceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var coins by remember { mutableStateOf(emptyList<CoinItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // MongoDB에서 즐겨찾기 코인 조회
        val mongoService = MongoDbService()
        mongoService.getFavoriteCoins { favoriteSymbols, error ->
            if (error == null && favoriteSymbols.isNotEmpty()) {
                println("MongoDB 연결 성공! 즐겨찾기 코인: $favoriteSymbols")
                val favoriteCoins = favoriteSymbols.map { symbol ->
                    CoinItem(symbol, "0.00", "0.00", true, true)
                }
                coins = favoriteCoins
                fetchAllPricesWithFuture(coins) { updatedCoins ->
                    coins = updatedCoins
                    isLoading = false
                }
            } else {
                println("MongoDB 오류 또는 즐겨찾기 없음: $error")
                isLoading = false
            }
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

                // 로딩 상태로 즉시 추가
                val loadingCoin = CoinItem(fullSymbol, "0.00", "0.00", true, true)
                coins = coins + loadingCoin
                showAddDialog = false

                // MongoDB에 저장
                val mongoService = MongoDbService()
                mongoService.saveFavoriteCoin(fullSymbol) { success, message ->
                    CoroutineScope(Dispatchers.Main).launch {
                        if (success) {
                            Toast.makeText(context, "코인이 추가되었습니다: $fullSymbol", Toast.LENGTH_SHORT).show()
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

    // 메인 화면: 코인 가격 목록만
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD) // 연한 블루
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
                Text(
                    "코인 가격 목록",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2) // 진한 블루
                )

                Row {
                    // + 버튼
                    IconButton(
                        onClick = { showAddDialog = true }
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
                            coins = coins.map { it.copy(isSpotLoading = true, isFutureLoading = true) }
                            fetchAllPricesWithFuture(coins) { updatedCoins ->
                                coins = updatedCoins
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

            // 로딩 상태 표시
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF2196F3) // 밝은 블루
                    )
                }
            } else if (coins.isEmpty()) {
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
                            onClick = { showAddDialog = true },
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
            } else {
                // 테이블 헤더
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFBBDEFB) // 밝은 블루
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "코인",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF1976D2) // 진한 블루
                        )
                        Text(
                            text = "Spot",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF4CAF50) // 녹색
                        )
                        Text(
                            text = "Future",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF9C27B0) // 보라색
                        )
                        Text(
                            text = "삭제",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFFF44336) // 빨간색
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(coins) { coin ->
                        CoinTableRow(
                            coin = coin,
                            onDelete = { symbol ->
                                val mongoService = MongoDbService()
                                mongoService.removeFavoriteCoin(symbol) { success, message ->
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (success) {
                                            Toast.makeText(context, "코인이 삭제되었습니다: $symbol", Toast.LENGTH_SHORT).show()
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
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 코인 심볼 (USDT 제거)
            Text(
                text = coin.symbol.replace("USDT", ""),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                textAlign = TextAlign.Center,
                color = Color(0xFF1976D2) // 진한 블루
            )

            // Spot 가격
            if (coin.isSpotLoading) {
                Box(
                    modifier = Modifier.weight(2f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Text(
                    text = formatPrice(coin.spotPrice),
                    fontSize = 14.sp,
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
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF9C27B0)
                    )
                }
            } else {
                Text(
                    text = formatPrice(coin.futurePrice),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF9C27B0)
                )
            }

            // 삭제 버튼
            IconButton(
                onClick = { onDelete(coin.symbol) },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "×",
                    fontSize = 18.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Spot과 Future 가격을 모두 조회하는 함수들
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

    // Spot 가격 조회
    fetchSpotPrice(symbol) { price ->
        spotPrice = price
        checkCompletion()
    }

    // Future 가격 조회
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

// 가격 포맷 함수 (큰 숫자를 읽기 쉽게)
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