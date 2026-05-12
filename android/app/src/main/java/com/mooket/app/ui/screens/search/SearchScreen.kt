package com.mooket.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mooket.app.data.model.SearchSuggest
import com.mooket.app.ui.theme.*

/**
 * 搜索激活页
 * 设计来源：Figma - node-id: 2-3657
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onMerchantClick: (Long, String) -> Unit,
    onProductClick: (Int, String, String) -> Unit,
    onCountryClick: (String, String) -> Unit,
    onFactoryClick: (String, String, String) -> Unit,
    onBrandClick: (String, String) -> Unit, // brandName, category
    onCountryProductClick: (String, String) -> Unit, // country, productName
    onCountryFactoryProductClick: (String, String, String) -> Unit, // country, factoryNo, productName
    onBrandProductClick: (String, String, String) -> Unit, // brandName, productName, category
    viewModel: SearchViewModel,
    category: String
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIos,
                            contentDescription = "返回",
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onBackClick() }
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFEFF5F3))
                                .border(
                                    1.dp,
                                    Color(0xFFBBCAC6).copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(start = 9.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFADB7B5),
                                modifier = Modifier.size(16.dp)
                            )

                            BasicTextField(
                                value = uiState.keyword,
                                onValueChange = { viewModel.updateKeyword(it, category) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp)
                                    .focusRequester(focusRequester),
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(Primary),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (uiState.keyword.isEmpty()) {
                                            Text(
                                                text = "搜索国家、厂号、产品、商家、品牌",
                                                fontSize = 15.sp,
                                                color = Color(0xFFADB7B5)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (uiState.keyword.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = Color(0xFFADB7B5),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterEnd)
                                        .clickable { viewModel.clearKeyword() }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            if (uiState.keyword.isNotEmpty()) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (uiState.suggestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到相关结果",
                            fontSize = 14.sp,
                            color = TextHint
                        )
                    }
                } else {
                    LazyColumn {
                        items(uiState.suggestions) { suggestion ->
                            SearchResultItem(
                                suggestion = suggestion,
                                onClick = {
                                    keyboardController?.hide()
                                    // 解析联想结果中的详细信息
                                    val parts = suggestion.text.split(" ")
                                    // 保存搜索历史到服务器（传递完整卡片数据）
                                    viewModel.saveSearchHistoryToServer(
                                        keyword = suggestion.text,
                                        searchType = suggestion.type,
                                        productId = if (suggestion.matchType == "product") suggestion.targetId else null,
                                        productName = if (suggestion.matchType == "product") suggestion.text else null,
                                        country = if (suggestion.matchType == "country" || suggestion.matchType == "factory" || suggestion.matchType == "combined") parts.getOrNull(0) else null,
                                        factoryNo = if (suggestion.matchType == "factory" || suggestion.matchType == "combined") parts.getOrNull(1) else null,
                                        brandId = null,
                                        merchantId = if (suggestion.matchType == "merchant") suggestion.targetId else null
                                    )
                                    when (suggestion.matchType) {
                                        "merchant" -> {
                                            viewModel.addToHistory(uiState.keyword)
                                            onMerchantClick(suggestion.targetId, category)
                                        }
                                        "product" -> {
                                            viewModel.addToHistory(uiState.keyword)
                                            onProductClick(suggestion.targetId.toInt(), category, suggestion.text)
                                        }
                                        "country" -> {
                                            viewModel.addToHistory(uiState.keyword)
                                            onCountryClick(suggestion.text, category)
                                        }
                                        "brand" -> {
                                            viewModel.addToHistory(uiState.keyword)
                                            // 品牌+产品：type="品牌+产品"，text="品牌名 产品名"
                                            if (suggestion.type == "品牌+产品") {
                                                val parts = suggestion.text.split(" ")
                                                if (parts.size >= 2) {
                                                    onBrandProductClick(parts[0], parts[1], category)
                                                }
                                            } else {
                                                onBrandClick(suggestion.text, category)
                                            }
                                        }
                                        "factory" -> {
                                            viewModel.addToHistory(uiState.keyword)
                                            // text format: "country factoryNo" e.g. "巴西 SIF1440"
                                            val parts = suggestion.text.split(" ")
                                            // 检查country是否为空，如果为空则不处理
                                            if (parts.size >= 2 && parts[0].isNotBlank()) {
                                                onFactoryClick(parts[0], parts[1], category)
                                            }
                                        }
                                        "combined" -> {
                                            // 处理组合类型：国家+产品、国家+厂号+产品等
                                            viewModel.addToHistory(uiState.keyword)
                                            val parts = suggestion.text.split(" ")
                                            // 检查country是否为空，如果为空则不处理
                                            when {
                                                // 国家+产品 (type="国家+产品")
                                                suggestion.type == "国家+产品" && parts.size >= 2 && parts[0].isNotBlank() -> {
                                                    onCountryProductClick(parts[0], parts[1])
                                                }
                                                // 国家+厂号+产品 (type="国家+厂号+产品")
                                                suggestion.type == "国家+厂号+产品" && parts.size >= 3 && parts[0].isNotBlank() -> {
                                                    onCountryFactoryProductClick(parts[0], parts[1], parts[2])
                                                }
                                                // 兼容：如果 type 不匹配但有 3 个部分，也当作国家+厂号+产品处理
                                                parts.size >= 3 && parts[0].isNotBlank() -> {
                                                    onCountryFactoryProductClick(parts[0], parts[1], parts[2])
                                                }
                                            }
                                        }
                                        else -> {
                                            // 处理 type=="国家+产品" 但 matchType 不是 combined 的情况
                                            if (suggestion.type == "国家+产品") {
                                                viewModel.addToHistory(uiState.keyword)
                                                val parts = suggestion.text.split(" ")
                                                if (parts.size >= 2 && parts[0].isNotBlank()) {
                                                    onCountryProductClick(parts[0], parts[1])
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // 无搜索关键词时
                if (uiState.searchHistory.isNotEmpty()) {
                    // 最近搜索标题
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "最近搜索",
                            fontSize = 14.sp,
                            color = Color(0xFF3C4947),
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.clearHistory() }
                        ) {
                            Text(
                                text = "清除全部",
                                fontSize = 12.sp,
                                color = Color(0xFF9DA4A3)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清除",
                                tint = Color(0xFF9DA4A3),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // 历史搜索词（横向排列）
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.searchHistory.forEach { historyItem ->
                            SearchHistoryChip(
                                keyword = historyItem,
                                onClick = {
                                    // 提取标准名（去掉别名后缀），确保搜索联想词能匹配
                                    val standardKeyword = if (historyItem.contains("(别名：")) {
                                        historyItem.substring(0, historyItem.indexOf("(别名："))
                                    } else {
                                        historyItem
                                    }
                                    viewModel.updateKeyword(standardKeyword, category)
                                    viewModel.addToHistory(standardKeyword)
                                }
                            )
                        }
                    }

                    // 提示文案
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("输入")
                            withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Medium)) {
                                append("关键词")
                            }
                            append("开始搜索")
                        },
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持搜索国家、厂号、产品、商家、品牌",
                        fontSize = 12.sp,
                        color = Color(0xFF3C4947),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // 搜索示例
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "搜索示例",
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "巴西", hint = "查找巴西相关信息")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "SIF504", hint = "查找厂号")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "牛腩", hint = "查找产品")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "巴西 牛腩", hint = "组合搜索")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "JBS 牛腩", hint = "品牌+产品搜索")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "\"公司名\"", hint = "查找商家")
                    }
                } else {
                    // 无历史时显示原有内容
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("输入")
                            withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Medium)) {
                                append("关键词")
                            }
                            append("开始搜索")
                        },
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "支持搜索国家、厂号、产品、商家、品牌",
                        fontSize = 12.sp,
                        color = Color(0xFF3C4947),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "搜索示例",
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "巴西", hint = "查找巴西相关信息")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "SIF504", hint = "查找厂号")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "牛腩", hint = "查找产品")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "巴西 牛腩", hint = "组合搜索")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "JBS 牛腩", hint = "品牌+产品搜索")
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchExampleItem(keyword = "\"公司名\"", hint = "查找商家")
                    }
                }
            }
        }
    }
}

/**
 * 解析搜索结果文本，分离主名称和别名
 * 只有明确包含"别名："标记的才提取别名
 * 例如: "顺沁园(广州)食品有限公司(别名：顺沁园)" -> Pair("顺沁园(广州)食品有限公司", "别名：顺沁园")
 * 例如: "顺发德（菏泽）供应链有限公司" -> Pair("顺发德（菏泽）供应链有限公司", null)
 */
