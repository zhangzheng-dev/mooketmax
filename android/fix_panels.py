with open('E:/project6/android/app/src/main/java/com/mooket/app/ui/screens/merchant/MerchantScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

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

with open('E:/project6/android/app/src/main/java/com/mooket/app/ui/screens/merchant/MerchantScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)