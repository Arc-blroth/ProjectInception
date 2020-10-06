package ai.arcblroth.projectInception.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ProjectInceptionConfig {

    public static int DISPLAY_SCALE = 64;
    public static String INCEPTION_EXTRA_VM_ARGS = "";
    public static boolean USE_TECHREBORN_RECIPES = false;
    // This make the child process not actually init Minecraft
    // so that I can test things without destroying my computer
    public static boolean USE_FAUX_INNER = false;

    public static int TATERWEBZ_SCALE = 128;
    public static String TATERWEBZ_HOME_PAGE = "https://google.com/";
    public static boolean WARN_INCOMPATIBLE_JRE = true;

    private static File getConfigFile() throws IOException {
        File f = FabricLoader.getInstance().getConfigDir().resolve("project_inception.cfg").toFile();
        if(!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        return f;
    }

    public static void load() {
        try {
            File configFile = getConfigFile();
            Properties properties = new Properties();
            FileInputStream fis = new FileInputStream(configFile);
            properties.load(fis);
            DISPLAY_SCALE = parseIntOrDefault(properties.getProperty("inception_display_scale"), 64);
            INCEPTION_EXTRA_VM_ARGS = properties.getProperty("inception_extra_vm_args", "");
            USE_TECHREBORN_RECIPES = parseBooleanOrDefault(properties.getProperty("use_techreborn_recipes"), false);
            USE_FAUX_INNER = parseBooleanOrDefault(properties.getProperty("inception_use_faux_inner"), false);
            TATERWEBZ_SCALE = parseIntOrDefault(properties.getProperty("taterwebz_display_scale"), 128);
            TATERWEBZ_HOME_PAGE = properties.getProperty("taterwebz_home_page", "https://google.com/");
            WARN_INCOMPATIBLE_JRE = parseBooleanOrDefault(properties.getProperty("warn_incompatible_jre"), true);
            fis.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config!");
        }
    }

    public static void save() {
        try {
            File configFile = getConfigFile();
            Properties properties = new Properties();
            properties.put("inception_display_scale", Integer.toString(DISPLAY_SCALE));
            properties.put("inception_extra_vm_args", INCEPTION_EXTRA_VM_ARGS);
            properties.put("use_techreborn_recipes", Boolean.toString(USE_TECHREBORN_RECIPES));
            properties.put("inception_use_faux_inner", Boolean.toString(USE_FAUX_INNER));
            properties.put("taterwebz_display_scale", Integer.toString(TATERWEBZ_SCALE));
            properties.put("taterwebz_home_page", TATERWEBZ_HOME_PAGE);
            properties.put("warn_incompatible_jre", Boolean.toString(WARN_INCOMPATIBLE_JRE));
            FileOutputStream fos = new FileOutputStream(configFile);
            properties.store(fos, "Project Inception Config");
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config!");
        }
    }

    private static int parseIntOrDefault(String in, int defaultVal) {
        if(in == null) return defaultVal;
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static boolean parseBooleanOrDefault(String in, boolean defaultVal) {
        if(in == null) return defaultVal;
        try {
            return Boolean.parseBoolean(in);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

}
