package com.saeson.fengchao.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.saeson.fengchao.App
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Config
import java.util.concurrent.Executor

class LockActivity : AppCompatActivity() {
    private lateinit var etPin: EditText
    private lateinit var btnUnlock: Button
    private lateinit var btnBiometric: ImageButton
    private lateinit var tvMsg: TextView
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        etPin = findViewById(R.id.et_pin)
        btnUnlock = findViewById(R.id.btn_unlock)
        btnBiometric = findViewById(R.id.btn_biometric)
        tvMsg = findViewById(R.id.tv_lock_msg)
        executor = ContextCompat.getMainExecutor(this)

        btnUnlock.setOnClickListener { tryUnlock(etPin.text.toString()) }

        val bm = BiometricManager.from(this)
        val canBio = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
        btnBiometric.visibility =
            if (Config.biometricEnabled && canBio && Config.hasPin()) View.VISIBLE else View.GONE
        btnBiometric.setOnClickListener { showBiometric() }

        if (btnBiometric.visibility == View.VISIBLE) showBiometric()
    }

    private fun showBiometric() {
        val prompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    tvMsg.text = errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("解锁凤巢")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    private fun tryUnlock(pin: String) {
        if (pin.isEmpty()) {
            tvMsg.text = "请输入 PIN"
            return
        }
        if (Config.checkPin(pin)) {
            unlock()
        } else {
            tvMsg.text = "PIN 错误"
            etPin.selectAll()
        }
    }

    private fun unlock() {
        App.unlocked = true
        Toast.makeText(this, "已解锁", Toast.LENGTH_SHORT).show()
        finish()
    }
}
