package com.fet.localAI.controller;

import com.fet.localAI.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;

@Controller
public class InfiniteController {

    private static final Logger logger = LoggerFactory.getLogger(InfiniteController.class);

    private final OllamaService ollamaService;

    @Value("${spring.ai.ollama.chat.model}")
    private String defaultModel;

    public InfiniteController(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    /**
     * 顯示無限鍊金術遊戲頁面
     */
    @GetMapping("/infinite")
    public String infinitePage(Model model) {
        model.addAttribute("modelName", defaultModel);
        logger.info("載入無限鍊金術頁面，使用模型: {}", defaultModel);
        return "infinite";
    }

    /**
     * 處理鍊金術合成請求
     * 前端會發送包含合成邏輯的 prompt
     */
     @GetMapping("/api/infinite/chat")
     @ResponseBody
     public String chat(@RequestParam String message, HttpSession session) {
         try {
             String chatId = "infinite_" + session.getId();

             logger.info("收到鍊金術合成請求，使用模型: {}", defaultModel);
             logger.debug("Prompt: {}", message);

             // 傳遞 defaultModel 給 Service
             String response = ollamaService.generateResponse(message, chatId, defaultModel);

             logger.info("AI 回應: {}", response);

             return response;
         } catch (Exception e) {
             logger.error("處理鍊金術請求時發生錯誤", e);
             return "{\"error\": \"合成失敗,請稍後再試\"}";
         }
     }

    /**
     * 清除對話歷史
     */
    @GetMapping("/infinite/clear")
    @ResponseBody
    public String clearHistory(HttpSession session) {
        String chatId = "infinite_" + session.getId();
        ollamaService.clearHistory(chatId);
        logger.info("已清除鍊金術對話歷史");
        return "{\"status\": \"success\"}";
    }
}

