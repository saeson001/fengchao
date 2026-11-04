package com.saeson.fengchao.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api

class NewRecordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_record)

        val etInput = findViewById<EditText>(R.id.et_input)
        val etName = findViewById<EditText>(R.id.et_name)
        val btnCreate = findViewById<Button>(R.id.btn_create)
        val tvMsg = findViewById<TextView>(R.id.tv_msg)

        btnCreate.setOnClickListener {
            val input = etInput.text.toString().trim()
            if (input.isEmpty()) {
                tvMsg.text = "请粘贴订阅 URL 或 vless:// 链接"
                return@setOnClickListener
            }
            btnCreate.isEnabled = false
            tvMsg.text = getString(R.string.loading)
            Api.convert(input, etName.text.toString()) { r ->
                btnCreate.isEnabled = true
                if (r.ok) {
                    val d = Api.gson.fromJson(r.body, com.saeson.fengchao.data.ConvertResult::class.java)
                    Toast.makeText(this, "创建成功：${d.config_name.ifBlank { d.token }}（${d.count} 节点）", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    tvMsg.text = r.errMsg("创建失败")
                }
            }
        }
    }
}
