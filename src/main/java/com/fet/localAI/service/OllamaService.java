package com.fet.localAI.service;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fet.localAI.tool.DateTimeTool;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import java.util.Base64;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.ai.model.function.FunctionCallback;
import java.util.function.Function;

@Service
public class OllamaService {

    private ChatModel chatModel;
    private final Map<String, List<Message>> chatHistory = new ConcurrentHashMap<>();
    private final DateTimeTool dateTimeTool;

    // @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    @Value("${spring.ai.ollama.base-url}")
    private String defaultBaseUrl;

    // @Value("${spring.ai.ollama.chat.model:gemma3:4b}")
    @Value("${spring.ai.ollama.chat.model}")
    private String defaultModel;

    private String currentBaseUrl;
    private String currentModel;
    private boolean isInitialized = false;
    private final FunctionCallback dateTimeFunction;

    public OllamaService(ChatModel chatModel, DateTimeTool dateTimeTool) {
        this.chatModel = chatModel;
        this.dateTimeTool = dateTimeTool;
        this.dateTimeFunction = FunctionCallback.builder()
                .function("get_current_datetime", dateTimeTool)
                .description("獲取目前日期時間，可指定格式與是否包含時間")
                .inputType(DateTimeTool.Request.class)
                .build();
    }

    public String generateResponse(String message, String chatId, String model) {
        // 先初始化配置,確保 currentBaseUrl 有值
        initializeConfig();

        // 如果傳入的 model 和當前使用的不同,才更新
        if (!model.equals(currentModel)) {
            updateConfig(currentBaseUrl, model);
        }

        return generateResponse(message, chatId);
    }

    /**
     * 初始化對話並設定角色 (System Prompt)
     */
    public void setSystemPrompt(String chatId, String systemPrompt) {
        List<Message> history = new ArrayList<>();
        // 將 Agent 的設定作為第一條 System Message
        history.add(new SystemMessage(systemPrompt));
        chatHistory.put(chatId, history);
    }

    private void initializeConfig() {
        if (!isInitialized) {
            // 如果 currentBaseUrl 尚未設定，則使用預設值
            if (currentBaseUrl == null || currentBaseUrl.isEmpty()) {
                currentBaseUrl = defaultBaseUrl;
            }
            if (currentModel == null || currentModel.isEmpty()) {
                currentModel = defaultModel;
            }

            isInitialized = true;

            System.out.println("Initializing Ollama Service with URL: " + currentBaseUrl + ", Model: " + currentModel);

            // 使用配置檔的設定初始化 ChatModel
            try {
                OllamaApi ollamaApi = new OllamaApi(currentBaseUrl);
                this.chatModel = OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(OllamaOptions.builder()
                                .model(currentModel)
                                .temperature(0.7) // 加入一些基本設定
                                .build())
                        .build();
            } catch (Exception e) {
                System.err.println("Failed to initialize Ollama with config: " + e.getMessage());
                // 保持使用注入的 chatModel
            }
        }
    }

    public void updateConfig(String baseUrl, String model) {
        try {
            this.currentBaseUrl = baseUrl;
            this.currentModel = model;

            // 創建新的 OllamaApi 和 ChatModel
            OllamaApi ollamaApi = new OllamaApi(baseUrl);
            this.chatModel = OllamaChatModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(OllamaOptions.builder()
                            .model(model)
                            .build())
                    .build();

            isInitialized = true;

            System.out.println("Ollama config updated - URL: " + baseUrl + ", Model: " + model);
        } catch (Exception e) {
            System.err.println("Failed to update Ollama config: " + e.getMessage());
            throw new RuntimeException("無法連線到 Ollama 服務，請檢查 URL 是否正確：" + baseUrl, e);
        }
    }

    public Map<String, String> getCurrentConfig() {
        initializeConfig();
        return Map.of(
                "ollamaUrl", currentBaseUrl,
                "model", currentModel);
    }

    public String generateResponse(String message, String chatId) {
        initializeConfig();

        List<Message> history = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());

        // 簡單的日期查詢處理 - 如果使用者詢問日期，直接回答
        if (isAskingForDate(message)) {
            DateTimeTool.Request request = new DateTimeTool.Request();
            request.setFormat("yyyy年MM月dd日");
            DateTimeTool.Response dateResponse = dateTimeTool.apply(request);
            String answer = dateResponse.getDescription();
            history.add(new UserMessage(message));
            history.add(new AssistantMessage(answer));
            return answer;
        }

        history.add(new UserMessage(message));

