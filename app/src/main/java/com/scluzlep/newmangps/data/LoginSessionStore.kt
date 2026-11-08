package com.scluzlep.newmangps.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.scluzlep.newmangps.data.model.PendingPayment
import org.json.JSONObject
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SavedLogin(val account: String, val password: String, val isImei: Boolean, val server: String)

class LoginSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("login_session", Context.MODE_PRIVATE)
    private val alias = "newsmy_login_session"

    fun save(login: SavedLogin) {
        val json = JSONObject()
            .put("account", login.account)
            .put("password", login.password)
            .put("imei", login.isImei)
            .put("server", login.server)
        saveJson("login", json)
    }

    fun savePendingPayment(payment: PendingPayment) {
        val orders = loadPendingPayments().filterNot { it.orderId == payment.orderId } + payment
        saveJson("payment", JSONObject().put("orders", JSONArray().apply {
            orders.forEach { put(paymentJson(it)) }
        }))
    }

    private fun paymentJson(payment: PendingPayment): JSONObject = JSONObject()
            .put("user", payment.user).put("server", payment.server)
            .put("teid", payment.teid).put("orderId", payment.orderId)
            .put("method", payment.method).put("merchantId", payment.merchantId)
            .put("buyType", payment.buyType).put("buyValue", payment.buyValue)
            .put("amount", payment.amount).put("currency", payment.currency)
            .put("title", payment.title).put("detail", payment.detail)
            .put("timeSeconds", payment.timeSeconds).put("paymentUrl", payment.paymentUrl)
            .put("discount", payment.discount)

    fun loadPendingPayments(): List<PendingPayment> = runCatching {
        val json = loadJson("payment") ?: return emptyList()
        val orders = json.optJSONArray("orders") ?: JSONArray().put(json)
        (0 until orders.length()).mapNotNull { index ->
            runCatching { paymentFromJson(orders.getJSONObject(index)) }.getOrNull()
        }
    }.getOrNull().orEmpty()

    private fun paymentFromJson(json: JSONObject): PendingPayment =
        PendingPayment(
            json.getString("user"), json.getString("server"), json.getString("teid"),
            json.getString("orderId"), json.getString("method"), json.getString("merchantId"),
            json.getInt("buyType"), json.getInt("buyValue"), json.getDouble("amount"),
            json.getString("currency"), json.getString("title"), json.getString("detail"),
            json.getLong("timeSeconds"), json.getString("paymentUrl"), json.getDouble("discount")
        )

    fun clearPendingPayment(orderId: String) {
        val remaining = loadPendingPayments().filterNot { it.orderId == orderId }
        if (remaining.isEmpty()) preferences.edit().remove("payment_iv").remove("payment_data").commit()
        else saveJson("payment", JSONObject().put("orders", JSONArray().apply {
            remaining.forEach { put(paymentJson(it)) }
        }))
    }

    private fun saveJson(prefix: String, json: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        check(preferences.edit()
            .putString("${prefix}_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("${prefix}_data", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .commit()) { "无法保存登录会话" }
    }

    fun load(): SavedLogin? = runCatching {
        val json = loadJson("login") ?: loadLegacyLogin() ?: return null
        SavedLogin(
            json.getString("account"), json.getString("password"),
            json.getBoolean("imei"), json.getString("server")
        )
    }.getOrNull()

    private fun loadJson(prefix: String): JSONObject? {
        val iv = preferences.getString("${prefix}_iv", null) ?: return null
        val data = preferences.getString("${prefix}_data", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return JSONObject(String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8))
    }

    private fun loadLegacyLogin(): JSONObject? {
        val iv = preferences.getString("iv", null) ?: return null
        val data = preferences.getString("data", null) ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return JSONObject(String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8))
    }

    fun clear() {
        preferences.edit().remove("login_iv").remove("login_data")
            .remove("iv").remove("data").commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}
