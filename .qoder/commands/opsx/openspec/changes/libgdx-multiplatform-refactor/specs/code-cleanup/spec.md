## ADDED Requirements

### Requirement: Remove embedded AndroidX sources
系统 SHALL 删除 `app/src/main/java/androidx/`、`app/src/main/java/android/support/` 和 `app/src/main/java/com/google/devtools/` 下的所有文件（约 400 个），改用 Gradle 依赖引入。

#### Scenario: Build after removal
- **WHEN** 删除内嵌源码并添加对应 Gradle 依赖后编译
- **THEN** 项目编译成功，功能不变

### Requirement: Unify R class references
系统 SHALL 将所有 `bin.p001mt.plus.TranslationData.R` 引用替换为标准 `com.magicwach.rdefense.R`，并删除手动维护的 `R.java` 和 `C0054R.java`。

#### Scenario: Resource reference after cleanup
- **WHEN** 代码中引用 `R.string.new_game`
- **THEN** 编译器解析到 Gradle 自动生成的 R 类，资源 ID 正确

### Requirement: Rename obfuscated classes
系统 SHALL 将 JADX 生成的混淆类名重命名为有意义的名称：

| 原名 | 新名 | 用途 |
|------|------|------|
| C0047C | Constants | 全局常量（方向、网格大小、限制值） |
| C0052G | GridConfig | 网格尺寸配置和屏幕适配 |
| C0054R | 删除 | 重复的 R 类副本 |

#### Scenario: Code references updated
- **WHEN** 重命名完成后编译
- **THEN** 所有引用 `C0047C.DIRECTION_RIGHT` 更新为 `Constants.DIRECTION_RIGHT`，编译通过

### Requirement: Package restructuring
系统 SHALL 将 `com.magicwach.rdefense` 下的类按职责分包：

```
com.magicwach.rdefense/
├── core/      ← GameState, Enemy, GameTower, Bullet, GameEvent
├── grid/      ← MovementGrid, CollisionGrid, GridObject, GridObjectOrder
├── level/     ← LevelData, LevelDataGenerator, MixerLevelGenerator
├── data/      ← TowerData, EnemyData, BulletData, ExplosionData, RewardData
├── render/    ← Display, ImageLoader, Starfield, ActiveTower
├── ui/        ← TowerButton, UpgradeButton, UpgradeDialog, HUD 组件
├── audio/     ← SoundManager
├── save/      ← QuickSave, SDBackup
├── activity/  ← 所有 Activity
└── util/      ← Vector, VectorLookup, FastRandom, NumberFormatter, Profiler
```

#### Scenario: Import paths updated
- **WHEN** 分包重组完成后编译
- **THEN** 所有 import 路径正确更新，编译通过

### Requirement: Remove dead code
系统 SHALL 识别并删除反编译产生的无用代码，包括：未使用的 `META-INF/` 文件、`build-data.properties`、`empty_asset_generated_by_bazel~`。

#### Scenario: Clean project structure
- **WHEN** 清理完成后
- **THEN** `app/src/main/` 仅包含 `java/`、`res/`、`AndroidManifest.xml`
