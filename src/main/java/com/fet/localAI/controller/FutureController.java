package com.fet.localAI.controller;

import com.fet.localAI.service.FutureService;
import com.fet.localAI.service.OllamaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class FutureController {

    private final FutureService futureService;
    private final OllamaService ollamaService;

    public FutureController(FutureService futureService, OllamaService ollamaService) {
        this.futureService = futureService;
        this.ollamaService = ollamaService;
    }

    /**
     * 渲染 AIOps 監控首頁
     */
    @GetMapping("/future")
    public String futurePage(Model model) {
        Map<String, String> config = ollamaService.getCurrentConfig();
        model.addAttribute("modelName", config.get("model"));
        return "future";
    }

    /**
     * 重置監控狀態與任務
     */
    @PostMapping("/api/future/reset")
    @ResponseBody
    public Map<String, String> resetStats() {
        futureService.reset();
        return Map.of("status", "success", "message", "狀態已重置為初始 mock 數據");
    }

    /**
     * 取得目前主機與指標統計數
     */
    @GetMapping("/api/future/stats")
    @ResponseBody
    public Map<String, Integer> getStats() {
        return futureService.getStats();
    }

    /**
     * 取得指標或主機清單明細
     */
    @GetMapping("/api/future/details")
    @ResponseBody
    public List<Map<String, String>> getDetails(@RequestParam String type) {
        return futureService.getDetails(type);
    }

    /**
     * 取得當前活躍中的自動調度任務流
     */
    @GetMapping("/api/future/tasks")
    @ResponseBody
    public List<Map<String, Object>> getActiveTasks() {
        return futureService.getActiveTasks();
    }

    /**
     * 授權 AI 執行並解決指定任務
     */
    @PostMapping("/api/future/tasks/{taskId}/resolve")
    @ResponseBody
    public Map<String, Object> resolveTask(@PathVariable String taskId) {
        boolean success = futureService.resolveTask(taskId);
        if (success) {
            return Map.of(
                    "status", "success",
                    "message", "任務已成功處理，系統指標已復原"
            );
        } else {
            return Map.of(
                    "status", "error",
                    "message", "任務已處理過或找不到該任務"
            );
        }
    }

    /**
     * AIOps 智能助理聊天介面
     */
    @PostMapping("/api/future/chat")
    @ResponseBody
    public Map<String, String> chat(@RequestParam String message, HttpSession session) {
        try {
            String chatId = "future_" + session.getId();

            // 如果該對話歷史還沒有初始化，設定 AIOps System Prompt
            if (ollamaService.getHistory(chatId).isEmpty()) {
                String systemPrompt = """
                        您是 AIOps 智能運維特助，已與企業監控系統（AIOps 監控中心）完整整合。
                        您將協助 SRE 團隊分析伺服器健康、網路品質、資安威脅並給出處置建議。
                        
                        當前系統的異常告警如下：
                        1. 【緊急】偵測到內部主機橫向攻擊 (勒索病毒防禦) - 感染源 HR-Desktop-04。
                        2. 【預防性維護】跨廠區專線 Packet Loss 持續惡化預警 - VPN 遺失率 4%。
                        3. 【空間預警】Order-DB-Primary 磁碟空間將於 45 分鐘內耗盡。
                        4. 【嚴重】核心交換機 tgw-core-01 路由崩潰，導致 14 台設備斷線。
                        5. 【高優先】API-Gateway-01 服務記憶體洩漏 (Memory Leak)。
                        
                        回應時請使用繁體中文（Taiwan），語氣需專業、簡潔且條理清晰。回答問題時，可多引用以上當前的告警資訊並給出 SRE 等級的具體建議。
                        """;
                ollamaService.setSystemPrompt(chatId, systemPrompt);
            }

            // 呼叫 Ollama 服務生成回應
            String response = ollamaService.generateResponse(message, chatId);

            return Map.of("status", "success", "response", response);
        } catch (Exception e) {
            return Map.of("status", "error", "response", "AI 助理目前無法連線：" + e.getMessage());
        }
    }

    /**
     * 清除 AI 助理對話歷史
     */
    @PostMapping("/api/future/chat/clear")
    @ResponseBody
    public Map<String, String> clearChat(HttpSession session) {
        String chatId = "future_" + session.getId();
        ollamaService.clearHistory(chatId);
        return Map.of("status", "success", "message", "AIOps 助理對話歷史已清除");
    }
}
