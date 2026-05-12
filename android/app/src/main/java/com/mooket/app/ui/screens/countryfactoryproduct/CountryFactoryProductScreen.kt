package com.mooket.app.ui.screens.countryfactoryproduct

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.lazy.LazyColumn
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mooket.app.R
import com.mooket.app.data.model.CountryFactoryProductDetail
import com.mooket.app.data.model.DailyPrice
import com.mooket.app.data.model.EmployeeOfferItem
import com.mooket.app.data.model.MerchantOfferGroup
import com.mooket.app.data.model.MerchantOption
import com.mooket.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 国家+厂号+产品详情页
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun CountryFactoryProductScreen(
    country: String,
    factoryNo: String,
    productName: String,
    category: String,
    onBackClick: () -> Unit,
    onFactoryClick: (String, String) -> Unit,
    onCountryFactoryDelete: (Int, String, String) -> Unit, // productId, productName, category -> go to ProductDetailScreen
    onProductDelete: (String, String, String) -> Unit, // country, factoryNo, category -> go to FactoryDetailScreen
    onSubstituteProductClick: (String, String, String, String) -> Unit, // country, factoryNo, productName, category
    onCopyPhone: (String) -> Unit = { /* 默认空实现 */ },
    onCallClick: (String) -> Unit = { /* 默认空实现 */ },
    onViewOriginalText: (String) -> Unit = { /* 默认空实现 */ },
    viewModel: CountryFactoryProductViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var originalText by remember { mutableStateOf("") }

    LaunchedEffect(country, factoryNo, productName) {
        viewModel.loadData(country, factoryNo, productName, category)
    }

    // 加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()  // 防止重复触发
            .debounce(100)  // 防抖动
            .collect { lastIndex ->
                if (lastIndex != null) {
                    val detail = uiState.detail
                    // 只有不在加载状态（isLoading和isListLoading都不在加载）且有更多页时才加载更多
                    if (!uiState.isLoading && !uiState.isListLoading && detail != null && detail.totalPages > detail.page) {
                        viewModel.loadMore()
                    }
                }
            }
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

                        Spacer(modifier = Modifier.width(2.dp))

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
                                .padding(start = 7.dp, end = 13.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 国家+厂号标签 - 点击进入产品详情页
                                SearchTag(
                                    text = "${country}${factoryNo}",
                                    onClick = {
                                        // 优先使用缓存的 productId，避免 TopAppBar 重组依赖 uiState
                                        val productId = viewModel.getCachedProductId()
                                        if (productId != null && productId > 0) {
                                            onCountryFactoryDelete(productId, productName, category)
                                        }
                                    }
                                )

                                // 产品标签 - 点击进入厂号详情页
                                SearchTag(
                                    text = productName,
                                    onClick = { onProductDelete(country, factoryNo, category) }
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFADB7B5),
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.CenterEnd)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (uiState.detail?.hasSubstitute == true) {
                Box(
                    modifier = Modifier
                        .offset(x = 12.dp, y = (-12).dp)
                        .width(32.dp)
                        .height(84.dp)
                        .background(
                            color = Color(0xFF171D1C).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        )
                        .clickable { onSubstituteProductClick(country, factoryNo, productName, category) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "平",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "替",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "产",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = "品",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
            }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues),
            state = listState
        ) {
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            } else if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = uiState.error ?: "加载失败", color = TextHint)
                    }
                }
            } else {
                uiState.detail?.let { detail ->
                    // 价格看板 - 非吸顶，随内容滚动
                    item {
                        PriceInfoCard(
                            detail = detail,
                            offerType = uiState.offerType,
                            isTrendExpanded = uiState.isTrendExpanded,
                            onToggleTrend = { viewModel.toggleTrend() }
                        )
                    }

                    // 浅绿间隔 - 不吸顶，随内容滚动
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Background)
                        )
                    }

                    // 排序栏 + 筛选栏 — 合并为同一个吸顶容器
                    val merchantOptions = derivedMerchantOptions(detail.merchantOffers)
                    val regionOptions = derivedRegionOptions(detail.merchantOffers)

                    stickyHeader(key = "cfp_sticky") {
                        Column(modifier = Modifier.background(Color.White)) {
                            SortBar(
                                currentSort = uiState.sortBy,
                                currentType = uiState.offerType,
                                onSortChange = { viewModel.switchSortBy(it) },
                                onTypeChange = { viewModel.switchOfferType(it) }
                            )
                            FilterBar(
                                activeFilters = uiState.activeFilters,
                                priceMin = uiState.priceMin,
                                priceMax = uiState.priceMax,
                                goodsTypes = uiState.goodsTypes,
                                feedingTypes = uiState.feedingTypes,
                                tags = uiState.tags,
                                offerType = uiState.offerType,
                                isFamousMerchant = uiState.isFamousMerchant,
                                selectedMerchants = uiState.selectedMerchants,
                                regions = uiState.regions,
                                merchantOptions = merchantOptions,
                                regionOptions = regionOptions,
                                onFilterClick = { viewModel.toggleFilter(it) },
                                onPriceRangeChange = { min, max -> viewModel.setPriceRange(min, max) },
                                onGoodsTypeToggle = { viewModel.toggleGoodsType(it) },
                                onFeedingTypeToggle = { viewModel.toggleFeedingType(it) },
                                onTagToggle = { viewModel.toggleTag(it) },
                                onFamousMerchantToggle = { viewModel.toggleFamousMerchant() },
                                onMerchantToggle = { viewModel.toggleMerchant(it) },
                                onRegionToggle = { viewModel.toggleRegion(it) },
                                onClearFilters = { viewModel.clearFilters() }
                            )
                        }
                    }

                    // 报盘列表（按商家分组）
                    itemsIndexed(
                        items = uiState.filteredMerchantOffers,
                        key = { _, merchantGroup -> merchantGroup.merchantId ?: merchantGroup.hashCode() }
                    ) { index, merchantGroup ->
                        Column {
                            MerchantOfferItem(
                                merchantGroup = merchantGroup,
                                offerType = uiState.offerType,
                                onClick = { },
                                onCopyPhone = onCopyPhone,
                                onCallClick = onCallClick,
                                onViewOriginalText = { text ->
                                    originalText = text
                                    showBottomSheet = true
                                }
                            )
                        }
                        if (index < uiState.filteredMerchantOffers.lastIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = Border, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // 底部加载状态
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (detail.page < detail.totalPages) {
                                if (uiState.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "加载更多",
                                        fontSize = 11.sp,
                                        color = Primary
                                    )
                                }
                            } else {
                                Text(
                                    text = "没有更多了～",
                                    fontSize = 11.sp,
                                    color = TextHint
                                )
                            }
                        }
                    }

                    // 底部留白，防止最后数据被遮住
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // 底部抽屉 - 原文详情
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "原文内容",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 原文内容
                Text(
                    text = if (originalText.isBlank()) "抱歉，暂无原文！" else originalText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = if (originalText.isBlank()) Color(0xFF9DA4A3) else Color(0xFF3C4947)
                )
            }
        }
    }
}

