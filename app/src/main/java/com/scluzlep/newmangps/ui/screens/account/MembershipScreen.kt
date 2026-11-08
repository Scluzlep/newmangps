package com.scluzlep.newmangps.ui.screens.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scluzlep.newmangps.data.model.DeviceDetail
import com.scluzlep.newmangps.data.model.DeviceModelCapability
import com.scluzlep.newmangps.data.model.PayOrder
import com.scluzlep.newmangps.data.model.PayPlan
import com.scluzlep.newmangps.data.model.PayPrice
import com.scluzlep.newmangps.data.model.PaymentMethod
import com.scluzlep.newmangps.data.model.PendingPayment
import com.scluzlep.newmangps.data.LoginSessionStore
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import com.scluzlep.newmangps.ui.LocalAppLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipScreen(
    teid: String, detail: DeviceDetail?, model: DeviceModelCapability?,
    repository: NewsmyRepository, onBack: () -> Unit
) {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val sessionStore = remember { LoginSessionStore(context) }
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<PayPlan>>(emptyList()) }
    var prices by remember { mutableStateOf<List<PayPrice>>(emptyList()) }
    var orders by remember { mutableStateOf<List<PayOrder>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedPrice by remember { mutableStateOf<PayPrice?>(null) }
    var methods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var pending by remember(teid) {
        mutableStateOf(sessionStore.loadPendingPayments().firstOrNull {
            it.teid == teid && it.user == repository.currentUser.value && it.server == repository.currentServer
        })
    }
    var paymentBusy by remember { mutableStateOf(false) }
    var paymentMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            val planResult = repository.getPayPlans()
            val priceResult = repository.getPayPrices()
            val orderResult = repository.getPayHistory(teid)
            plans = planResult.getOrNull().orEmpty()
            prices = priceResult.getOrNull().orEmpty()
            orders = orderResult.getOrNull().orEmpty()
            error = planResult.exceptionOrNull()?.message ?: priceResult.exceptionOrNull()?.message
                ?: orderResult.exceptionOrNull()?.message
            loading = false
        }
    }
    LaunchedEffect(teid) { refresh() }
    LaunchedEffect(pending) {
        val order = pending ?: return@LaunchedEffect
        repeat(120) {
            delay(5_000)
            val paid = repository.checkPayment(order).getOrNull() == true
            if (paid) {
                val result = repository.finishPayment(order)
                if (result.getOrDefault(false)) {
                    pending = null
                    sessionStore.clearPendingPayment(order.orderId)
                    paymentMessage = language.text("PaymentServiceUpdated", "支付成功，服务已更新")
                    refresh()
                } else paymentMessage = result.exceptionOrNull()?.message ?: language.text("ServiceActivationFailed", "服务开通失败")
                return@LaunchedEffect
            }
        }
        paymentMessage = language.text("PaymentNotConfirmed", "尚未查到付款结果，请稍后手动查询")
    }
    val modelPlanId = model?.payPlan

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(language.text("MembershipAndRenewal", "会员与续费")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, language.text("Back", "返回")) } },
            actions = { IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, language.text("Refresh", "刷新")) } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(detail?.carNum?.ifBlank { teid } ?: teid, style = MaterialTheme.typography.titleLarge)
                Text("${language.text("ExpiryDate", "服务到期")}：${detail?.formattedLimitDate ?: "--"}")
                Text(if (detail?.isExpired == true) language.text("HaveExpired", "已到期") else
                    "${language.text("ServiceStatus", "服务状态")}：${if (detail == null) language.text("Unknown", "未知") else language.text("Valid", "有效")}")
                Spacer(Modifier.height(16.dp))
                Text(language.text("PlanPrices", "套餐价格"), style = MaterialTheme.typography.titleMedium)
            }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            paymentMessage?.let { item { Text(it, modifier = Modifier.padding(vertical = 8.dp)) } }
            pending?.let { order -> item {
                ListItem(headlineContent = { Text(language.text("PendingOrder", "待确认订单")) },
                    supportingContent = { Text("${order.orderId} · ${order.amount} ${order.currency}") },
                    trailingContent = { TextButton(onClick = {
                        scope.launch {
                            paymentBusy = true
                            val result = repository.finishPayment(order)
                            if (result.getOrDefault(false)) {
                                pending = null
                                sessionStore.clearPendingPayment(order.orderId)
                                paymentMessage = language.text("PaymentServiceUpdated", "支付成功，服务已更新")
                                refresh()
                            } else paymentMessage = result.exceptionOrNull()?.message ?: language.text("PaymentNotConfirmed", "尚未确认付款")
                            paymentBusy = false
                        }
                    }, enabled = !paymentBusy) { Text(language.text("Search", "查询")) } })
            } }
            val buyType = if ((detail?.limitTimeSeconds ?: 0) in 1..100) 7 else 1
            val visiblePrices = prices.filter { price ->
                price.buyType == buyType && (modelPlanId == null || price.planId == modelPlanId)
            }
            items(visiblePrices) { price ->
                val plan = plans.firstOrNull { it.id == price.planId }
                ListItem(
                    headlineContent = { Text(plan?.name?.ifBlank { "${language.text("PayPlan", "套餐")} ${price.planId}" } ?: "${language.text("PayPlan", "套餐")} ${price.planId}") },
                    supportingContent = { Text("${language.text("Type", "类型")} ${price.buyType} · ${language.text("Term", "期限")} ${price.buyValue}") },
                    trailingContent = {
                        TextButton(onClick = {
                            selectedPrice = price
                            methods = emptyList()
                            scope.launch {
                                paymentBusy = true
                                val result = repository.getPaymentMethods(teid, price.buyType)
                                methods = result.getOrNull().orEmpty()
                                paymentMessage = result.exceptionOrNull()?.message ?: if (methods.isEmpty())
                                    language.text("NoPaymentChannelNeedHelp", "暂无可用支付渠道") else null
                                paymentBusy = false
                            }
                        }, enabled = !paymentBusy && pending == null) {
                            Text("${price.price} ${price.currency}")
                        }
                    }
                )
                HorizontalDivider()
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text(language.text("OrderHistory", "订单记录"), style = MaterialTheme.typography.titleMedium)
            }
            items(orders) { order ->
                val time = if (order.timeSeconds > 0) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    .format(Date(order.timeSeconds * 1000)) else "--"
                ListItem(
                    headlineContent = { Text(order.title.ifBlank { "${language.text("OrderID", "订单")} ${order.orderId}" }) },
                    supportingContent = { Text("$time · ${order.content.ifBlank { order.orderId }}") },
                    trailingContent = { Text("${order.amount} ${order.currency}") }
                )
                HorizontalDivider()
            }
            if (orders.isEmpty() && !loading) item { Text(language.text("NoOrderHistory", "暂无订单记录")) }
        }
    }

    selectedPrice?.let { price ->
        AlertDialog(onDismissRequest = { selectedPrice = null },
            title = { Text(language.text("SelectPaymentMethod", "选择支付渠道")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${plans.firstOrNull { it.id == price.planId }?.name ?: language.text("PayPlan", "套餐")} · ${price.buyValue} ${language.text("Day", "天")}")
                    if (paymentBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    methods.forEach { method ->
                        val due = (price.price - method.discount).coerceAtLeast(1.0)
                        TextButton(onClick = {
                            val title = plans.firstOrNull { it.id == price.planId }?.name ?: "设备服务"
                            scope.launch {
                                paymentBusy = true
                                val result = repository.startPayment(teid, price, title, method)
                                if (result.isSuccess) {
                                    val order = result.getOrThrow()
                                    sessionStore.savePendingPayment(order)
                                    pending = order
                                    selectedPrice = null
                                    paymentMessage = "订单已创建，等待服务器确认付款"
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(order.paymentUrl))) }
                                        .onFailure { paymentMessage = "无法打开支付页面：${it.message}" }
                                } else paymentMessage = result.exceptionOrNull()?.message ?: "下单失败"
                                paymentBusy = false
                            }
                        }, enabled = !paymentBusy) {
                            Text("${if (method.method == "wxpay") language.text("WeChatPay", "微信支付") else language.text("Alipay", "支付宝")} · $due ${price.currency}")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedPrice = null }) { Text(language.text("Cancel", "取消")) } })
    }
}
