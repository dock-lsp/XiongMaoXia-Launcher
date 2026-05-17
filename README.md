# 熊猫侠 - Android 车机桌面启动器

一个简洁、功能丰富的 Android 车机桌面启动器应用，专为车载大屏设计。

## ✨ 功能特性

### 🏠 主界面
- 简洁的深色主题界面，适配车机大屏显示
- 实时时钟和日期显示
- 动态应用网格，自动识别已安装应用
- 地图和音乐快捷卡片

### 🎵 音乐功能
- 自动识别已安装的音乐应用（酷我、QQ音乐、网易云、酷狗等）
- 支持共存版/双开版应用识别
- 悬浮音乐卡片，实时显示播放状态
- 媒体控制：播放/暂停、上一曲、下一曲

### 🗺️ 导航功能
- 自动识别已安装的导航应用（高德、百度、腾讯地图等）
- 支持车机版导航应用
- 地图画中画悬浮功能

### 🎨 主题与壁纸
- 深色/浅色主题切换
- 内置多款精美壁纸
- 支持自定义壁纸（从相册选择）
- 实时主题切换效果

### 📱 应用管理
- 应用管理器：查看、卸载应用
- 自定义底部快捷应用（最多5个）
- 应用分类识别（导航、音乐、视频、工具等）

### 🔧 实用工具
- 音量调节面板（媒体、铃声、闹钟）
- 文件管理器入口
- 一键清理后台
- 悬浮球快捷功能

## 🛠️ 技术栈

- **语言**: Kotlin + Java
- **最低 SDK**: 26 (Android 8.0)
- **目标 SDK**: 34 (Android 14)
- **构建工具**: Gradle 8.4 + AGP 8.2.0
- **UI 框架**: Material Design Components + ViewBinding
- **架构**: 单 Activity 多功能模块

## 📦 项目结构

```
app/src/main/java/com/pandora/
├── carlauncher/
│   ├── MainActivity.kt          # 主界面
│   ├── AppRecognizer.kt        # 应用识别工具
│   ├── ThemeManager.kt         # 主题管理
│   ├── WallpaperManager.kt     # 壁纸管理
│   ├── FloatingMusicService.kt # 悬浮音乐服务
│   ├── FloatingMapService.kt   # 悬浮地图服务
│   └── ...                     # 其他功能模块
└── floating/
    ├── MusicFloatingView.java  # 悬浮音乐视图
    ├── MapFloatingView.java    # 悬浮地图视图
    └── ...                     # 其他悬浮窗组件
```

## 🚀 构建

```bash
# 克隆项目
git clone https://github.com/dock-lsp/XiongMaoXia-Launcher.git

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

## 📋 权限说明

| 权限 | 用途 |
|------|------|
| QUERY_ALL_PACKAGES | 查询已安装应用（桌面必需） |
| SYSTEM_ALERT_WINDOW | 显示悬浮窗 |
| FOREGROUND_SERVICE | 前台服务保活 |
| ACCESS_FINE_LOCATION | 车速监测 |
| BLUETOOTH_* | 蓝牙音乐功能 |
| READ_EXTERNAL_STORAGE | 文件管理功能 |

## 🎯 已完成功能

- [x] 主界面布局和主题
- [x] 应用识别和分类
- [x] 悬浮音乐卡片
- [x] 媒体控制功能
- [x] 主题中心（壁纸设置）
- [x] 深色/浅色主题切换
- [x] 音量调节面板
- [x] 自定义快捷应用
- [x] 导航应用识别
- [x] 地图画中画功能

## 📝 更新日志

### v1.10.0
- 完成悬浮音乐媒体控制功能
- 完成主题中心功能
- 优化应用识别逻辑，支持更多音乐和导航应用
- 修复多处已知问题

## 📄 许可证

MIT License

---

**开发者**: dock-lsp  
**GitHub**: https://github.com/dock-lsp/XiongMaoXia-Launcher
