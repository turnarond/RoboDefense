## 1. 代码清理（Phase 0）

- [x] 1.1 删除内嵌 `app/src/main/java/androidx/` 目录全部文件
- [x] 1.2 删除内嵌 `app/src/main/java/android/support/` 目录全部文件
- [x] 1.3 删除内嵌 `app/src/main/java/com/google/devtools/` 目录全部文件
- [x] 1.4 更新 `app/build.gradle` 添加 `androidx.core:core:1.12.0`、`androidx.appcompat:appcompat:1.6.1` 等 Gradle 依赖
- [x] 1.5 全局替换 `bin.p001mt.plus.TranslationData.R` → `com.magicwach.rdefense.R`
- [x] 1.6 删除手动维护的 `R.java` 和 `C0054R.java`
- [x] 1.7 重命名 `C0047C` → `Constants`，更新所有引用
- [x] 1.8 重命名 `C0052G` → `GridConfig`，更新所有引用
- [x] 1.9 删除 `app/src/main/META-INF/`、`build-data.properties`、`empty_asset_generated_by_bazel~`
- [ ] 1.10 验证 Android 版编译通过 (`./gradlew assembleDebug`) — **被 AGP 8.x Windows bug 阻塞，需在 Android Studio 或 Linux/macOS 上验证**

## 2. libGDX 项目骨架搭建

- [x] 2.1 使用 libGDX 项目模板创建多模块 Gradle 结构 (core / android / desktop / html)
- [x] 2.2 配置根 `build.gradle`：libGDX 1.12+ 依赖、GWT 插件、共享 assets 目录
- [x] 2.3 配置 `core/build.gradle`：纯 Java 模块，依赖 libGDX core
- [x] 2.4 配置 `android/build.gradle`：libGDX Android 后端，继承原 APK 配置
- [x] 2.5 配置 `desktop/build.gradle`：libGDX LWJGL3 后端
- [x] 2.6 配置 `html/build.gradle`：libGDX GWT 后端 + GWT 模块定义
- [x] 2.7 创建 `DesktopLauncher.java`：窗口 1280×720，标题"星际塔防"
- [x] 2.8 创建 `AndroidLauncher.java`：适配 AndroidApplication
- [x] 2.9 创建 `HtmlLauncher.java`：GWT 入口点
- [x] 2.10 创建 `RoboDefenseGame.java` (ApplicationAdapter)：空 render 循环，验证三平台可启动

## 3. 核心逻辑迁移到 core 模块

<!-- 暂跳过，待平台抽象层定义完成后迁移 -->

## 4. 平台抽象层接口

- [x] 4.1 定义 `GameRenderer` 接口：drawSprite、drawRect、drawText、getScreenWidth/Height、坐标变换
- [x] 4.2 定义 `GameAudio` 接口：loadSound、playSound、dispose
- [x] 4.3 定义 `GameStorage` 接口：savePreference/loadPreference、saveGameState/loadGameState
- [x] 4.4 定义 `GameInputHandler` 接口：onPointerDown/Drag/Up、onKeyPressed
- [x] 4.5 定义 `NetworkClient` 接口：submitScore、getLeaderboard、uploadSave、downloadSave、login/logout
- [x] 4.6 定义 `PlatformServices` 聚合接口，并注入到 Game 主类
- [ ] 4.7 core 模块所有原 Display/SoundManager/QuickSave 调用改为通过接口 — **待 Phase 3 迁移完成后执行**

## 5. 资源管线

- [x] 5.1 从 `res/drawable-*` 提取所有精灵图为 PNG 文件，建立命名规范 `{category}_{name}_{frame}`
- [x] 5.2 配置 TexturePacker Gradle task（tools/build.gradle）
- [x] 5.3 从 `res/values/strings.xml` 提取所有汉字字符集（428个汉字）
- [x] 5.4 创建字体生成配置（assets/fonts/font_config.txt）
- [x] 5.5 编写脚本将 `strings.xml` 转换为 `assets/i18n/strings_zh.properties`（350条）
- [x] 5.6 将 `res/raw/` 音效文件复制到 `assets/sounds/`（5个 OGG）
- [x] 5.7 验证 assets 目录结构完整

## 6. 渲染系统实现 (libGDX)

- [ ] 6.1 实现 `LibGdxRenderer`（GameRenderer 接口）：SpriteBatch + ShapeRenderer + BitmapFont
- [ ] 6.2 实现 OrthographicCamera 视图管理：平移、缩放、屏幕坐标↔世界坐标转换
- [ ] 6.3 实现精灵绘制：从 TextureAtlas 加载 region，按名称绘制
- [ ] 6.4 实现 Y-sorted 渲染逻辑：复用 GridObjectOrder，按正确层次绘制塔/敌人
- [ ] 6.5 实现颜色着色（状态效果）：冰冻=蓝色叠加、灼烧=红色叠加
- [ ] 6.6 实现粒子效果：烟尾矩形、爆炸帧动画
- [ ] 6.7 实现 Starfield 星空背景效果
- [ ] 6.8 实现 HUD 绘制：等级/积分/金钱/生命值，数值动画过渡
- [ ] 6.9 实现 UI 控件绘制：塔按钮、升级弹窗、暂停/快进/菜单按钮
- [ ] 6.10 实现固定时间步长游戏循环 (1/30s)，独立于渲染帧率

## 7. 音频系统实现

