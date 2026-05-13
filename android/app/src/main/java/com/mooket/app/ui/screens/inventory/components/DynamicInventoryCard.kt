package com.mooket.app.ui.screens.inventory.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mooket.app.ui.screens.inventory.DynamicInventoryCardData
import com.mooket.app.ui.theme.*

/**
 * 动态库存卡片组件
 */
@Composable
fun DynamicInventoryCard(
    data: DynamicInventoryCardData,
    modifier: Modifier = Modifier
) {
    val profitColor = when {
        data.estimatedProfit == null || data.estimatedProfit == 0.0 -> TextHint
        data.estimatedProfit > 0 -> Primary
        else -> Error
    }

    val profitPrefix = when {
        data.estimatedProfit == null || data.estimatedProfit == 0.0 -> ""
        data.estimatedProfit > 0 -> "+"
        else -> ""
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp), spotColor = Color(0x10000000))
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 第一行：产品名称 + 预估盈利
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.productName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (data.estimatedProfit != null && data.estimatedProfit != 0.0)
                        "$profitPrefix${formatProfit(data.estimatedProfit)}"
                    else "-",
                    color = profitColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 第二行：重量 + 件数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "重量", value = formatWeight(data.weight))
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(label = "件数", value = "${data.pieces}件")
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第三行：当前成本 + 现货价
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoChip(label = "当前成本", value = data.currentCost?.let { "${String.format("%.2f", it)}元/KG" } ?: "-")
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(label = "现货价", value = data.spotPrice?.let { "${String.format("%.2f", it)}元/KG" } ?: "-")
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第四行：国家 + 厂号 + 物理状态
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (!data.country.isNullOrBlank()) {
                    InfoChip(label = "国家", value = data.country)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (!data.factoryCode.isNullOrBlank()) {
                    InfoChip(label = "厂号", value = data.factoryCode)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (!data.physicalStatus.isNullOrBlank()) {
                    InfoChip(label = "状态", value = data.physicalStatus)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 第五行：箱号
            Text(
                text = "箱号: ${data.containerId}",
                color = TextHint,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            color = TextHint,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 格式化盈利
 */
private fun formatProfit(profit: Double): String {
    return when {
        profit >= 10000 -> String.format("%.1f万", profit / 10000)
        profit >= 1000 -> String.format("%.1fK", profit / 1000)
        else -> String.format("%.0f", profit)
    }
}

/**
 * 格式化重量
 */
private fun formatWeight(weight: Double): String {
    return when {
        weight >= 1000 -> String.format("%.1fT", weight / 1000)
        else -> String.format("%.0fKG", weight)
    }
}
