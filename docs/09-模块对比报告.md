# 模块对比报告

## 项目模块概览

- `core`
  - 共享游戏逻辑、渲染抽象、输入、音频、选项、存档等。
  - 依赖 libGDX 核心库和 FreeType。
- `desktop`
  - 桌面启动器，使用 `Lwjgl3Application`。
  - 负责启动 `RoboDefenseGame`、窗口配置、WSL 软件 GL 重新启动。
- `app`
  - Android 原生应用模块。
  - 包含原始 decompiled Android UI、选项界面、资源目录和原生启动逻辑。
- `android`
  - 顶层目录存在，但 `settings.gradle` 中当前未包含，说明项目当前编译目标是 `app` 模块而非该 `android` 模块。

## 1. 构建与配置差异

### `settings.gradle`
- 当前包含模块：`core`, `desktop`, `app`。
- 注释掉模块：`android`, `html`。
- 说明：桌面与共享核心可构建，Android APK 由 `app` 模块而非 `android` 模块生成。

### `gradle.properties`
- `android.nonFinalResIds=false`：Android 运行时兼容性要求，针对 `app` 模块的 R.id switch-case。
- 该配置对 `desktop` 无影响，但必须保留以支持 Android 资源编译。

### `core/build.gradle`
- 作为 Java library，兼容 Java 8。
- 依赖 `gdx` 与 `gdx-freetype`。

### `desktop/build.gradle`
- 使用 `java` 插件。
- 依赖 `:core`、`gdx-backend-lwjgl3`、原生 desktop 平台库。
- `run` 任务通过 `assets` 目录作为 workingDir，说明 desktop 资源依赖静态 assets 目录而非 Android res 体系。

### `app/build.gradle`
- 使用 `com.android.application` 插件。
- Android 具体配置包含 `compileSdkVersion 34`、`minSdkVersion 14`、`targetSdkVersion 28`。
- buildFeatures: `buildConfig = false`，与 decompiled 项目风格一致。

## 2. 选项系统对比

### Android (`app` 模块)
- `app/src/main/java/com/magicwach/rdefense/OptionsData.java` 直接使用 `SharedPreferences` 读取/保存。
- 选项表由 `OptionProp` 数组初始化，包含 13 个选项。
- `OptionsActivity.java` 负责创建保存 UI。
- Android 选项名称使用 `res/values/strings.xml` 定义：`od_hq_graphics`、`od_bitmap_filtering`、`od_16_bit_background`、`od_prevent_screen_lock`、`od_show_battery_gauge` 等。

### Core (`core` 模块)
- `core/src/main/java/com/rdefense/core/config/OptionsData.java` 同样定义 13 个选项，并使用 `GameStorage` 抽象存储。
- 选项名称、默认值、键名与 Android 原始实现语义一致。
- `RoboDefenseGame.applyOptions()` 中实际应用：
  - 音量/静音
  - HQ 图像模式
  - 位图过滤
  - 16-bit 背景
  - 低 FPS 模式
  - 屏幕锁定防止
- `core` 已实现 HD 回退时禁用 HQ 并持久化。

### 对比结论
- 选项定义和语义一致，`core` 已成功复刻 Android 主要图形/行为选项。
- 唯一差异在存储层级：Android 直接 `SharedPreferences`，`core` 通过 `LibGdxStorage` 统一存储接口。

## 3. 资源加载与 HD 回退对比

### Android 资源体系
- Android 使用 `app/src/main/res/drawable-*` 及多语言 `values-*/strings.xml`。
- 原始 HD 加载逻辑不依赖文件名后缀，而是通过 `GridConfig.GRID_PIXEL_SIZE` / `GridConfig.UI_PIXEL_SIZE` 与 `ImageLoader.loadResourceWithDensity()` 实现 density scaling。
- `GameActivity.reInit()` 中：
  - `OptionsData.optionValue(8)` 决定是否尝试 HD 图像
  - HD 加载失败时回退 SD，并设置 `oom_triggered = true`
  - 若最后仍失败，会降级到 SD 并继续加载

### Core/Desktop 资源体系
- 使用 `assets/images/` 目录，图片直接按文件名加载。
- `LibGdxRenderer.findRegion()` 和 `findSpriteSheetFrames()` 优先查找 HQ 变体：`_2x`, `_large`, `_hd`。
- 如果 `useHQGraphics` 启用但加载 HD 失败，当前实现会：
  - 触发 `HdFallbackListener`
  - 禁用 HQ 并回退到基础文件名
  - 这与 Android 原始回退语义一致，但具体实现机制不同
