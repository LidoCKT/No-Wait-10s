# 构建

需要 Python 3、JDK 17 或更新版本，以及 Android SDK：

- Android SDK Platform 36
- Android SDK Build-Tools 36.1.0

设置 `JAVA_HOME` 和 `ANDROID_SDK_ROOT`，在仓库根目录运行：

```sh
python scripts/build.py
```

脚本从 Maven Central 获取固定版本 libxposed 102.0.0，并核对 SHA-256。
API 仅供编译，service 和 interface 打入 APK。
产物：`dist/NoWait10s-0.1.0-unsigned.apk`，已完成 zipalign，需要自行签名才能安装。

示例（将路径和别名替换为自己的）：

```sh
apksigner sign --ks /private/path/release.jks --ks-key-alias nowait10s --out NoWait10s-0.1.0.apk dist/NoWait10s-0.1.0-unsigned.apk
apksigner verify --verbose NoWait10s-0.1.0.apk
```

请保留自己的签名密钥；后续版本必须使用相同证书才能覆盖安装。
不要将密钥或密码上传到仓库。

此构建流程不需要反编译工具、原模块 APK 或手机日志。
