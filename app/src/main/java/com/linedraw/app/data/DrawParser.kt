package com.linedraw.app.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class DrawParser {
    fun parse(html: String, now: Long = System.currentTimeMillis()): List<Draw> {
        require(html.toByteArray().size <= 2_000_000) { "來源頁面過大" }
        val doc = Jsoup.parse(html)
        val stores = doc.select("#page-draws .draw-store")
        require(stores.isNotEmpty()) { "找不到抽選區；保留上次資料" }
        val result = mutableListOf<Draw>()
        val sourceIds = mutableSetOf<String>()
        val derivedIds = mutableMapOf<String, Int>()
        stores.forEach { store ->
            val name = store.selectFirst(".draw-store-name")?.text()?.trim().orEmpty()
            val city = store.attr("data-draw-city").ifBlank { "未分類" }
            val label = store.selectFirst(".draw-start")?.text().orEmpty()
            require(name.isNotBlank()) { "店家名稱缺漏" }
            val (start, end) = DrawSchedule.parse(label)
            val rows = store.select(".draw-item")
            require(rows.isNotEmpty()) { "店家抽選資料不完整" }
            rows.forEach rowLoop@{ row ->
                // 原站也列出純販售商品；只略過沒有任何連結或操作標記的展示列。
                val actionable = row.hasAttr("data-draw-id") || row.hasAttr("data-draw-href") ||
                    row.hasAttr("data-draw-url") ||
                    row.hasClass("draw-item-clickable") || row.hasAttr("onclick") || row.hasAttr("onkeydown") ||
                    row.attr("role") == "link" || row.select("a[href], button").isNotEmpty()
                val product = row.selectFirst(".draw-product")?.text()?.trim().orEmpty()
                if (!actionable && product.isNotEmpty() && product.length < 500) return@rowLoop
                // 連結才是抽選列可執行的本體（原站自己也用 href 當備援），所以只要求連結存在。
                val url = linkOf(row)
                require(url.isNotEmpty()) { "部分抽選列缺漏身份或網址" }
                // 原站少數列只有 data-draw-href、沒有 data-draw-id（例如 2026/10/08 的「來玩聚-楠梓家樂福」）。
                // ID 不是使用者可見的資料，缺漏時由「店家＋連結＋商品」推導穩定識別碼，不要讓整頁同步失敗。
                val id = row.attr("data-draw-id").trim().ifBlank {
                    val base = derivedSourceId(name, url, product)
                    val occurrence = (derivedIds[base] ?: 0) + 1
                    derivedIds[base] = occurrence
                    if (occurrence == 1) base else "$base#$occurrence"
                }
                require(id.isNotEmpty() && sourceIds.add(id)) { "來源 ID 空白或重複" }
                require(product.isNotEmpty() && product.length < 500 && name.length < 200) { "商品資料不完整" }
                val key = LinkPolicy.activityKey(url, name, start?.toString() ?: label)
                result += Draw("$id:${digest(url).take(16)}", id, key, name, city, product, url, label, start, end, result.size, syncedAt = now)
            }
        }
        require(result.size in 1..5000) { "抽選筆數異常" }
        result.groupBy { it.activityKey }.values.forEach { rows ->
            require(rows.map { it.store }.distinct().size == 1) { "同一活動的店家描述衝突" }
        }
        return result
    }

    /** 原站先後用過 data-draw-href 與 data-draw-url；兩者都沒有時才退回列內的 a[href]（原站 JS 也是這樣找連結）。 */
    private fun linkOf(row: Element): String =
        row.attr("data-draw-href").trim()
            .ifBlank { row.attr("data-draw-url").trim() }
            .ifBlank { row.selectFirst("a[href]")?.attr("href")?.trim().orEmpty() }

    /** 只在原站未提供 data-draw-id 時使用；同一組合重複出現時以 #n 區分，維持穩定且唯一的列識別。 */
    private fun derivedSourceId(store: String, url: String, product: String): String =
        "derived-" + digest("$store\n$url\n$product").take(24)

}
