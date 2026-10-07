# 0.1.3-standalone 驗證紀錄

日期：2026-10-08。比較基底：0.1.2-standalone。

## 變更

- 修正 Funbox 來源解析中斷：原站 2026/10/08 版本的「來玩聚-楠梓家樂福」有 9 列只有 `data-draw-href`、沒有 `data-draw-id`，舊版 `DrawParser` 會以「部分抽選列缺漏身份或網址」中止整頁同步，導致 40 店 431 筆全部同步失敗、只顯示上次資料。
- 缺少 `data-draw-id` 時，改以「店家＋連結＋商品」推導穩定識別碼（`derived-<sha256 前 24 碼>`）；同一組合重複出現時以 `#n` 區分。推導值只依內容計算，跨次同步一致，本機紀錄與接續邏輯可延續。
- 抽選列連結解析改為 `data-draw-href` → `data-draw-url` → 列內 `a[href]`（原站內嵌 JS 也是這樣找連結，且資料檔曾以 `data-draw-url` 輸出）。
- 安全底線不變：完全沒有連結的列、重複的原站 ID、非允許網域（`lin.ee`／`liff.line.me`／`line.me` 以外的 https）仍整頁拒絕，寧可保留上次有效清單。
- `versionCode` 3 → 4，`versionName` 0.1.2-standalone → **0.1.3-standalone**；隱私說明版本字串同步更新（內容未變）。
- 抽選引擎的派送、辨識、接續與批次規則未變更；本次只影響來源頁面解析。

## 驗證結果

| 檢查 | 結果 |
| --- | --- |
| JVM／Robolectric 單元測試 | **200 項通過，0 失敗、0 錯誤、0 略過**（16 個測試類別） |
| 新增回歸測試 | `ParserTest` 23 項（含 4 項針對缺 ID 列、推導識別碼唯一性、舊連結屬性、原站節錄 fixture） |
| 缺陷涵蓋確認 | 將 `DrawParser.kt` 換回修復前版本重跑 `ParserTest`：`Tests run: 23, Failures: 4`，確認新測試確實抓到此缺陷 |
| 立即來源驗證 | 以與 `DrawParser.kt` 逐行對應的 jsoup 版本解析原站當下頁面：431 列／40 店全部解析成功，9 列使用推導識別碼，`rowKey` 全不重複，連結全在允許網域 |
| 既有快照未回歸 | `funbox-20260922` 20 列、`funbox-20260924` 623 列、`funbox-20261002` 1289 列，與 0.1.2 相同 |
| 邊界案例 | 只有 `data-draw-id`／只有 `onclick`／只有 `role=link`／`http://`／非 LINE 網域／有連結無商品 → 仍整頁拒絕；純展示列（採取上架販售）仍略過 |
| 建置 | `assembleDebug`、`assemblePilot`、`assembleRelease` 皆 BUILD SUCCESSFUL（AGP 9.3.0、Gradle 9.5.0、Daemon JDK 25、compileSdk 37、build-tools 36.0.0） |
| Release 設定 | `debuggable=false`、`BuildConfig.DEBUG=false`、`minifyEnabled=false`（未開 R8）、v3 簽章 |

未執行：模擬器／實機 instrumentation 測試（`connectedDebugAndroidTest`）、各品牌 Android 無障礙設定選單與受限制設定實機確認。這些沿用 0.1.2 的限制，未因本次解析修正而改變。

## 已知來源端問題（App 端已可正常處理）

原站「來玩聚-楠梓家樂福」該 9 列在原站網頁上**點擊沒有反應**：原站 `openDrawRow` 開頭是 `if (!href || !id) return;`，而這 9 列同時缺少 `data-draw-id` 與 `onclick`。App 端已能在清單中顯示並抽選這些列；原站端仍建議補上 `data-draw-id`（或放寬該守門條件）。

## APK

- 檔名：`LineDraw-0.1.3-standalone.apk`
- package：`com.linedraw.standalone`，versionCode 4，minSdk 31／targetSdk 36／compileSdk 37
- 簽章：沿用既有 debug 測試憑證（憑證 SHA-256 `13f5e26b…20aea5`），可覆蓋 0.1.0～0.1.2 的既有安裝並保留本機紀錄；另提供以專用 release 金鑰簽署的版本，供全新安裝使用
- 打包資產：LICENSE、NOTICE、PRIVACY、QUICK_START 與倉庫文件逐位元相同

本輪驗證在 macOS 以命令列工具鏈完成（Android SDK platform 37.0／build-tools 36.0.0／Gradle 9.5.0／Temurin JDK 25），未使用 Android Studio。