/**
 * 计算商家选项（非 @Composable）
 */
private fun derivedMerchantOptions(merchantOffers: List<MerchantOfferGroup>): List<MerchantOption> {
    return merchantOffers.mapNotNull { group ->
        group.merchantId?.let { id -> MerchantOption(id, group.merchantName ?: "未知商家") }
    }.distinctBy { it.id }
}

/**
 * 计算地区选项（非 @Composable）
 */
private fun derivedRegionOptions(merchantOffers: List<MerchantOfferGroup>): List<String> {
    return merchantOffers.flatMap { group ->
        group.employeeOffers.mapNotNull { it.goodsLocation }
    }.distinct()
}

/**
 * 搜索标签 - 带删除功能的可点击标签
 */
@Composable
fun SearchTag(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Primary)
            .clickable { onClick() }
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color.White
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "删除",
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * 顶部价格信息卡片
 */
@Composable
private fun PriceInfoCard(
    detail: CountryFactoryProductDetail,
    offerType: String,
    isTrendExpanded: Boolean,
    onToggleTrend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 标题行：国家+厂号 · 产品名
        Text(
            text = "${detail.country}${detail.factoryNo} · ${detail.productName}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 价格信息行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // 日期标签
                Text(
                    text = if (offerType == "offer") "近2日报盘价格区间（RMB）" else "近2日求购价格区间（RMB）",
                    fontSize = 10.sp,
                    color = TextHint
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 价格区间
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    val hasPrice = detail.priceMin != null || detail.priceMax != null
                    val priceText = if (detail.priceMin != null && detail.priceMin > 0 && detail.priceMax != null && detail.priceMax > 0) {
                        "¥${formatPrice(detail.priceMin)}-${formatPrice(detail.priceMax)}"
                    } else if (detail.priceMin != null) {
                        "¥${formatPrice(detail.priceMin)}"
                    } else if (detail.priceMax != null) {
                        "¥${formatPrice(detail.priceMax)}"
                    } else {
                        "暂无报价"
                    }

                    Text(
                        text = priceText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Primary
                    )

                    if (hasPrice) {
                        Text(
                            text = "/kg",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 涨跌指示
                    if (detail.priceChange != null && detail.priceChangeRate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (detail.priceChange >= 0) R.drawable.ic_price_trend_up else R.drawable.ic_price_trend_down
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(10.dp, 6.dp),
                                tint = if (detail.priceChange >= 0) Color(0xFFA53321) else Primary
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = "${if (detail.priceChange >= 0) "+" else ""}${formatPrice(detail.priceChange)}  ${formatPrice(detail.priceChangeRate)}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (detail.priceChange >= 0) Color(0xFFA53321) else Primary
                            )
                        }
                    }
                }
            }

            // 7日报价走势
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "7日报价走势",
                    fontSize = 10.sp,
                    color = Color(0xFF9DA4A3),
                    modifier = Modifier.align(Alignment.End)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (detail.priceHistory7Days.isNotEmpty()) {
                    SparklineChart(
                        data = detail.priceHistory7Days,
                        modifier = Modifier
                            .width(82.dp)
                            .height(28.dp)
                    )
                } else {
                    Text(
                        text = "暂无走势数据",
                        fontSize = 10.sp,
                        color = TextHint
                    )
                }
            }
        }

        // 分割线
        Divider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = Border
        )

        // 统计行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                StatItem(label = "报盘数", value = detail.offerCount.toString())
                StatItem(label = "求购数", value = detail.inquiryCount.toString())
                StatItem(label = "商家数", value = detail.merchantCount.toString())
            }

            // 展开数据按钮
            Row(
                modifier = Modifier
                    .background(Color(0xFF171D1C).copy(alpha = 0.05f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onToggleTrend() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTrendExpanded) "收起数据" else "展开数据",
                    fontSize = 10.sp,
                    color = TextPrimary
                )
                Icon(
                    imageVector = if (isTrendExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = TextPrimary
                )
            }
        }

        // 趋势图展开区域
        if (isTrendExpanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Border)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "近30日价格趋势",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PriceTrendChart(
                data = detail.priceHistory30Days,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}

/**
 * 统计项
 */