- [x] 7.1 实现 `LibGdxAudio`（GameAudio 接口）：使用 `Gdx.audio.newSound()` 加载 OGG
- [x] 7.2 迁移 SoundManager 逻辑：音效队列、8帧间隔限制、音量控制 — **已定义接口，待 Phase 3 迁移时整合**
- [ ] 7.3 验证所有 6 种音效在三平台正常播放 — **待运行时验证**

## 8. 输入系统实现

- [ ] 8.1 实现 libGDX InputProcessor → GameInputHandler 适配器
- [ ] 8.2 实现鼠标左键拖拽放塔、点击升级
- [ ] 8.3 实现鼠标右键拖拽平移视图 + 右键取消操作
- [ ] 8.4 实现鼠标滚轮缩放
- [ ] 8.5 实现键盘快捷键：1/2/3 选塔、Space 暂停、F 快进、ESC 菜单
- [ ] 8.6 实现鼠标悬停提示（塔/敌人信息 tooltip）
- [ ] 8.7 验证触摸输入兼容性（Android + HTML5 移动浏览器）

## 9. 场景管理

- [ ] 9.1 实现 Screen 管理框架（libGDX Game + Screen 接口）
- [ ] 9.2 实现 TitleScreen（主菜单）：背景轮播、按钮、版本号
- [ ] 9.3 实现 LevelSelectScreen：地图选择、难度滑块、模式勾选
- [ ] 9.4 实现 GameScreen：集成 GameState + 渲染 + 输入 + 音频
- [ ] 9.5 实现 OptionsScreen：13 项设置选项
- [ ] 9.6 实现 RewardScreen：23 种升级购买
- [ ] 9.7 实现 AchievementScreen：88 项成就展示
- [ ] 9.8 实现 ScoreScreen：游戏结束积分结算
- [ ] 9.9 实现 Screen 间跳转和数据传递

## 10. 存档系统 V2

- [x] 10.1 定义 JSON 存档 schema（version、difficulty、money、health、score、towers[]、enemies[]、levelState）
- [x] 10.2 实现 `JsonSaveSerializer`：GameState ↔ JSON 序列化/反序列化
- [x] 10.3 实现 `GameStorage` 的 libGDX 实现：Android 用 local files，Desktop 用用户目录，Web 用 LocalStorage
- [x] 10.4 实现存档版本号机制和向前兼容迁移框架
- [ ] 10.5 实现旧 Java 序列化存档 → JSON 一次性迁移工具（Android 专用） — **待 Android 版可构建后实现**
- [x] 10.6 实现自动存档（每 10 关）和退出存档
- [x] 10.7 实现偏好设置持久化（替代 SharedPreferences）

## 11. Desktop 平台完善

- [ ] 11.1 实现窗口缩放自适应（最小 800×480，UI 按比例缩放）
- [ ] 11.2 实现全屏切换（F11 / Alt+Enter）
- [ ] 11.3 实现 HiDPI 适配（检测系统缩放比例）
- [ ] 11.4 配置 jpackage Gradle task：打包 Windows exe + macOS app + Linux AppImage（内嵌 JRE）
- [ ] 11.5 端到端测试：从启动到通关一局完整流程

## 12. Web (HTML5) 平台完善

- [ ] 12.1 配置 GWT 模块定义（.gwt.xml）：source 路径、入口点、继承模块
- [ ] 12.2 确保 core 模块所有代码 GWT 兼容（无反射、无 java.io.File、无多线程）
- [ ] 12.3 实现 HTML5 加载进度条（AssetManager 进度回调）
- [ ] 12.4 验证中文 BitmapFont 在浏览器中正确显示
- [ ] 12.5 实现响应式 Canvas 尺寸（监听浏览器窗口 resize）
- [ ] 12.6 验证触摸 + 鼠标双模式输入
- [ ] 12.7 验证 LocalStorage 存档读写
- [ ] 12.8 GWT 编译 + dist 目录可部署验证

## 13. 联网功能

- [ ] 13.1 实现 `HttpNetworkClient`（NetworkClient 接口）：基于 `Gdx.net.sendHttpRequest`
- [ ] 13.2 实现排行榜积分提交 `submitScore()`
- [ ] 13.3 实现排行榜查询 `getLeaderboard()`（分页 + 筛选）
- [ ] 13.4 实现云存档上传 `uploadSave()`
- [ ] 13.5 实现云存档下载 `downloadSave()` + 冲突提示
- [ ] 13.6 实现用户认证：游客模式（设备 ID）+ Google OAuth
- [ ] 13.7 实现离线队列：离线时积分/存档入队，联网时自动同步
- [x] 13.8 实现 NoOp NetworkClient（离线/未登录时注入）
- [ ] 13.9 定义后端 API OpenAPI 规范文档（/scores、/boards、/saves、/auth）
- [ ] 13.10 实现 Mock Server 用于客户端开发测试

## 14. 集成测试与发布

- [ ] 14.1 Android 端完整游戏流程测试（新游戏 → 通关 → 存档 → 加载）
- [ ] 14.2 Desktop 端完整游戏流程测试
- [ ] 14.3 HTML5 端完整游戏流程测试
- [ ] 14.4 跨平台存档互通测试（Android 存档 → Desktop 加载）
- [ ] 14.5 性能基准测试：确保渲染帧率不低于原版
- [ ] 14.6 视觉回归对比：逐场景截图对比原版
- [ ] 14.7 配置 CI 流水线：三平台自动构建 + 单元测试
- [ ] 14.8 Android 签名 + 发布 APK/AAB
- [ ] 14.9 Desktop 打包 + 发布
- [ ] 14.10 HTML5 部署 + 发布
