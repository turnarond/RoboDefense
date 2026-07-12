## ADDED Requirements

### Requirement: Desktop window management
系统 SHALL 提供 Desktop Launcher，支持窗口模式和全屏模式切换，默认窗口大小为 1280×720。

#### Scenario: Launch game on desktop
- **WHEN** 用户运行 Desktop 可执行文件
- **THEN** 游戏在 1280×720 窗口中启动，显示主菜单

#### Scenario: Toggle fullscreen
- **WHEN** 用户按 F11 或 Alt+Enter
- **THEN** 游戏在全屏和窗口模式之间切换，UI 自适应新分辨率

### Requirement: Window resize handling
系统 SHALL 支持自由调整窗口大小，最小 800×480，UI 元素按比例缩放。

#### Scenario: Resize window smaller
- **WHEN** 用户将窗口从 1280×720 拖小到 960×540
- **THEN** 游戏视图和 UI 按比例缩放，不出现黑边或裁剪

### Requirement: Keyboard input mapping
系统 SHALL 支持以下键盘快捷键：1/2/3 选择塔类型、Space 暂停/恢复、F 快进切换、ESC 打开菜单/返回。

#### Scenario: Press 1 to select gun tower
- **WHEN** 用户按数字键 1
- **THEN** 激活枪塔选择状态，等同于点击第一个塔按钮

#### Scenario: Press Space to pause
- **WHEN** 游戏运行中用户按 Space
- **THEN** 游戏暂停，再次按 Space 恢复

#### Scenario: Press ESC to open menu
- **WHEN** 游戏中用户按 ESC
- **THEN** 弹出选项菜单（新游戏/设置/保存退出/返回菜单）

### Requirement: Mouse wheel zoom
系统 SHALL 支持鼠标滚轮缩放游戏视图，替代 Android 版的顶部缩放滑块。

#### Scenario: Scroll up to zoom in
- **WHEN** 用户向上滚动鼠标滚轮
- **THEN** 游戏视图以鼠标位置为中心放大

#### Scenario: Scroll down to zoom out
- **WHEN** 用户向下滚动鼠标滚轮
- **THEN** 游戏视图以鼠标位置为中心缩小

### Requirement: Right-click drag to pan
系统 SHALL 支持鼠标右键拖拽平移视图，同时保留 WASD 键盘平移。

#### Scenario: Right-click drag to scroll map
- **WHEN** 用户按住鼠标右键并拖拽
- **THEN** 游戏地图视图跟随鼠标移动方向平移

### Requirement: Mouse hover tooltips
系统 SHALL 在鼠标悬停在塔或敌人上时显示信息提示（塔：类型/等级/DPS；敌人：类型/血量/状态）。

#### Scenario: Hover over placed tower
- **WHEN** 鼠标静止在一个已放置的塔上 0.5 秒
- **THEN** 显示浮动提示框：塔名称、当前等级、攻击力、射程

### Requirement: Right-click cancel
系统 SHALL 支持鼠标右键取消当前操作（取消塔放置、关闭升级面板）。

#### Scenario: Cancel tower placement with right-click
- **WHEN** 用户正在拖拽放置塔时点击右键
- **THEN** 取消放置操作，回到空闲状态

### Requirement: HiDPI display support
系统 SHALL 正确处理 HiDPI/Retina 屏幕，UI 元素在高 DPI 下不会过小。

#### Scenario: Launch on 4K display with 200% scaling
- **WHEN** 游戏在 4K 显示器（200% 系统缩放）上启动
- **THEN** UI 元素大小与 1080p 100% 缩放时视觉相同

### Requirement: Desktop packaging
系统 SHALL 支持打包为独立可执行文件，内嵌 JRE，用户无需安装 Java。

#### Scenario: Run packaged Windows executable
- **WHEN** 用户双击 robo-defense.exe
- **THEN** 游戏直接启动，无需预装 Java 运行时