@Composable
private fun StatItem(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextHint
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/**
 * 计算筛选后的数据（在 Composable 上下文中）
 */
@Composable
private fun rememberFilteredData(
    detail: CountryFactoryProductDetail,
    isFamousMerchant: Boolean,
    selectedMerchants: Set<Long>,
    regions: Set<String>,
    priceMin: String?,
    priceMax: String?,
    goodsTypes: Set<String>,
    feedingTypes: Set<String>,
    tags: Set<String>
): Triple<List<MerchantOfferGroup>, List<MerchantOption>, List<String>> {
    // 计算商家选项
    val merchantOptions = remember(detail.merchantOffers) {
        detail.merchantOffers.mapNotNull { group ->
            group.merchantId?.let { id -> MerchantOption(id, group.merchantName ?: "未知商家") }
        }.distinctBy { it.id }
    }

    // 计算地区选项
    val regionOptions = remember(detail.merchantOffers) {
        detail.merchantOffers.mapNotNull { group ->
            group.employeeOffers.mapNotNull { it.goodsLocation }
        }.flatten().distinct()
    }

    // 应用筛选逻辑
    val filteredMerchantOffers = remember(
        detail.merchantOffers, priceMin, priceMax, goodsTypes, feedingTypes,
        tags, isFamousMerchant, selectedMerchants, regions
    ) {
        detail.merchantOffers.mapNotNull { group ->
            // 知名商家过滤
            if (isFamousMerchant && !group.isFamousMerchant) {
                return@mapNotNull null
            }
            // 商家筛选过滤
            if (selectedMerchants.isNotEmpty()) {
                if (group.merchantId == null || !selectedMerchants.contains(group.merchantId)) {
                    return@mapNotNull null
                }
            }
            // 地区过滤
            if (regions.isNotEmpty()) {
                val hasRegion = group.employeeOffers.any { offer ->
                    offer.goodsLocation != null && regions.any { region -> offer.goodsLocation!!.contains(region) }
                }
                if (!hasRegion) return@mapNotNull null
            }
            // 过滤员工报价
            val filteredOffers = group.employeeOffers.filter { offer ->
                var passes = true
                // 价格区间过滤
                if (priceMin != null || priceMax != null) {
                    val price = offer.price?.replace(Regex("[^\\d.]"), "")?.toDoubleOrNull()
                    if (price != null) {
                        if (priceMin != null && price < priceMin.toDouble()) passes = false
                        if (priceMax != null && price > priceMax.toDouble()) passes = false
                    } else {
                        passes = false
                    }
                }
                // 货物类型过滤
                if (goodsTypes.isNotEmpty()) {
                    if (offer.goodsType == null || !goodsTypes.contains(offer.goodsType)) {
                        passes = false
                    }
                }
                // 饲养方式过滤
                if (feedingTypes.isNotEmpty()) {
                    val hasFeedingType = feedingTypes.any { ft ->
                        offer.tags?.contains(ft) == true
                    }
                    if (!hasFeedingType) passes = false
                }
                // 标签过滤
                if (tags.isNotEmpty()) {
                    if (offer.tags == null || !tags.any { tag -> offer.tags!!.contains(tag) }) {
                        passes = false
                    }
                }
                passes
            }
            if (filteredOffers.isNotEmpty()) {
                group.copy(employeeOffers = filteredOffers, offerCount = filteredOffers.size)
            } else null
        }
    }

    return Triple(filteredMerchantOffers, merchantOptions, regionOptions)
}

/**
 * 筛选栏
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FilterBar(
    activeFilters: Set<String>,
    priceMin: String?,
    priceMax: String?,
    goodsTypes: Set<String>,
    feedingTypes: Set<String>,
    tags: Set<String>,
    offerType: String,
    isFamousMerchant: Boolean,
    selectedMerchants: Set<Long>,
    regions: Set<String>,
    merchantOptions: List<MerchantOption>,
    regionOptions: List<String>,
    onFilterClick: (String) -> Unit,
    onPriceRangeChange: (String?, String?) -> Unit,
    onGoodsTypeToggle: (String) -> Unit,
    onFeedingTypeToggle: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onFamousMerchantToggle: () -> Unit,
    onMerchantToggle: (Long) -> Unit,
    onRegionToggle: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var currentFilterType by remember { mutableStateOf("") }

    val hasActiveFilters = activeFilters.isNotEmpty() || priceMin != null || priceMax != null ||
            goodsTypes.isNotEmpty() || feedingTypes.isNotEmpty() || tags.isNotEmpty() ||
            isFamousMerchant || selectedMerchants.isNotEmpty() || regions.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 筛选按钮行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 知名商家
            FilterChip(
                text = "知名商家",
                isActive = isFamousMerchant,
                onClick = { onFamousMerchantToggle() }
            )
            // 商家筛选
            FilterChip(
                text = "商家筛选",
                isActive = selectedMerchants.isNotEmpty(),
                onClick = {
                    currentFilterType = "商家筛选"
                    showFilterSheet = true
                }
            )
            // 地区筛选
            FilterChip(
                text = "地区",
                isActive = regions.isNotEmpty(),
                onClick = {
                    currentFilterType = "地区"
                    showFilterSheet = true
                }
            )
            // 价格区间
            FilterChip(
                text = "价格区间",
                isActive = priceMin != null || priceMax != null,
                onClick = {
                    currentFilterType = "价格区间"
                    showFilterSheet = true
                }
            )
            // 货物类型
            FilterChip(
                text = "货物类型",
                isActive = goodsTypes.isNotEmpty(),
                onClick = {
                    currentFilterType = "货物类型"
                    showFilterSheet = true
                }
            )
            // 饲养方式
            FilterChip(
                text = "饲养方式",
                isActive = feedingTypes.isNotEmpty(),
                onClick = {
                    currentFilterType = "饲养方式"
                    showFilterSheet = true
                }
            )
            // 标签
            FilterChip(
                text = "标签",
                isActive = tags.isNotEmpty(),
                onClick = {
                    currentFilterType = "标签"
                    showFilterSheet = true
                }
            )
            // 清除筛选
            if (hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFFFE4E4))
                        .clickable { onClearFilters() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "清除",
                        fontSize = 12.sp,
                        color = Color(0xFFFF4444)
                    )
                }
            }
        }

        // 已选条件标签显示
        if (hasActiveFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 价格区间标签
                if (priceMin != null || priceMax != null) {
                    val priceText = when {
                        priceMin != null && priceMax != null -> "¥$priceMin-$priceMax"
                        priceMin != null -> "≥¥$priceMin"
                        priceMax != null -> "≤¥$priceMax"
                        else -> ""
                    }
                    SelectedFilterTag(text = priceText, onRemove = { onPriceRangeChange(null, null) })
                }
                // 货物类型标签
                goodsTypes.forEach { type ->
                    SelectedFilterTag(text = type, onRemove = { onGoodsTypeToggle(type) })
                }
                // 饲养方式标签
                feedingTypes.forEach { type ->
                    SelectedFilterTag(text = type, onRemove = { onFeedingTypeToggle(type) })
                }
                // 标签
                tags.forEach { tag ->
                    SelectedFilterTag(text = tag, onRemove = { onTagToggle(tag) })
                }
                // 商家筛选标签
                selectedMerchants.forEach { merchantId ->
                    val merchantName = merchantOptions.find { it.id == merchantId }?.name ?: "商家$merchantId"
                    SelectedFilterTag(text = merchantName, onRemove = { onMerchantToggle(merchantId) })
                }
                // 地区标签
                regions.forEach { region ->
                    SelectedFilterTag(text = region, onRemove = { onRegionToggle(region) })
                }
            }
        }
    }

    // 筛选 BottomSheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            when (currentFilterType) {
                "商家筛选" -> MerchantFilterSheet(
                    selectedMerchants = selectedMerchants,
                    merchantOptions = merchantOptions,
                    onToggle = onMerchantToggle,
                    onDismiss = { showFilterSheet = false }
                )
                "地区" -> RegionFilterSheet(
                    selectedRegions = regions,
                    regionOptions = regionOptions,
                    onToggle = onRegionToggle,
                    onDismiss = { showFilterSheet = false }
                )
                "价格区间" -> PriceFilterSheet(
                    priceMin = priceMin,
                    priceMax = priceMax,
                    onPriceChange = onPriceRangeChange,
                    onDismiss = { showFilterSheet = false }
                )
                "货物类型" -> GoodsTypeFilterSheet(
                    selectedTypes = goodsTypes,
                    onToggle = onGoodsTypeToggle,
                    onDismiss = { showFilterSheet = false }
                )
                "饲养方式" -> FeedingTypeFilterSheet(
                    selectedTypes = feedingTypes,
                    onToggle = onFeedingTypeToggle,
                    onDismiss = { showFilterSheet = false }
                )
                "标签" -> TagsFilterSheet(
                    selectedTags = tags,
                    offerType = offerType,
                    onToggle = onTagToggle,
                    onDismiss = { showFilterSheet = false }
                )
            }
        }
    }
}

/**
 * 已选筛选标签
 */
@Composable
private fun SelectedFilterTag(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(PrimaryLight)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = Primary)
        Box(modifier = Modifier.clickable { onRemove() }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(12.dp),
                tint = Primary
            )
        }
    }
}

