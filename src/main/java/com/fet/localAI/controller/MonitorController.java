package com.fet.localAI.controller;

import com.fet.localAI.service.OllamaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import java.util.Map;

@Controller
public class MonitorController {

    private final OllamaService ollamaService;

    public MonitorController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    @GetMapping("/monitor")
    public String monitorPage(Model model) {
        // 可以傳遞一些初始參數給前端，例如當前使用的模型名稱
        Map<String, String> config = ollamaService.getCurrentConfig();
        model.addAttribute("modelName", config.get("model"));
        return "monitor";
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        Map<String, String> config = ollamaService.getCurrentConfig();
        model.addAttribute("modelName", config.get("model"));
        return "dashboard";
    }

    @GetMapping("/mt")
    public String mtPage(Model model) {
        Map<String, String> config = ollamaService.getCurrentConfig();
        model.addAttribute("modelName", config.get("model"));
        return "mt";
    }

    @GetMapping("/api/monitor/config")
    @ResponseBody
    public Map<String, String> getConfig() {
        return ollamaService.getCurrentConfig();
    }

    @GetMapping(value = "/api/monitor/data", produces = "application/json")
    @ResponseBody
    public String getMonitorData() {
        try {
            ClassPathResource res = new ClassPathResource("data.json");
            java.nio.charset.Charset utf8 = java.util.Objects.requireNonNull(java.nio.charset.Charset.forName("UTF-8"));
            return StreamUtils.copyToString(res.getInputStream(), utf8);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    @PostMapping("/api/monitor/analyze")
    @ResponseBody
    public Map<String, Object> analyze(@RequestParam String message, HttpSession session) {
        try {
            String chatId = session.getId();

            // 強制 JSON 格式的 System Prompt
            String systemInstruction = """
                    You are a strict JSON generator.
                    Output ONLY valid JSON.
                    Do not include markdown formatting (like ```json ... ```).
                    Do not include any conversational text or polite phrases.
                    The output must start with { and end with }.
                    """;

            // 呼叫 OllamaService 生成回應 (使用 JSON 模式)
            String fullPrompt = systemInstruction + "\n\n" + message;
            String response = ollamaService.generateJsonResponse(fullPrompt, chatId);

            return Map.of(
                    "status", "success",
                    "response", response);
        } catch (Exception e) {
            return Map.of(
                    "status", "error",
                    "message", "分析失敗：" + e.getMessage());
        }
    }
}
