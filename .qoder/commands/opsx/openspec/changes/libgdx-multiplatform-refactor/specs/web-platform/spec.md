## ADDED Requirements

### Requirement: GWT compilation target
系统 SHALL 使用 libGDX GWT 后端将游戏编译为 HTML5/JavaScript，可在现代浏览器中运行。

#### Scenario: Build HTML5 version
- **WHEN** 运行 `./gradlew :html:dist`
- **THEN** 输出 `html/build/dist/` 目录包含可部署的 HTML/JS/资源文件

#### Scenario: Run in browser
- **WHEN** 用户在 Chrome/Firefox/Safari 中打开游戏 URL
- **THEN** 游戏加载并显示主菜单，可正常开始游戏

### Requirement: Chinese font support in HTML5
系统 SHALL 在 HTML5 版本中正确显示所有中文文本，使用预生成的 BitmapFont（仅包含实际使用的汉字）。

#### Scenario: Display Chinese text on web
- **WHEN** 游戏在浏览器中加载主菜单
- **THEN** 所有按钮文字（"新游戏"、"继续游戏"等）正确显示中文

### Requirement: Touch and mouse compatibility
系统 SHALL 在 HTML5 版本中同时支持触摸屏（移动浏览器）和鼠标操作。

#### Scenario: Play on mobile browser
- **WHEN** 用户在手机浏览器中触摸拖拽放置塔
- **THEN** 塔放置行为与 Android 原生版一致

#### Scenario: Play with mouse on desktop browser
- **WHEN** 用户在桌面浏览器中使用鼠标点击和拖拽
- **THEN** 操作映射与 Desktop 版一致

### Requirement: Asset async loading with progress
系统 SHALL 在 HTML5 版本启动时显示加载进度条，所有资源异步加载完成后进入游戏。

#### Scenario: Show loading progress
- **WHEN** 用户首次打开游戏 URL
- **THEN** 显示加载进度条（0%~100%），加载完成后自动进入主菜单

### Requirement: LocalStorage persistence
系统 SHALL 在 HTML5 版本中使用浏览器 LocalStorage 存储游戏设置和存档。

#### Scenario: Save game in browser
- **WHEN** 游戏在浏览器中触发存档
- **THEN** 存档数据写入 LocalStorage，刷新页面后可恢复

### Requirement: GWT-safe code in core module
core 模块 SHALL 仅使用 GWT 兼容的 Java API 子集，不使用反射、多线程、java.io.File 等 GWT 不支持的功能。

#### Scenario: Core module passes GWT compilation
- **WHEN** 运行 GWT 编译器处理 core 模块
- **THEN** 编译成功，无 GWT 不兼容 API 报错

### Requirement: Responsive canvas sizing
系统 SHALL 使 HTML5 游戏画布自适应浏览器窗口大小，支持全屏 API。

#### Scenario: Browser window resize
- **WHEN** 用户调整浏览器窗口大小
- **THEN** 游戏画布自适应新尺寸，UI 按比例缩放
