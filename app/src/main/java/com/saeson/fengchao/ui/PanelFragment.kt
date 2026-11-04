package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.saeson.fengchao.R

/**
 * 面板快捷入口：应用内 WebView 打开。
 * 185 直连被墙，其 3x-ui 面板标注可能无法打开。
 */
class PanelFragment : Fragment() {

    private data class Entry(val title: String, val sub: String, val url: String)

    private val entries = listOf(
        Entry("v2c 网页管理端", "走 CF 前置，手机可用", "http://3x.820804.xyz:2095/manage"),
        Entry("140 3x-ui 面板", "147.79.20.66:8284（直连）", "http://147.79.20.66:8284/mGo8HWHf/"),
        Entry("185 3x-ui 面板", "IP 被墙，手机大概率打不开", "http://185.115.207.27:8284/")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val box = LinearLayout(requireContext())
        box.orientation = LinearLayout.VERTICAL

        for (e in entries) {
            val item = layoutInflater.inflate(R.layout.item_panel, box, false)
            val tvTitle = item.findViewById<TextView>(R.id.tv_title)
            val tvSub = item.findViewById<TextView>(R.id.tv_sub)
            tvTitle.text = e.title
            tvSub.text = e.sub
            item.setOnClickListener {
                val i = Intent(requireContext(), PanelWebViewActivity::class.java)
                i.putExtra("url", e.url)
                i.putExtra("title", e.title)
                startActivity(i)
            }
            box.addView(item)
        }
        return box
    }
}
