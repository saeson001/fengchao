package com.saeson.fengchao.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.saeson.fengchao.R

class PanelWebViewActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_panel_web)

        val url = intent.getStringExtra("url")
        if (url == null) {
            finish()
            return
        }
        val title = intent.getStringExtra("title") ?: "面板"
        setTitle(title)

        val web = findViewById<WebView>(R.id.web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        if (savedInstanceState == null) web.loadUrl(url)
    }

    override fun onBackPressed() {
        val web = findViewById<WebView>(R.id.web)
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
