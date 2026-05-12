# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**牧集 (Mooket)** - 全球肉类供应链 B2B 商机与行情搜索平台
- **Android App**: Kotlin + Jetpack Compose (minSdk 24, targetSdk 34)
- **Backend**: Java 17 + Spring Boot 3.2.0 + MyBatis Plus
- **Remote Server**: `ai-pg-43.139.56.124` (root@43.139.56.124)

## 构建命令

### 后端 (E:\project6\social)
```cmd
:: 运行
mvn spring-boot:run

:: 打包
mvn package -DskipTests
```

### Android (E:\project6\android)
```bash
./gradlew assembleDebug    # 调试 APK
./gradlew assembleRelease  # 发布 APK
```

APK 输出目录：`app/build/outputs/apk/debug/app-debug.apk`

## SSH 部署

**必须使用 SSH Skill**，禁止直接使用 `ssh`/`scp` 命令：
```bash
# 执行远程命令
python ~/.claude/skills/ssh-skill/scripts/ssh_execute.py <alias> "<command>"

# 上传文件
MSYS_NO_PATHCONV=1 python ~/.claude/skills/ssh-skill/scripts/ssh_upload.py <alias> "<local>" "<remote>"
```

服务器别名：`ai-pg-43.139.56.124`

部署新版本：
1. 停止旧进程：`killall -9 java`
2. 上传 JAR：`social-1.0.0-SNAPSHOT.jar` → `/tmp/social.jar`
3. 启动：`cd /tmp && nohup java -jar social.jar --server.port=8080 > /tmp/social.log 2>&1 &`
4. 日志：`tail -50 /tmp/social.log`

## 架构

### 项目结构
```
E:\project6\
├── android/                    # Android App (Kotlin + Jetpack Compose)
├── social/                     # Backend (Java + Spring Boot)
├── sql/                        # SQL scripts
│   ├── init/                   # PostgreSQL 初始化
│   └── sync/                   # MySQL→PostgreSQL 同步
└── CLAUDE.md                   # 本文件
```

### Android App 结构
```
app/src/main/java/com/mooket/app/
├── MainActivity.kt              # 入口，设置导航
├── navigation/Navigation.kt     # NavHost，路由
├── ui/
│   ├── screens/home/            # HomeScreen
│   ├── screens/search/          # SearchScreen, SearchViewModel
│   ├── screens/merchant/       # MerchantScreen, MerchantViewModel
│   ├── screens/product/         # ProductDetailScreen, ProductDetailViewModel
│   ├── screens/country/        # CountryDetailScreen
│   ├── screens/factory/         # FactoryDetailScreen
│   ├── screens/countryproduct/  # CountryProductScreen, CountryProductViewModel
│   └── screens/profile/          # ProfileScreen, ProfileViewModel (个人中心)
└── data/
    ├── model/Models.kt          # 数据类
    ├── api/ApiService.kt        # Retrofit API
    └── repository/             # Repository 层
```

### Android 导航流程
```
HomeScreen → SearchScreen → ProductDetailScreen
     ↓ (category)           ↓ (productId, category, productName)
  SearchScreen           → MerchantScreen
                              ↓ (merchantId, category)
                          MerchantScreen

搜索联想 → 国家详情 / 厂号详情 / 国家+产品详情

国家厂号产品详情页 (CountryFactoryProductScreen)
  ↑ 点击热门商家/列表卡片进入
  ↓ 点击"平替"进入
平替产品页 (SubstituteProductScreen) → 数据对比页 (DataComparisonScreen)
```

**注意**：`HomeCardsScreen` 是瀑布流展示页面，与 `HomeScreen` 并列，都有 Tab 切换（自选数据/历史搜索数据）。

### 后端多数据源架构

项目使用**四数据源**架构：

| 数据源 | 数据库 | 包路径 |
|--------|--------|--------|
| primary | PostgreSQL (mooket_db) | `com.mooket.social.mapper`<br>`com.mooket.social.entity` |
| mysql | mallee_muji_social | `com.mooket.social.mysql.mapper` |
| erp | mallee_muji_erp | `com.mooket.social.erp.mapper` |
| uac | mallee_muji_uac | `com.mooket.social.uac.mapper` |

