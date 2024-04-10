package net.just_s.sds;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static net.just_s.sds.SDSMod.config;

public class ConfigurationManager
{
    private static final String CONFIG_VERSION = FabricLoader.getInstance().getModContainer("sds").get().getMetadata().getVersion().getFriendlyString();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("SDS.json").toFile();

    public static void loadConfig() {
        try {
            if (configFile.exists()) {
                StringBuilder contentBuilder = new StringBuilder();
                try (Stream<String> stream = Files.lines(configFile.toPath(), StandardCharsets.UTF_8)) {
                    stream.forEach(s -> contentBuilder.append(s).append("\n"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                config = gson.fromJson(contentBuilder.toString(), Config.class);
            } else {
                config = new Config();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        setConfig(config);
    }

    public static void saveConfig() {
        config.lastLoadedVersion = CONFIG_VERSION;
        try {
            FileWriter fileWriter = new FileWriter(configFile);
            fileWriter.write(gson.toJson(getConfig()));
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onInit() {
        if(!configFile.exists()) {
            saveConfig();
        }else{
            loadConfig();
            if(!Objects.equals(config.lastLoadedVersion, CONFIG_VERSION)) saveConfig();
        }
    }

    public static void setConfig(Config config) {
        SDSMod.config = config;
    }

    public static Config getConfig() {
        return config;
    }

    public static class Config {
        public DataList allowed;
        public DataList forbidden;
        public Messages messages;
        public boolean whitelist = false;

        public static class Messages{
            public String select = "Property «%s» was selected (%s).";
            public String change = "Property «%s» was modified (%s).";
            public String nomodify = "This block is not modifiable.";
            public String notfound = "Properties for this block weren't found.";
        }

        public static class DataList {
            public Map<String, List<String>> blocks = Maps.newHashMap();
            public Set<String> properties = Sets.newHashSet();
            public Set<String> tags = Sets.newHashSet();
        }

        public Config(){
            this.messages = new Messages();
            this.forbidden = new DataList();
            this.allowed = new DataList();
        }
        private String lastLoadedVersion = "";
    }
}
