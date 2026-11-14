package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Config

/**
 * 面板快捷入口：应用内 WebView 打开。
 * 面板清单从 v2c 服务端 global-config 拉取（vps_traffic_sources），
 * 服务端增删面板无需发新版 APK；v2c 管理端入口由 baseUrl 推导。
 */
class PanelFragment : Fragment() {

    private data class Entry(val title: String, val sub: String, val url: String)

    private lateinit var box: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        box = LinearLayout(requireContext())
        box.orientation = LinearLayout.VERTICAL
        return box
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        load()
    }

    private fun loadingRow(text: String): TextView {
        val tv = TextView(requireContext())
        tv.setPadding(dp(16), dp(14), dp(16), dp(14))
        tv.text = text
        tv.textSize = 14f
        return tv
    }

    private fun load() {
        box.removeAllViews()
        box.addView(loadingRow("正在从服务端拉取面板配置..."))
        Api.globalConfig { r ->
            if (!isAdded) return@globalConfig
            box.removeAllViews()
            if (!r.ok) {
                val tv = loadingRow("拉取失败：${r.errMsg("global-config 请求异常")}\n点击重试")
                tv.setOnClickListener { load() }
                box.addView(tv)
                return@globalConfig
            }
            val entries = mutableListOf(
                Entry("v2c 网页管理端", "订阅配置中心（走 CF）", Config.baseUrl.trimEnd('/') + "/manage")
            )
            try {
                val arr = r.json()?.getAsJsonArray("vps_traffic_sources")
                if (arr != null) {
                    for (el in arr) {
                        val o = el.asJsonObject
                        val url = o.get("url")?.asString ?: continue
                        val host = try {
                            java.net.URI(url).host ?: url
                        } catch (e: Exception) {
                            url
                        }
                        entries.add(Entry("$host 3x-ui 面板", url, normalize(url)))
                    }
                }
            } catch (e: Exception) {
                // 配置结构异常时保留 v2c 入口
            }
            if (entries.size == 1) {
                box.addView(loadingRow("服务端未配置任何面板"))
            }
            for (e in entries) addEntryRow(e)
        }
    }

    /** 面板 URL 补全尾部斜杠（3x-ui 隐蔽路径必须以 / 结尾才能进入） */
    private fun normalize(url: String): String {
        val u = url.trim().trimEnd('/')
        return "$u/"
    }

    private fun addEntryRow(e: Entry) {
        val item = LinearLayout(requireContext())
        item.orientation = LinearLayout.VERTICAL
        item.setPadding(dp(16), dp(12), dp(16), dp(12))

        val tvTitle = TextView(requireContext())
        tvTitle.text = e.title
        tvTitle.textSize = 15f
        item.addView(tvTitle)

        val tvSub = TextView(requireContext())
        tvSub.text = e.sub
        tvSub.textSize = 13f
        item.addView(tvSub)

        item.setOnClickListener {
            val i = Intent(requireContext(), PanelWebViewActivity::class.java)
            i.putExtra("url", e.url)
            i.putExtra("title", e.title)
            startActivity(i)
        }
        box.addView(item)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
