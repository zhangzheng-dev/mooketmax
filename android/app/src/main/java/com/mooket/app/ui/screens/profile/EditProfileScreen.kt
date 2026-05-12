package com.mooket.app.ui.screens.profile

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yalantis.ucrop.UCrop
import com.mooket.app.ui.theme.*
import java.io.File

/**
 * 编辑资料页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { com.mooket.app.data.repository.ProfileRepository(com.mooket.app.data.api.RetrofitClient.apiService) }
    val viewModel: EditProfileViewModel = remember { EditProfileViewModel(repository) }
    val uiState by viewModel.uiState.collectAsState()

    // 行业身份选择弹窗
    var showIdentitySheet by remember { mutableStateOf(false) }
    // 头像选择弹窗
    var showAvatarSheet by remember { mutableStateOf(false) }
    // 等待相机权限通过后启动相机
    var pendingCameraLaunch by remember { mutableStateOf(false) }

    // 临时编辑状态（未保存）
    var tempNickname by remember { mutableStateOf("") }
    var tempAvatarUrl by remember { mutableStateOf<String?>(null) }
    var tempIdentityTags by remember { mutableStateOf<List<String>>(emptyList()) }
    val focusManager = LocalFocusManager.current

    // 初始化临时状态
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // 同步后端数据到临时状态（只要有一个字段为空就同步）
    LaunchedEffect(uiState.nickname, uiState.identityTags) {
        if (tempNickname.isEmpty() && uiState.nickname.isNotEmpty()) {
            tempNickname = uiState.nickname
        }
        if (tempIdentityTags.isEmpty() && uiState.identityTags.isNotEmpty()) {
            tempIdentityTags = uiState.identityTags
        }
    }

    // 头像URL变化时同步到临时状态（用于拍照/上传后刷新预览）
    LaunchedEffect(uiState.avatarUrl, uiState.pendingAvatarUrl) {
        // 优先显示本地预览（pendingAvatarUrl），否则显示已保存的 avatarUrl
        val previewUrl = uiState.pendingAvatarUrl ?: uiState.avatarUrl
        if (!previewUrl.isNullOrEmpty()) {
            tempAvatarUrl = previewUrl
        }
    }

    // UCrop 裁剪结果
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.let { data ->
                val resultUri = UCrop.getOutput(data)
                if (resultUri != null) {
                    viewModel.setCroppedFileUri(resultUri)
                    viewModel.uploadCroppedAvatar(resultUri)
                }
            }
        }
    }

    // 裁剪启动函数
    fun startCrop(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(
            File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )
        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropFrame(false)
            setCompressionQuality(90)
        }
        val intent = UCrop.of(sourceUri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .getIntent(context)
        cropLauncher.launch(intent)
    }

    // 拍照选择器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // 拍照成功，先裁剪再上传
            val uri = viewModel.getTempImageFileUri(context)
            if (uri != null) {
                startCrop(uri)
            }
        }
    }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { startCrop(it) }
    }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCameraLaunch) {
            pendingCameraLaunch = false
            val tempFile = viewModel.getTempImageFile(context)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            viewModel.setTempFileUri(context, tempFile)
            cameraLauncher.launch(uri)
        }
    }

    // Loading overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        // Header
        EditProfileHeader(
            onBackClick = onBackClick,
            onSaveClick = {
                viewModel.saveProfile(context, tempNickname, tempIdentityTags)
            }
        )

        // 基本信息 Section Title
        Text(
            text = "基本信息",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // 头像 Section - 靠左
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryLight)
                    .clickable {
                        showAvatarSheet = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!tempAvatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = tempAvatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "头像",
                        tint = Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 基本信息 Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Border)
        ) {
            Column {
                // 昵称行
                NicknameRow(
                    nickname = tempNickname,
                    onNicknameChange = { tempNickname = it }
                )

                InfoDivider()

                // 姓名行（实名认证状态）
                RealNameRow(
                    realNameStatus = uiState.realNameStatus,
                    realName = uiState.realName,
                    onRealNameVerifyClick = { /* TODO: 跳转实名认证 */ }
                )

                InfoDivider()

                // 关联手机（只读）
                ReadOnlyInfoRow(
                    label = "关联手机",
                    value = uiState.phone?.let { "${it.substring(0, 3)}***${it.substring(it.length - 4)}" } ?: ""
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 标签 Section Title
        Text(
            text = "标签",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 行业身份 Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = BorderStroke(1.dp, Border)
        ) {
            // 行业身份 - 点击弹出选择器
            IdentityTagRow(
                label = "行业身份",
                selectedTags = tempIdentityTags,
                onClick = { showIdentitySheet = true }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 保存成功
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaveSuccess()
        }
    }

    // 头像选择 BottomSheet
    if (showAvatarSheet) {
        AvatarSelectionSheet(
            onDismiss = { showAvatarSheet = false },
            onCameraClick = {
                showAvatarSheet = false
                pendingCameraLaunch = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                showAvatarSheet = false
                galleryLauncher.launch("image/*")
            }
        )
    }

    // 行业身份选择 BottomSheet
    if (showIdentitySheet) {
        IdentitySelectionSheet(
            selectedTags = tempIdentityTags,
            availableTags = listOf("海外服务商", "贸易商", "加工厂/商超", "其他"),
            onDismiss = { showIdentitySheet = false },
            onConfirm = { selected ->
                tempIdentityTags = selected
                showIdentitySheet = false
            }
        )
    }
}

