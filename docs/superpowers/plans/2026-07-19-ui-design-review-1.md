# UI 评审实施计划 1A：设计一致性与交互可用性

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended). Steps use checkbox (`- [ ]`) syntax.

**Goal:** 产出 `docs/07-UI设计交互评审报告.md` — 覆盖全部 9 Screen + UiRenderer 的设计一致性与交互可用性评审，每条差异附 P0-P3 分级。

**Architecture:** 利用已有的三份探索报告（原版 Android UI、桌面 Screen、渲染管线）作为数据基底，派 3 个深度分析代理分别审查共性/逐界面/交互差距，主线程复核高严重项后撰写报告文件。

**Tech Stack:** 只读分析——Grep / Read / 原版反编译源码（`C:/Users/yanch/AppData/Local/Temp/apk_decompiled/sources/com/magicwach/rdefense/`）+ 当前源码（`D:/personal/RoboDefense/core/src/main/java/com/rdefense/core/`）。

**依据:** `docs/superpowers/specs/2026-07-18-ui-review-design.md` 第 2 节。

**输入数据:** 三份探索报告的结论已在本会话 context 中，复用为分析的起点。不需要代理重新扫全量——代理聚焦于差异识别和分级。

## Global Constraints

- 纯只读——不修改任何代码文件
- 所有输出用简体中文
- 每条 P0/P1 附双边证据：`原版文件:行号` ↔ `当前文件:行号`
- 代理产出不直接采信——主线程逐条复核 P0/P1 原始源码后才写入报告
- 视觉风格差异（科幻 vs 原版 Android 自带主题）为 P3 有意差异

---

### Task 1: 共性审查 — 色彩/间距/字体/UI 模式

**Scope:** 9 Screen + UiRenderer 的横向一致性

- [ ] **Step 1: 色彩体系一致性审查**

抽取每个 Screen 和 UiRenderer 的 RGB 值使用规律。已知基准：
- 主背景 `(0.03,0.05,0.12)` 深蓝
- 标题栏 `(0.06,0.08,0.16)` + 蓝顶线 `(0.2,0.36,0.55)`
- 文字主色 `(0.75-0.95)` 浅灰蓝系
- 金钱绿 `(0.3,1.0,0.3)` / 警告黄 / 危险红

对每个 Screen 抽取所有 `drawRect` 和 `drawText` 调用中的 RGB 值，检查是否有违反上述体系的硬编码颜色。重点看 MainMenuScreen（金色标题 `0.95,0.85,0.25`）和 LevelSelectScreen（地图卡片绿色 `0.12,0.45,0.18`）是否与全局体系冲突。

- [ ] **Step 2: 间距规范审查**

抽取每个 Screen 的标题栏高度、按钮尺寸、行间距、边距。检查这些值的一致性和是否符合 24px 最小点击目标。

- [ ] **Step 3: 字体尺寸层级审查**

抽取 `drawText` 调用，确认是否存在一致的标题/正文/辅助三级尺寸体系。

- [ ] **Step 4: UI 模式遵循率**

确认以下模式在 9 Screen 中的出现频率：
- 标题栏（`y=sh-34, h=34, 深蓝底色+蓝色顶线`）
- 面板（`深色矩形+蓝色装饰线`）
- 返回按钮（`110x36 或 100x34 左下`）
- 标准行高

标记不遵循标准的偏离项。

- [ ] **Step 5: 编译（无代码变更，仅验证工作树清洁）**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 6: 产出章节草稿**

写入临时文件 `docs/.ui-review-1a-consistency.md` 供主线程 Task 4 使用。

---

### Task 2: 逐界面交互审查

**Scope:** 每个 Screen + UiRenderer 的布局合理性和交互完整性

将 10 个界面分为三组并行审查：

**组 A — 游戏内**（最高复杂度）:
- GamePlayScreen (893 行)
- UiRenderer (612 行)

审查点：
- HUD 信息密度是否合理（顶部 8 个显示元素是否有视觉层级）
- 暂停菜单、升级对话框、塔按钮、控制按钮的热区大小和位置
- 键盘快捷键是否有视觉提示（当前仅底部一行文字提示）
- 鼠标悬停反馈是否存在（当前无 hover）
- 放置失败/消息提示的可见性和时长

**组 B — 菜单/选择界面**:
- MainMenuScreen (231 行)
- LevelSelectScreen (412 行)
- OptionsScreen (98 行)

审查点：
- 按钮层级和视觉权重（主要/次要/危险操作是否用颜色区分）
- 确认对话框的行为一致性（MainMenu 用 rect 模拟 → 其他界面是否有同类需求）
- LevelSelect 混合器面板的热区是否足够大（5 位数码的 ▲/▼ 仅 18x12px）
- Options 开关行的高度和间距是否符合 44px 触控标准

