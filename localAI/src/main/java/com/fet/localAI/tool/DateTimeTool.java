package com.fet.localAI.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * Tool for getting current date and time information
 */
@Component
public class DateTimeTool implements Function<DateTimeTool.Request, DateTimeTool.Response> {

    public static class Request {
        @JsonProperty(required = false)
        @JsonPropertyDescription("日期格式，例如：'yyyy-MM-dd' 或 'yyyy年MM月dd日'，預設為 'yyyy-MM-dd'")
        private String format;

        @JsonProperty(required = false)
        @JsonPropertyDescription("是否包含時間，預設為 false（只顯示日期）")
        private Boolean includeTime;

        public Request() {
        }

        public String getFormat() {
            return format != null ? format : "yyyy-MM-dd";
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public Boolean getIncludeTime() {
            return includeTime != null ? includeTime : false;
        }

        public void setIncludeTime(Boolean includeTime) {
            this.includeTime = includeTime;
        }
    }

    public static class Response {
        private String date;
        private String dayOfWeek;
        private String description;

        public Response(String date, String dayOfWeek, String description) {
            this.date = date;
            this.dayOfWeek = dayOfWeek;
            this.description = description;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @Override
    public Response apply(Request request) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(request.getFormat());

            String dateStr;
            String dayOfWeek;

            if (request.getIncludeTime()) {
                LocalDateTime now = LocalDateTime.now();
                dateStr = now.format(formatter);
                dayOfWeek = getDayOfWeekInChinese(now.getDayOfWeek().getValue());
            } else {
                LocalDate today = LocalDate.now();
                dateStr = today.format(formatter);
                dayOfWeek = getDayOfWeekInChinese(today.getDayOfWeek().getValue());
            }

            String description = String.format("今天是 %s，星期%s", dateStr, dayOfWeek);

            return new Response(dateStr, dayOfWeek, description);
        } catch (Exception e) {
            return new Response("", "", "無法取得日期：" + e.getMessage());
        }
    }

    private String getDayOfWeekInChinese(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "一";
            case 2 -> "二";
            case 3 -> "三";
            case 4 -> "四";
            case 5 -> "五";
            case 6 -> "六";
            case 7 -> "日";
            default -> "未知";
        };
    }
}
