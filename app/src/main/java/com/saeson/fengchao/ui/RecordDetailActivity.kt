package com.saeson.fengchao.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Fmt
import com.saeson.fengchao.data.RecordDetail

class RecordDetailActivity : AppCompatActivity() {

    private var recordId: Long = -1
    private var detail: RecordDetail? = null
    private lateinit var progress: ProgressBar
    private lateinit var tvName: TextView
    private lateinit var tvMeta: TextView
    private lateinit var tvUrl: TextView
    private lateinit var tvYaml: TextView
    private lateinit var btnToggleYaml: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_detail)

        recordId = intent.getLongExtra("id", -1)
        val autoCopy = intent.getBooleanExtra("auto_copy", false)

        progress = findViewById(R.id.progress)
        tvName = findViewById(R.id.tv_name)
        tvMeta = findViewById(R.id.tv_meta)
        tvUrl = findViewById(R.id.tv_url)
        tvYaml = findViewById(R.id.tv_yaml)
        btnToggleYaml = findViewById(R.id.btn_toggle_yaml)

        findViewById<View>(R.id.btn_copy).setOnClickListener { copyUrl() }
        findViewById<View>(R.id.btn_share).setOnClickListener { shareUrl() }
        findViewById<View>(R.id.btn_refresh).setOnClickListener { refreshOne() }
        findViewById<View>(R.id.btn_delete).setOnClickListener { confirmDelete() }
        btnToggleYaml.setOnClickListener {
            val hidden = tvYaml.visibility == View.GONE
            tvYaml.visibility = if (hidden) View.VISIBLE else View.GONE
            btnToggleYaml.text = if (hidden) "收起 YAML" else getString(R.string.yaml_preview)
        }

        load()

        if (autoCopy) {
            // 长按列表进来的快捷复制：等详情加载完自动复制一次
            tvUrl.postDelayed({ copyUrl() }, 800)
        }
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        Api.recordDetail(recordId) { r ->
            progress.visibility = View.GONE
            if (isFinishing || isDestroyed) return@recordDetail
            if (!r.ok) {
                Toast.makeText(this, r.errMsg("加载失败"), Toast.LENGTH_LONG).show()
                return@recordDetail
            }
            val d = Api.gson.fromJson(r.body, RecordDetail::class.java)
            detail = d
            tvName.text = d.config_name.ifBlank { "记录 #${d.id}" }
            tvMeta.text = "${d.node_count} 节点 · 拉取 ${d.update_count} 次\n创建 ${Fmt.shortTime(d.created_at)} · 更新 ${Fmt.shortTime(d.updated_at)}\ntoken: ${d.token}"
            tvUrl.text = d.download_url
            tvYaml.text = d.yaml_content
        }
    }

    private fun copyUrl() {
        val url = detail?.download_url
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "还没加载完成", Toast.LENGTH_SHORT).show()
            return
        }
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("sub_url", url))
        Toast.makeText(this, "订阅链接已复制", Toast.LENGTH_SHORT).show()
    }

    private fun shareUrl() {
        val url = detail?.download_url
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "还没加载完成", Toast.LENGTH_SHORT).show()
            return
        }
        val name = detail?.config_name.orEmpty()
        val text = if (name.isBlank()) url else "$name 订阅：$url"
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(i, "分享订阅"))
    }

    private fun refreshOne() {
        progress.visibility = View.VISIBLE
        Api.refreshRecord(recordId) { r ->
            progress.visibility = View.GONE
            Toast.makeText(this, if (r.ok) r.json()?.get("message")?.asString ?: "刷新完成" else r.errMsg("刷新失败"), Toast.LENGTH_LONG).show()
            if (r.ok) load()
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("删除后公开订阅链接立即失效，且不可恢复。确定删除？")
            .setPositiveButton(R.string.delete) { _, _ ->
                Api.deleteRecord(recordId) { r ->
                    Toast.makeText(this, if (r.ok) "已删除" else r.errMsg("删除失败"), Toast.LENGTH_SHORT).show()
                    if (r.ok) finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