        Prompt prompt = new Prompt(history);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        history.add(new AssistantMessage(response));
        return response;
    }

    public String generateJsonResponse(String message, String chatId) {
        initializeConfig();

        List<Message> history = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());

        history.add(new UserMessage(message));

        OllamaOptions options = OllamaOptions.builder()
                .model(currentModel)
                .format("json")
                .build();

        Prompt prompt = new Prompt(history, options);
        String response = chatModel.call(prompt).getResult().getOutput().getText();

        history.add(new AssistantMessage(response));
        return response;
    }

    private boolean isAskingForDate(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("今天") || lowerMessage.contains("日期") ||
                lowerMessage.contains("幾號") || lowerMessage.contains("星期幾") ||
                lowerMessage.contains("what day") || lowerMessage.contains("what date") ||
                lowerMessage.contains("today");
    }

    public void clearHistory(String chatId) {
        chatHistory.remove(chatId);
    }

    public List<Message> getHistory(String chatId) {
        return chatHistory.getOrDefault(chatId, new ArrayList<>());
    }

    public String generateResponseWithImage(String userMessage, String base64Image, String chatId) {
        try {
            List<Message> history = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());

            // Remove "data:image/xxx;base64," prefix if present
            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            // Decode base64 to bytes
            byte[] imageBytes = Base64.getDecoder().decode(imageData);

            // Create user message with image
            UserMessage userMsg = new UserMessage(userMessage,
                    List.of(new org.springframework.ai.model.Media(MimeTypeUtils.IMAGE_PNG,
                            new ByteArrayResource(imageBytes))));
            history.add(userMsg);

            // Generate response
            Prompt prompt = new Prompt(new ArrayList<>(history));
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            // Add AI response to history
            history.add(new AssistantMessage(response));

            return response;
        } catch (Exception e) {
            System.err.println("Error generating response with image: " + e.getMessage());
            e.printStackTrace();
            return "處理圖片時發生錯誤：" + e.getMessage();
        }
    }

    public Flux<String> generateResponseStream(String message, String chatId) {
        initializeConfig();

        List<Message> history = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());
        history.add(new UserMessage(message));

        Prompt prompt = new Prompt(new ArrayList<>(history));

        if (chatModel instanceof StreamingChatModel) {
            StreamingChatModel streamingModel = (StreamingChatModel) chatModel;
            StringBuilder fullResponse = new StringBuilder();

            return streamingModel.stream(prompt)
                    .map(chatResponse -> {
                        String content = chatResponse.getResult().getOutput().getText();
                        fullResponse.append(content);
                        return content;
                    })
                    .doOnComplete(() -> {
                        history.add(new AssistantMessage(fullResponse.toString()));
                    });
        } else {
            // Fallback to non-streaming
            String response = chatModel.call(prompt).getResult().getOutput().getText();
            history.add(new AssistantMessage(response));
            return Flux.just(response);
        }
    }

    public Flux<String> generateResponseStreamWithImage(String userMessage, String base64Image, String chatId) {
        try {
            List<Message> history = chatHistory.computeIfAbsent(chatId, k -> new ArrayList<>());

            String imageData = base64Image;
            if (base64Image.contains(",")) {
                imageData = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageData);
            UserMessage userMsg = new UserMessage(userMessage,
                    List.of(new org.springframework.ai.model.Media(MimeTypeUtils.IMAGE_PNG,
                            new ByteArrayResource(imageBytes))));
            history.add(userMsg);

            Prompt prompt = new Prompt(new ArrayList<>(history));

            if (chatModel instanceof StreamingChatModel) {
                StreamingChatModel streamingModel = (StreamingChatModel) chatModel;
                StringBuilder fullResponse = new StringBuilder();

                return streamingModel.stream(prompt)
                        .map(chatResponse -> {
                            String content = chatResponse.getResult().getOutput().getText();
                            fullResponse.append(content);
                            return content;
                        })
                        .doOnComplete(() -> {
                            history.add(new AssistantMessage(fullResponse.toString()));
                        });
            } else {
                String response = chatModel.call(prompt).getResult().getOutput().getText();
                history.add(new AssistantMessage(response));
                return Flux.just(response);
            }
        } catch (Exception e) {
            return Flux.error(new RuntimeException("處理圖片時發生錯誤：" + e.getMessage()));
        }
    }

    public List<String> getAvailableModels(String baseUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = baseUrl.endsWith("/") ? baseUrl + "api/tags" : baseUrl + "/api/tags";

            String response = restTemplate.getForObject(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode models = root.get("models");

            if (models != null && models.isArray()) {
                return StreamSupport.stream(models.spliterator(), false)
                        .map(model -> model.get("name").asText())
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Failed to fetch models from " + baseUrl + ": " + e.getMessage());
            throw new RuntimeException("無法連線到 Ollama 服務或獲取模型列表", e);
        }
    }
}