private fun parseSearchText(text: String): Pair<String, String?> {
    // 只有明确包含"别名："才提取别名
    val aliasIndex = text.lastIndexOf("别名：")
    if (aliasIndex > 0) {
        // 找到"别名："前最近的左括号
        val beforeAlias = text.substring(0, aliasIndex)
        val leftBracketCn = beforeAlias.lastIndexOf('（')
        val leftBracketEn = beforeAlias.lastIndexOf('(')
        val leftBracket = maxOf(leftBracketCn, leftBracketEn)

        if (leftBracket >= 0) {
            // 找对应右括号
            val afterLeft = text.substring(leftBracket)
            val rightBracketCn = afterLeft.indexOf('）')
            val rightBracketEn = afterLeft.indexOf(')')
            val rightBracketLocal = maxOf(rightBracketCn, rightBracketEn)

            if (rightBracketLocal >= 0) {
                val alias = text.substring(leftBracket + 1, leftBracket + rightBracketLocal)
                val main = text.substring(0, leftBracket) + text.substring(leftBracket + rightBracketLocal + 1)
                return main.trim() to alias
            }
        }
    }

    // 没有"别名："标记，整个文本就是主名称，无别名
    return text to null
}

/**
 * 搜索结果项
 */
@Composable
private fun SearchResultItem(
    suggestion: SearchSuggest,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFFADB7B5),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val (mainText, aliasText) = parseSearchText(suggestion.text)
                Column {
                    Text(
                        text = highlightKeyword(mainText, suggestion.keyword),
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    if (aliasText != null) {
                        Text(
                            text = "（$aliasText）",
                            fontSize = 12.sp,
                            color = TextHint
                        )
                    }
                }
            }
            Text(
                text = suggestion.type,
                fontSize = 12.sp,
                color = Color(0xFF3C4947)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEFF5F3))
                .padding(start = 16.dp)
        )
    }
}

