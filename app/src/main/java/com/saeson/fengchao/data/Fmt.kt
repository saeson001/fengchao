package com.saeson.fengchao.data

import java.security.MessageDigest

object Fmt {
    fun sha256(s: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    /** 字节转可读大小 */
    fun bytes(b: Long): String {
        if (b <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var v = b.toDouble()
        var i = 0
        while (v >= 1024 && i < units.size - 1) {
            v /= 1024
            i++
        }
        return if (i >= 3) String.format("%.2f %s", v, units[i]) else String.format("%.0f %s", v, units[i])
    }

    /** ISO 时间转简短展示（取日期 + 时分） */
    fun shortTime(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return iso.replace('T', ' ').take(16)
    }

    /** 毫秒时间戳到剩余天数 */
    fun daysLeft(expiryMs: Long): Int {
        if (expiryMs <= 0) return Int.MAX_VALUE
        val diff = expiryMs - System.currentTimeMillis()
        return ((diff + 86_399_000L) / 86_400_000L).toInt()
    }
}
