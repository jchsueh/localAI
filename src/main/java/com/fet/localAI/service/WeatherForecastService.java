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

@Service
public class WeatherForecastService {

    private static final String API_URL = "https://opendata.cwa.gov.tw/api/v1/rest/datastore/F-C0032-001";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate;
    private final String apiKey;

    public WeatherForecastService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${cwa.api.key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
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
            ResponseEntity<String> response = restTemplate.getForEntity(builder.build(true).toUri(), String.class);
            return objectMapper.readTree(response.getBody());
        } catch (RestClientException | IOException e) {
            throw new IllegalStateException("無法取得氣象資料", e);
        }
    }
}
