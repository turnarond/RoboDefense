package com.rdefense.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.rdefense.core.RoboDefenseGame;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 桌面版启动器
 * 使用 LWJGL3 后端
 */
public class DesktopLauncher {

    public static void main(String[] args) throws Exception {
        // WSL2 下 Mesa 硬件 GL 会 SIGSEGV，需强制软件渲染
        if (isWsl() && System.getenv("LIBGL_ALWAYS_SOFTWARE") == null) {
            relaunchWithSoftwareGL(args);
            return;
        }

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("星际塔防（Robo Defense）");
        
        // 设置默认窗口大小为地图最大尺寸（24x12 网格 * 32 像素 = 768x384）加上UI区域
        int mapWidth = 24 * 32;  // 最大地图宽度
        int mapHeight = 12 * 32; // 最大地图高度
        int uiHeight = 100;      // UI区域高度（顶部状态栏 + 底部按钮）
        int windowWidth = mapWidth;
        int windowHeight = mapHeight + uiHeight;
        config.setWindowedMode(windowWidth, windowHeight);
        
        // 禁止调整窗口大小（最小和最大尺寸相同）
        config.setWindowSizeLimits(windowWidth, windowHeight, windowWidth, windowHeight);
        // 完全禁用窗口调整
        config.setResizable(false);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setWindowIcon("images/icon.png");

        new Lwjgl3Application(new RoboDefenseGame(new DesktopPlatformServices()), config);
    }

    /** 检测是否在 WSL 环境中运行 */
    private static boolean isWsl() {
        try {
            File procVersion = new File("/proc/version");
            if (!procVersion.exists()) return false;
            java.util.Scanner sc = new java.util.Scanner(procVersion);
            String content = sc.useDelimiter("\\Z").next();
            sc.close();
            String lower = content.toLowerCase();
            return lower.contains("microsoft") || lower.contains("wsl");
        } catch (Exception e) {
            return false;
        }
    }

    /** 以 LIBGL_ALWAYS_SOFTWARE=1 重新启动当前进程 */
    private static void relaunchWithSoftwareGL(String[] args) throws Exception {
        System.out.println("[DesktopLauncher] 检测到 WSL 环境，启用软件渲染模式...");

        // 构建启动命令：java [jvm-args] -cp <classpath> DesktopLauncher [app-args]
        String javaHome = System.getProperty("java.home");
        String javaExec = javaHome + File.separator + "bin" + File.separator + "java";
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExec);

        // 传递当前 JVM 参数（如 -Xmx 等）
        for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!jvmArg.startsWith("-agentlib:jdwp") && !jvmArg.startsWith("-Xrunjdwp")) {
                cmd.add(jvmArg);
            }
        }

        // 获取当前 JAR 路径
        String cp = System.getProperty("java.class.path");
        cmd.add("-cp");
        cmd.add(cp);
        cmd.add(DesktopLauncher.class.getName());

        for (String arg : args) cmd.add(arg);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Map<String, String> env = pb.environment();
        env.put("LIBGL_ALWAYS_SOFTWARE", "1");

        pb.inheritIO();
        Process proc = pb.start();
        System.exit(proc.waitFor());
    }
}

