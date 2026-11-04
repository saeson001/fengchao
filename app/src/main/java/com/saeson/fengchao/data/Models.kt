package com.saeson.fengchao.data

/** v2c admin API 数据模型（字段名与响应 JSON 一致） */

data class Record(
    val id: Long = 0,
    val created_at: String = "",
    val updated_at: String = "",
    val original_links: String = "",
    val subscription_urls: String = "",
    val client_ip: String = "",
    val token: String = "",
    val node_count: Int = 0,
    val config_name: String = "",
    val yaml_size: Long = 0,
    val update_count: Int = 0
)

data class RecordDetail(
    val id: Long = 0,
    val created_at: String = "",
    val updated_at: String = "",
    val original_links: String = "",
    val subscription_urls: String = "",
    val xui_sub_url: String = "",
    val yaml_content: String = "",
    val client_ip: String = "",
    val token: String = "",
    val filename: String = "",
    val node_count: Int = 0,
    val config_name: String = "",
    val download_url: String = "",
    val update_count: Int = 0
)

data class RecordsPage(
    val records: List<Record> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val per_page: Int = 20,
    val total_pages: Int = 0
)

data class PanelClient(
    val panel: String = "",
    val panel_host: String = "",
    val inbound_remark: String = "",
    val email: String = "",
    val sub_id: String = "",
    val sub_url: String = "",
    val total: Long = 0,
    val expiry: Long = 0,
    val up: Long = 0,
    val down: Long = 0
)

data class PanelClientsResp(
    val success: Boolean = false,
    val clients: List<PanelClient> = emptyList(),
    val errors: List<String> = emptyList()
)

data class ConvertResult(
    val yaml: String = "",
    val count: Int = 0,
    val token: String = "",
    val config_name: String = "",
    val download_url: String = ""
)
