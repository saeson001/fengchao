# ADR-0002: 凤巢选原生 Kotlin 而非 WebView 壳

日期：2026-11-04
状态：已接受

## 背景

需要 Android 端管理 v2c 订阅与 VPS。可选：原生 Kotlin、WebView 壳（v2c 管理页做移动端适配 + 壳补原生能力）、移动端 PWA。

## 决策

原生 Kotlin 应用，直接调用 v2c admin API，服务端零改动。

## 理由

- v2c admin API 已齐全（登录/记录/刷新/全局配置/面板客户端流量），原生端无需服务端配合
- 本机构建环境就绪（JDK 17 + Gradle 8.7 + Android SDK 34），原生构建无额外安装成本
- 复制/分享是核心诉求，原生 ClipboardManager 与系统分享面板体验最好
- 功能页面少（登录/列表/详情/新建/看板/面板/设置），原生工作量可控
- WebView 壳的优势（UI 免重发更新）对低频使用的个人工具价值有限

## 后果

- 界面改动需重新打包发版
- 需自行维护 OkHttp 会话与 401 自动重登（v2c 用 session cookie 鉴权）
