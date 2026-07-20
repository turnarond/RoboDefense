package com.rdefense.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.rdefense.core.config.OptionsData;
import com.rdefense.core.platform.PlatformServices;
import com.rdefense.core.audio.SoundManager;
import com.rdefense.core.platform.libgdx.LibGdxAudio;
import com.rdefense.core.platform.libgdx.LibGdxRenderer;
import com.rdefense.core.platform.libgdx.LibGdxStorage;
import com.rdefense.core.platform.libgdx.NoOpNetworkClient;
import com.rdefense.core.game.AchievementData;
import com.rdefense.core.game.RewardData;
import com.rdefense.core.render.AchievementRenderer;
import com.rdefense.core.render.CameraManager;
import com.rdefense.core.render.GameLoop;
import com.rdefense.core.save.PlayerPrefs;
import com.rdefense.core.save.db.GameSaveManager;
import com.rdefense.core.save.db.SQLiteSaveManager;
import com.rdefense.core.scene.GameScreen;
import com.rdefense.core.scene.MainMenuScreen;

/**
 * 星际塔防主游戏类
 * 作为所有平台共享的入口点，整合渲染、音频、输入、存档等系统
 */
public class RoboDefenseGame extends ApplicationAdapter {

    // 游戏界面所需的全部汉字（来自 strings_zh.properties）
    // 游戏界面全部汉字（从 strings_zh.properties + 所有 UI 源码中提取，897 个唯一字符）
    private static final String CHINESE_CHARS =
        "艾碍安按暗把吧百败板版半帮榜棒包保爆碑贝备背倍被本崩比币必闭壁避边编变便遍标表别冰兵并波播不布步部裁彩踩菜参操槽册侧测层插查察差拆产尝常厂场超朝车称成承乘程痴池迟持尺冲抽出初除础储处触穿传串窗创垂纯戳此次从存寸错达打大代带待单但弹当挡档导到道得地的登等低迪敌底递第典点电店调掉迭叠顶定丢动冻都抖读度渡端短段断队对顿多额而尔二发法翻反返范方防仿访放飞非费分风封疯否服浮符俯辅父负附复赋富覆该改盖感干高告割格隔个各给根更工公功攻供恭共构够估固关观管光归圭规鬼滚果过哈还害含函汉憾航毫豪好号耗合何和核贺黑很横衡轰红后候弧户护花滑化画话坏欢环缓幻换黄灰恢回悔会绘混活火或获击机积基激及级极即集辑计记际迹继加家甲价坚间监兼检减剪简见件建健舰渐溅键箭将奖降交角较阶接节结捷解界金仅进近禁经精景警径静镜久旧就狙居局矩举具炬俱据距聚决觉绝军卡开槛看康考科可克刻客空控口库跨块快宽狂况框溃扩括拉来莱赖栏婪蓝览劳老乐了雷类累冷离里理力历立励利例粒连廉练链两亮量列裂邻临吝灵零留流录路伦轮逻洛络落率绿滤略码卖满曼慢锚冒没枚每美门们迷免面描秒名明命摸模魔末默某母目墓幕哪那纳难内能你逆凝钮农努排盘判旁抛炮配喷碰批匹偏片飘频品平屏迫普期齐启起气弃汽器千迁签前钱枪强墙且切轻清情擎请求区曲取去趣圈权全却确然燃染让扰人认任仍容如入若塞赛三散骚色啬杀筛删闪伤商上烧少蛇舍设射伸身升生声胜剩失施时实史使始示世市式事饰试视是适释收手首受售殊输属署鼠束述数刷顺说烁思斯死四送搜素速算随损缩所索锁它塔台太态泰贪坦特提题体替天添填条跳贴庭停通同童统头投透突图推退拖外弯完玩万亡网往望威围维尾为未位文纹问我乌无武务物误雾析息习喜戏系下先闲显险现限线相享想向项巷象像消小效协写谢心新信星行形型幸性修须需许序续蓄旋选炫渲学血询循训烟延沿颜演验焰央药要耀也业一依移遗已以义议异译意溢因音引隐应英鹰迎赢映硬永用优由油铀游有右幼于余愉与雨语狱预域阈遇御元原圆缘源院约跃云允运再在载暂蚤造则责择增炸展占战张长障找照罩遮折者这着针帧真阵整正证之支知执直值植止只指至志制质致置中终种重周轴逐主助注祝拽转装状撞追缀准桌灼资子自字踪总走奏足阻组最左作坐座做▶⏸⏩⏹☰草混迹院？！▲▼←→↑↓×";

    private PlatformServices services;
    private LibGdxRenderer renderer;
    private CameraManager camera;
    private BitmapFont font;
    private GameLoop gameLoop;
    private GameScreen currentScreen;
    private OptionsData optionsData;
    /** 数据库存档管理器 */
    private GameSaveManager gameSaveManager;
    private PlayerPrefs playerPrefs;
    /** 成就渲染器 */
    private AchievementRenderer achievementRenderer;
    /** 音效管理器 */
    private SoundManager soundManager;
    // 若在初始化阶段收到短消息回退而当前 screen 为空，则先缓存，后续 setScreen 时展示
    private String pendingShortMessage = null;

