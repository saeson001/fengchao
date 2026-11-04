package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Config

class LoginActivity : AppCompatActivity() {
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvMsg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUser = findViewById(R.id.et_username)
        etPass = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvMsg = findViewById(R.id.tv_msg)
        val cbRemember = findViewById<CheckBox>(R.id.cb_remember)

        cbRemember.isChecked = true
        etUser.setText(Config.username ?: "")
        btnLogin.setOnClickListener { doLogin(cbRemember.isChecked) }
    }

    private fun doLogin(remember: Boolean) {
        val user = etUser.text.toString().trim()
        val pass = etPass.text.toString()
        if (user.isEmpty() || pass.isEmpty()) {
            tvMsg.text = getString(R.string.username) + " / " + getString(R.string.password) + " 不能为空"
            return
        }
        btnLogin.isEnabled = false
        tvMsg.text = getString(R.string.loading)
        Api.login(user, pass) { ok, msg ->
            btnLogin.isEnabled = true
            if (ok) {
                if (remember) Config.saveCredentials(user, pass)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvMsg.text = msg
            }
        }
    }
}
