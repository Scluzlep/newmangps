package com.scluzlep.newmangps.data.model

data class PaymentMethod(val method: String, val merchantId: String, val discount: Double)

data class PendingPayment(
    val user: String, val server: String, val teid: String,
    val orderId: String, val method: String, val merchantId: String,
    val buyType: Int, val buyValue: Int, val amount: Double, val currency: String,
    val title: String, val detail: String, val timeSeconds: Long,
    val paymentUrl: String, val discount: Double
)
