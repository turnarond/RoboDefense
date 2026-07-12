## ADDED Requirements

### Requirement: JSON-based save format
系统 SHALL 使用 JSON 格式存储游戏存档，替代 Java ObjectOutputStream 序列化。存档文件包含版本号字段。

#### Scenario: Save game state
- **WHEN** 游戏触发存档（手动或自动每10关）
- **THEN** 生成 JSON 文件包含：version、difficulty、money、health、score、towers[]、enemies[]、levelState

#### Scenario: Load game state
- **WHEN** 玩家选择"继续游戏"
- **THEN** 读取 JSON 存档，校验 version 字段，恢复完整游戏状态

### Requirement: Save format versioning
系统 SHALL 在存档中包含 `version` 整数字段，加载时根据版本号执行向前兼容迁移。

#### Scenario: Load older version save
- **WHEN** 加载 version=1 的存档，当前版本为 2
- **THEN** 执行 v1→v2 迁移逻辑（如补充新增字段默认值），成功加载

#### Scenario: Reject incompatible future version
- **WHEN** 加载 version=99 的存档（高于当前版本）
- **THEN** 提示"存档版本过高，请更新游戏"，不尝试加载

### Requirement: Cross-platform save compatibility
存档格式 SHALL 在 Android、Desktop、Web 三个平台间完全互通。

#### Scenario: Transfer save from Android to Desktop
- **WHEN** 将 Android 上的存档 JSON 复制到 Desktop 存档目录
- **THEN** Desktop 版可正常加载该存档

### Requirement: Legacy save migration
Android 版 SHALL 提供一次性迁移工具，将旧 Java 序列化存档转换为新 JSON 格式。

#### Scenario: First launch after upgrade
- **WHEN** 用户更新到新版 APK，本地存在旧格式存档
- **THEN** 自动检测并迁移为 JSON 格式，迁移前备份原文件，迁移成功后提示"存档已升级"

#### Scenario: Migration failure
- **WHEN** 旧存档文件损坏导致迁移失败
- **THEN** 提示"存档迁移失败"，保留原文件不删除，游戏仍可正常启动（无存档状态）

### Requirement: Preferences storage
系统 SHALL 使用统一的键值对接口存储游戏偏好设置（难度、选项、奖励等级等），各平台实现：Android=SharedPreferences，Desktop=文件，Web=LocalStorage。

#### Scenario: Save option toggle
- **WHEN** 玩家在设置界面切换"开启声音"
- **THEN** 调用 `storage.savePreference("sound_enabled", "true")` 持久化

### Requirement: Auto-save mechanism
系统 SHALL 每 10 关自动保存一次，以及在玩家主动退出时保存。

#### Scenario: Auto-save every 10 levels
- **WHEN** 玩家通过第 10/20/30/... 关
- **THEN** 自动执行完整存档（静默，不打断游戏）

#### Scenario: Save on quit
- **WHEN** 玩家选择"保存并退出"
- **THEN** 保存当前完整状态后退出到主菜单

### Requirement: Cloud sync interface
存档系统 SHALL 预留云同步接口，当 `NetworkClient` 可用时自动将存档上传/下载。

#### Scenario: Auto-upload after save
- **WHEN** 本地存档完成且用户已登录且在线
- **THEN** 后台异步上传存档到云端，不阻塞游戏
