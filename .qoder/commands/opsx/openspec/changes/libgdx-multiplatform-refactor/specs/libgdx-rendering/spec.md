## ADDED Requirements

### Requirement: SpriteBatch-based rendering pipeline
系统 SHALL 使用 libGDX `SpriteBatch` 实现所有 2D 精灵渲染，替代 Android Canvas `drawBitmap`。所有精灵从 `TextureAtlas` 加载。

#### Scenario: Render game frame with towers and enemies
- **WHEN** GameScreen 的 `render()` 方法被调用
- **THEN** 使用 SpriteBatch 按正确渲染层次绘制：背景 → 底层装饰 → 塔/地面敌人(Y排序) → 顶层装饰 → 粒子 → 爆炸 → 弹药 → 飞行敌人 → UI

### Requirement: OrthographicCamera for view control
系统 SHALL 使用 `OrthographicCamera` 实现视图平移和缩放，替代 Android Matrix 变换。

#### Scenario: Player zooms in
- **WHEN** 玩家调整缩放滑块或使用鼠标滚轮放大
- **THEN** Camera.zoom 减小，视图放大，精灵按比例放大显示

#### Scenario: Player pans view
- **WHEN** 玩家拖拽地图空白区域
- **THEN** Camera.position 平移，视图跟随移动

### Requirement: ShapeRenderer for geometry
系统 SHALL 使用 `ShapeRenderer` 绘制非精灵图形（攻击范围圈、生命值条、粒子矩形）。

#### Scenario: Show tower attack range
- **WHEN** 玩家拖拽塔到地图上或打开升级面板时
- **THEN** 使用 ShapeRenderer 绘制半透明圆形表示攻击范围

### Requirement: BitmapFont for text rendering
系统 SHALL 使用 libGDX `BitmapFont` 渲染所有文本，字体文件预生成包含所需中文字符。

#### Scenario: Render HUD score text
- **WHEN** HUD 需要显示积分 "积分: 125,000"
- **THEN** 使用 BitmapFont.draw() 绘制中文+数字混排文本，位置和大小正确

### Requirement: Y-sorted rendering order
系统 SHALL 保持原有的 Y 坐标排序渲染机制（GridObjectOrder），确保塔和地面敌人按从上到下的顺序绘制，飞行单位始终在最上层。

#### Scenario: Enemy walks behind a tower
- **WHEN** 一个地面敌人的 Y 坐标小于某个塔的 Y 坐标
- **THEN** 该敌人在该塔之前绘制（被塔遮挡）

### Requirement: Color tinting for status effects
系统 SHALL 使用 `Batch.setColor()` 实现敌人状态效果着色（冰冻=蓝色叠加，灼烧=红色叠加），替代 Android PorterDuffColorFilter。

#### Scenario: Frozen enemy renders with blue tint
- **WHEN** 敌人处于减速状态 (slow_counter > 0)
- **THEN** 绘制该敌人精灵时设置蓝色着色 `batch.setColor(0.5f, 0.5f, 1f, 1f)`

### Requirement: Particle and explosion effects
系统 SHALL 实现烟尾粒子（矩形）和爆炸动画（精灵序列），视觉效果与原版一致。

#### Scenario: Rocket leaves smoke trail
- **WHEN** 火箭弹飞行中每帧产生 Particle 事件
- **THEN** 在弹道路径上绘制灰色半透明矩形，逐帧淡出

### Requirement: Fixed timestep game loop
系统 SHALL 使用固定时间步长 (1/30 秒) 更新游戏逻辑，与原版 30fps 帧率一致。渲染帧率可独立于逻辑帧率（插值或跳帧）。

#### Scenario: High refresh rate display
- **WHEN** Desktop 在 144Hz 显示器上运行
- **THEN** 游戏逻辑仍以 30fps 更新，渲染可以更高帧率插值显示
