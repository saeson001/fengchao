package com.saeson.fengchao.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.fragment.app.Fragment
import com.saeson.fengchao.App
import com.saeson.fengchao.R
import com.saeson.fengchao.data.Api
import com.saeson.fengchao.data.Config

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val box = LinearLayout(requireContext())
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(24), dp(20), dp(24))

        // ---- 服务器地址 ----
        box.addView(sectionLabel("v2c 管理端地址（走 CF 前置）"))
        val etBase = inputEdit(Config.baseUrl)
        box.addView(etBase)
        val btnSaveBase = button("保存地址")
        box.addView(btnSaveBase)
        btnSaveBase.setOnClickListener {
            val v = etBase.text.toString().trim()
            if (!v.startsWith("http://") && !v.startsWith("https://")) {
                Toast.makeText(requireContext(), "地址需以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Config.baseUrl = v
            Toast.makeText(requireContext(), "已保存", Toast.LENGTH_SHORT).show()
        }

        // ---- 应用锁 ----
        box.addView(sectionLabel("安全"))

        // 先声明指纹开关（swLock 的监听器里会引用它），显示顺序由 addView 决定
        val swBio = Switch(requireContext())
        swBio.text = "指纹解锁（需设备支持且已录入指纹）"
        swBio.isChecked = Config.biometricEnabled

        val swLock = Switch(requireContext())
        swLock.text = "启用应用锁（打开需 PIN）"
        swLock.isChecked = Config.lockEnabled
        box.addView(swLock)
        swLock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (Config.hasPin()) {
                    Config.lockEnabled = true
                } else {
                    // 开启前先设 PIN
                    swLock.isChecked = false
                    promptSetPin { pin ->
                        Config.setPin(pin)
                        Config.lockEnabled = true
                        swLock.isChecked = true
                        swBio.isEnabled = true
                        Toast.makeText(requireContext(), "应用锁已开启", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Config.lockEnabled = false
                Config.biometricEnabled = false
                swBio.isChecked = false
                swBio.isEnabled = false
                Toast.makeText(requireContext(), "应用锁已关闭", Toast.LENGTH_SHORT).show()
            }
        }

        swBio.isEnabled = Config.lockEnabled && Config.hasPin() && biometricAvailable()
        box.addView(swBio)
        swBio.setOnCheckedChangeListener { _, checked ->
            Config.biometricEnabled = checked
        }

        val btnChangePin = button("修改 PIN")
        box.addView(btnChangePin)
        btnChangePin.setOnClickListener {
            promptSetPin { pin ->
                Config.setPin(pin)
                Toast.makeText(requireContext(), "PIN 已更新", Toast.LENGTH_SHORT).show()
            }
        }

        // ---- 账号 ----
        box.addView(sectionLabel("账号"))
        val tvUser = TextView(requireContext())
        tvUser.text = "当前用户：${Config.username ?: "-"}"
        tvUser.textSize = 15f
        box.addView(tvUser)

        val btnLogout = button("退出登录")
        box.addView(btnLogout)
        btnLogout.setOnClickListener {
            Api.logout {
                Config.clearCredentials()
                App.unlocked = false
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
        }

        // ---- 关于 ----
        box.addView(sectionLabel("关于"))
        val tvAbout = TextView(requireContext())
        tvAbout.text = "凤巢 v1.0.0\nv2c 订阅与 VPS 随身管理\n管理通道：${Config.baseUrl}"
        tvAbout.textSize = 13f
        box.addView(tvAbout)

        return box
    }

    private fun biometricAvailable(): Boolean {
        val bm = BiometricManager.from(requireContext())
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun promptSetPin(onOk: (String) -> Unit) {
        val ctx = requireContext()
        val container = LinearLayout(ctx)
        container.setPadding(dp(24), dp(12), dp(24), dp(0))
        val et = EditText(ctx)
        et.hint = "4-8 位数字"
        et.inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        container.addView(et)

        AlertDialog.Builder(ctx)
            .setTitle("设置 PIN")
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                val pin = et.text.toString()
                if (pin.length in 4..8 && pin.all { it.isDigit() }) {
                    onOk(pin)
                } else {
                    Toast.makeText(ctx, "PIN 需为 4-8 位数字", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sectionLabel(text: String): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 13f
        tv.setPadding(0, dp(18), 0, dp(6))
        return tv
    }

    private fun inputEdit(text: String): EditText {
        val et = EditText(requireContext())
        et.setText(text)
        et.textSize = 14f
        return et
    }

    private fun button(text: String): Button {
        val b = Button(requireContext())
        b.text = text
        return b
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
