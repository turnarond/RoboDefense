## ADDED Requirements

### Requirement: Platform services interface
系统 SHALL 定义 `PlatformServices` 顶层接口，作为所有平台能力的统一入口点。各平台 Launcher 通过该接口注入具体实现。

#### Scenario: Game core accesses renderer without platform coupling
- **WHEN** GameState 需要获取屏幕尺寸计算网格布局
- **THEN** 通过 `PlatformServices.getRenderer().getScreenWidth/Height()` 获取，core 模块无 Android/Desktop/Web 直接依赖

### Requirement: Renderer abstraction interface
系统 SHALL 定义 `GameRenderer` 接口，提供以下能力：绘制精灵（位置、缩放、旋转）、绘制矩形、绘制文本、获取屏幕尺寸、坐标变换。

#### Scenario: Draw a tower sprite
- **WHEN** Display 层需要在 (gridx, gridy) 绘制一个防御塔
- **THEN** 调用 `renderer.drawSprite(spriteName, pixelX, pixelY)` 完成绘制，无需关心底层是 SpriteBatch 还是 Canvas

#### Scenario: Draw text with alignment
- **WHEN** HUD 需要绘制右对齐的金钱数值 "$2400"
- **THEN** 调用 `renderer.drawText("$2400", x, y, size, color, TextAlign.RIGHT)`

### Requirement: Audio abstraction interface
系统 SHALL 定义 `GameAudio` 接口，提供：加载音效、播放音效（带音量）、释放资源。

#### Scenario: Play gun sound effect
- **WHEN** 枪塔发射子弹时 SoundManager 请求播放音效
- **THEN** 调用 `audio.playSound("gun", volume)` 播放，底层实现由平台决定

### Requirement: Storage abstraction interface
系统 SHALL 定义 `GameStorage` 接口，提供：键值对读写（偏好设置）、二进制/JSON 数据读写（游戏存档）。

#### Scenario: Save game preferences
- **WHEN** 玩家修改设置后退出设置界面
- **THEN** 调用 `storage.savePreference(key, value)` 持久化，Android 用 SharedPreferences，Desktop 用文件，Web 用 LocalStorage

#### Scenario: Save game state
- **WHEN** 游戏触发自动存档（每10关）
- **THEN** 调用 `storage.saveGameState(jsonBytes)` 持久化完整游戏状态

### Requirement: Input abstraction interface
系统 SHALL 定义 `GameInputHandler` 接口，将触摸/鼠标/键盘事件统一为：pointerDown、pointerDrag、pointerUp、keyPressed。

#### Scenario: Mouse click maps to pointer down
- **WHEN** Desktop 平台用户点击鼠标左键
- **THEN** 平台层调用 `inputHandler.onPointerDown(x, y)` 传入归一化坐标

#### Scenario: Keyboard shortcut mapped to key event
- **WHEN** Desktop 用户按下数字键 "1"
- **THEN** 平台层调用 `inputHandler.onKeyPressed(KEY_1)` 触发选择第一种塔

### Requirement: Network client interface
系统 SHALL 定义 `NetworkClient` 接口，提供：排行榜提交/查询、云存档上传/下载、用户登录。该接口为可选注入，离线模式下为 null 或 NoOp 实现。

#### Scenario: Offline mode without network
- **WHEN** 游戏启动但无网络连接或未配置 NetworkClient
- **THEN** 所有依赖联网的功能静默跳过，游戏正常运行

### Requirement: Core module zero platform dependency
core 模块 SHALL 不包含任何 `android.*`、`javax.swing.*`、`com.google.gwt.*` 或其他平台特定 import。仅允许 `com.badlogic.gdx.*`（libGDX 跨平台 API）和标准 Java（GWT 兼容子集）。

#### Scenario: Core module compiles without Android SDK
- **WHEN** 仅编译 core 模块 (`./gradlew :core:compileJava`)
- **THEN** 编译成功，无需 Android SDK 环境