    public RoboDefenseGame() {
        this(null);
    }

    public RoboDefenseGame(PlatformServices services) {
        this.services = services;
    }

    @Override
    public void create() {
        // 1. 初始化平台服务
        if (services == null) {
            services = new PlatformServices(
                    null,
                    new LibGdxAudio(),
                    new LibGdxStorage(),
                    null,
                    new NoOpNetworkClient()
            );
        }

        // 2. 用 FreeType 从 simhei.ttf 生成支持中文的位图字体
        font = buildChineseFont(18);

        // 3. 初始化 FastRandom（必须在任何 Screen 创建前调——LevelSelectScreen 构造函数用它生成 mixer 种子）
        com.rdefense.core.game.FastRandom.init();

        // 4. 初始化渲染器（散装纹理模式，无需 TextureAtlas）
        renderer = new LibGdxRenderer(font);
        camera = new CameraManager();
        if (services.getRenderer() == null) {
            services.setRenderer(renderer);
        }

        // 3.5. 初始化选项系统
        optionsData = new OptionsData(services.getStorage());
        applyOptions();

        // 3.6. 初始化存档系统（SQLite数据库）
        // 使用应用程序本地目录，避免权限问题
        String saveDir = "saves/";
        gameSaveManager = new SQLiteSaveManager(saveDir + "game_saves.db");

        // 3.7. 初始化偏好设置（使用数据库存储）
        playerPrefs = new PlayerPrefs(gameSaveManager);

        // 3.8. 初始化成就系统
        AchievementData.init(gameSaveManager);
        achievementRenderer = new AchievementRenderer();

        // 3.9. 初始化奖励系统
        RewardData.init(gameSaveManager);

        // 标记游戏已正常启动（用于崩溃检测，与原版 GAME_STARTED_OK_PREF_STR 一致）
        playerPrefs.putGameStartedOk(false);
        // 注册 HD 回退回调：发生 OOM 等回退时禁用 HQ，并持久化设置
        renderer.setHdFallbackListener(new com.rdefense.core.platform.libgdx.LibGdxRenderer.HdFallbackListener() {
            @Override
            public void onHdFallback(String reason) {
                Gdx.app.log("RoboDefense", "HD fallback: " + reason + " — disabling HQ mode");
                try {
                    if (optionsData.setOptionValue(OptionsData.HQ_GRAPHICS_MODE, false)) {
                        applyOptions();
                    }
                } catch (Throwable t) {
                    Gdx.app.error("RoboDefense", "Failed to disable HQ option", t);
                }
                // 在 UI 层显示短消息通知用户
                try {
                    if (currentScreen != null) {
                        currentScreen.showShortMessage("已禁用高品质图像（内存不足）");
                    } else {
                        pendingShortMessage = "已禁用高品质图像（内存不足）";
                    }
                } catch (Throwable t) {
                    Gdx.app.error("RoboDefense", "Failed to show fallback message", t);
                }
            }
        });

        // Debug: 根据系统属性触发一次强制 HD 回退，便于验证短消息显示
        try {
            String debugProp = System.getProperty("rdefense.debugForceHdFallback");
            Gdx.app.log("RoboDefense", "debugForceHdFallback=" + debugProp);
            if ("true".equalsIgnoreCase(debugProp)) {
                if (renderer instanceof com.rdefense.core.platform.libgdx.LibGdxRenderer) {
                    Gdx.app.log("RoboDefense", "Invoking forceHdFallback via debug property");
                    ((com.rdefense.core.platform.libgdx.LibGdxRenderer) renderer).forceHdFallback("debug property");
                    Gdx.app.log("RoboDefense", "forceHdFallback invoked");
                } else {
                    Gdx.app.log("RoboDefense", "renderer is not LibGdxRenderer, cannot force fallback");
                }
            }
        } catch (Throwable ignored) { }

        // 4. 初始化游戏循环
        gameLoop = new GameLoop();
        gameLoop.init();

        // 5. 初始化相机
        camera.init(
                800,
                600,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight(),
                32
        );

        // Debug: 若设置了直接跳过主菜单，则直接进入游戏场景，便于验证短消息
        try {
            String debugStartGame = System.getProperty("rdefense.debugStartGame");
            Gdx.app.log("RoboDefense", "debugStartGame=" + debugStartGame);
            if ("true".equalsIgnoreCase(debugStartGame)) {
                setScreen(new com.rdefense.core.scene.GamePlayScreen(this));
            } else {
                setScreen(new MainMenuScreen(this));
            }
        } catch (Throwable t) {
            Gdx.app.error("RoboDefense", "Failed to apply debugStartGame", t);
            setScreen(new MainMenuScreen(this));
        }

        // 6. 初始化音频
        services.getAudio().loadSound("fire", "sounds/fire.ogg");
        services.getAudio().loadSound("gun", "sounds/gun.ogg");
        services.getAudio().loadSound("ice", "sounds/ice.ogg");
        services.getAudio().loadSound("mortar", "sounds/mortar.ogg");
        services.getAudio().loadSound("rocket", "sounds/rocket.ogg");
        services.getAudio().loadSound("achievement", "sounds/achievement.ogg");

        // 6.5. 初始化音效管理器
        soundManager = SoundManager.getInstance();
        soundManager.init(services);
        soundManager.preloadSounds();

        // 7. 进入主菜单场景
        setScreen(new MainMenuScreen(this));

        Gdx.app.log("RoboDefense", "游戏初始化完成");
    }

