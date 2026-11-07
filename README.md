# HyperOS 节约10s（NoWait10s）

让小米安全中心的倒计时确认按钮提前可点击，仍由用户决定是否确认。
使用现代 libxposed API 102，作用域仅为手机管家 / 安全中心
（`com.miui.securitycenter`）。

## 功能与默认设置

| 功能 | 默认 | 说明 |
| --- | --- | --- |
| 启用小米功能 | 开 | 总开关 |
| 解禁倒计时按钮 | 开 | 让符合匹配规则的确认按钮提前可点击，不自动确认 |
| 自动点下一步和允许 | 关 | 自动点击匹配的确认按钮 |
| 跳过时提示 | 关 | 自动点击成功时显示提示 |

解禁后倒计时文字可能继续显示。“按钮可点”不表示已经授予权限。
初次使用建议保持自动点击关闭。

## 使用方法

1. 安装支持 libxposed API 102 的框架，并安装本模块。
2. 在 LSPosed 启用本模块，作用域选择手机管家 / 安全中心。
3. 打开模块设置，确认服务连接成功及功能开关状态。
4. 首次启用、更新或切换同类模块后，重启目标进程或手机。
5. 同时使用原 BetterMIUI 等同类模块可能重复 Hook，测试时请只启用一个。

## 兼容性

在 Redmi K40 (HyperOS EU 1.0.10.0)、Redmi Note 12 Turbo (HyperOS EU 3.0.5.0) 中测试正常。

- 不支持只提供旧版 Xposed API 的框架。

## 来源与致谢

本项目参考 BetterMIUI 1.5（`com.mikutart.bettermiui`）的功能行为和
对其 APK 的分析，重新实现了其中的小米倒计时功能及现代 API 配置链路。
不是原作者发布的官方续作，也不是恢复出的原项目源码。
原版还包含 ColorOS 功能，本项目仅迁移小米部分。
感谢原作者的功能设计及 libxposed 项目。

## 源码

源码：https://github.com/LidoCKT/No-Wait-10s

## 界面

<a href="https://raw.githubusercontent.com/LidoCKT/No-Wait-10s/main/images/settings.png"><img src="https://raw.githubusercontent.com/LidoCKT/No-Wait-10s/main/images/settings.png" alt="模块设置界面" width="300"></a>

点击截图查看大图。

## 许可

本项目代码采用 BSD-3-Clause，详见 [LICENSE](https://github.com/LidoCKT/No-Wait-10s/blob/main/LICENSE)。
libxposed 依赖按其 Apache-2.0 许可分发，详见源码仓库的 `THIRD_PARTY_NOTICES.md`。

## 构建

参见 [BUILDING.md](BUILDING.md)。
