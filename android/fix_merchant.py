with open('E:/project6/android/app/src/main/java/com/mooket/app/ui/screens/merchant/MerchantScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Merge stickyHeaders
old = '''            // Tab stickyHeader
            stickyHeader(key = "merchant_tab") {
                TabSection(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    priceSortOrder = priceSortOrder,
                    onPriceSortToggle = onPriceSortToggle,
                    sortByRecommend = sortByRecommend,
                    onSortByRecommendToggle = onSortByRecommendToggle
                )
            }

            // Filter stickyHeader
            stickyHeader(key = "merchant_filter") {
                FilterSection(
                    onCountryFactoryClick = onCountryFactoryClick,
                    hasCountrySelection = hasCountrySelection,
                    onRegionClick = onRegionClick,
                    hasRegionSelection = selectedRegions.isNotEmpty(),
                    onProductClick = onProductClick,
                    hasProductSelection = selectedProducts.isNotEmpty(),
                    onGoodsTypeClick = onGoodsTypeClick,
                    hasGoodsTypeSelection = selectedGoodsTypes.isNotEmpty(),
                    onFeedingMethodClick = onFeedingMethodClick,
                    hasFeedingMethodSelection = selectedFeedingMethods.isNotEmpty(),
                    activeFilter = when {
                        showCountryFactoryFilter -> "countryFactory"
                        showRegionFilter -> "region"
                        showProductFilter -> "product"
                        showGoodsTypeFilter -> "goodsType"
                        showFeedingMethodFilter -> "feedingMethod"
                        else -> null
                    }
                )
            }'''

new = '''            // Combined Tab + Filter stickyHeader
            stickyHeader(key = "merchant_sticky") {
                Column {
                    TabSection(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        priceSortOrder = priceSortOrder,
                        onPriceSortToggle = onPriceSortToggle,
                        sortByRecommend = sortByRecommend,
                        onSortByRecommendToggle = onSortByRecommendToggle
                    )
                    FilterSection(
                        onCountryFactoryClick = onCountryFactoryClick,
                        hasCountrySelection = hasCountrySelection,
                        onRegionClick = onRegionClick,
                        hasRegionSelection = selectedRegions.isNotEmpty(),
                        onProductClick = onProductClick,
                        hasProductSelection = selectedProducts.isNotEmpty(),
                        onGoodsTypeClick = onGoodsTypeClick,
                        hasGoodsTypeSelection = selectedGoodsTypes.isNotEmpty(),
                        onFeedingMethodClick = onFeedingMethodClick,
                        hasFeedingMethodSelection = selectedFeedingMethods.isNotEmpty(),
                        activeFilter = when {
                            showCountryFactoryFilter -> "countryFactory"
                            showRegionFilter -> "region"
                            showProductFilter -> "product"
                            showGoodsTypeFilter -> "goodsType"
                            showFeedingMethodFilter -> "feedingMethod"
                            else -> null
                        }
                    )
                }
            }'''

if old in content:
    content = content.replace(old, new)
    print('1. Sticky merged OK')
else:
    print('ERROR: sticky pattern')

# 2. Remove green Box from TabSection
old_tab = '''    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 分隔背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color(0xFFF4FBF8))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {'''

new_tab = '''    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {'''

if old_tab in content:
    content = content.replace(old_tab, new_tab)
    print('2. TabSection green box removed OK')
else:
    print('ERROR: tab pattern')

# 3. Add sheetState and currentFilterType after listState
content = content.replace(
    'val listState = rememberLazyListState()',
    'val listState = rememberLazyListState()\n    val sheetState = rememberModalBottomSheetState()\n    var currentFilterType by remember { mutableStateOf("") }'
)
print('3. sheetState added OK')

# 4. Convert filter panels to ModalBottomSheet
old_panels = '''        // filter overlay panels
        if (showCountryFactoryFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onFilterConfirm() }
            )
            CountryFactoryFilterPanel(
                offers = currentProducts,
                selectedCountry = selectedCountry,
                selectedFactories = selectedFactories,
                onCountrySelected = onCountrySelected,
                onFactoryToggle = onFactoryToggle,
                onReset = onFilterReset,
                onConfirm = onFilterConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showRegionFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onRegionConfirm() }
            )
            RegionFilterPanel(
                offers = currentProducts,
                selectedRegions = selectedRegions,
                onRegionToggle = onRegionToggle,
                onReset = onRegionReset,
                onConfirm = onRegionConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showProductFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onProductConfirm() }
            )
            ProductFilterPanel(
                offers = currentProducts,
                selectedProducts = selectedProducts,
                onProductToggle = onProductToggle,
                onReset = onProductReset,
                onConfirm = onProductConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showGoodsTypeFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onGoodsTypeConfirm() }
            )
            GoodsTypeFilterPanel(
                selectedGoodsTypes = selectedGoodsTypes,
                onGoodsTypeToggle = onGoodsTypeToggle,
                onReset = onGoodsTypeReset,
                onConfirm = onGoodsTypeConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showFeedingMethodFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.60f))
                    .clickable { onFeedingMethodConfirm() }
            )
            FeedingMethodFilterPanel(
                selectedFeedingMethods = selectedFeedingMethods,
                onFeedingMethodToggle = onFeedingMethodToggle,
                onReset = onFeedingMethodReset,
                onConfirm = onFeedingMethodConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 商家头部'''

new_panels = '''        // filter overlay panels - ModalBottomSheet from bottom
        if (showCountryFactoryFilter || showRegionFilter || showProductFilter || showGoodsTypeFilter || showFeedingMethodFilter) {
            currentFilterType = when {
                showCountryFactoryFilter -> "countryFactory"
                showRegionFilter -> "region"
                showProductFilter -> "product"
                showGoodsTypeFilter -> "goodsType"
                showFeedingMethodFilter -> "feedingMethod"
                else -> ""
            }
        }
        if (showCountryFactoryFilter || showRegionFilter || showProductFilter || showGoodsTypeFilter || showFeedingMethodFilter) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (showCountryFactoryFilter) onFilterConfirm()
                    else if (showRegionFilter) onRegionConfirm()
                    else if (showProductFilter) onProductConfirm()
                    else if (showGoodsTypeFilter) onGoodsTypeConfirm()
                    else if (showFeedingMethodFilter) onFeedingMethodConfirm()
                },
                sheetState = sheetState
            ) {
                when (currentFilterType) {
                    "countryFactory" -> CountryFactoryFilterPanel(
                        offers = currentProducts,
                        selectedCountry = selectedCountry,
                        selectedFactories = selectedFactories,
                        onCountrySelected = onCountrySelected,
                        onFactoryToggle = onFactoryToggle,
                        onReset = onFilterReset,
                        onConfirm = onFilterConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "region" -> RegionFilterPanel(
                        offers = currentProducts,
                        selectedRegions = selectedRegions,
                        onRegionToggle = onRegionToggle,
                        onReset = onRegionReset,
                        onConfirm = onRegionConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "product" -> ProductFilterPanel(
                        offers = currentProducts,
                        selectedProducts = selectedProducts,
                        onProductToggle = onProductToggle,
                        onReset = onProductReset,
                        onConfirm = onProductConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "goodsType" -> GoodsTypeFilterPanel(
                        selectedGoodsTypes = selectedGoodsTypes,
                        onGoodsTypeToggle = onGoodsTypeToggle,
                        onReset = onGoodsTypeReset,
                        onConfirm = onGoodsTypeConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                    "feedingMethod" -> FeedingMethodFilterPanel(
                        selectedFeedingMethods = selectedFeedingMethods,
                        onFeedingMethodToggle = onFeedingMethodToggle,
                        onReset = onFeedingMethodReset,
                        onConfirm = onFeedingMethodConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 商家头部'''

if old_panels in content:
    content = content.replace(old_panels, new_panels)
    print('4. Filter panels -> ModalBottomSheet OK')
else:
    print('ERROR: panels pattern not found')

# 5. Add OptIn and imports
content = content.replace(
    'import androidx.compose.foundation.Image',
    'import androidx.compose.foundation.ExperimentalFoundationApi\nimport androidx.compose.foundation.Image'
)
content = content.replace(
    '@OptIn(ExperimentalFoundationApi::class)',
    '@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)'
)
print('5. Imports+OptIn OK')

with open('E:/project6/android/app/src/main/java/com/mooket/app/ui/screens/merchant/MerchantScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print('Written OK')