/**
 * 高亮关键词
 */
private fun highlightKeyword(text: String, keyword: String): AnnotatedString {
    if (keyword.isEmpty()) {
        return AnnotatedString(text)
    }
    return buildAnnotatedString {
        var currentIndex = 0
        val lowerText = text.lowercase()
        val lowerKeyword = keyword.lowercase()
        var keywordIndex = lowerText.indexOf(lowerKeyword)
        while (keywordIndex >= 0) {
            if (keywordIndex > currentIndex) {
                append(text.substring(currentIndex, keywordIndex))
            }
            withStyle(style = SpanStyle(color = Primary, fontWeight = FontWeight.Medium)) {
                append(text.substring(keywordIndex, keywordIndex + keyword.length))
            }
            currentIndex = keywordIndex + keyword.length
            keywordIndex = lowerText.indexOf(lowerKeyword, currentIndex)
        }
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Composable
private fun SearchExampleItem(
    keyword: String,
    hint: String,
    rowModifier: Modifier = Modifier
) {
    Row(
        modifier = rowModifier
            .wrapContentWidth()
            .background(Color.White, RoundedCornerShape(2.dp))
            .border(1.dp, Color(0xFFdee4e1), RoundedCornerShape(2.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF171d1c),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(7.dp)
                .background(Color(0xFF3C4947))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = keyword,
            fontSize = 12.sp,
            color = Color(0xFF171d1c),
            fontWeight = FontWeight.Medium,
            softWrap = false
        )
        Text(
            text = " · $hint",
            fontSize = 12.sp,
            color = Color(0xFF3C4947),
            softWrap = false
        )
    }
}

/**
 * 历史搜索标签
 */
@Composable
private fun SearchHistoryChip(
    keyword: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0xFFEFF5F3), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFFBBCAC6).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = Color(0xFF6B7A78),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = keyword,
            fontSize = 12.sp,
            color = TextPrimary,
            maxLines = 1
        )
    }
}
