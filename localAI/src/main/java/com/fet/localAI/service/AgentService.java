package com.fet.localAI.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AgentService {

    @Value("${agents.path}")
    private String agentsPath;

    // 定義部門翻譯
    private static final Map<String, String> FOLDER_TRANSLATIONS = Map.of(
            "engineering", "工程部門",
            "product", "產品部門",
            "marketing", "行銷部門",
            "design", "設計部門",
            "project-management", "專案管理",
            "studio-operations", "工作室營運",
            "testing", "測試與基準",
            "bonus", "額外工具"
    );

    // Agent 描述繁體中文翻譯
    private static final Map<String, String> AGENT_DESCRIPTIONS = createDescriptionMap();

    private static Map<String, String> createDescriptionMap() {
        Map<String, String> map = new HashMap<>();
        // Engineering 工程部門
        map.put("ai-engineer", "AI 工程師 - 專精於整合 AI 功能與機器學習模型");
        map.put("backend-architect", "後端架構師 - 設計可擴展的伺服器架構");
        map.put("devops-automator", "DevOps 自動化專家 - 建立 CI/CD 流程與基礎設施");
        map.put("frontend-developer", "前端開發者 - 打造現代化的使用者介面");
        map.put("mobile-app-builder", "行動應用開發者 - 開發跨平台行動應用");
        map.put("rapid-prototyper", "快速原型開發者 - 快速實作概念驗證");
        map.put("test-writer-fixer", "測試撰寫專家 - 編寫與修復測試程式碼");

        // Design 設計部門
        map.put("brand-guardian", "品牌守護者 - 維護品牌一致性與識別");
        map.put("ui-designer", "UI 設計師 - 創造美觀且實用的使用者介面");
        map.put("ux-researcher", "UX 研究員 - 深入了解使用者需求與行為");
        map.put("visual-storyteller", "視覺說故事專家 - 透過視覺傳達品牌故事");
        map.put("whimsy-injector", "趣味注入專家 - 為產品增添創意與趣味元素");

        // Marketing 行銷部門
        map.put("app-store-optimizer", "應用商店優化專家 - 提升 App Store 能見度");
        map.put("content-creator", "內容創作者 - 製作吸引人的行銷內容");
        map.put("growth-hacker", "成長駭客 - 透過創新策略推動用戶增長");
        map.put("instagram-curator", "Instagram 策展人 - 經營 IG 社群與內容");
        map.put("reddit-community-builder", "Reddit 社群建立者 - 建立與管理 Reddit 社群");
        map.put("tiktok-strategist", "TikTok 策略專家 - 制定病毒式短影音策略");
        map.put("twitter-engager", "Twitter 互動專家 - 提升 Twitter 參與度");

        // Product 產品部門
        map.put("feedback-synthesizer", "回饋綜合分析師 - 整合並分析用戶回饋");
        map.put("sprint-prioritizer", "衝刺優先級管理者 - 排定開發優先順序");
        map.put("trend-researcher", "趨勢研究員 - 追蹤產業趨勢與競爭分析");

        // Project Management 專案管理
        map.put("experiment-tracker", "實驗追蹤者 - 追蹤與分析 A/B 測試結果");
        map.put("project-shipper", "專案交付者 - 確保專案準時交付");
        map.put("studio-producer", "工作室製作人 - 統籌專案執行與資源");

        // Studio Operations 工作室營運
        map.put("analytics-reporter", "數據分析報告者 - 產生營運數據報告");
        map.put("finance-tracker", "財務追蹤者 - 管理預算與財務報表");
        map.put("infrastructure-maintainer", "基礎設施維護者 - 維護系統穩定運行");
        map.put("legal-compliance-checker", "法規遵循檢查者 - 確保符合法律規範");
        map.put("support-responder", "客戶支援回應者 - 處理用戶問題與疑問");

        // Testing 測試與基準
        map.put("api-tester", "API 測試專家 - 測試與驗證 API 功能");
        map.put("performance-benchmarker", "效能基準測試者 - 評估系統效能表現");
        map.put("test-results-analyzer", "測試結果分析師 - 分析測試數據與結果");
        map.put("tool-evaluator", "工具評估者 - 評估開發工具與技術");
        map.put("workflow-optimizer", "工作流程優化者 - 優化開發流程效率");

        // Bonus 額外工具
        map.put("joker", "說笑話高手 - 增添團隊歡樂氣氛");
        map.put("studio-coach", "工作室教練 - 提供團隊指導與建議");

        return map;
    }

    public static class AgentInfo {
        public String id; // 檔名 (不含副檔名)
        public String name;
        public String description;
        public String department; // 中文部門名稱
        public String fullPath;
        public String folder; // 原始資料夾名

        public AgentInfo(String id, String name, String description, String department, String fullPath, String folder) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.department = department;
            this.fullPath = fullPath;
            this.folder = folder;
        }
    }

    /**
     * 掃描並列出所有可用的 Agent
     */
    public List<AgentInfo> listAgents() {
        List<AgentInfo> agents = new ArrayList<>();
        Path startPath = Paths.get(agentsPath);

        if (!Files.exists(startPath)) {
            System.err.println("Agents path not found: " + agentsPath);
            return agents;
        }

        try (Stream<Path> stream = Files.walk(startPath)) {
            List<Path> files = stream
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.toString().contains("node_modules"))
                    .filter(p -> !p.getFileName().toString().equalsIgnoreCase("README.md"))
                    .collect(Collectors.toList());

            for (Path file : files) {
                agents.add(parseAgentFile(file, startPath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 排序：先按部門，再按名稱
        agents.sort(Comparator.comparing((AgentInfo a) -> a.folder).thenComparing(a -> a.id));
        return agents;
    }

    /**
     * 讀取並解析特定 Agent 的 System Prompt
     */
    public String getSystemPrompt(String fullPath) {
        try {
            String content = Files.readString(Paths.get(fullPath));
            // 移除 YAML Frontmatter (---\n ... \n---\n)
            return content.replaceAll("(?s)^---\\n(.*?)\\n---\\n", "").trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read agent file: " + fullPath, e);
        }
    }

    private AgentInfo parseAgentFile(Path file, Path rootPath) throws IOException {
        String content = Files.readString(file);
        String fileName = file.getFileName().toString().replace(".md", "");

        // 取得相對路徑的資料夾名稱作為部門
        String folderName = "root";
        Path relative = rootPath.relativize(file);
        if (relative.getNameCount() > 1) {
            folderName = relative.getParent().getFileName().toString();
        }

        String department = FOLDER_TRANSLATIONS.getOrDefault(folderName.toLowerCase(), folderName.toUpperCase());
        String name = fileName;

        // 優先使用繁體中文描述映射表
        String description = AGENT_DESCRIPTIONS.getOrDefault(fileName, "AI 助理專家");

        // 解析 YAML (用於獲取 name，如果有的話)
        Pattern yamlPattern = Pattern.compile("(?s)^---\\n(.*?)\\n---");
        Matcher yamlMatcher = yamlPattern.matcher(content);
        if (yamlMatcher.find()) {
            String yamlBlock = yamlMatcher.group(1);


            // 抓取 name (如果有的話)
            Pattern namePattern = Pattern.compile("name:\\s*(.*)");
            Matcher nameMatcher = namePattern.matcher(yamlBlock);
            if (nameMatcher.find()) {
                name = nameMatcher.group(1).trim();
            }
        }

        return new AgentInfo(fileName, name, description, department, file.toAbsolutePath().toString(), folderName);
    }
}