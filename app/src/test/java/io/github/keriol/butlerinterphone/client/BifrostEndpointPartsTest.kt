package io.github.keriol.butlerinterphone.client

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BifrostEndpointPartsTest {

    @Test
    fun defaultsProtocolToHttp() {
        assertEquals(
            "http://example.test:5055",
            BifrostEndpointParts(
                host = "example.test",
                port = "5055",
            ).toBaseUrl(),
        )
    }

    @Test
    fun parsesBuildConfigUrlIntoSeparateFields() {
        assertEquals(
            BifrostEndpointParts(
                protocol = "https",
                host = "example.test",
                port = "8443",
            ),
            BifrostEndpointParts.fromUrl(
                "https://example.test:8443"
            ),
        )
    }

    @Test
    fun rejectsOutOfRangePort() {
        assertThrows(IllegalArgumentException::class.java) {
            BifrostEndpointParts(
                host = "example.test",
                port = "65536",
            ).toBaseUrl()
        }
    }
}
