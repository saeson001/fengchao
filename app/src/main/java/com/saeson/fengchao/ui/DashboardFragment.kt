package com.saeson.fengchao.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Fmt
import com.saeson.fengchao.data.PanelClient
import com.saeson.fengchao.data.PanelClientsResp

/**
 * VPS 看板（只读）：数据来自 panel-clients（全部已配置面板的启用客户端）
 * 与 global-config（面板清单，用于判断在线/离线）。
 * 顶部 Spinner 切换面板（185 / 147），下拉刷新仅在列表位于顶部时生效。
 */
class DashboardFragment : Fragment() {

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var scroll: android.widget.ScrollView
    private lateinit var spinner: Spinner
    private lateinit var progress: ProgressBar
    private lateinit var container: LinearLayout
    private lateinit var tvEmpty: TextView

    // 最近一次成功加载的缓存
    private var cachedHosts: List<String> = emptyList()      // 展示顺序
    private var cachedClients: Map<String, List<PanelClient>> = emptyMap()
    private var cachedBadPanels: Set<String> = emptySet()
    private var cachedErrors: List<String> = emptyList()

    private var selectedHost: String? = null   // 记住当前选择，刷新后还原

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipe = view.findViewById(R.id.swipe)
        scroll = view.findViewById(R.id.scroll)
        spinner = view.findViewById(R.id.spinner_panel)
        progress = view.findViewById(R.id.progress)
        container = view.findViewById(R.id.container)
        tvEmpty = view.findViewById(R.id.tv_empty)

        // 仅当列表滚到顶部时才允许下拉刷新
        scroll.viewTreeObserver.addOnScrollChangedListener {
            swipe.isEnabled = !scroll.canScrollVertically(-1)
        }
        scroll.setOnScrollChangeListener { _, _, _, _, _ ->
            swipe.isEnabled = !scroll.canScrollVertically(-1)
        }

        swipe.setOnRefreshListener { load() }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val host = cachedHosts.getOrNull(pos) ?: return
                if (host != selectedHost) {
                    selectedHost = host
                    renderSelected()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        load()
    }

    override fun onResume() {
        super.onResume()
        if (container.childCount == 0) load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE

        var gcfgSources: List<String>? = null
        var clientsResp: Api.Resp? = null

        fun render() {
            val sources = gcfgSources
            val resp = clientsResp
            if (sources == null || resp == null) return
            if (!isAdded) return
            progress.visibility = View.GONE
            swipe.isRefreshing = false

            if (!resp.ok) {
                Toast.makeText(requireContext(), resp.errMsg("看板数据加载失败"), Toast.LENGTH_LONG).show()
                return
            }
            val data = Api.gson.fromJson(resp.body, PanelClientsResp::class.java)
            val badPanels = data.errors.mapNotNull { e ->
                e.substringAfterLast(": ", "").ifBlank { null }
            }.toSet()

            val grouped = data.clients.groupBy { it.panel_host }
            // 展示顺序：global-config 源顺序优先，其余按出现顺序追加
            val hosts = (sources.filter { grouped.containsKey(it) } +
                    grouped.keys.filter { it !in sources }).distinct()

            cachedHosts = hosts
            cachedClients = grouped
            cachedBadPanels = badPanels
            cachedErrors = data.errors

            if (data.clients.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                container.removeAllViews()
                return
            }
            tvEmpty.visibility = View.GONE

            setupSpinner()
            if (selectedHost == null || selectedHost !in hosts) selectedHost = hosts.firstOrNull()
            val idx = hosts.indexOf(selectedHost)
            if (idx >= 0 && spinner.selectedItemPosition != idx) {
                spinner.setSelection(idx, false)
            }
            renderSelected()
        }

        Api.globalConfig { r ->
            if (r.ok) {
                val arr = r.json()?.getAsJsonArray("vps_traffic_sources")
                gcfgSources = arr?.mapNotNull { el ->
                    val o = el.asJsonObject
                    val u = o.get("url")?.asString.orEmpty()
                    try {
                        java.net.URI(u).host ?: u
                    } catch (e: Exception) {
                        u
                    }
                }
            }
            render()
        }

        Api.panelClients { r ->
            clientsResp = r
            render()
        }
    }

    private fun setupSpinner() {
        val labels = cachedHosts.map { h ->
            val n = cachedClients[h]?.size ?: 0
            "$h（$n 个客户端）"
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /** 按当前 selectedHost 重渲染列表（不重新拉接口） */
    private fun renderSelected() {
        if (!isAdded) return
        container.removeAllViews()
        val host = selectedHost ?: return

        addStatusRow(host, host !in cachedBadPanels)

        for (e in cachedErrors) addNoteRow("面板异常：$e")

        val clients = cachedClients[host].orEmpty().sortedByDescending { it.up + it.down }
        for (c in clients) addClientRow(c)
    }

    private fun addStatusRow(host: String, online: Boolean) {
        val ctx = requireContext()
        val tv = TextView(ctx)
        tv.setPadding(dp(16), dp(10), dp(16), dp(10))
        tv.text = if (online) "● $host  在线" else "● $host  异常/离线"
        tv.setTextColor(ctx.getColor(if (online) R.color.state_ok else R.color.state_bad))
        tv.textSize = 15f
        container.addView(tv)
    }

    private fun addNoteRow(text: String) {
        val tv = TextView(requireContext())
        tv.setPadding(dp(16), dp(4), dp(16), dp(4))
        tv.text = text
        tv.textSize = 13f
        tv.setTextColor(requireContext().getColor(R.color.state_bad))
        container.addView(tv)
    }

    private fun addClientRow(c: PanelClient) {
        val ctx = requireContext()
        val box = LinearLayout(ctx)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(16), dp(6), dp(16), dp(6))

        val used = c.up + c.down
        val title = TextView(ctx)
        title.text = "${c.email}  [${c.inbound_remark}]"
        title.textSize = 15f
        box.addView(title)

        val usage = TextView(ctx)
        val totalTxt = if (c.total > 0) Fmt.bytes(c.total) else "不限量"
        usage.text = "已用 ${Fmt.bytes(used)} / $totalTxt"
        usage.textSize = 13f
        box.addView(usage)

        if (c.total > 0) {
            val bar = android.widget.ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal)
            bar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6))
            bar.max = 10000
            bar.progress = ((used.toDouble() / c.total) * 10000).toInt().coerceIn(0, 10000)
            box.addView(bar)
        }

        val expiry = TextView(ctx)
        if (c.expiry > 0) {
            val days = Fmt.daysLeft(c.expiry)
            expiry.text = if (days >= 0) "剩余 $days 天" else "已到期"
            expiry.setTextColor(ctx.getColor(if (days in 0..7) R.color.state_bad else R.color.state_ok))
        } else {
            expiry.text = "永不过期"
        }
        expiry.textSize = 13f
        box.addView(expiry)

        container.addView(box)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
