## ADDED Requirements

### Requirement: TextureAtlas generation
系统 SHALL 将所有精灵图（从 Android res/drawable-* 提取）打包为 libGDX TextureAtlas 格式，使用 TexturePacker 工具。

#### Scenario: Generate atlas from source images
- **WHEN** 运行资源打包 Gradle task (`./gradlew packTextures`)
- **THEN** 输出 `assets/images/game.atlas` + 对应 PNG 图集文件

#### Scenario: Load atlas at runtime
- **WHEN** 游戏启动加载资源
- **THEN** 通过 `TextureAtlas("game.atlas")` 加载，所有精灵可通过名称引用

### Requirement: BitmapFont generation for Chinese
系统 SHALL 预生成包含所有游戏中使用的中文字符的 BitmapFont 文件（.fnt + .png），使用 Hiero 或 libGDX FreeTypeFontGenerator。

#### Scenario: Generate font with required characters
- **WHEN** 从 strings.xml 提取所有不重复的汉字（约 500 个）+ ASCII
- **THEN** 生成 `assets/fonts/game_font.fnt` 覆盖全部所需字符

#### Scenario: Missing character fallback
- **WHEN** 运行时遇到字体中未包含的字符
- **THEN** 显示占位符（方框），不崩溃

### Requirement: I18N bundle for localization
系统 SHALL 将 Android `res/values/strings.xml` 转换为 libGDX `I18NBundle` 格式（.properties 文件）。

#### Scenario: Load Chinese strings
- **WHEN** 游戏启动时加载本地化资源
- **THEN** 通过 `I18NBundle.createBundle("i18n/strings", Locale.CHINESE)` 获取所有中文文本

#### Scenario: String with format parameters
- **WHEN** 需要显示格式化文本如 "你已完成 %d 项成就"
- **THEN** 调用 `bundle.format("achievement_points_plural", count)` 返回正确中文文本

### Requirement: Sound file conversion
系统 SHALL 将 Android `res/raw/` 中的音效文件转换为 libGDX 兼容格式（OGG Vorbis），放置在 `assets/sounds/` 目录。

#### Scenario: Load sound assets
- **WHEN** SoundManager 初始化时加载音效
- **THEN** 通过 `Gdx.audio.newSound(Gdx.files.internal("sounds/gun.ogg"))` 加载成功

### Requirement: Sprite naming convention
资源管线 SHALL 建立精灵命名规范：`{category}_{name}_{frame}` （如 `tower_gun_0`、`enemy_soldier_walk_3`），确保 TextureAtlas region 名称可预测。

#### Scenario: Reference sprite by name
- **WHEN** 渲染代码需要绘制枪塔第 2 帧
- **THEN** 调用 `atlas.findRegion("tower_gun", 2)` 获取对应精灵区域

### Requirement: Asset directory structure
系统 SHALL 使用以下资源目录结构：

```
assets/
├── images/         ← TextureAtlas 文件
├── sounds/         ← OGG 音效文件
├── fonts/          ← BitmapFont 文件
└── i18n/           ← I18NBundle 文件
```

#### Scenario: All platforms share same assets
- **WHEN** Android、Desktop、HTML5 构建时
- **THEN** 三个平台使用同一份 assets 目录资源，无需平台特定资源
