# Anti-Gravity 懶人包工作規則

## 安全原則
- 嚴禁複製、記錄或提交 NotebookLM 的 cookie、token，或 `notebooks.json`、筆記本 ID 清單。
- 嚴禁提交 API key、GitHub token、Firebase Admin SDK 憑證。
- 收工時先檢查 `git status` 與 `git diff`，只 stage 本次相關檔案，不使用 `git add .`。
- 不儲存學生真名，一律使用班級代號與座號。

## 開工 / 收工流程
### 開工
1. 讀取專案根目錄的 `ANTIGRAVITY.md` 或同等規則檔。
2. 讀取 Obsidian 專案駕駛艙。
3. 執行 `git status` 與最近 commit 檢查。
4. 回報目前狀態與建議下一步。
5. 不自動 pull、commit 或 push。

### 收工
1. 檢查是否有敏感資料（如 API key、token、憑證、學生真名）。
2. 更新 Obsidian 專案駕駛艙（完成事項、下一步、踩坑）。
3. 只有固定規則或路徑改變時才更新 `ANTIGRAVITY.md`。
4. 執行 `git status` 與 diff 檢查。
5. 只 stage 本次相關檔案，不使用無差別 `git add .`。
6. 產生 commit message，確認後 commit / push。
7. 回報 Obsidian、規則檔與 GitHub 同步結果。

### 新專案初始化
- 詢問：專案名稱、用途、工作資料夾、是否建立 GitHub repo、是否公開/私有、是否需要 GitHub Pages / Firebase / 其他部署、Obsidian vault 與專案駕駛艙位置。
- 建立或補齊：`ANTIGRAVITY.md`、`README.md`、`.gitignore`、Git repo、GitHub repo、Obsidian 專案駕駛艙。
- 盤點已存在檔案，只補缺口，不覆蓋既有設定。

## 語言與環境
- 回應使用繁體中文（Taiwan）。
- 涉及檔案操作時，回報完整產出位置。
- 在 Windows 環境下使用 PowerShell 語法，執行 npm 命令時改用 `npm.cmd` 或 `npx.cmd`，避免 execution policy 限制。
- 若 Windows 發生 Python/nlm 編碼錯誤，可在 PowerShell 中設定 `$env:PYTHONIOENCODING = "utf-8"`。
