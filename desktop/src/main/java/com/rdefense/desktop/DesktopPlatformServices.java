package com.rdefense.desktop;

import com.rdefense.core.platform.GameAudio;
import com.rdefense.core.platform.GameInputHandler;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.platform.GameStorage;
import com.rdefense.core.platform.PlatformServices;
import com.rdefense.core.platform.libgdx.LibGdxAudio;
import com.rdefense.core.platform.libgdx.LibGdxStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 桌面平台专用服务扩展。
 */
public class DesktopPlatformServices extends PlatformServices {

    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d{1,3})%?");

    private LibGdxAudio audioInstance;
    private LibGdxStorage storageInstance;

    public DesktopPlatformServices() {
        super(null, null, null, null, null);
    }

    public DesktopPlatformServices(GameRenderer renderer, GameAudio audio,
                                   GameStorage storage, GameInputHandler inputHandler) {
        super(renderer, audio, storage, inputHandler, null);
    }

    @Override
    public GameAudio getAudio() {
        if (super.getAudio() != null) {
            return super.getAudio();
        }
        if (audioInstance == null) {
            audioInstance = new LibGdxAudio();
        }
        return audioInstance;
    }

    @Override
    public GameStorage getStorage() {
        if (super.getStorage() != null) {
            return super.getStorage();
        }
        if (storageInstance == null) {
            storageInstance = new LibGdxStorage();
        }
        return storageInstance;
    }

    @Override
    public void dispose() {
        if (audioInstance != null) {
            audioInstance.dispose();
        }
        if (storageInstance != null) {
            storageInstance.dispose();
        }
        super.dispose();
    }

    @Override
    public void setScreenTimeoutLock(boolean lock) {
        // 桌面平台没有标准 Java API 来控制系统屏幕锁定/休眠。
        // 此处保留 no-op，避免在 core 逻辑中抛出异常。
    }

    @Override
    public int getBatteryLevel() {
        try {
            return queryBatteryLevel();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int queryBatteryLevel() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return queryBatteryWindows();
        }
        if (os.contains("mac")) {
            return queryBatteryMac();
        }
        if (os.contains("nux") || os.contains("linux")) {
            return queryBatteryLinux();
        }
        return -1;
    }

    private static int queryBatteryWindows() {
        String[] command = {"cmd", "/c", "wmic PATH Win32_Battery Get EstimatedChargeRemaining /Value"};
        return runCommandAndExtractInt(command, PERCENT_PATTERN);
    }

    private static int queryBatteryMac() {
        String[] command = {"pmset", "-g", "batt"};
        return runCommandAndExtractInt(command, PERCENT_PATTERN);
    }

    private static int queryBatteryLinux() {
        File powerSupply = new File("/sys/class/power_supply");
        if (!powerSupply.exists() || !powerSupply.isDirectory()) {
            return -1;
        }
        File[] items = powerSupply.listFiles();
        if (items == null) {
            return -1;
        }
        for (File item : items) {
            if (!item.isDirectory()) continue;
            File typeFile = new File(item, "type");
            String type = readFile(typeFile);
            if (type == null) continue;
            if (!type.trim().equalsIgnoreCase("Battery") && !item.getName().toLowerCase().contains("bat")) {
                continue;
            }
            int capacity = readCapacity(new File(item, "capacity"));
            if (capacity >= 0) {
                return capacity;
            }
        }
        return -1;
    }

    private static int readCapacity(File file) {
        String value = readFile(file);
        if (value == null) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignore) {
            return -1;
        }
    }

    private static int runCommandAndExtractInt(String[] command, Pattern pattern) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        try {
                            return Integer.parseInt(matcher.group(1));
                        } catch (NumberFormatException ignore) {
                            // ignore and continue
                        }
                    }
                }
            }
            process.destroy();
        } catch (IOException ignored) {
        }
        return -1;
    }

    private static String readFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        } catch (IOException ignored) {
            return null;
        }
    }
}
