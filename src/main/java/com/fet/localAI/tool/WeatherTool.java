package com.fet.localAI.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fet.localAI.service.WeatherForecastService;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class WeatherTool implements Function<WeatherTool.Request, WeatherTool.Response> {

    private final WeatherForecastService weatherService;

    public WeatherTool(WeatherForecastService weatherService) {
        this.weatherService = weatherService;
    }

    @Override
    public Response apply(Request request) {
        try {
            JsonNode forecast = weatherService.getForecast(
                    null,
                    null,
                    request.locationName,
                    request.elementName,
                    request.limit,
                    request.offset
            );

            return new Response(
                    "成功取得 " + (request.locationName != null ? request.locationName : "全台") + " 的天氣資訊",
                    forecast.toString()
            );
        } catch (Exception e) {
            return new Response("無法取得天氣資訊: " + e.getMessage(), null);
        }
    }

    public static class Request {
        @JsonProperty(required = false)
        @JsonPropertyDescription("地區名稱,例如:臺北市、新北市、桃園市")
        public String locationName;

        @JsonProperty(required = false)
        @JsonPropertyDescription("氣象要素,例如:Wx(天氣現象)、PoP(降雨機率)、MinT(最低溫)、MaxT(最高溫)")
        public String elementName;

        @JsonProperty(required = false)
        @JsonPropertyDescription("限制回傳筆數")
        public Integer limit;

        @JsonProperty(required = false)
        @JsonPropertyDescription("偏移量")
        public Integer offset;
    }

    public static class Response {
        public final String description;
        public final String data;

        public Response(String description, String data) {
            this.description = description;
            this.data = data;
        }
    }
}