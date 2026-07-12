## Context

当前项目是一个从 APK 反编译的 Android 塔防游戏（星际塔防 / Robo Defense），约 60 个 Java 源文件。游戏逻辑与 Android 平台 API 深度耦合：渲染使用 Canvas/Paint/Bitmap，音频使用 MediaPlayer，持久化使用 SharedPreferences + Java ObjectOutputStream。

代码分为三个耦合层级：
- **Tier 3 纯逻辑**（~30 个类）：GameState、Enemy、GameTower、Bullet、MovementGrid 等 — 无平台依赖
- **Tier 2 混合**（~10 个类）：引用了 Bitmap 类型但逻辑可移植
- **Tier 1 强耦合**（~20 个类）：GameActivity、Display、ImageLoader、SoundManager 等 — 必须重写

业务需求：保留 Android 版、新增 Web 版、后续支持联网功能（排行榜、云存档）。

## Goals / Non-Goals

**Goals:**
- 一套核心代码同时输出 Android、Desktop、HTML5 三个平台
- 游戏核心逻辑零平台依赖，通过接口与平台层交互
- 渲染性能不低于原生 Canvas 方案（利用 libGDX OpenGL 加速）
- 存档格式跨平台统一，支持版本升级和云端同步
- 为联网功能（排行榜、云存档）预留统一 API 层
- 保持游戏玩法和数值体系不变

**Non-Goals:**
- 不重新设计游戏玩法或平衡性
- 不制作高清美术资源（沿用现有精灵图，后续可替换）
- 不实现后端服务（仅定义客户端接口协议）
- 不支持 iOS 平台（libGDX 支持但不在本期范围内）
- 不实现多人对战或社交功能

## Decisions

### D1: 游戏框架选择 — libGDX

**选择**: libGDX 1.12+

**备选方案**:
| 方案 | 优势 | 劣势 |
|------|------|------|
| libGDX | Java 复用最大化；原生 Android+Desktop+HTML5；2D 性能优秀 | GWT 编译慢；HTML5 受 GWT 限制 |
| JavaFX | Canvas API 最相似 | 无 Android 支持；生态弱；不支持 Web |
| Kotlin Multiplatform + Compose | 现代技术栈 | 需全部改写为 Kotlin；Compose Canvas 性能未验证 |
| 引擎重写 (Godot/Unity) | 工具链成熟 | 完全重写，Java 代码不可复用 |

**理由**: 现有代码为 Java，libGDX 允许 Tier 3 类直接复制到 core 模块无需修改。SpriteBatch 与 Canvas 概念对等，迁移映射明确。

### D2: 项目结构 — Gradle 多模块

```
robo-defense/
├── core/         ← 共享游戏逻辑 + libGDX 渲染/音频实现
├── android/      ← Android Launcher
├── desktop/      ← Desktop Launcher (LWJGL3)
├── html/         ← HTML5 Launcher (GWT)
└── assets/       ← 共享资源目录
```

**理由**: 遵循 libGDX 标准项目模板，社区工具链兼容，Gradle 多项目构建可并行编译。

### D3: 平台抽象层设计 — 接口 + 依赖注入

```java
// core 模块中定义接口
public interface PlatformServices {
    GameRenderer getRenderer();
    GameAudio getAudio();
    GameStorage getStorage();
    NetworkClient getNetwork();  // 可选，联网时注入
}
```

各平台 Launcher 负责注入具体实现。core 模块通过接口调用，不引入任何平台依赖。

**理由**: 比 libGDX 自带的 `Gdx.app`/`Gdx.files` 更明确的依赖边界；便于单元测试 mock。

### D4: 渲染方案 — SpriteBatch + TextureAtlas

**Android Canvas API → libGDX 映射**:
| Canvas | libGDX |
|--------|--------|
| `drawBitmap(bmp, x, y, paint)` | `batch.draw(region, x, y)` |
| `drawRect(rect, paint)` | `shapeRenderer.rect()` |
| `drawText(str, x, y, paint)` | `font.draw(batch, str, x, y)` |
| `Matrix` 变换 | `OrthographicCamera` + `batch.setTransformMatrix()` |
| `Paint.setAlpha()` | `batch.setColor(r, g, b, alpha)` |
| `PorterDuffColorFilter` | `batch.setColor()` 着色 或自定义 Shader |

**资源打包**: 所有精灵图打包为 TextureAtlas（TexturePacker），减少 draw call。

### D5: 存档格式 — JSON + 版本号

```json
{
  "version": 2,
  "difficulty": 15,
  "money": 2400,
  "health": 18,
  "score": 125000,
  "towers": [...],
  "enemies": [...],
  "levelState": {...}
}
```

