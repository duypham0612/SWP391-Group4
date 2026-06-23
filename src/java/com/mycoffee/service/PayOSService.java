package com.mycoffee.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class PayOSService {
    // Thông tin API Key từ trang quản trị PayOS của bạn
    private static final String CLIENT_ID = "965f76da-4caf-4a90-ae0c-518a1d58f78d";
    private static final String API_KEY = "92f220db-fae0-43ea-ab25-f18711431484";
    // HÃY ĐẢM BẢO COPY TOÀN BỘ CHUỖI CHECKSUM TỪ PAYOS DÁN VÀO ĐÂY
    private static final String CHECKSUM_KEY = "38a678b6cb4d7ce9bda44a31e46aea99db667b6e648ead5dd0785807f1a66154";

    private static String hmacSha256(String data, String key) throws Exception {
        byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(byteKey, "HmacSHA256");
        sha256_HMAC.init(secretKey);
        byte[] macData = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : macData) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public String createPaymentLink(int orderCode, int amount, String description, String returnUrl, String cancelUrl) {
        try {
            String signatureData = "amount=" + amount
                    + "&cancelUrl=" + cancelUrl
                    + "&description=" + description
                    + "&orderCode=" + orderCode
                    + "&returnUrl=" + returnUrl;

            String signature = hmacSha256(signatureData, CHECKSUM_KEY);

            String jsonPayload = "{"
                    + "\"orderCode\":" + orderCode + ","
                    + "\"amount\":" + amount + ","
                    + "\"description\":\"" + description + "\","
                    + "\"cancelUrl\":\"" + cancelUrl + "\","
                    + "\"returnUrl\":\"" + returnUrl + "\","
                    + "\"signature\":\"" + signature + "\""
                    + "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api-merchant.payos.vn/v2/payment-requests"))
                    .header("Content-Type", "application/json")
                    .header("x-client-id", CLIENT_ID)
                    .header("x-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            System.out.println(">>> PHẢN HỒI TỪ PAYOS: " + responseBody);

            if (responseBody.contains("\"checkoutUrl\":\"")) {
                int start = responseBody.indexOf("\"checkoutUrl\":\"") + 15;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end).replace("\\/", "/");
            }
        } catch (Exception e) {
            System.out.println("Lỗi kết nối PayOS: " + e.getMessage());
        }
        return null;
    }
}