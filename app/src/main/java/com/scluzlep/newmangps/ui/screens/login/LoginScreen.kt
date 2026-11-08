package com.scluzlep.newmangps.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scluzlep.newmangps.ui.theme.*
import com.scluzlep.newmangps.ui.LocalAppLanguage

@Composable
fun LoginScreen(
    onLoginClick: (String, String, Boolean) -> Unit,
    serverName: String,
    servers: List<Pair<String, String>>,
    onServerSelected: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val language = LocalAppLanguage.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: IMEI, 1: Username
    var accountInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var serverMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // App Logo / Title (Xiaomi HyperOS Style)
        Text(
            text = language.text("AppName", "纽曼GPS"),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = language.text("AppTagline", "设备位置与告警"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // HyperSegmentedControl Tab
        HyperSegmentedControl(
            items = listOf(language.text("ImeiLogin", "IMEI 设备直连"),
                language.text("AccountLogin", "平台账号登录")),
            selectedIndex = selectedTab,
            onItemSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Login Input Card
        HyperCard {
            Text(
                text = if (selectedTab == 0) language.text("EnterImei", "输入定位器 IMEI 号")
                    else language.text("EnterAccount", "输入平台账号"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = accountInput,
                onValueChange = { accountInput = it },
                label = { Text(if (selectedTab == 0) language.text("Imei15Digits", "15位设备 IMEI 号")
                    else language.text("AccountUsername", "账号用户名")) },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.Numbers else Icons.Default.Person,
                        contentDescription = null,
                        tint = HyperBlue
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (selectedTab == 0) KeyboardType.Number else KeyboardType.Text
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text(language.text("Password", "访问密码")) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = HyperBlue
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = HyperRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            HyperButton(
                text = language.text("Login", "登录"),
                onClick = {
                    onLoginClick(accountInput, passwordInput, selectedTab == 0)
                },
                isLoading = isLoading
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box {
            TextButton(onClick = { serverMenu = true }) { Text("${language.text("Server", "服务器")}: $serverName") }
            DropdownMenu(expanded = serverMenu, onDismissRequest = { serverMenu = false }) {
                servers.forEach { (url, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = {
                        serverMenu = false
                        onServerSelected(url)
                    })
                }
            }
        }
    }
}

