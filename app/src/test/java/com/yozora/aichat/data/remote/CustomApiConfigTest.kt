package com.yozora.aichat.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetSocketAddress
import java.net.Proxy

class CustomApiConfigTest {
    @Test
    fun buildsRoutesFromHostVersionPrefixOrFullCompletionUrl() {
        listOf("vendor.example", "https://vendor.example", "https://vendor.example/v1/", "https://vendor.example/v1/chat/completions")
            .forEach { base ->
                val config = CustomApiConfig(base)
                assertEquals("https://vendor.example/v1/chat/completions", config.endpoint("chat/completions"))
                assertEquals("https://vendor.example/v1/models", config.endpoint("models"))
            }
    }

    @Test
    fun preservesGoogleAndGatewayPrefixes() {
        assertEquals("https://generativelanguage.googleapis.com/v1beta/openai/models",
            CustomApiConfig(CustomApiConfig.GOOGLE_BASE_URL).endpoint("models"))
        assertEquals("https://vendor.example/gateway/v2/chat/completions",
            CustomApiConfig("https://vendor.example/gateway/v2/").endpoint("chat/completions"))
    }

    @Test
    fun rejectsMalformedUrlsAndCredentialsInBaseUrl() {
        // A bare host is accepted on purpose (https:// is assumed); anything that
        // smuggles credentials, a query or a fragment into the base URL is not.
        listOf("", "ftp://vendor.example", "https://user:secret@vendor.example",
            "https://vendor.example?key=secret", "https://vendor.example/#fragment").forEach {
            assertNotNull(CustomApiConfig(it).validationError())
        }
    }

    @Test
    fun parsesHttpAndSocksProxiesAndRejectsInvalidProxy() {
        val config = CustomApiConfig("https://vendor.example", "socks5://127.0.0.1:1080")
        assertNull(config.validationError())
        assertEquals(Proxy.Type.SOCKS, config.networkProxy()!!.type())
        assertEquals(1080, (config.networkProxy()!!.address() as InetSocketAddress).port)
        assertEquals(Proxy.Type.HTTP, config.copy(proxyUrl = "http://proxy.example:8080").networkProxy()!!.type())
        assertNotNull(config.copy(proxyUrl = "https://proxy.example:8080").validationError())
        assertNotNull(config.copy(proxyUrl = "socks5://127.0.0.1:99999").validationError())
    }

    @Test
    fun restoresOldDefaultsAndRoundTripsConfiguration() {
        assertEquals(CustomApiConfig(), CustomApiConfig.fromJson(null))
        val config = CustomApiConfig(CustomApiConfig.GOOGLE_BASE_URL, "socks5://localhost:1080")
        assertEquals(config, CustomApiConfig.fromJson(config.toJson()))
    }
}