/**
 * 价格区间筛选 Sheet
 */
@Composable
private fun PriceFilterSheet(
    priceMin: String?,
    priceMax: String?,
    onPriceChange: (String?, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var minText by remember { mutableStateOf(priceMin ?: "") }
    var maxText by remember { mutableStateOf(priceMax ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "价格区间", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(16.dp))

        // 价格输入框
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = minText,
                onValueChange = { newValue ->
                    // 只允许数字和小数点，最多2位小数
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    val parts = filtered.split(".")
                    val formatted = when {
                        parts.size > 2 -> parts[0] + "." + parts[1]
                        parts.size == 2 && parts[1].length > 2 -> parts[0] + "." + parts[1].take(2)
                        else -> filtered
                    }
                    minText = formatted
                    errorText = null
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("最低价", color = TextHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )

            Text(text = "至", color = TextHint)

            OutlinedTextField(
                value = maxText,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                    val parts = filtered.split(".")
                    val formatted = when {
                        parts.size > 2 -> parts[0] + "." + parts[1]
                        parts.size == 2 && parts[1].length > 2 -> parts[0] + "." + parts[1].take(2)
                        else -> filtered
                    }
                    maxText = formatted
                    errorText = null
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("最高价", color = TextHint) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border
                )
            )
        }

        // 错误提示
        if (errorText != null) {
            Text(
                text = errorText!!,
                color = Color(0xFFFF4444),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 提示文字
        Text(
            text = "不填最低价则不过滤低价，不填最高价则不过滤高价",
            fontSize = 12.sp,
            color = TextHint
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    minText = ""
                    maxText = ""
                    errorText = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("重置")
            }
            Button(
                onClick = {
                    val min = minText.toDoubleOrNull()
                    val max = maxText.toDoubleOrNull()
                    if (min != null && max != null && min > max) {
                        errorText = "最低价不能大于最高价"
                    } else {
                        onPriceChange(minText.takeIf { it.isNotEmpty() }, maxText.takeIf { it.isNotEmpty() })
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("确定")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 货物类型筛选 Sheet
 */
@Composable
private fun GoodsTypeFilterSheet(
    selectedTypes: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val goodsTypes = listOf("现货", "半期货", "期货")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "货物类型", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(12.dp))

        goodsTypes.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(type) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = type, fontSize = 14.sp)
                Checkbox(
                    checked = selectedTypes.contains(type),
                    onCheckedChange = { onToggle(type) },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
            }
            Divider(color = Border, thickness = 0.5.dp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("确定")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 饲养方式筛选 Sheet
 */
@Composable
private fun FeedingTypeFilterSheet(
    selectedTypes: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val feedingTypes = listOf("草饲", "谷饲")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "饲养方式", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(16.dp))

        feedingTypes.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(type) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = type, fontSize = 14.sp)
                Checkbox(
                    checked = selectedTypes.contains(type),
                    onCheckedChange = { onToggle(type) },
                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                )
            }
            Divider(color = Border, thickness = 0.5.dp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("确定")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 标签筛选 Sheet
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsFilterSheet(
    selectedTags: Set<String>,
    offerType: String,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // 报盘标签和求购标签
    val offerTags = listOf("今日报价", "急售", "正品", "低价", "批发")
    val inquiryTags = listOf("长期采购", "急需", "大量采购", "同行转售")

    val tags = if (offerType == "offer") offerTags else inquiryTags

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "标签筛选", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                val isSelected = selectedTags.contains(tag)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Primary else Color(0xFFF3F6F5))
                        .clickable { onToggle(tag) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("确定")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 商家筛选 Sheet
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MerchantFilterSheet(
    selectedMerchants: Set<Long>,
    merchantOptions: List<MerchantOption>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val filteredMerchants = remember(searchText, merchantOptions) {
        if (searchText.isEmpty()) merchantOptions
        else merchantOptions.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "商家筛选", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(8.dp))

        // 搜索框
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索商家", color = TextHint, fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextHint, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Border
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 已选商家标签
        if (selectedMerchants.isNotEmpty()) {
            Text(text = "已选商家", fontSize = 12.sp, color = TextHint)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                selectedMerchants.forEach { merchantId: Long ->
                    val merchant: MerchantOption? = merchantOptions.find { it.id == merchantId }
                    if (merchant != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = merchant.name, fontSize = 11.sp, color = Primary)
                                Box(modifier = Modifier.padding(start = 2.dp).clickable { onToggle(merchantId) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除",
                                        modifier = Modifier.size(12.dp),
                                        tint = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 商家列表（可滚动）
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 400.dp)
        ) {
            items(filteredMerchants.size) { index ->
                val merchant = filteredMerchants[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(merchant.id) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = merchant.name, fontSize = 13.sp)
                    Checkbox(
                        checked = selectedMerchants.contains(merchant.id),
                        onCheckedChange = { onToggle(merchant.id) },
                        colors = CheckboxDefaults.colors(checkedColor = Primary),
                        modifier = Modifier.height(32.dp)
                    )
                }
                if (index < filteredMerchants.size - 1) {
                    Divider(color = Border, thickness = 0.5.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("确定")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 地区筛选 Sheet
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegionFilterSheet(
    selectedRegions: Set<String>,
    regionOptions: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = "地区筛选", fontSize = 16.sp, fontWeight = FontWeight.Medium)

        Spacer(modifier = Modifier.height(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            regionOptions.forEach { region ->
                val isSelected = selectedRegions.contains(region)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Primary else Color(0xFFF3F6F5))
                        .clickable { onToggle(region) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = region,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("确定")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 筛选芯片
 */
@Composable
private fun FilterChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(if (isActive) Primary else Color(0xFFF3F6F5))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isActive) Color.White else TextPrimary
        )
    }
}

/**
 * 排序栏
 */
@Composable
private fun SortBar(
    currentSort: String,
    currentType: String,
    onSortChange: (String) -> Unit,
    onTypeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // 表头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：报盘/求购切换
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TypeTab("报盘", currentType == "offer", onClick = { onTypeChange("offer") })
                TypeTab("求购", currentType == "inquiry", onClick = { onTypeChange("inquiry") })
            }

            // 右侧：综合推荐/发布时间/价格 - 跟厂号详情页完全一致
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                SortTab("综合推荐", currentSort == "comprehensive", onClick = { onSortChange("comprehensive") })
                SortTab("发布时间", currentSort == "publish_time", onClick = { onSortChange("publish_time") })
                // 价格排序（整个区域可点击，cycling 在 ViewModel 中处理）
                PriceSortButton(
                    isPriceAscActive = currentSort == "price_asc",
                    isPriceDescActive = currentSort == "price_desc",
                    onPriceClick = { onSortChange("price") }
                )
            }
        }

        Divider(color = Border, thickness = 0.5.dp)
    }
}

/**
 * 排序标签
 */
@Composable
private fun SortTab(text: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) TextPrimary else TextHint
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(Primary, RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

/**
 * 价格排序按钮（带上下箭头）
 */
@Composable
private fun PriceSortButton(
    isPriceAscActive: Boolean,
    isPriceDescActive: Boolean,
    onPriceClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onPriceClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "价格",
                fontSize = 14.sp,
                fontWeight = if (isPriceAscActive || isPriceDescActive) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isPriceAscActive || isPriceDescActive) TextPrimary else TextHint
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp, 8.dp)
                        .background(
                            if (isPriceAscActive) Primary else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "升序",
                        tint = if (isPriceAscActive) Color.White else Color(0xFF8B8B8B),
                        modifier = Modifier.size(12.dp, 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(12.dp, 8.dp)
                        .background(
                            if (isPriceDescActive) Primary else Color.Transparent,
                            RoundedCornerShape(1.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "降序",
                        tint = if (isPriceDescActive) Color.White else Color(0xFF8B8B8B),
                        modifier = Modifier.size(12.dp, 8.dp)
                    )
                }
            }
        }
        // 显示下划条
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(3.dp)
                .background(if (isPriceAscActive || isPriceDescActive) Primary else Color.Transparent, RoundedCornerShape(1.dp))
        )
    }
}

/**
 * 类型标签（报盘/求购）
 */
@Composable
private fun TypeTab(text: String, isActive: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) TextPrimary else TextHint
        )
        if (isActive) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .background(Primary, RoundedCornerShape(1.dp))
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

/**
 * 商家报盘分组项（可展开/收起）- 参考商家详情页样式
 */
@Composable
private fun MerchantOfferItem(
    merchantGroup: MerchantOfferGroup,
    offerType: String,
    onClick: () -> Unit,
    onCopyPhone: (String) -> Unit = {},
    onCallClick: (String) -> Unit = {},
    onViewOriginalText: (String) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 创建绑定到商家电话的回调
    val copyPhoneCallback: () -> Unit = {
        merchantGroup.merchantPhone?.let { phone ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("phone", phone)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "已复制手机号", Toast.LENGTH_SHORT).show()
        }
    }
    val callPhoneCallback: () -> Unit = {
        merchantGroup.merchantPhone?.let { phone ->
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            context.startActivity(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 主卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFBFFFE))
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 12.dp)
        ) {
            // 商家名和价格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：商家名 + 报盘数
                val merchantName = merchantGroup.merchantName ?: "商家"
                val isNameLong = merchantName.length > 10
                val isFamous = merchantGroup.isFamousMerchant

                if (isNameLong) {
                    // 商家名过长，两行布局
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：知名商家徽章 + 皇冠 + 商家名
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 知名商家徽章 + 皇冠（连接在一起）
                                if (isFamous) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF254d5a), RoundedCornerShape(2.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "知名商家",
                                                fontSize = 9.sp,
                                                color = Color.White
                                            )
                                        }
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_merchant_crown),
                                            contentDescription = "知名商家",
                                            modifier = Modifier
                                                .width(26.dp)
                                                .height(18.dp)
                                                .offset(x = (-10).dp)
                                        )
                                    }
                                    Text(
                                        text = merchantName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 170.dp)
                                            .offset(x = (-8).dp)
                                    )
                                } else {
                                    Text(
                                        text = merchantName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 商家名不长，一行布局
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // 知名商家徽章 + 皇冠（连接在一起）
                        if (isFamous) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF254d5a), RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "知名商家",
                                        fontSize = 9.sp,
                                        color = Color.White
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.ic_merchant_crown),
                                    contentDescription = "知名商家",
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(18.dp)
                                        .offset(x = (-10).dp)
                                )
                            }
                            Text(
                                text = merchantName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 170.dp)
                                    .offset(x = (-8).dp)
                            )
                        } else {
                            Text(
                                text = merchantName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // 右侧：价格范围 + 展开图标
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 价格范围
                    val minPrice = merchantGroup.employeeOffers.mapNotNull { it.price?.toDoubleOrNull() }.minOrNull()
                    val maxPrice = merchantGroup.employeeOffers.mapNotNull { it.price?.toDoubleOrNull() }.maxOrNull()

                    if (minPrice != null && maxPrice != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (minPrice == maxPrice) "¥$minPrice" else "¥$minPrice-$maxPrice",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Primary
                            )
                            Text(
                                text = "/kg",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextPrimary
                            )
                        }
                    } else {
                        Text(
                            text = "协商报价",
                            fontSize = 12.sp,
                            color = TextHint
                        )
                    }

                    // 展开图标 - 在右边
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            colorFilter = if (!isExpanded) ColorFilter.tint(Color(0xFFBFCAC8)) else null,
                            modifier = Modifier
                                .size(12.dp, 16.dp)
                                .graphicsLayer { rotationZ = if (isExpanded) 270f else 90f }
                        )
                    }
                }
            }

            // 标签行 - 货物所在地 + 类型（合并显示所有不重复的地点和类型）
            // 货物地点格式为"省份/城市"、"省份城市"或"省份\城市"，只显示城市部分
            val allLocations = merchantGroup.employeeOffers.mapNotNull { extractCity(it.goodsLocation) }.filter { it.isNotEmpty() }.distinct()
            val allTypes = merchantGroup.employeeOffers.mapNotNull { it.goodsType }.filter { it.isNotEmpty() }.distinct()
            val allTags = merchantGroup.employeeOffers.mapNotNull { it.tags?.split(",")?.filter { t -> t.isNotBlank() }?.take(4) }.flatten().distinct().take(4)
            if (allLocations.isNotEmpty() || allTypes.isNotEmpty() || allTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (allLocations.isNotEmpty()) {
                        OfferTag(text = allLocations.joinToString("/"), bgColor = Color(0xFFF2F8F7), textColor = Primary, hasIcon = true)
                    }
                    if (allTypes.isNotEmpty()) {
                        OfferTag(text = allTypes.joinToString("/"), bgColor = Color(0xFFF2F3FF), textColor = Color(0xFF485B88), hasIcon = false)
                    }
                    allTags.forEach { tag ->
                        val (bgColor, txtColor) = when {
                            tag.contains("大日期") || tag.contains("日期") -> Color(0xFFF2F3FF) to Color(0xFF3163DC)
                            tag.contains("可开票") || tag.contains("票") -> Color(0xFFFFF5E4) to Color(0xFFA07D17)
                            tag.contains("品牌") -> Color(0xFFFFF0F0) to Color(0xFFDC3545)
                            else -> Color(0xFFF2F8F7) to Color(0xFF3C4947)
                        }
                        OfferTag(text = tag.trim(), bgColor = bgColor, textColor = txtColor, hasIcon = false)
                    }
                }
            }
        }

        // 展开的员工报价列表
        if (isExpanded) {
            merchantGroup.employeeOffers.forEach { employeeOffer ->
                EmployeeOfferCard(
                    employeeOffer = employeeOffer,
                    merchantPhone = merchantGroup.merchantPhone,
                    onCopyPhone = copyPhoneCallback,
                    onCallClick = callPhoneCallback,
                    onViewOriginalText = onViewOriginalText
                )
            }
        }
    }
}