**理由**: 跨平台兼容（SharedPreferences 仅 Android）；可读性强便于调试；版本号支持向前兼容迁移；JSON 天然适合 HTTP 传输（云存档）。

**旧存档迁移**: Android 版首次启动时检测旧 Java 序列化存档，自动转换为 JSON 格式。

### D6: 联网架构 — REST + 离线优先

```
客户端                          服务端
┌──────────┐    HTTPS/JSON    ┌──────────┐
│NetworkClient│ ──────────────→ │ API Server│
│ (接口)      │                │          │
│ - submitScore()             │ /scores  │
│ - getLeaderboard()          │ /boards  │
│ - uploadSave()              │ /saves   │
│ - downloadSave()            │ /saves/:id│
│ - login()                   │ /auth    │
└──────────┘                  └──────────┘
```

**离线优先策略**:
- 所有操作先写入本地
- 联网时后台同步
- 冲突解决：云端 lastModified 时间戳 > 本地则提示用户选择

### D7: HTML5 / GWT 适配策略

- 使用 libGDX 的 GWT 后端
- 中文字体：预生成 BitmapFont（包含所有 strings.xml 中用到的汉字）
- 音频：Web Audio API（libGDX GWT 自动适配）
- 存储：LocalStorage（通过 `Gdx.app.getPreferences()`）
- 限制：不支持 Java 反射、需避免 GWT 不兼容的 Java API

### D8: 游戏循环迁移

```
原方案:                         新方案:
Monitor Thread → postInvalidate    libGDX 框架自动调用
onDraw() → nextState + draw        render(float delta):
                                     update(delta)  ← GameState.nextState()
                                     draw()         ← SpriteBatch 渲染
```

libGDX 自带固定/可变步长游戏循环，无需手动管理 Monitor 线程。使用固定步长 (1/30s) 保持与原版一致的游戏速度。

## Risks / Trade-offs

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| GWT 编译时间长 (2-5分钟) | 开发效率低 | Desktop 开发为主，GWT 仅在发布前验证 |
| GWT 不支持部分 Java API (反射、多线程) | 编译失败 | 使用 GWT-safe 子集；core 模块禁用反射 |
| 中文 BitmapFont 体积大 | HTML5 加载慢 | 仅打包 strings.xml 中出现的汉字 (~500字)；WOFF2 压缩 |
| 旧存档迁移失败 | 玩家数据丢失 | 迁移前备份原文件；提供手动恢复入口 |
| libGDX SpriteBatch 与 Canvas 行为差异 | 视觉不一致 | 逐模块对比截图验证；建立视觉回归基线 |
| 联网功能需后端服务 | 客户端完成但无法端到端测试 | 定义 Mock Server + OpenAPI spec；客户端支持离线模式 |
| 对象池模式在 libGDX 中的适配 | 内存管理差异 | 保留现有池化逻辑；libGDX Pool 类可选替换 |
| TextureAtlas 打包后资源热更新困难 | 修改精灵需重新打包 | Gradle task 自动化打包流程 |

## Migration Plan

### 阶段式迁移（非一次性切换）

1. **Phase 0 — 清理当前代码** (不改架构)
   - 删除内嵌 androidx、统一 R 类、重命名混淆类
   - 确保 Android 版可正常构建和运行

2. **Phase 1 — 建立 libGDX 项目骨架**
   - 创建多模块结构
   - 迁移 Tier 3 纯逻辑类到 core
   - Desktop Launcher 可启动空窗口

3. **Phase 2 — 渲染迁移**
   - 实现 GameRenderer (libGDX)
   - 资源转换 (TextureAtlas + BitmapFont)
   - 逐步迁移 Display 逻辑

4. **Phase 3 — 完整功能**
   - 输入、音频、UI 控件
   - 存档系统 V2
   - 所有 Screen

5. **Phase 4 — 联网 + Web**
   - NetworkClient 接口 + 实现
   - GWT 适配 + 发布
   - 排行榜 + 云存档

### 回滚策略

- 每个 Phase 可独立运行：Phase 0 产出仍是可工作的 Android APK
- Git 分支策略：`main`（稳定 Android 版）、`feature/libgdx-migration`（重构分支）
- Phase 1-2 期间 Android 版从 main 继续维护，重构完成后合并

## Open Questions

1. **后端技术栈**：排行榜/云存档服务用什么实现？(Spring Boot / Node.js / Serverless)
2. **用户认证方案**：游戏登录用第三方 OAuth（Google/微信）还是自建账号？
3. **HTML5 发布平台**：部署到自有服务器还是 itch.io / 游戏平台？
4. **旧版 Android APK 维护**：重构期间是否继续发布旧架构版本？
5. **CI/CD**：是否需要自动构建流水线（GitHub Actions / Jenkins）？
