package com.mooket.app.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mooket.app.R
import com.mooket.app.ui.theme.*

/**
 * 手机号输入页面
 */
@Composable
fun PhoneInputScreen(
    onSendCode: (String) -> Unit,
    onOneClickLogin: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var agreementChecked by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 29.dp)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_mooket_max_logo),
                contentDescription = "MooketMax Logo",
                modifier = Modifier
                    .padding(top = 108.dp)
                    .height(29.29.dp)
                    .width(180.dp)
            )

            // 标题
            Column(
                modifier = Modifier.padding(top = 66.dp)
            ) {
                Text(
                    text = "欢迎来到MooketMax",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = "未注册绑定的手机号将自动注册",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // 手机号输入 - 往上移，减少空白
            Column(
                modifier = Modifier.padding(top = 30.dp)
            ) {
                Text(
                    text = "手机号",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box {
                    BasicTextField(
                        value = phone,
                        onValueChange = {
                            if (it.length <= 11 && it.all { c -> c.isDigit() }) {
                                phone = it
                                onClearError()
                            }
                        },
                        textStyle = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(Primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (phone.isEmpty()) {
                                    Text(
                                        text = "输入手机号用于登录/注册",
                                        fontSize = 15.sp,
                                        color = TextHint
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    // 删除按钮 - 有数字时显示
                    if (phone.isNotEmpty()) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_delete_input),
                            contentDescription = "清除",
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(18.dp)
                                .clickable {
                                    phone = ""
                                    onClearError()
                                }
                        )
                    }
                    // 下划线
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        color = Border
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 协议勾选
            Row(
                modifier = Modifier.clickable { agreementChecked = !agreementChecked },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (agreementChecked) Primary else Surface)
                        .border(1.dp, if (agreementChecked) Primary else Border, RoundedCornerShape(2.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (agreementChecked) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "勾选",
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("已阅读并同意")
                        pushStyle(SpanStyle(color = Color(0xFF12877c)))
                        append("服务条款")
                        pushStyle(SpanStyle(color = TextPrimary))
                        append("和")
                        pushStyle(SpanStyle(color = Color(0xFF12877c)))
                        append("隐私政策")
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF333333)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 登录按钮
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSendCode(phone)
                },
                enabled = phone.length == 11 && agreementChecked && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Color(0x66006A61)
                ),
                shape = RoundedCornerShape(4.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "登录",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            // 错误提示
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = Error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 第三方登录
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "第三方登录",
                    fontSize = 14.sp,
                    color = Color(0xFF3C4947)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOneClickLogin() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ctcc),
                        contentDescription = "天翼一键登录",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}