- assets 目录中实际存在所有 `SpriteNames` 引用文件，且只在 `assets/images` 中定义了两个明显 HQ 变体：`mixer_2x1`, `mixer_2x2`, `bullet_large.png`, `shell_large.png`。

### 对比结论
- 资源文件名在 desktop/core 与 Android 资源列表之间已基本对齐。
- HD 变体策略不同：Android 通过 density scaling；desktop/core 通过后缀文件名查找。
- 当前实现可继续保留这种差异，但建议在文档中注明两者为“等价但不同实现方式”。

## 4. 平台服务差异

### Core 抽象层
- `core/src/main/java/com/rdefense/core/platform/PlatformServices.java` 作为平台服务聚合。
- 默认实现：
  - `setScreenTimeoutLock(boolean)` no-op
  - `getBatteryLevel()` 返回 `-1`
- 该接口为 platform-specific behavior 提供扩展点。

### Desktop 当前状态
- `DesktopLauncher` 仅负责窗口和 GL 启动，不创建桌面特定 `PlatformServices` 子类。
- 因此桌面平台默认仍是 core 的 no-op 实现。
- 结果：
  - `SCREEN_TIMEOUT_LOCK` 选项在 desktop 上没有实际效果
  - `SHOW_BATTERY_GAUGE` 会持续隐藏，因为 `getBatteryLevel()` 返回 `-1`

### Android 原始行为
- `GameActivity` 使用 `PowerManager` 获取 wake lock，以实现 `SCREEN_TIMEOUT_LOCK`。
- 电量显示在 Android 端通过 `BatteryManager` / `BatteryBroadcastReceiver` 或类似机制提供。
- 这部分行为尚未映射到 desktop/core 的抽象层。

### 对比结论
- 桌面平台服务仍存在缺口：`SCREEN_TIMEOUT_LOCK` 和电量读取未实现。
- 推荐将这些功能分离为 desktop-specific `PlatformServices` 扩展，而不破坏 core 共享逻辑。

## 5. 构建/资源路径对比

| 项目 | 主要资源位置 | 资源加载方式 | HD 资源策略 |
|---|---|---|---|
| Android `app` | `app/src/main/res/drawable-*` | Android `Resources` / `BitmapFactory` | density scaling、GridConfig 控制 |
| Core/shared | `assets/images` | libGDX `Gdx.files.internal()` | 后缀文件名 `_2x/_large/_hd` |
| Desktop | `desktop` + `assets` as working dir | LWJGL3 + libGDX | same as core |

## 6. 修复建议

### 建议 1：补齐桌面平台服务
- 实现桌面 `PlatformServices` 子类，至少提供：
  - `setScreenTimeoutLock(boolean)`：对桌面可实现 no-op 或简单日志，但最好明确这个选项在 desktop 上无实际效果。
  - `getBatteryLevel()`：如无法读取，可返回 `-1` 并在 UI 中显示“桌面不支持电量显示”。
- 这样避免选项在 desktop 上产生“看起来可用但无效”的错觉。

### 建议 2：统一选项存储说明
- 在 docs 中说明 `core` 使用 `LibGdxStorage` 统一存储，而 Android 原始模块直接使用 `SharedPreferences`。
- 如果未来需要 Android 原生 APK 兼容，可考虑用 `GameStorage` 适配 `SharedPreferences`，保持核心层一致。

### 建议 3：记录 HD 变体实现差异
- 在注释或文档中明确：Android 侧基于 density scaling；core/desktop 侧基于命名后缀查找。
- 对于未来资源补齐或迁移，避免误以为后缀文件名必须存在于 Android 原始资源目录中。

### 建议 4：确认 `android` 顶层模块状态
- `settings.gradle` 注释掉了 `android` 模块。
- 若该模块是历史遗留或备用代码，建议补一条说明；若需要恢复 Android 原始模块构建，则应修复 AGP 兼容问题并取消注释。

## 7. 总结

- `core` 与 Android 选项、资源引用已完成高度对齐。
- 关键差异点主要是桌面平台行为实现和 HD 回退机制。
- 资源名称存在性已验证，没有发现缺失引用。
- 下一阶段应重点补齐 `desktop` 平台服务，并将当前 HD 实现差异写入文档或注释。

---

若希望，我可以继续根据本报告直接修复桌面 `PlatformServices` 差异并补齐 `getBatteryLevel()` / `setScreenTimeoutLock()`。