/**
 * 员工报价卡片 - 参考商家详情页样式
 */
@Composable
private fun EmployeeOfferCard(
    employeeOffer: EmployeeOfferItem,
    merchantPhone: String?,
    onViewOriginalText: (String) -> Unit = {},
    onCopyPhone: () -> Unit = {},
    onCallClick: () -> Unit = {}
) {
    // 解析重量数据
    val (weightValue, weightUnit) = remember(employeeOffer.weight) { parseWeight(employeeOffer.weight) }
    val hasWeight = weightValue.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color(0xFFFBFFFE),
                    RoundedCornerShape(4.dp)
                )
                .border(0.5.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 用户信息和价格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 左侧：头像 + 用户名 + 手机号（超长时换行）
                    if (hasWeight) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_avatar),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = employeeOffer.userNickname ?: "员工",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            merchantPhone?.let { phone ->
                                Text(
                                    text = phone,
                                    fontSize = 12.sp,
                                    color = TextHint,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_avatar),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = employeeOffer.userNickname ?: "员工",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            merchantPhone?.let { phone ->
                                Text(
                                    text = phone,
                                    fontSize = 12.sp,
                                    color = TextHint,
                                    modifier = Modifier.padding(start = 26.dp)
                                )
                            }
                        }
                    }

                    // 右侧：重量 + 价格
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 重量
                        if (weightValue.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = weightValue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (weightUnit.isNotEmpty()) {
                                    Text(
                                        text = weightUnit,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                        // 价格
                        if (employeeOffer.price != null && employeeOffer.price != "协商报价") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "¥${employeeOffer.price}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                                Text(
                                    text = "/kg",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = TextPrimary
                                )
                            }
                        } else {
                            Text(
                                text = "协商报价",
                                fontSize = 12.sp,
                                color = TextHint
                            )
                        }
                    }
                }

                // 时间、地区、标签
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(employeeOffer.publishTime),
                        fontSize = 11.sp,
                        color = Color(0xFF3C4947)
                    )

                    if (employeeOffer.goodsLocation?.isNotEmpty() == true) {
                        OfferTag(
                            text = extractCity(employeeOffer.goodsLocation),
                            bgColor = Color(0xFFF2F8F7),
                            textColor = Primary,
                            hasIcon = true
                        )
                    }
                    if (employeeOffer.goodsType?.isNotEmpty() == true) {
                        OfferTag(
                            text = employeeOffer.goodsType,
                            bgColor = Color(0xFFF3F6F5),
                            textColor = Color(0xFF3C4947),
                            hasIcon = false
                        )
                    }
                    employeeOffer.tags?.split(",")?.filter { it.isNotBlank() }?.take(4)?.forEach { tag ->
                        val (bgColor, txtColor) = when {
                            tag.contains("大日期") || tag.contains("日期") -> Color(0xFFF2F3FF) to Color(0xFF3163DC)
                            tag.contains("可开票") || tag.contains("票") -> Color(0xFFFFF5E4) to Color(0xFFA07D17)
                            tag.contains("整柜") || tag.contains("柜") -> Color(0xFFFFF0ED) to Color(0xFFD54941)
                            tag.contains("一口价") || tag.contains("价") -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                            else -> Color(0xFFF3F6F5) to Color(0xFF3C4947)
                        }
                        OfferTag(text = tag.trim(), bgColor = bgColor, textColor = txtColor, hasIcon = false)
                    }
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmployeeActionButton(
                        icon = Icons.Default.Description,
                        text = "查看原文",
                        textColor = Color(0xFF3C4947),
                        onClick = { onViewOriginalText(employeeOffer.offerOriginalText ?: employeeOffer.offerType ?: "") },
                        iconPainter = painterResource(id = R.drawable.ic_book)
                    )

                    VerticalDivider()

                    EmployeeActionButton(
                        icon = Icons.Default.ContentCopy,
                        text = "添加微信",
                        textColor = Color(0xFF3C4947),
                        onClick = onCopyPhone,
                        iconPainter = painterResource(id = R.drawable.ic_add_square)
                    )

                    VerticalDivider()

                    EmployeeActionButton(
                        icon = Icons.Default.Call,
                        text = "拨打电话",
                        textColor = Primary,
                        onClick = onCallClick
                    )
                }
            }
        }
    }
}

