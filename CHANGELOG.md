# Changelog

## v2.6.0 (2026-07-20) — APK Fidelity + UI Redesign

85 commits. Based on `docs/06-APK差距分析报告.md` comprehensive 60-class comparison vs original Android APK (Build 2900).

### Game Logic — APK Fidelity
- Restored `saveScore` 4-bonus settlement (20% win + 1% HP + 20% perfect + money×difficulty×2)
- Restored `RewardData.gameWon` difficulty progression system
- Fixed score divisor 500 (was 100), per-map multiplier table, kill bonus formula
- Fixed splash radius 2500→256, mine radius 2500→2048
- Fixed sell multiplier 2×→1.5×, SAM cost reduction, death frame calculation
- Fixed enemy speed scaling, burn semantics, mine flyer immunity, shockwave type 10
- Restored TOUGH_MASK dead code, enemy placement rejection check
- Added event/enemy caps, load validation, auto-save request
- Fixed 10 save fields, message slot management
- Fixed tower upgrade tree (medium gun branch error)
- Fixed Y-sorting via GridObjectOrder

### UI — Full Redesign
- **Main Menu**: "Command Center" theme — deep void, gold beacon, holo-cyan, scan-line animation
- **Battle HUD**: "Tactical Overlay" — compact header, HP≤3 pulse breathing, state in footer
- **Pause/Upgrade**: Glass-panel semi-transparent overlays
- **Shop**: Pure icon buttons (base+turret), color-state encoding
- **Achievements**: 56px rows with descriptions + progress bars
- **Title bar/Back button**: Unified to `GameScreen` base class
- CHINESE_CHARS: Added 10+ missing glyphs

### Architecture
- Extracted `GameSceneRenderer` (525 lines) + `GameInputController` (67 lines)
- `GamePlayScreen`: 931→553 lines (-41%)
- begin/end batches: 10-19→2 pairs/frame
- Removed dead code: GameRewardCalculator, HudRenderer, tower_pool

### New Features
- Mixer 5-digit selector, Starfield particles, ScoreOverlay animation
- New game confirmation dialog, keyboard shortcut hints

## v2.5.0 (2026-07-17)
Initial desktop port from Android APK. libGDX 1.12.1 / LWJGL3 backend.