/**
 * 头像选择 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarSelectionSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "选择头像",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // 拍照
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCameraClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "拍照",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "拍照",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }

            Divider(color = Border, thickness = 0.5.dp)

            // 从相册选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGalleryClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "相册",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "从相册选择",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 取消按钮
            Button(
                onClick = { onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Background),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border)
            ) {
                Text(
                    text = "取消",
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * 行业身份选择 BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdentitySelectionSheet(
    selectedTags: List<String>,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(selectedTags.toList()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        windowInsets = WindowInsets(0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择行业身份",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TextHint,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标签选项
            availableTags.forEach { tag ->
                val isSelected = tempSelected.contains(tag)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tempSelected = if (isSelected) {
                                tempSelected - tag
                            } else {
                                tempSelected + tag
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (tag != availableTags.last()) {
                    Divider(color = Border, thickness = 0.5.dp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 确认按钮
            Button(
                onClick = { onConfirm(tempSelected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "确认",
                    fontSize = 16.sp,
                    color = Surface
                )
            }
        }
    }
}

/**
 * 编辑资料页 Header
 */
@Composable
private fun EditProfileHeader(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBackIos,
            contentDescription = "返回",
            tint = TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBackClick() }
        )
        Text(
            text = "编辑资料",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Text(
            text = "保存",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Primary,
            modifier = Modifier.clickable { onSaveClick() }
        )
    }
}

/**
 * 昵称可编辑行
 */
@Composable
private fun NicknameRow(
    nickname: String,
    onNicknameChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "昵称",
            fontSize = 14.sp,
            color = Color(0xFF3C4947)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            BasicTextField(
                value = nickname,
                onValueChange = { onNicknameChange(it) },
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 14.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.End
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (nickname.isEmpty()) {
                            Text(
                                text = "请输入昵称",
                                fontSize = 14.sp,
                                color = TextHint
                            )
                        }
                        innerTextField()
                    }
                },
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 姓名行（实名认证状态）
 */
@Composable
private fun RealNameRow(
    realNameStatus: String?,
    realName: String?,
    onRealNameVerifyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = realNameStatus != "approved") { onRealNameVerifyClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "姓名",
            fontSize = 14.sp,
            color = Color(0xFF3C4947)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (realNameStatus) {
                "approved" -> {
                    Text(
                        text = realName ?: "",
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF2F8F7), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "已认证",
                            fontSize = 11.sp,
                            color = Primary
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .background(PrimaryLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "去实名认证",
                            fontSize = 12.sp,
                            color = Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 只读信息行
 */
@Composable
private fun ReadOnlyInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF3C4947)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary
        )
    }
}

/**
 * 行业身份行
 */
@Composable
private fun IdentityTagRow(
    label: String,
    selectedTags: List<String>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF3C4947)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTags.joinToString("、").ifEmpty { "请选择" },
                fontSize = 14.sp,
                color = if (selectedTags.isEmpty()) TextHint else TextPrimary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 分隔线
 */
@Composable
private fun InfoDivider() {
    Divider(
        color = Border,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
