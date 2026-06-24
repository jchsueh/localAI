package com.fet.localAI.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fet.localAI.service.WeatherForecastService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherForecastService weatherForecastService;

    public WeatherController(WeatherForecastService weatherForecastService) {
        this.weatherForecastService = weatherForecastService;
    }

    @GetMapping("/forecast")
    public ResponseEntity<JsonNode> getForecast(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime timeFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime timeTo,
            @RequestParam(required = false) String locationName,
            @RequestParam(required = false) String elementName,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {

        JsonNode response = weatherForecastService.getForecast(
                timeFrom,
                timeTo,
                locationName,
                elementName,
                limit,
                offset);
        return ResponseEntity.ok(response);
    }
}
