package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Fmt
import com.saeson.fengchao.data.Record
import com.saeson.fengchao.data.RecordsPage

class RecordsFragment : Fragment() {

    private lateinit var adapter: RecordAdapter
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnRefreshAll: TextView
    private val records = mutableListOf<Record>()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_records, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = RecordAdapter(records,
            onItemClick = { r -> openDetail(r.id, false) })

        rv = view.findViewById(R.id.rv_records)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        swipe = view.findViewById(R.id.swipe)
        swipe.setOnRefreshListener { load() }
        progress = view.findViewById(R.id.progress)
        tvEmpty = view.findViewById(R.id.tv_empty)
        etSearch = view.findViewById(R.id.et_search)
        btnRefreshAll = view.findViewById(R.id.btn_refresh_all)

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable { load() }.also { handler.postDelayed(it, 400) }
            }
        })

        btnRefreshAll.setOnClickListener {
            btnRefreshAll.isEnabled = false
            Toast.makeText(requireContext(), "全部刷新中，可能需要一些时间…", Toast.LENGTH_SHORT).show()
            Api.refreshAll { r ->
                if (view.isAttachedToWindow) btnRefreshAll.isEnabled = true
                val ctx = context ?: return@refreshAll
                if (r.ok) {
                    Toast.makeText(ctx, "全部刷新完成", Toast.LENGTH_SHORT).show()
                    load()
                } else {
                    Toast.makeText(ctx, r.errMsg("刷新失败"), Toast.LENGTH_LONG).show()
                }
            }
        }

        view.findViewById<FloatingActionButton>(R.id.fab_new).setOnClickListener {
            startActivity(Intent(requireContext(), NewRecordActivity::class.java))
        }

        load()
    }

    private fun openDetail(id: Long, autoCopy: Boolean) {
        val i = Intent(requireContext(), RecordDetailActivity::class.java)
        i.putExtra("id", id)
        i.putExtra("auto_copy", autoCopy)
        startActivity(i)
    }

    private var firstResume = true

    override fun onResume() {
        super.onResume()
        if (firstResume) {
            firstResume = false // onViewCreated 已加载过，避免首帧双重请求
        } else if (this::adapter.isInitialized) {
            load()
        }
    }

    fun load() {
        if (!this::progress.isInitialized) return
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        Api.records(1, 100, etSearch.text.toString()) { r ->
            if (!isAdded) return@records
            progress.visibility = View.GONE
            swipe.isRefreshing = false
            if (!r.ok) {
                Toast.makeText(requireContext(), r.errMsg("加载失败"), Toast.LENGTH_LONG).show()
                return@records
            }
            val page = Api.gson.fromJson(r.body, RecordsPage::class.java)
            records.clear()
            records.addAll(page.records)
            adapter.notifyDataSetChanged()
            tvEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}

class RecordAdapter(
    private val items: List<Record>,
    private val onItemClick: (Record) -> Unit
) : RecyclerView.Adapter<RecordAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tv_name)
        val tvMeta: TextView = v.findViewById(R.id.tv_meta)
        val tvLinks: TextView = v.findViewById(R.id.tv_links)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_record, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val r = items[pos]
        h.tvName.text = r.config_name.ifBlank { "记录 #${r.id}" }
        h.tvMeta.text = "${r.node_count} 节点 · 拉取 ${r.update_count} 次 · ${Fmt.shortTime(r.created_at)}"
        val src = r.subscription_urls.ifBlank { r.original_links }
        h.tvLinks.text = if (src.length > 80) src.take(80) + "…" else src
        h.itemView.setOnClickListener { onItemClick(r) }
    }
}