/**
 * 解析重量值
 */
private fun parseWeight(weight: String?): Pair<String, String> {
    if (weight.isNullOrBlank()) {
        return "" to ""
    }
    val regex = Regex("^([\\d.]+)(.*)$")
    val match = regex.find(weight.trim())
    return if (match != null) {
        val (value, unit) = match.destructured
        val numValue = value.toDoubleOrNull()
        val roundedValue = if (numValue != null) {
            val rounded = Math.round(numValue * 10.0) / 10.0
            if (rounded == rounded.toLong().toDouble()) {
                rounded.toLong().toString()
            } else {
                String.format("%.1f", rounded)
            }
        } else {
            value
        }
        roundedValue to unit.trim()
    } else {
        weight to ""
    }
}

/**
 * 提取城市名称（去掉省份前缀）
 * 格式可能是：河北省/北京、河北省北京、河北省\北京 等
 */
private fun extractCity(location: String?): String {
    if (location.isNullOrBlank()) return ""
    return try {
        when {
            location.contains("/") -> location.substringAfter("/").trim()
            location.contains("\\") -> location.substringAfter("\\").trim()
            location.contains("省") -> location.substringAfter("省").trim()
            else -> location.trim()
        }
    } catch (e: Exception) {
        location
    }
}

/**
 * 格式化时间（支持 yyyy-MM-dd HH:mm 和 yyyy-MM-dd'T'HH:mm:ss 格式）
 */
