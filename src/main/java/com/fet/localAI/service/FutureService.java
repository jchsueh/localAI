package com.fet.localAI.service;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class FutureService {

    private final Map<String, Integer> stats = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, String>>> details = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> tasks = new CopyOnWriteArrayList<>();
    private final Set<String> resolvedTaskIds = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void init() {
        reset();
    }

    public synchronized void reset() {
        resolvedTaskIds.clear();

        // 1. 初始化統計數據
        stats.put("hosts-running", 128);
        stats.put("hosts-down", 14);
        stats.put("network-warning", 1);
        stats.put("metrics-normal", 842);
        stats.put("metrics-abnormal", 24);
        stats.put("security-alerts", 3);

        // 2. 初始化詳細清單
        List<Map<String, String>> hostsDownList = new ArrayList<>();
        hostsDownList.add(createDetailItem("IIMS-TEST", "IP: 192.168.1.10", "Ping Timeout (已 15 分鐘)", "已納入: 網路斷線任務"));
        hostsDownList.add(createDetailItem("db-mysql-iimsdb", "IP: 10.17.0.11", "Ping Timeout (已 15 分鐘)", "已納入: 網路斷線任務"));
        details.put("hosts-down", hostsDownList);

        List<Map<String, String>> hostsRunningList = new ArrayList<>();
        hostsRunningList.add(createDetailItem("tgw02-standby", "IP: 192.168.2.10", "正常運行中", "無須處置"));
        details.put("hosts-running", hostsRunningList);

        List<Map<String, String>> netWarningList = new ArrayList<>();
        netWarningList.add(createDetailItem("HQ-to-Branch-VPN", "ISP: Hinet Enterprise", "Packet Loss 攀升至 4%", "已納入: 網路品質預警任務"));
        details.put("network-warning", netWarningList);

        List<Map<String, String>> metricsAbnormalList = new ArrayList<>();
        metricsAbnormalList.add(createDetailItem("HTTP 500 Error Rate", "服務: API-Gateway-01", "錯誤率飆升至 45%", "已納入: 閘道崩潰任務"));
        metricsAbnormalList.add(createDetailItem("Memory Usage", "服務: API-Gateway-01", "用量 98% (OOM 風險)", "已納入: 閘道崩潰任務"));
        metricsAbnormalList.add(createDetailItem("Disk Space", "磁區: /var/lib/mysql", "容量剩餘 4% (20GB)", "已納入: 容量預警任務"));
        details.put("metrics-abnormal", metricsAbnormalList);

        List<Map<String, String>> metricsNormalList = new ArrayList<>();
        metricsNormalList.add(createDetailItem("CPU Load Average", "主機: k8s-worker-01", "負載 25%", "無須處置"));
        details.put("metrics-normal", metricsNormalList);

        List<Map<String, String>> securityAlertsList = new ArrayList<>();
        securityAlertsList.add(createDetailItem("Abnormal Port Scan", "來源 IP: 192.168.5.44 (HR)", "發起 4,500 次連線", "已納入: 勒索病毒防禦任務"));
        details.put("security-alerts", securityAlertsList);

        // 3. 初始化調度任務流
        tasks.clear();
        tasks.add(createTaskItem("task-sec", "URGENT: SECURITY", "偵測到內部主機橫向攻擊 (勒索病毒防禦)",
                "AI 行為分析：分析防火牆與 EDR 日誌，發現 `HR-Desktop-04` 正頻繁利用 RDP 與 SMB 協定，對同網段另外 42 臺主機進行異常掃描與連線測試。",
                "purple", "授權 AI 執行網路隔離", true, "security"));

        tasks.add(createTaskItem("task-pl", "PREDICTIVE MAINTENANCE", "跨廠區專線 Packet Loss 持續惡化預警",
                "AI 網路預測模型：偵測到 `HQ-to-Branch-VPN` 的 Ping 封包遺失率在過去 6 小時內攀升至 4%。根據趨勢預測，72 小時內將引發嚴重斷線 (Loss > 15%)。",
                "teal", "授權 AI 執行流量繞流與報修", true, "packet_loss"));

        tasks.add(createTaskItem("task-cap", "PREDICTIVE WARNING", "核心資料庫儲存空間即將耗盡",
                "AI 趨勢預測：因逢電商大檔期，預測 `Order-DB-Primary` 的磁碟空間將於 45 分鐘後完全耗盡。AI 已備妥雲端擴容腳本。",
                "yellow", "授權 AI 執行線上擴容", true, "capacity"));

        tasks.add(createTaskItem("task-net", "CRITICAL", "核心交換機路由崩潰導致批次斷線",
                "AI 根因分析：偵測到 14 個設備主機同時失去連線。經分析網路拓樸，確認為 `tgw-core-01` 節點的 BGP 路由表溢位導致崩潰。",
                "red", "一鍵重啟 BGP 路由進程", false, ""));

        tasks.add(createTaskItem("task-app", "HIGH PRIORITY", "API 閘道服務崩潰 (已收斂 18 個異常指標)",
                "AI 根因分析：在 24 個異常監控項目中，有 18 個皆源自於 `API-Gateway-01` 服務記憶體洩漏 (Memory Leak)。建議直接將流量切換至備援節點並重啟。",
                "orange", "授權 AI 切換備援與重啟", true, "api_topology"));
    }

    private Map<String, String> createDetailItem(String name, String detail, String status, String aiStatus) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("detail", detail);
        map.put("status", status);
        map.put("ai_status", aiStatus);
        return map;
    }

    private Map<String, Object> createTaskItem(String id, String category, String title, String description,
                                               String colorClass, String actionText, boolean hasGraph, String graphType) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("category", category);
        map.put("title", title);
        map.put("description", description);
        map.put("colorClass", colorClass);
        map.put("actionText", actionText);
        map.put("hasGraph", hasGraph);
        map.put("graphType", graphType);
        return map;
    }

    public Map<String, Integer> getStats() {
        return stats;
    }

    public List<Map<String, String>> getDetails(String type) {
        return details.getOrDefault(type, Collections.emptyList());
    }

    public List<Map<String, Object>> getActiveTasks() {
        List<Map<String, Object>> active = new ArrayList<>();
        for (Map<String, Object> task : tasks) {
            if (!resolvedTaskIds.contains((String) task.get("id"))) {
                active.add(task);
            }
        }
        return active;
    }

    /**
     * 執行 AI 處置，更新狀態並傳回結果
     */
    public synchronized boolean resolveTask(String taskId) {
        if (resolvedTaskIds.contains(taskId)) {
            return false;
        }
        resolvedTaskIds.add(taskId);

        // 模擬執行處置後，監控指標自動恢復正常
        switch (taskId) {
            case "task-sec":
                // 減少資安威脅
                stats.put("security-alerts", Math.max(0, stats.get("security-alerts") - 1));
                // 更新清單狀態
                updateDetailStatus("security-alerts", "Abnormal Port Scan", "已隔離 HR-Desktop-04 網路，威脅排除");
                break;
            case "task-pl":
                // 減少網路預警數
                stats.put("network-warning", Math.max(0, stats.get("network-warning") - 1));
                updateDetailStatus("network-warning", "HQ-to-Branch-VPN", "已自動繞流備援 SD-WAN，Packet Loss 回歸 0%");
                break;
            case "task-cap":
                // 異常指標減少，磁碟擴容完成
                stats.put("metrics-abnormal", Math.max(0, stats.get("metrics-abnormal") - 1));
                stats.put("metrics-normal", stats.get("metrics-normal") + 1);
                updateDetailStatus("metrics-abnormal", "Disk Space", "擴容完成，剩餘容量 54% (500GB)");
                break;
            case "task-net":
                // 路由重啟，斷線主機全數恢復
                stats.put("hosts-down", 0);
                stats.put("hosts-running", stats.get("hosts-running") + 14);
                details.put("hosts-down", new ArrayList<>()); // 清空斷線清單
                break;
            case "task-app":
                // 閘道重啟，收斂的 18 個異常指標全數歸零，轉為健康
                stats.put("metrics-abnormal", Math.max(0, stats.get("metrics-abnormal") - 18));
                stats.put("metrics-normal", stats.get("metrics-normal") + 18);
                // 移除閘道的異常監控項目
                removeDetailItem("metrics-abnormal", "HTTP 500 Error Rate");
                removeDetailItem("metrics-abnormal", "Memory Usage");
                break;
        }

        return true;
    }

    private void updateDetailStatus(String type, String name, String newStatus) {
        List<Map<String, String>> list = details.get(type);
        if (list != null) {
            for (Map<String, String> item : list) {
                if (name.equals(item.get("name"))) {
                    item.put("status", newStatus);
                    item.put("ai_status", "已完成處置");
                }
            }
        }
    }

    private void removeDetailItem(String type, String name) {
        List<Map<String, String>> list = details.get(type);
        if (list != null) {
            list.removeIf(item -> name.equals(item.get("name")));
        }
    }
}