**组 C — 内容展示界面**:
- AchievementScreen (231 行)
- RewardScreen (182 行)
- CreditsScreen (94 行)
- GameSaveScreen (296 行)

审查点：
- 列表滚动的交互一致性（滚轮/键盘/点击条三种方式是否都支持）
- 选中态和 hover 态是否有视觉区分
- GameSave 列表行高 60px 是否合适
- Credits 自动滚动速度和可读性

- [ ] **Step 1: 编译（工作树验证）**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 2: 产出章节草稿**

写入临时文件 `docs/.ui-review-1a-per-screen.md`。

---

### Task 3: 交互模式差距分析

**Scope:** 原版 Android UX 模式 → 桌面适配的差距

对照原版 9 Activity + GameHud / TowerButton / UpgradeDialog / ScoreOverlay / AchievementAlert 等的交互模式：

- [ ] **Step 1: 原版触摸拖拽 → 桌面替代方案审查**

- 塔放置：原版拖拽 TowerButton 出区域 → 触发放置；桌面版点击塔按钮 → 点击地图格
  - 差距：桌面版无拖拽预览、无"拖出按钮区域取消"的防误触
- 缩放：原版双指 Pinch + DisplayScaleUI 滑块；桌面版滚轮 + 滑块
  - 差距：滚轮无刻度反馈、滑块（已实现）与原版 DisplayScaleUI 对比

- [ ] **Step 2: 原版弹出/弹窗模式 → 桌面替代方案**

- 升级对话框：原版 8 帧从塔位置展开动画 + togglePause 自动暂停；桌面版直接显示 240px 面板
  - 差距：展开动画缺失（P3 有意简化）、dialog 外点击关闭无动画
- 暂停菜单：原版 Android Options Menu (4 项硬件菜单)；桌面版 3 按钮面板
  - 差距：原版有"新游戏"选项而桌面版无（已在游戏结束回调中处理）
- 确认框：原版 AlertDialog（系统组件有标准外观）；桌面版 drawRect 模拟
  - 差距：桌面版无遮罩层（仅有面板本身）→ 误操作风险

- [ ] **Step 3: 原版 HUD → 桌面对比**

- HudEntry 数字滚动 vs 当前直接跳转（已在计划②中修复千位分隔，但滚动动画仍缺失）
- ScoreOverlay 9 状态动画 vs 当前简化版（已实现 5 行淡入，但无倒计数动画）
- AchievementAlert 边框描绘动画 vs 当前 AchievementRenderer 动画（均为 5 状态，差异在于触发速度）

- [ ] **Step 4: 编译 + 产出章节草稿**

```bash
./gradlew :core:compileJava
```

写入 `docs/.ui-review-1a-interaction-gaps.md`。

---

### Task 4: 主线程复核与分级

- [ ] **Step 1: 通读三份草稿** (`docs/.ui-review-1a-*.md`)

- [ ] **Step 2: 对报告的每条 P0/P1 逐条打开双边源码复核**

优先级：
1. 热区小于 24px 的控件（LevelSelect 混合器箭头、AchievementScreen 分类标签）
2. 缺失的交互反馈（无 hover、按钮无按下态）
3. 色彩体系偏离项（硬编码 vs 统一常量）
4. 各界面独占实现的可复用模式（标题栏 9 处重复）

- [ ] **Step 3: 对每条复核项分级**

P0 = 操作无法完成或严重困惑（热区无法命中、关键反馈缺失）
P1 = 各界面表现不一致、缺少必要视觉提示
P2 = 可优化但不阻塞使用
P3 = 风格选择差异、原版 Android 独有特性

- [ ] **Step 4: 产出分级清单**

写入 `docs/.ui-review-1a-graded.md`。

---

### Task 5: 撰写并提交报告

- [ ] **Step 1: 将四份草稿合并为最终报告**

输出文件：`docs/07-UI设计交互评审报告.md`

按照规格的五章结构组织：
1. 共性审查
2. 逐界面审查
3. 交互模式差距
4. 分级结论
5. 设计建议

- [ ] **Step 2: 自审**

检查：无占位符 / 内部一致 / P0/P1 有双边证据 / 无歧义

- [ ] **Step 3: 编译（确保工作树清洁）**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add docs/07-UI设计交互评审报告.md docs/superpowers/plans/2026-07-19-ui-design-review-1.md
rm -f docs/.ui-review-1a-*.md
git commit -m "docs: UI 设计交互评审报告 — 共性/逐界面/交互差距（报告一）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## 计划自审记录

- 规格覆盖：报告一的 5 章全覆盖（共性审查→Task 1, 逐界面审查→Task 2, 交互差距→Task 3, 分级→Task 4, 设计建议→Task 5 合并步骤）
- 占位符：无——所有任务给出具体文件和审查维度
- 类型一致性：N/A（纯分析报告，无代码接口）
- 依赖链：Task 1/2/3 可并行 → Task 4 依赖前三份草稿 → Task 5 依赖分级清单
