package com.fet.localAI.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;

@Service
public class WeatherForecastService {

    private static final String API_URL = "https://opendata.cwa.gov.tw/api/v1/rest/datastore/F-C0032-001";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final String apiKey;

    private static org.springframework.http.client.ClientHttpRequestFactory createUnsecureRequestFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .sslContext(sc)
                    .sslParameters(new javax.net.ssl.SSLParameters())
                    .build();

            return new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        } catch (Exception e) {
            throw new RuntimeException("無法建立不安全的 ClientHttpRequestFactory", e);
        }
    }

    public WeatherForecastService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${cwa.api.key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> createUnsecureRequestFactory())
                .build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public JsonNode getForecast(ZonedDateTime timeFrom,
                                 ZonedDateTime timeTo,
                                 String locationName,
                                 String elementName,
                                 Integer limit,
                                 Integer offset) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_URL)
                .queryParam("Authorization", apiKey)
                .queryParam("format", "JSON")
                .queryParam("limit", limit != null ? limit : 10)
                .queryParam("offset", offset != null ? offset : 1);

        if (locationName != null && !locationName.isBlank()) {
            builder.queryParam("locationName", locationName);
        }
        if (elementName != null && !elementName.isBlank()) {
            builder.queryParam("elementName", elementName);
        }
        if (timeFrom != null) {
            builder.queryParam("timeFrom", ISO_FORMATTER.format(timeFrom));
        }
        if (timeTo != null) {
            builder.queryParam("timeTo", ISO_FORMATTER.format(timeTo));
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(builder.build().encode().toUri(), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException | IOException e) {
            throw new IllegalStateException("無法取得氣象資料", e);
        }
    }
}