private fun formatTime(timeString: String?): String {
    if (timeString.isNullOrEmpty()) return ""
    return try {
        // 尝试两种格式
        val date = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(timeString)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(timeString)
            } catch (e: Exception) {
                null
            }
        } ?: return timeString.takeLast(5) // 解析失败则只显示时分

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val dateCalendar = Calendar.getInstance().apply { time = date }

        val prefix = when {
            dateCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            dateCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "今天"
            dateCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
            dateCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "昨天"
            else -> ""
        }
        if (prefix.isNotEmpty()) {
            "$prefix ${outputFormat.format(date)}"
        } else {
            outputFormat.format(date)
        }
    } catch (e: Exception) {
        timeString.takeLast(5)
    }
}

/**
 * 员工操作按钮
 */
@Composable
private fun EmployeeActionButton(
    icon: ImageVector,
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = textColor
        )
    }
}

/**
 * 垂直分隔线
 */
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(0.5.dp)
            .height(13.dp)
            .background(Color(0xFF3C4947))
    )
}

/**
 * 报价标签
 */
@Composable
private fun OfferTag(
    text: String,
    bgColor: Color,
    textColor: Color,
    hasIcon: Boolean
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(1.dp))
            .padding(horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (hasIcon) {
                Image(
                    painter = painterResource(id = R.drawable.ic_location),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text(
                text = text,
                fontSize = 10.sp,
                color = textColor
            )
        }
    }
}

