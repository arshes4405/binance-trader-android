package com.example.ver20

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2196F3) // 밝은 블루
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "계좌조회 화면",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2) // 진한 블루
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "API 키 설정 후 잔고를 조회할 수 있습니다",
            color = Color.Gray
        )
    }
}