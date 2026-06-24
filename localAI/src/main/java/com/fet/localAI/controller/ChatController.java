package com.fet.localAI.controller;

import com.fet.localAI.service.AgentService;
import com.fet.localAI.service.OllamaService;
import com.fet.localAI.service.WebSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.io.IOException;

import jakarta.servlet.http.HttpSession;

@Controller
public class ChatController {

    private final OllamaService ollamaService;
    private final WebSearchService webSearchService;
    private final AgentService agentService;

    public ChatController(OllamaService ollamaService, WebSearchService webSearchService, AgentService agentService) {
        this.ollamaService = ollamaService;
        this.webSearchService = webSearchService;
        this.agentService = agentService;
    }

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }

    // 1. 取得所有可用 Agent 清單
    @GetMapping("/api/agents")
    @ResponseBody
    public Map<String, Object> getAgents() {
        try {
            List<AgentService.AgentInfo> agents = agentService.listAgents();
            return Map.of(
                    "status", "success",
                    "agents", agents
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    // 2. 切換角色
    @PostMapping("/api/chat/agent")
    @ResponseBody
    public Map<String, String> switchAgent(
            @RequestParam String filePath,
            HttpSession session) {
        try {
            String chatId = session.getId();
            // 讀取該 Agent 的 System Prompt
            String systemPrompt = agentService.getSystemPrompt(filePath);

            // 設定給 OllamaService (這會清空舊對話)
            ollamaService.setSystemPrompt(chatId, systemPrompt);

            return Map.of(
                    "status", "success",
                    "message", "已切換角色，準備就緒！"
            );
        } catch (Exception e) {
            return Map.of("status", "error", "message", "切換失敗：" + e.getMessage());
        }
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public Map<String, String> chat(
            @RequestParam String message, 
            @RequestParam(required = false) String image,
            HttpSession session) {
        try {
            String chatId = session.getId();
            String response;
            if (image != null && !image.isEmpty()) {
                response = ollamaService.generateResponseWithImage(message, image, chatId);
            } else {
                response = ollamaService.generateResponse(message, chatId);
            }
            return Map.of("response", response, "status", "success");
        } catch (Exception e) {
            return Map.of("response", "抱歉，發生錯誤：" + e.getMessage(), "status", "error");
        }
    }
    
    @GetMapping("/api/chat/stream")
    public SseEmitter chatStream(
            @RequestParam String message,
            @RequestParam(required = false) String image,
            HttpSession session) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        String chatId = session.getId();
        
        new Thread(() -> {
            try {
                Flux<String> stream;
                if (image != null && !image.isEmpty()) {
                    stream = ollamaService.generateResponseStreamWithImage(message, image, chatId);
                } else {
                    stream = ollamaService.generateResponseStream(message, chatId);
                }
                
                stream.subscribe(
                    chunk -> {
                        try {
                            emitter.send(SseEmitter.event().data(chunk));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    error -> emitter.completeWithError(error),
                    () -> emitter.complete()
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
        
        return emitter;
    }

    @PostMapping("/api/chat/clear")
    @ResponseBody
    public Map<String, String> clearChat(HttpSession session) {
        try {
            String chatId = session.getId();
            ollamaService.clearHistory(chatId);
            return Map.of("status", "success", "message", "聊天記錄已清除");
        } catch (Exception e) {
            return Map.of("status", "error", "message", "清除失敗");
        }
    }

    @PostMapping("/api/config")
    @ResponseBody
    public Map<String, String> updateConfig(
            @RequestParam String ollamaUrl,
            @RequestParam String model,
            HttpSession session) {
        try {
            ollamaService.updateConfig(ollamaUrl, model);
            return Map.of("status", "success", "message", "配置已更新");
        } catch (Exception e) {
            return Map.of("status", "error", "message", "配置更新失敗：" + e.getMessage());
        }
    }

    @GetMapping("/api/config")
    @ResponseBody
    public Map<String, String> getConfig() {
        return ollamaService.getCurrentConfig();
    }

//    @GetMapping("/api/models")
//    public List<String> fetchModels() {
//        return ollamaService.getAvailableModels(ollamaService.getCurrentConfig().get("ollamaUrl"));
//    }
//
//
    @GetMapping("/api/models")
    @ResponseBody
    public Map<String, Object> getAvailableModels(@RequestParam String ollamaUrl) {
        try {
            List<String> models = ollamaService.getAvailableModels(ollamaUrl);
            return Map.of(
                "status", "success",
                "models", models
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "message", "無法連線到 Ollama 服務：" + e.getMessage()
            );
        }
    }
    
    @GetMapping("/api/search")
    @ResponseBody
    public Map<String, Object> webSearch(@RequestParam String query, 
                                         @RequestParam(defaultValue = "5") int maxResults) {
        try {
            List<Map<String, String>> results = webSearchService.search(query, maxResults);
            return Map.of(
                "status", "success",
                "results", results
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "message", "搜尋失敗：" + e.getMessage()
            );
        }
    }
    
    @PostMapping("/api/chat/search")
    @ResponseBody
    public Map<String, String> chatWithSearch(
            @RequestParam String message,
            @RequestParam(defaultValue = "3") int searchResults,
            HttpSession session) {
        try {
            String chatId = session.getId();
            
            // 執行網頁搜尋
            String searchContext = webSearchService.searchAndFormat(message, searchResults);
            
            // 將搜尋結果加入對話上下文
            String enhancedMessage = "根據以下網頁搜尋結果回答問題：\n\n" + 
                                    searchContext + "\n\n" +
                                    "問題：" + message;
            
            String response = ollamaService.generateResponse(enhancedMessage, chatId);
            
            return Map.of(
                "response", response,
                "searchContext", searchContext,
                "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                "response", "抱歉，發生錯誤：" + e.getMessage(),
                "status", "error"
            );
        }
    }
}
