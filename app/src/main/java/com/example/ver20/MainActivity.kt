package com.example.ver20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ver20.ui.theme.Ver20Theme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Ver20Theme {
                BinanceTraderApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinanceTraderApp() {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("가격조회", Icons.Default.Search),
        TabItem("계좌조회", Icons.Default.AccountBox),
        TabItem("시세분석", Icons.Default.Info),
        TabItem("거래내역", Icons.Default.List)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Binance Trader") }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> PriceScreen(modifier = Modifier.padding(paddingValues))
            1 -> AccountScreen(modifier = Modifier.padding(paddingValues))
            2 -> AnalysisScreen(modifier = Modifier.padding(paddingValues))
            3 -> TradeHistoryScreen(modifier = Modifier.padding(paddingValues))
        }
    }
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)

// 가격조회 화면
@Composable
fun PriceScreen(modifier: Modifier = Modifier) {
    var coins by remember {
        mutableStateOf(listOf(
            CoinItem("BTCUSDT", "0.00", true),
            CoinItem("ETHUSDT", "0.00", true),
            CoinItem("BNBUSDT", "0.00", true),
            CoinItem("ADAUSDT", "0.00", true),
            CoinItem("SOLUSDT", "0.00", true)
        ))
    }

    var customSymbol by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        fetchAllPrices(coins) { updatedCoins ->
            coins = updatedCoins
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 상단: 커스텀 코인 추가
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("새 코인 추가", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    OutlinedTextField(
                        value = customSymbol,
                        onValueChange = { customSymbol = it.uppercase() },
                        label = { Text("심볼 (예: BTC, ETH, DOGE)") },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (customSymbol.isNotEmpty()) {
                                val fullSymbol = if (customSymbol.uppercase().endsWith("USDT")) {
                                    customSymbol.uppercase()
                                } else {
                                    "${customSymbol.uppercase()}USDT"
                                }

                                val newCoin = CoinItem(fullSymbol, "0.00", true)
                                coins = coins + newCoin
                                fetchSinglePrice(fullSymbol) { price ->
                                    coins = coins.map {
                                        if (it.symbol == fullSymbol)
                                            it.copy(price = price, isLoading = false)
                                        else it
                                    }
                                }
                                customSymbol = ""
                            }
                        }
                    ) {
                        Text("추가")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 하단: 코인 리스트
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("코인 가격 목록", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Button(
                        onClick = {
                            coins = coins.map { it.copy(isLoading = true) }
                            fetchAllPrices(coins) { updatedCoins ->
                                coins = updatedCoins
                            }
                        }
                    ) {
                        Text("새로고침")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn {
                    items(coins) { coin ->
                        CoinItemRow(coin = coin)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// 계좌조회 화면
@Composable
fun AccountScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountBox,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "계좌조회 화면",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("API 키 설정 후 잔고를 조회할 수 있습니다")
    }
}

// 시세분석 화면
@Composable
fun AnalysisScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "시세분석 화면",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("RSI, 볼린저밴드 등 기술적 지표를 분석합니다")
    }
}

// 거래내역 화면
@Composable
fun TradeHistoryScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "거래내역 화면",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("과거 거래 내역을 확인할 수 있습니다")
    }
}

@Composable
fun CoinItemRow(coin: CoinItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = coin.symbol,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (coin.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = "$${coin.price}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun fetchAllPrices(currentCoins: List<CoinItem>, callback: (List<CoinItem>) -> Unit) {
    val symbols = currentCoins.map { it.symbol }
    val results = mutableListOf<CoinItem>()
    var completedRequests = 0

    symbols.forEach { symbol ->
        fetchSinglePrice(symbol) { price ->
            results.add(CoinItem(symbol, price, false))
            completedRequests++

            if (completedRequests == symbols.size) {
                callback(results.sortedBy { it.symbol })
            }
        }
    }
}

fun fetchSinglePrice(symbol: String, callback: (String) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.binance.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(BinanceApi::class.java)

    api.getPrice(symbol).enqueue(object : Callback<PriceResponse> {
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