package com.scluzlep.newmangps

import com.google.gson.Gson
import com.scluzlep.newmangps.data.api.NewsmySigner
import com.scluzlep.newmangps.data.model.ApiResponse
import com.scluzlep.newmangps.data.model.DevicePosition
import com.scluzlep.newmangps.data.model.formatTimestamp
import com.scluzlep.newmangps.data.model.parseDeviceTime
import com.scluzlep.newmangps.data.repository.NewsmyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class BugFixesTest {

    @Test
    fun apiResponseToRowsHandlesJsonNullCellsAndShortOrNullRows() {
        val json = """
            {
              "m_isResultOk": 1,
              "m_arrField": ["strTEID", "strName", "nSpeed"],
              "m_arrRecord": [
                ["861000000000001", null, "45"],
                ["861000000000002"],
                null
              ]
            }
        """.trimIndent()
        val parsed = Gson().fromJson(json, ApiResponse::class.java)
        val rows = parsed.toRows()

        assertEquals(3, rows.size)
        assertEquals("861000000000001", rows[0]["strTEID"])
        assertEquals("", rows[0]["strName"])
        assertEquals("45", rows[0]["nSpeed"])
        assertEquals(mapOf("strTEID" to "861000000000001", "nSpeed" to "45"), rows[0].filterValues(String::isNotBlank))

        assertEquals("861000000000002", rows[1]["strTEID"])
        assertEquals("", rows[1]["strName"])
        assertEquals("", rows[1]["nSpeed"])

        assertEquals("", rows[2]["strTEID"])
        assertEquals("", rows[2]["strName"])
        assertEquals("", rows[2]["nSpeed"])

        val direct = ApiResponse(
            isResultOk = 1,
            fields = listOf("a", "b"),
            records = listOf(listOf("1", null), null)
        ).toRows()
        assertEquals(2, direct.size)
        assertEquals("1", direct[0]["a"])
        assertEquals("", direct[0]["b"])
        assertEquals("", direct[1]["a"])
    }

    @Test
    fun parseDeviceTimeAndFormatTimestampAreConsistentAcrossThaiAndUsLocales() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val usSpace = parseDeviceTime("2026-05-01 12:30:00")
            val usIso = parseDeviceTime("2026-05-01T12:30:00")
            val usFormatted = formatTimestamp(usSpace)

            @Suppress("DEPRECATION")
            Locale.setDefault(Locale("th", "TH"))
            val thSpace = parseDeviceTime("2026-05-01 12:30:00")
            val thIso = parseDeviceTime("2026-05-01T12:30:00")
            val thFromRow = DevicePosition.fromRow(mapOf("nTime" to "2026-05-01 12:30:00")).timeSeconds
            val thFormatted = formatTimestamp(thSpace)

            assertTrue(usSpace > 1_700_000_000L)
            assertEquals(usSpace, usIso)
            assertEquals(usSpace, thSpace)
            assertEquals(usIso, thIso)
            assertEquals(usSpace, thFromRow)
            assertEquals("2026-05-01 12:30:00", usFormatted)
            assertEquals(usFormatted, thFormatted)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun paymentFormattingAvoidsFloatPrecisionArtifactsAndLocaleCommas() {
        val original = Locale.getDefault()
        try {
            val discounted = NewsmyRepository.calculatePaymentAmount(19.9, 10.0)
            assertEquals(9.9, discounted, 0.0)

            val clamped = NewsmyRepository.calculatePaymentAmount(5.0, 10.0)
            assertEquals(1.0, clamped, 0.0)

            Locale.setDefault(Locale.FRANCE)
            assertEquals("9.90", NewsmyRepository.formatPaymentAmount(discounted))
            assertEquals("1.00", NewsmyRepository.formatPaymentAmount(clamped))

            Locale.setDefault(Locale.US)
            assertEquals("9.90", NewsmyRepository.formatPaymentAmount(discounted))
            assertEquals("100.00", NewsmyRepository.formatPaymentAmount(100.0))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun restoreServerNormalizesLegacyHttpClusterUrlAndRejectsUntrusted() {
        val repository = NewsmyRepository()
        assertEquals(NewsmySigner.DEFAULT_SERVER, repository.currentServer)
        assertEquals("https://www.appxmg.com", NewsmySigner.CLUSTER_SERVER)

        repository.restoreServer("http://www.appxmg.com")
        assertEquals("https://www.appxmg.com", repository.currentServer)

        repository.restoreServer("https://server.newsmy-nmgps.com")
        assertEquals("https://server.newsmy-nmgps.com", repository.currentServer)

        repository.restoreServer("http://www.appxmg.com/")
        assertEquals("https://www.appxmg.com", repository.currentServer)

        repository.restoreServer("https://untrusted.example.com")
        assertEquals("https://www.appxmg.com", repository.currentServer)
    }
}
