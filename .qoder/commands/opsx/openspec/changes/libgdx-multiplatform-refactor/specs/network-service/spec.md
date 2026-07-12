## ADDED Requirements

### Requirement: Network client interface definition
系统 SHALL 定义 `NetworkClient` 接口，包含以下方法：`submitScore`、`getLeaderboard`、`uploadSave`、`downloadSave`、`login`、`logout`、`isAuthenticated`。

#### Scenario: Interface available in core module
- **WHEN** core 模块代码需要调用联网功能
- **THEN** 通过 `NetworkClient` 接口调用，无需知道具体 HTTP 实现

### Requirement: Leaderboard score submission
系统 SHALL 支持在游戏胜利时将积分提交到在线排行榜，包含：玩家ID、地图、难度、积分、时间戳。

#### Scenario: Submit score after winning
- **WHEN** 玩家通关一局游戏且处于在线状态
- **THEN** 调用 `networkClient.submitScore(scoreData)` 提交积分到服务端

#### Scenario: Submit score while offline
- **WHEN** 玩家通关但处于离线状态
- **THEN** 积分存入本地队列，下次联网时自动提交

### Requirement: Leaderboard query
系统 SHALL 支持查询排行榜，支持按地图和难度筛选，支持分页。

#### Scenario: View global leaderboard
- **WHEN** 玩家在成就界面选择查看排行榜
- **THEN** 显示当前地图+难度的 Top 100 玩家积分列表

#### Scenario: View personal best
- **WHEN** 玩家查看自己的排行榜位置
- **THEN** 显示玩家在各地图的最高积分和排名

### Requirement: Cloud save upload
系统 SHALL 支持将本地游戏存档上传到云端，每个用户最多保存 5 个云存档槽位。

#### Scenario: Manual cloud save
- **WHEN** 玩家在设置中选择"上传存档到云端"
- **THEN** 当前游戏存档（JSON）通过 HTTPS 上传到服务端，关联用户 ID

#### Scenario: Auto cloud sync after game win
- **WHEN** 玩家通关且已登录且在线
- **THEN** 自动将最新的奖励积分和成就数据同步到云端

### Requirement: Cloud save download
系统 SHALL 支持从云端下载存档到本地，覆盖前需用户确认。

#### Scenario: Download cloud save to new device
- **WHEN** 玩家在新设备登录并选择"从云端恢复"
- **THEN** 列出云端所有存档槽位，选择后下载并应用

#### Scenario: Conflict resolution
- **WHEN** 本地和云端存档都存在且时间戳不同
- **THEN** 提示用户选择"使用本地"或"使用云端"，显示各自的最后修改时间

### Requirement: User authentication
系统 SHALL 支持用户登录，首期支持游客模式（设备 ID）和第三方 OAuth（Google）。

#### Scenario: Guest login
- **WHEN** 用户首次启动游戏
- **THEN** 自动以游客身份注册（设备 ID），无需手动操作

#### Scenario: Google OAuth login
- **WHEN** 用户在设置中选择"绑定 Google 账号"
- **THEN** 跳转 OAuth 流程，绑定成功后数据关联到 Google 账号

#### Scenario: Cross-device login
- **WHEN** 用户在新设备上使用 Google 登录
- **THEN** 自动关联之前的游戏数据（积分、成就、云存档）

### Requirement: Offline-first architecture
系统 SHALL 采用离线优先策略，所有游戏功能在无网络时正常工作，联网功能静默降级。

#### Scenario: Play without internet
- **WHEN** 设备完全离线
- **THEN** 游戏所有核心功能（战斗、存档、设置）正常工作，排行榜/云存档功能显示"离线"状态

#### Scenario: Network restored
- **WHEN** 设备从离线恢复到在线
- **THEN** 后台自动同步待提交的积分和存档，无需用户干预

### Requirement: REST API protocol
客户端与服务端 SHALL 使用 RESTful JSON API 通信，HTTPS 加密，Bearer Token 认证。

#### Scenario: API request with authentication
- **WHEN** 客户端调用任何需要认证的 API
- **THEN** 请求头包含 `Authorization: Bearer <token>`，服务端验证 token 有效性
