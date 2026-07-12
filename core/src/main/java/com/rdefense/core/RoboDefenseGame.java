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
    private static final String CHINESE_CHARS =
        "一万上下不个中之乌乐也习了事于人从代以件价任会传伤伦但位何余作你使保俯俱倍候值停储先光免入关兵典再冒军农冲减凝出击分初到制前剩力功加务动助努励劳区升单卖卡厂历发取受可史右号合同后吝否吧启命和品售啬喜喷器回因围固图在圭地场坚坦型域基塔填增声多大天太失奏奖好始威子存学完定实害富对导射将少尔尝就屏属工左已巷市帧帮幕干平并幸幼度庭廉开弃式引弹强当录形很得御快性恭悔想愉意慢憾戏成或战所手打扰找护拉拖择拽持按损换掉控提收攻放效敌整斯新方无时明星是显景暂更最有期未本机杀束条来极枪格棒槽模次欢止此武每比池汽没油波泰洛消混游滑滤演火炫炬炮炸点烟烧焰燃爆片版物特狂狙狱率玩现生用甲电略疯痴的看真码破础示祝禁离种科积程空突童等箭系级线练经结统续缓缩网置美翻耀老者背胜能自至航色艾节花范药莱获菜蓄行衡被裂装要视角解让训设试说调豪贝败质费贺赛赢起超趣路跳踩车转载输达过迎返还这进连迪迫迷迹退送选通速造遍道遗那部都里重量金钮钱铀链锁错长间防际院险随难集雨雷需非音顶项顿频骚高鬼鹰";

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

        // 3. 初始化渲染器（散装纹理模式，无需 TextureAtlas）
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
