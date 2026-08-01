package com.cafe.web.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrLinkTest {

    @Test
    void omitsStandardHttpPort() {
        assertEquals("http://shop.vn/cafe-shop",
                QrLink.absoluteBase("http", "shop.vn", 80, "/cafe-shop"));
    }

    @Test
    void omitsStandardHttpsPort() {
        assertEquals("https://shop.vn",
                QrLink.absoluteBase("https", "shop.vn", 443, ""));
    }

    @Test
    void keepsNonStandardPort() {
        assertEquals("http://localhost:8080/cafe-shop",
                QrLink.absoluteBase("http", "localhost", 8080, "/cafe-shop"));
    }

    @Test
    void prefersForwardedProtoAndHost() {
        assertEquals("https://cafechain.vn/cafe-shop",
                QrLink.absoluteBase("http", "10.0.0.4", 8080, "/cafe-shop",
                        "https", "cafechain.vn", null));
    }

    @Test
    void keepsForwardedHostExplicitPort() {
        assertEquals("https://cafechain.vn:8443/cafe-shop",
                QrLink.absoluteBase("http", "10.0.0.4", 8080, "/cafe-shop",
                        "https", "cafechain.vn:8443", null));
    }

    @Test
    void forwardedPortHeaderWins() {
        assertEquals("https://cafechain.vn:9443",
                QrLink.absoluteBase("http", "10.0.0.4", 8080, "",
                        "https", "cafechain.vn", "9443"));
    }

    @Test
    void usesFirstHopOfChainedHeaders() {
        assertEquals("https://cafechain.vn/cafe-shop",
                QrLink.absoluteBase("http", "10.0.0.4", 8080, "/cafe-shop",
                        "https, http", "cafechain.vn, internal.lan", null));
    }

    @Test
    void fallsBackToRequestWhenNoForwardedHeaders() {
        assertEquals("http://localhost:8080/cafe-shop",
                QrLink.absoluteBase("http", "localhost", 8080, "/cafe-shop", null, null, null));
    }

    @Test
    void ignoresBlankAndMalformedForwardedValues() {
        assertEquals("http://localhost:8080/cafe-shop",
                QrLink.absoluteBase("http", "localhost", 8080, "/cafe-shop", "  ", "", "abc"));
    }

    @Test
    void buildsCustomerMenuUrl() {
        assertEquals("http://shop.vn/cafe-shop/qr/menu?t=QR-CN01-T01",
                QrLink.menuUrl("http://shop.vn/cafe-shop", "QR-CN01-T01"));
    }

    @Test
    void encodesQrCodeQueryValue() {
        assertEquals("https://shop.vn/qr/menu?t=QR+T%2F01%3F",
                QrLink.menuUrl("https://shop.vn", "QR T/01?"));
    }

    @Test
    void normalizesConfiguredPublicBase() {
        assertEquals("http://10.33.74.42:8080/cafe-shop",
                QrLink.normalizeConfiguredBase(" http://10.33.74.42:8080/cafe-shop/ "));
    }

    @Test
    void rejectsMalformedConfiguredPublicBase() {
        assertEquals(null, QrLink.normalizeConfiguredBase("localhost:8080/cafe-shop"));
    }
}