**关键配置：**
- `SocialApplication.java` - 定义 `primaryDataSource` (PostgreSQL)
- `MysqlDataSourceConfig.java` - MySQL social 数据源配置
- `ErpDataSourceConfig.java` - MySQL erp 数据源配置

### 数据同步架构

数据从多个 MySQL 源同步到 PostgreSQL：

```
MySQL 源                          →  PostgreSQL 目标
├── social_online_business        →  biz_offer
├── social_standard_goods_name   →  dict_product
├── erp_base_approval            →  dict_factory
├── erp_base_approval            →  dict_brand
├── user_base_info               →  dict_merchant
├── rbac_user                    →  rel_user_merchant
```

**同步顺序（无循环依赖）：**
1. `dict_product` - 产品字典
2. `dict_factory` - 厂号字典（brand_id 置空）
3. `dict_brand` - 品牌字典（通过 factory_no 查 dict_factory）
4. `dict_merchant` - 商家字典
5. `rel_user_merchant` - 用户商家关联
6. `biz_offer` - 报盘/求购（通过 factory_no 查 factory_id，通过 factory_id 查 brand_id）

**手动同步 API：**
```bash
POST /api/v1/sync/dict-product      # 产品字典
POST /api/v1/sync/dict-factory     # 厂号字典
POST /api/v1/sync/dict-brand       # 品牌字典
POST /api/v1/sync/dict-merchant    # 商家字典
POST /api/v1/sync/rel-user-merchant # 用户商家关联
POST /api/v1/sync/biz-offer/initial # 报盘增量同步
POST /api/v1/sync/biz-offer/full   # 报盘全量同步
```

### 搜索架构

`SearchServiceImpl` 实现搜索联想词功能：
1. `parseEntities()` - 解析关键词中的实体（国家、厂号、品牌、产品、商家）
2. 实体匹配优先级：先别名精确匹配 → 别名模糊匹配 → 标准名双向匹配
3. `generate*Suggestions()` - 按优先级生成联想结果（7个优先级）

**别名匹配规则：**
- 产品别名 `alias_list` 使用 `[,，、]` 分隔
- 显示格式：`标准品名(别名：XXX)`（搜索时输入别名）

## 关键文件