    /**
     * 使用 FreeType 生成支持中文的位图字体
     * 若字体文件不存在则回退到默认 ASCII 字体
     */
    private BitmapFont buildChineseFont(int size) {
        if (!Gdx.files.internal("fonts/simhei.ttf").exists()) {
            Gdx.app.log("RoboDefense", "未找到 fonts/simhei.ttf，使用默认字体（中文不可见）");
            BitmapFont fallback = new BitmapFont();
            fallback.getData().setScale(1.5f);
            return fallback;
        }
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("fonts/simhei.ttf"));
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = size;
        param.characters = FreeTypeFontGenerator.DEFAULT_CHARS + CHINESE_CHARS;
        BitmapFont generated = generator.generateFont(param);
        generator.dispose();
        Gdx.app.log("RoboDefense", "中文字体加载成功（size=" + size + "）");
        return generated;
    }

    @Override
    public void render() {
        if (currentScreen != null) {
            currentScreen.render(Gdx.graphics.getDeltaTime());
        }
    }

    public void applyOptions() {
        if (optionsData == null || services == null) {
            return;
        }

        if (services.getAudio() != null) {
            services.getAudio().setMasterVolume(
                    optionsData.optionValue(OptionsData.ENABLE_SOUND) ? 1.0f : 0.0f);
        }

        if (soundManager != null) {
            soundManager.setSoundEnabled(optionsData.optionValue(OptionsData.ENABLE_SOUND));
        }

        if (renderer != null) {
            renderer.setUseLinearFiltering(
                    optionsData.optionValue(OptionsData.BITMAP_FILTERING)
                            || optionsData.optionValue(OptionsData.HQ_GRAPHICS_MODE));
            renderer.setUse16BitBackground(optionsData.optionValue(OptionsData.BACKGROUND_16BIT));
            renderer.setUseHQGraphics(optionsData.optionValue(OptionsData.HQ_GRAPHICS_MODE));
        }

        if (gameLoop != null) {
            gameLoop.setLowerFpsMode(optionsData.optionValue(OptionsData.LOWER_FPS));
        }

        services.setScreenTimeoutLock(optionsData.optionValue(OptionsData.SCREEN_TIMEOUT_LOCK));
    }

    public void setScreen(GameScreen screen) {
        if (this.currentScreen != null) {
            this.currentScreen.hide();
        }
        this.currentScreen = screen;
        screen.show();
        // 消息由具体支持短消息的场景自行消费，避免在不支持该功能的主菜单上丢失。
    }

    /**
     * 消费并清除可能存在的待展示短消息（用于在场景切换后显示创建期间的通知）
     */
    public String consumePendingShortMessage() {
        String msg = pendingShortMessage;
        pendingShortMessage = null;
        return msg;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (camera != null) {
            camera.updateScreenSize(width, height);
        }
        if (currentScreen != null) {
            currentScreen.resize(width, height);
        }
    }

    @Override
    public void pause() {
        if (currentScreen != null) {
            currentScreen.pause();
        }
    }

    @Override
    public void resume() {
        if (currentScreen != null) {
            currentScreen.resume();
        }
    }

    @Override
    public void dispose() {
        if (currentScreen != null) {
            currentScreen.dispose();
        }
        if (renderer != null) {
            renderer.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (services != null && services.getAudio() != null) {
            services.getAudio().dispose();
        }
        if (gameSaveManager != null) {
            gameSaveManager.close();
        }
        Gdx.app.log("RoboDefense", "资源释放完成");
    }

    public PlatformServices getServices() { return services; }
    public GameLoop getGameLoop() { return gameLoop; }
    public CameraManager getCamera() { return camera; }
    public OptionsData getOptions() { return optionsData; }
    public PlayerPrefs getPlayerPrefs() { return playerPrefs; }
    /** 获取数据库存档管理器 */
    public GameSaveManager getGameSaveManager() { return gameSaveManager; }
    /** 获取成就渲染器 */
    public AchievementRenderer getAchievementRenderer() { return achievementRenderer; }
    /** 获取音效管理器 */
    public SoundManager getSoundManager() { return soundManager; }
}
