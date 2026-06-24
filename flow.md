這是一個非常棒的系統工程問題。當你將原本簡單的 4 個元素擴充到 7 個元素（加入 I/O Wait、TCP、Load Avg）後，單純的「讀取 -> 顯示」流程會變複雜，因為你需要處理**「時間差計算 (Delta Calculation)」**。

這裡為你規劃完整的**「監控鍊金術系統流程圖」**，分為四個階段：**採集 (Mining)** -> **精煉 (Refining)** -> **融合 (Synthesizing)** -> **展示 (Displaying)**。

---

### 第一階段：採集 (Mining - Linux SNMP)

**目標：** 確保 Linux 乖乖吐出原始數據。

1. **確認 OID 來源：**
* 確保 `snmpd.conf` 有權限讀取以下標準 MIBs：
* `UCD-SNMP-MIB` (用於 System CPU, I/O Wait, Load Avg)
* `HOST-RESOURCES-MIB` (用於 Process 列表)
* `TCP-MIB` (用於 TCP 連線數)




2. **配置 `extend` (如果需要)：**
* 如同之前討論，Process 級別的精確 CPU/Mem 可能需要 shell script 輔助。



---

### 第二階段：精煉 (Refining - Java Spring Boot)

**目標：** 把 SNMP 的「原始數字」變成人類和 AI 看得懂的「百分比」。
**這是最關鍵的一步**，因為像 CPU 和 I/O Wait 在 SNMP 中是「累計計數器 (Counter)」，你必須做**微分運算**。

**Java 內部的處理流程：**

1. **第一次採樣 (T1):**
* `snmp.get(ssCpuRawIdle)` -> 得到 10000
* `snmp.get(ssCpuRawWait)` -> 得到 500


2. **等待 (Sleep):**
* 暫停 1 秒 (`Thread.sleep(1000)`)。


3. **第二次採樣 (T2):**
* `snmp.get(ssCpuRawIdle)` -> 得到 10090
* `snmp.get(ssCpuRawWait)` -> 得到 520


4. **計算 Delta (變化量):**
* 總時間差 (Total Ticks) = (User + Nice + System + Idle + Wait...) 的兩次差值總和。
* **I/O Wait %** = `(520 - 500) / Total_Delta * 100`
* **CPU Usage %** = `100 - ((Idle_T2 - Idle_T1) / Total_Delta * 100)`



> **💡 程式碼提示：** 你需要一個 `Map<String, Long> previousValues` 來暫存上一次的數據，或者簡單地在 function 內做 `sleep` (雖然會卡住執行緒，但做 MVP 最簡單)。

---

### 第三階段：融合 (Synthesizing - Prompt Engineering)

**目標：** 將 7 個數據組裝成一段 AI 能理解的故事。

你需要將計算好的數據填入一個更豐富的 Prompt Template。

**Java 建構 Prompt 的範例：**

```java
// 這些變數都是你在第二階段算出來的
String prompt = """
You are a Senior Site Reliability Engineer (SRE).
Analyze the following server telemetry to diagnose the system health.

--- SYSTEM METRICS ---
[A] System CPU Usage: %.2f%%
[B] Target Process CPU: %.2f%%
[C] System Memory: %.2f%%
[D] Target Process Mem: %d MB
[E] Disk I/O Wait: %.2f%%  <-- 新加入
[F] Active TCP Connections: %d  <-- 新加入
[I] Load Average (1min): %.2f  <-- 新加入

--- CONTEXT ---
The server has %d CPU cores. (So Load Avg > %d indicates saturation).

--- TASK ---
1. Identify the primary bottleneck (CPU, Memory, Disk I/O, or Network).
2. Determine if the Target Process is the culprit or a victim.
3. Output strictly in JSON format.
""".formatted(sysCpu, procCpu, sysMem, procMem, ioWait, tcpConn, loadAvg, coreCount, coreCount);

```

---

### 第四階段：展示 (Displaying - HTML/JS)

**目標：** 讓使用者看到「儀表板」與「AI 診斷」。

1. **前端請求：** JS 呼叫 `/analyze` API。
2. **後端執行：** Spring Boot 跑完上述 Step 2 & 3，等待 Ollama 回傳 JSON。
3. **前端渲染：**
* **左側 (數據)：** 顯示 7 個元素的即時數值 (可以用 Progress Bar 顯示 %)。
* **右側 (AI)：** 彈出原本寫好的 `result-overlay`，顯示 AI 的結論 (如：「硬碟 I/O 阻塞」)。



---

### 完整的 Sequence Diagram (文字版)

```text
User (Browser)      Spring Boot (Java)      Linux Server (SNMP)      Ollama (AI)
      |                    |                        |                    |
      |--- 1. 點擊"診斷" -->|                        |                    |
      |                    |--- 2. Get OID (T1) --->|                    |
      |                    |<-- Raw Counters -------|                    |
      |                    | (Wait 1 sec)           |                    |
      |                    |--- 3. Get OID (T2) --->|                    |
      |                    |<-- Raw Counters -------|                    |
      |                    |                        |                    |
      |                    |--- 4. 計算 Delta % --->|                    |
      |                    |   (算出 CPU, IO 等)    |                    |
      |                    |                        |                    |
      |                    |--- 5. 組合 Prompt ---->|                    |
      |                    |----------------------->|--- 6. 推理 ------>|
      |                    |<-----------------------|<-- JSON Result ---|
      |                    |                        |                    |
      |<-- 7. 回傳 JSON ---|                        |                    |
      |                    |                        |                    |
      |--- 8. 顯示彈窗 ---->|                        |                    |

```

### 給開發者的實作建議

**不要一次做完所有元素！** 建議依照這個順序迭代：

1. **v1.0 (現狀):** 只有 CPU/Mem。
2. **v1.1 (加入 I/O Wait):** 這最難，因為要算 Delta。先把這個邏輯寫好，因為它是判斷「假死」的關鍵。
3. **v1.2 (加入 Load Avg & TCP):** 這兩個通常是「瞬時值 (Gauge)」，直接讀 SNMP 就好，不用算 Delta，實作最簡單。

你想先看 **v1.1 (如何用 Java 算 SNMP Delta)** 的程式碼範例嗎？