### 后端核心文件
| 文件 | 说明 |
|------|------|
| `DataSyncScheduler.java` | 数据同步调度器（定时+手动触发） |
| `DataSyncService.java` | biz_offer 同步服务 |
| `BrandSyncService.java` | dict_brand 同步服务 |
| `FactorySyncService.java` | dict_factory 同步服务 |
| `MerchantSyncService.java` | dict_merchant 同步服务 |
| `UserMerchantSyncService.java` | rel_user_merchant 同步服务 |
| `SearchServiceImpl.java` | 搜索服务核心实现 |
| `MerchantServiceImpl.java` | 商家详情服务 |
| `SocialApplication.java` | 主数据源配置 |
| `AppController.java` | App版本检查 + APK下载接口 |
| `UserController.java` | 用户资料（含mooketNo） |
| `AuthController.java` | 登录注册（含UAC mooket_no回填） |

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/search/suggest?category=牛&keyword=xxx` | GET | 搜索联想词 |
| `/api/v1/merchant/{id}?category=牛` | GET | 商家详情（按分类过滤） |
| `/api/v1/product/{id}?category=牛&offerType=offer` | GET | 产品详情（带分页） |
| `/api/v1/factory/filter?category=牛` | GET | 厂号筛选数据 |
| `/api/v1/app/version` | GET | App版本检查（返回versionCode/hasUpdate/updateUrl） |
| `/api/v1/app/download/apk` | GET | APK文件下载（用于应用内更新） |
| `/api/v1/user/profile` | GET | 用户资料（含mooketNo） |
| `POST /api/v1/sync/*` | POST | 数据同步接口 |

**统一响应格式**：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 关键模式

- **两套卡片构建路径**：首页卡片用 `HomeStatServiceImpl`，自选/历史搜索卡片用 `SearchHistoryServiceImpl`。修 bug 时必须确认走的是哪个路径，数据不一致问题往往源于此。
- **热门商家定义**：`aggregateByMerchantForFactoryProduct` SQL 按"今日报盘数 desc"取前3名，无其他排序权重。

- **ID 类型**：使用 `Long`（非 Integer）- 值可能超过 19 位
- **Upsert**：PostgreSQL `ON CONFLICT DO UPDATE` 去重
- **数据保留**：biz_offer 数据仅保留 2 天
- **搜索历史**：通过 SharedPreferences 在 Android 端持久化
- **分类过滤**：后端使用 `category` 参数按类型过滤产品（牛/猪/羊/禽/水产）

## 数据库

- PostgreSQL: `localhost:5432/mooket_db` (主数据源)
- SQL 脚本: `E:\project6\sql\init\` (PostgreSQL), `sql/sync/` (MySQL→PostgreSQL)
- **MCP Server**: PostgreSQL 通过 `.mcp.json` 配置，可直接查询 `mooket_db`

## 同步依赖顺序

**dict_factory → dict_brand → biz_offer**（有强依赖，必须按序同步）

| 同步顺序 | 数据源 | 目标表 | 依赖关系 |
|----------|--------|--------|----------|
| 1 | dict_product | 无依赖 |
| 2 | dict_factory | 无依赖 |
| 3 | dict_brand | 依赖 dict_factory（通过 factory_no 查 factory_id） |
| 4 | dict_merchant | 无依赖 |
| 5 | rel_user_merchant | 无依赖 |
| 6 | biz_offer | 依赖 dict_factory 和 dict_brand |

## 价格异常值过滤

产品详情页价格区间使用 **IQR 20% 过滤**：
- 高于 Q3 + 1.5×IQR 或低于 Q1 - 1.5×IQR 的价格视为异常值（如 9053 元等错误价格）
- 过滤后重新计算 Min/Max 价格区间
- **统计指标（报盘数/商家数/工厂数）不受影响**

## 注意事项

1. 后端运行端口：**8080**（Android API 指向 `http://43.139.56.124:8080/`）
2. `mvn spring-boot:run` 前必须先停止旧进程，否则端口冲突
3. dict_factory 必须在 dict_brand 之前同步；dict_brand 必须在 biz_offer 之前
4. 默认搜索分类是"牛"；猪肉用"猪"
5. Android 开启明文流量支持 HTTP API 通信
6. **价格异常值过滤**：产品详情页价格区间会过滤偏离中位数20%以上的异常价格（如9053元的错误价格），但报盘数/商家数/工厂数统计不受影响

### 后端额外注意事项

1. **数据源 Bean 名称**：
   - PostgreSQL: `primaryDataSource`（带 @Primary）
   - MySQL social: `mysqlDataSource`
   - MySQL erp: `erpDataSource`

2. **biz_offer 关联关系**：
   - product_id ← dict_product（通过 product_name）
   - factory_id ← dict_factory（通过 factory_no）
   - brand_id ← dict_brand（通过 factory_id）
   - merchant_id ← dict_merchant（通过 contact_phone）

3. **Upsert 模式**：使用 PostgreSQL `ON CONFLICT DO UPDATE` 去重，关键字段组合：`user_nickname + product_name + country + factory_no + offer_type + feeding_type + fat_ratio`（使用 COALESCE 处理 NULL 值）

4. **MyBatis SQL 中 HTML 转义的正确写法**：
   - MyBatis `<script>` 标签内的 `&gt;/&lt;` 会被正确解析为 `>=<`
   - **非 `<script>` 标签的 `@Select` SQL 中使用 `&gt;/&lt;` 会触发 Jackson HTML 转义**，导致 SQL 传入 PostgreSQL 时变成 `column "gt" does not exist`
   - **规则**：SQL 中包含 `>=` 或 `<=`  comparisons 时，无论是否在 `<script>` 标签内，都应确认转义字符被正确解析
   - **最佳实践**：所有包含比较运算符的 SQL 都使用 `<script>` 标签包裹

---

## Figma MCP 集成规则

本规则定义如何将 Figma 设计转换为 Jetpack Compose 代码。

### 设计资源位置

| 资源类型 | 位置 |
|---------|------|
| 颜色主题 | `app/src/main/java/com/mooket/app/ui/theme/Theme.kt` |
| 颜色值 | `app/src/main/java/com/mooket/app/ui/theme/Color.kt` |
| 图标/图片 | `app/src/main/res/drawable/` |
| 屏幕组件 | `app/src/main/java/com/mooket/app/ui/screens/` |

### 颜色映射

Figma 设计中的颜色必须映射到 Theme.kt 中定义的颜色常量：

```kotlin
// Theme.kt 中的主色
val Primary = Color(0xFF006A61)      // 深绿色 - 主色
val PrimaryLight = Color(0xFFE8F5F3)  // 浅绿色背景
val Background = Color(0xFFF4FBF8)   // 浅绿色背景
val TextPrimary = Color(0xFF171D1C)  // 主文字黑色
val TextHint = Color(0xFF6C7A77)     // 提示文字
val Border = Color(0xFFDEE4E1)       // 边框灰色
```

**禁止硬编码颜色** - 必须使用 Theme.kt 中定义的颜色常量。

### 组件结构规则

- **Screen 组件**：`ui/screens/{feature}/ScreenName.kt`
- **ViewModel**：`ui/screens/{feature}/ViewModelName.kt`
- **数据模型**：`data/model/Models.kt`
- **主题文件**：`ui/theme/Theme.kt`、`ui/theme/Color.kt`

### Figma 实现流程

1. 使用 `get_design_context` 获取节点结构
2. 使用 `get_screenshot` 获取视觉参考
3. 将 React+Tailwind 输出转换为 Jetpack Compose
4. 映射 Figma 颜色到 Theme.kt 颜色常量
5. 使用 `Modifier` 进行布局和样式设置
6. 验证视觉一致性

### 布局规则

- 使用 `Column`、`Row`、`Box` 作为基础布局容器
- 使用 `fillMaxWidth()`、`fillMaxHeight()` 填充父容器
- 使用 `padding()` 和 `offset()` 调整间距
- 使用 `RoundedCornerShape` 处理圆角

### 样式规则

- 文字使用 `Text()` 配合 `fontSize`、`color`、`fontWeight`
- 背景使用 `background(Color.XXX)`
- 边框使用 `border(width, color, shape)`
- 阴影使用 `shadow(elevation, shape, spotColor)`
- 图标使用 `Icon(imageVector = Icons.Default.XXX, ...)`

### 图标使用

优先使用 Material Icons：
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
```

自定义图标在 `res/drawable/` 目录，使用 `painterResource(id = R.drawable.ic_xxx)` 引用。

### 状态管理

- 使用 `remember { mutableStateOf(...) }` 管理组件状态
- 使用 `collectAsStateWithLifecycle()` 收集 ViewModel 状态流
- 使用 `LaunchedEffect` 处理副作用
- **注意**：`derivedStateOf` 不要用 `remember` 包裹，否则只计算一次

### 常见问题

- **LazyColumn key 碰撞**：使用复合 key 避免重复 hashCode 崩溃
- **缓存名错误**：新增缓存需在 `CacheConfig.java` 中声明
- **端口不匹配**：确认 Android `RetrofitClient.kt` 和后端端口一致（当前 8080）

### Android 状态管理规范

- `remember { mutableStateOf }` 用于 Composable 局部状态
- **跨导航保持的状态必须提升到 ViewModel**（用 `StateFlow`）。`popBackStack()` 回来时 Composable 重新创建，`remember` 状态丢失，ViewModel 生命周期跨越导航所以安全
- 引用 ViewModel 状态时直接用 `uiState.xxx`，不要派生 `val` 再引用（会导致 Compose 重组失效）

### DTO 对齐规范

Backend DTO 的导航字段（如 `HotSearchItem.country/factoryNo/productId`）Android DTO 也必须有对应字段。字段缺失会导致功能不可用（如热门搜索卡片无法点击跳转）。

### MCP Server

PostgreSQL MCP 配置在 `.mcp.json`，可直连查询 `mooket_db`。

### SSH Skill 路径注意（Windows）

- Python 脚本路径用正斜杠：`C:/Users/...` 或 `~/.claude/skills/ssh-skill/scripts/`
- 上传/下载命令**必须**加 `MSYS_NO_PATHCONV=1` 前缀防止 MSYS 路径转换
- 示例：
  ```bash
  MSYS_NO_PATHCONV=1 python "C:/Users/zhangzheng/.claude/skills/ssh-skill/scripts/ssh_upload.py" ai-pg-43.139.56.124 "E:/project6/social/target/social-1.0.0-SNAPSHOT.jar" "/tmp/social.jar"
  ```
