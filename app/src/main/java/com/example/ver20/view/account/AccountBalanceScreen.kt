// AccountBalanceScreen.kt - Future 계좌 전용으로 업데이트

package com.example.ver20.view.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import android.util.Log
import com.example.ver20.dao.binance.FuturesAccountInfo
import com.example.ver20.dao.binance.FuturesPositionInfo
import com.example.ver20.dao.binance.FuturesAssetInfo
import com.example.ver20.dao.binance.RealBinanceService
import com.example.ver20.dao.dataclass.ApiKeyData
import com.example.ver20.dao.dataclass.ApiKeyService

@Composable
fun AccountBalanceScreen(
    modifier: Modifier = Modifier,
    onShowSecuritySettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val apiKeyService = remember { ApiKeyService(context) }
    val binanceService = remember { RealBinanceService() }
    val coroutineScope = rememberCoroutineScope()

    // 상태 변수들
    var hasApiKeys by remember { mutableStateOf(false) }
    var isTestnet by remember { mutableStateOf(true) }
    var apiKeyData by remember { mutableStateOf<ApiKeyData?>(null) }

    // Future 계좌 데이터
    var futuresAccountInfo by remember { mutableStateOf<FuturesAccountInfo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 어두운 테마 그라데이션
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF121212),
            Color(0xFF1E1E1E)
        )
    )

    // Future 계좌 데이터 로드 함수
    fun loadFuturesData() {
        if (apiKeyData == null) return

        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            try {
                Log.d("AccountScreen", "🚀 Future 계좌 데이터 로드 시작")

                val (success, futuresInfo) = binanceService.getFuturesAccountInfo(apiKeyData!!)

                if (success && futuresInfo != null) {
                    futuresAccountInfo = futuresInfo
                    errorMessage = null
                    Log.d("AccountScreen", "✅ Future 계좌 로드 성공: 총 잔고 ${futuresInfo.totalWalletBalance}")
                } else {
                    errorMessage = "Future 계좌 정보 조회 실패"
                    Log.e("AccountScreen", "❌ Future 계좌 로드 실패")
                }

            } catch (e: Exception) {
                Log.e("AccountScreen", "❌ Future 데이터 로드 예외: ${e.message}")
                errorMessage = "데이터 로드 오류: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // 초기 데이터 로드
    LaunchedEffect(Unit) {
        val keys = apiKeyService.getApiKeys()
        hasApiKeys = keys != null
        apiKeyData = keys

        if (keys != null) {
            isTestnet = keys.isTestnet
            loadFuturesData()
        }
    }

    // 메인 UI
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== 헤더 카드 =====
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "💰 Future 계좌조회",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                            if (hasApiKeys) {
                                Text(
                                    if (isTestnet) "🧪 테스트넷" else "🔴 메인넷",
                                    fontSize = 12.sp,
                                    color = if (isTestnet) Color(0xFFFF9800) else Color(0xFFF44336)
                                )
                            }
                        }

                        Row {
                            if (hasApiKeys) {
                                IconButton(
                                    onClick = { loadFuturesData() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "새로고침",
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onShowSecuritySettings,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = Color(0xFFFFD700)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ===== 총 자산 카드 (미실현 손익 포함) =====
        if (futuresAccountInfo != null) {
            item {
                TotalAssetsCard(futuresAccountInfo!!)
            }
        }

        // ===== 컨텐츠 =====
        when {
            !hasApiKeys -> {
                item {
                    NoApiKeyCard(onShowSecuritySettings)
                }
            }
            isLoading -> {
                item {
                    LoadingCard()
                }
            }
            errorMessage != null -> {
                item {
                    ErrorCard(errorMessage!!) { loadFuturesData() }
                }
            }
            futuresAccountInfo != null -> {
                // Future 계좌 요약
                item {
                    FuturesSummaryCard(futuresAccountInfo!!)
                }

                // Future 자산 목록
                if (futuresAccountInfo!!.assets.isNotEmpty()) {
                    item {
                        FuturesAssetsCard(futuresAccountInfo!!.assets)
                    }
                }

                // 포지션 목록
                if (futuresAccountInfo!!.positions.isNotEmpty()) {
                    item {
                        PositionsCard(futuresAccountInfo!!.positions)
                    }
                }
            }
        }
    }
}

// ===== 총 자산 카드 (미실현 손익 포함) =====
@Composable
private fun TotalAssetsCard(futuresInfo: FuturesAccountInfo) {
    val formatter = DecimalFormat("$#,##0.00")
    val totalAssets = futuresInfo.totalWalletBalance + futuresInfo.totalUnrealizedProfit
    val isPositive = futuresInfo.totalUnrealizedProfit >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "총 자산 (미실현 손익 포함)",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                formatter.format(totalAssets),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "미실현 손익: ${formatter.format(futuresInfo.totalUnrealizedProfit)}",
                fontSize = 12.sp,
                color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===== Future 계좌 요약 카드 =====
@Composable
private fun FuturesSummaryCard(futuresInfo: FuturesAccountInfo) {
    val formatter = DecimalFormat("$#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📈 Future 계좌 상세",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "지갑 잔고",
                    value = formatter.format(futuresInfo.totalWalletBalance),
                    color = Color.White
                )
                SummaryItem(
                    label = "마진 잔고",
                    value = formatter.format(futuresInfo.totalMarginBalance),
                    color = Color(0xFF2196F3)
                )
                SummaryItem(
                    label = "가용 잔고",
                    value = formatter.format(futuresInfo.availableBalance),
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

// ===== Future 자산 카드 =====
@Composable
private fun FuturesAssetsCard(assets: List<FuturesAssetInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "🪙 Future 자산 (${assets.size}개)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(12.dp))

            assets.forEach { asset ->
                FuturesAssetRow(asset)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ===== 포지션 카드 =====
@Composable
private fun PositionsCard(positions: List<FuturesPositionInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📊 보유 포지션 (${positions.size}개)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700)
            )

            Spacer(modifier = Modifier.height(12.dp))

            positions.forEach { position ->
                PositionRow(position)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ===== 요약 아이템 =====
@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ===== Future 자산 행 =====
@Composable
private fun FuturesAssetRow(asset: FuturesAssetInfo) {
    val formatter = DecimalFormat("#,##0.##")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                asset.asset,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "지갑: ${formatter.format(asset.walletBalance)}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                formatter.format(asset.marginBalance),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64B5F6)
            )
            if (asset.unrealizedProfit != 0.0) {
                Text(
                    "${if (asset.unrealizedProfit >= 0) "+" else ""}${formatter.format(asset.unrealizedProfit)}",
                    fontSize = 11.sp,
                    color = if (asset.unrealizedProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

// ===== 포지션 행 =====
@Composable
private fun PositionRow(position: FuturesPositionInfo) {
    val formatter = DecimalFormat("#,##0.####")
    val isLong = position.positionAmt > 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    position.symbol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isLong) "LONG" else "SHORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLong) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            Text(
                "수량: ${formatter.format(kotlin.math.abs(position.positionAmt))} | 진입가: ${formatter.format(position.entryPrice)}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "${if (position.unrealizedProfit >= 0) "+" else ""}${formatter.format(position.unrealizedProfit)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (position.unrealizedProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Text(
                "${position.leverage}x",
                fontSize = 11.sp,
                color = Color(0xFFFF9800)
            )
        }
    }
}

// ===== API 키 없음 카드 =====
@Composable
private fun NoApiKeyCard(onShowSecuritySettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFFF9800)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "API 키가 설정되지 않았습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Future 계좌 정보를 조회하려면\n바이낸스 API 키를 설정해주세요",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onShowSecuritySettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("API 키 설정")
            }
        }
    }
}

// ===== 로딩 카드 =====
@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD700)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Future 계좌 정보를 불러오는 중...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ===== 에러 카드 =====
@Composable
private fun ErrorCard(errorMessage: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFF44336)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "데이터 로드 실패",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("다시 시도")
            }
        }
    }
}