/**
 * 员工报价操作按钮
 */
@Composable
private fun EmployeeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(1.dp, Border, RoundedCornerShape(2.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = TextHint
        )
        Text(
            text = text,
            fontSize = 11.sp,
            color = TextHint
        )
    }
}

/**
 * 格式化发布时间（显示今天/昨天 + 时分）
 */
private fun formatPublishTime(publishTime: String?): String {
    if (publishTime.isNullOrBlank()) return ""
    return try {
        // 格式为 yyyy-MM-dd HH:mm
        val parts = publishTime.trim().split(" ")
        if (parts.size >= 2) {
            val datePart = parts[0].trim()
            val timePart = parts[1].trim().substring(0, minOf(5, parts[1].length))
            val today = java.time.LocalDate.now().toString()
            val yesterday = java.time.LocalDate.now().minusDays(1).toString()
            val prefix = when (datePart) {
                today -> "今天"
                yesterday -> "昨天"
                else -> datePart
            }
            "$prefix $timePart"
        } else {
            publishTime
        }
    } catch (e: Exception) {
        publishTime
    }
}

/**
 * 标签项
 */
@Composable
private fun TagItem(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(1.dp))
            .background(bgColor)
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = textColor
        )
    }
}

/**
 * 迷你折线图
 */
@Composable
private fun SparklineChart(
    data: List<DailyPrice>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    val lineColor = Primary.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val prices = data.mapNotNull { it.avgPrice }
        if (prices.isEmpty()) return@Canvas

        val minPrice = prices.minOrNull() ?: return@Canvas
        val maxPrice = prices.maxOrNull() ?: return@Canvas
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        // 绘制折线
        val path = Path()
        data.forEachIndexed { index, dailyPrice ->
            val x = index * stepX
            val y = height - ((dailyPrice.avgPrice!! - minPrice) / priceRange * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * 价格趋势图（与国家+产品详情页一致）
 */
@Composable
private fun PriceTrendChart(
    data: List<DailyPrice>,
    modifier: Modifier = Modifier
) {
    val primaryColor = Primary
    val lineColor = Primary.copy(alpha = 0.3f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val stepX = size.width.toFloat() / (data.size - 1).coerceAtLeast(1)
                            val index = ((offset.x / stepX) + 0.5f).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == index) null else index
                        }
                    }
            ) {
                if (data.isEmpty()) return@Canvas

                val prices = data.mapNotNull { it.avgPrice }
                if (prices.isEmpty()) return@Canvas

                val minPrice = prices.minOrNull() ?: return@Canvas
                val maxPrice = prices.maxOrNull() ?: return@Canvas
                val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

                val width = size.width
                val height = size.height
                val stepX = width / (data.size - 1).coerceAtLeast(1)

                // 绘制背景网格线
                for (i in 0..4) {
                    val y = height * i / 4
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 绘制数据线
                val path = Path()
                var firstPoint = true

                data.forEachIndexed { index, item ->
                    if (item.avgPrice != null) {
                        val x = index * stepX
                        val y = height - ((item.avgPrice - minPrice) / priceRange * height).toFloat()

                        if (firstPoint) {
                            path.moveTo(x, y)
                            firstPoint = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 绘制数据点
                data.forEachIndexed { index, item ->
                    if (item.avgPrice != null) {
                        val x = index * stepX
                        val y = height - ((item.avgPrice - minPrice) / priceRange * height).toFloat()

                        // 选中的点放大
                        val radius = if (selectedIndex == index) 6.dp.toPx() else 3.dp.toPx()

                        drawCircle(
                            color = primaryColor,
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
            }

            // Tooltip - 显示在点击位置下方
            selectedIndex?.let { index ->
                val item = data[index]
                if (item.avgPrice != null) {
                    val prices = data.mapNotNull { it.avgPrice }
                    val minPrice = prices.minOrNull() ?: 0.0
                    val maxPrice = prices.maxOrNull() ?: 1.0
                    val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice
                    val chartHeight = 120.dp
                    val chartWidth = 300.dp
                    val stepX = chartWidth.value / (data.size - 1).coerceAtLeast(1)
                    val x = index * stepX
                    val y = chartHeight.value - ((item.avgPrice - minPrice) / priceRange * chartHeight.value).toFloat()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .offset(y = (-36).dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .shadow(2.dp, RoundedCornerShape(4.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${item.date} ",
                                fontSize = 11.sp,
                                color = TextHint
                            )
                            Text(
                                text = "¥${formatPrice(item.avgPrice)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X轴日期标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val datesToShow = if (data.size > 7) {
                listOf(0, data.size / 4, data.size / 2, data.size * 3 / 4, data.size - 1)
            } else {
                data.indices.toList()
            }

            datesToShow.forEach { index ->
                if (index < data.size) {
                    Text(
                        text = data[index].date,
                        fontSize = 10.sp,
                        color = TextHint
                    )
                }
            }
        }
    }
}

/**
 * 格式化价格
 */
private fun formatPrice(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
