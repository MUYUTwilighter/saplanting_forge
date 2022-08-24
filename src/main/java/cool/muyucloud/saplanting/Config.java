package cool.muyucloud.saplanting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

public class Config {
    private static final Config CONFIG = new Config();

    private boolean plantEnable = true;
    private boolean plantLarge = true;
    private boolean blackListEnable = true;
    private boolean allowSapling = true;
    private boolean allowCrop = false;
    private boolean allowMushroom = false;
    private boolean allowFungus = false;
    private boolean allowFlower = false;
    private boolean allowOther = false;
    private boolean showTitleOnPlayerConnected = false;
    private boolean ignoreShape = false;    // plant a sapling in a shape that would not let it grow
    private int plantDelay = 40;
    private int avoidDense = 2;
    private int playerAround = 2;
    private final Path CONFIG_PATH = FMLLoader.getGamePath().resolve("config").resolve("saplanting.json");
    private final HashSet<BlockItem> blackList = new HashSet<>();

    public static String stringPath() {
        return CONFIG.CONFIG_PATH.toString();
    }

    public static boolean isItemAllowed(BlockItem item) {
        if (CONFIG.blackListEnable && inBlackList(item)) {
            return false;
        }

        BushBlock block = ((BushBlock) item.getBlock());
        if (block instanceof SaplingBlock) {
            return CONFIG.allowSapling;
        } else if (block instanceof CropsBlock) {
            return CONFIG.allowCrop;
        } else if (block instanceof MushroomBlock) {
            return CONFIG.allowMushroom;
        } else if (block instanceof FungusBlock) {
            return CONFIG.allowFungus;
        } else if (block instanceof FlowerBlock) {
            return CONFIG.allowFlower;
        } else {
            return CONFIG.allowOther;
        }
    }

    public static boolean inBlackList(BlockItem item) {
        return CONFIG.blackList.contains(item);
    }

    public static void addToBlackList(BlockItem item) {
        CONFIG.blackList.add(item);
    }

    public static void rmFromBlackList(BlockItem item) {
        CONFIG.blackList.remove(item);
    }

    public static void clearBlackList() {
        CONFIG.blackList.clear();
    }

    public static String strBlackList() {
        StringBuilder output = new StringBuilder();
        boolean flag = false;

        for (Item item : CONFIG.blackList) {
            if (flag) {
                output.append(", ");
            } else {
                flag = true;
            }

            output.append(Registry.ITEM.getKey(item).getNamespace())
                    .append(':')
                    .append(Registry.ITEM.getKey(item).getPath());
        }

        return output.toString();
    }

    public static int blackListSize() {
        return CONFIG.blackList.size();
    }

    public static void load() {
        CONFIG.loadFromFile();
    }

    public static boolean load(String name) {
        return CONFIG.loadFromFile(name);
    }

    public static void save() {
        CONFIG.saveToFile();
    }

    public static boolean getPlantEnable() {
        return CONFIG.plantEnable;
    }

    public static boolean getPlantLarge() {
        return CONFIG.plantLarge;
    }

    public static boolean getBlackListEnable() {
        return CONFIG.blackListEnable;
    }

    public static boolean getAllowSapling() {
        return CONFIG.allowSapling;
    }

    public static boolean getAllowCrop() {
        return CONFIG.allowCrop;
    }

    public static boolean getAllowMushroom() {
        return CONFIG.allowMushroom;
    }

    public static boolean getAllowFungus() {
        return CONFIG.allowFungus;
    }

    public static boolean getAllowFlower() {
        return CONFIG.allowFlower;
    }

    public static boolean getAllowOther() {
        return CONFIG.allowOther;
    }

    public static boolean getShowTitleOnPlayerConnected() {
        return CONFIG.showTitleOnPlayerConnected;
    }

    public static boolean getIgnoreShape() {
        return CONFIG.ignoreShape;
    }

    public static int getPlantDelay() {
        return CONFIG.plantDelay;
    }

    public static int getAvoidDense() {
        return CONFIG.avoidDense;
    }

    public static int getPlayerAround() {
        return  CONFIG.playerAround;
    }

    public static void setPlantEnable(boolean plantEnable) {
        CONFIG.plantEnable = plantEnable;
    }

    public static void setPlantLarge(boolean plantLarge) {
        CONFIG.plantLarge = plantLarge;
    }

    public static void setBlackListEnable(boolean value) {
        CONFIG.blackListEnable = value;
    }

    public static void setAllowSapling(boolean value) {
        CONFIG.allowSapling = value;
    }

    public static void setAllowCrop(boolean value) {
        CONFIG.allowCrop = value;
    }

    public static void setAllowMushroom(boolean value) {
        CONFIG.allowMushroom = value;
    }

    public static void setAllowFungus(boolean value) {
        CONFIG.allowFungus = value;
    }

    public static void setAllowFlower(boolean value) {
        CONFIG.allowFlower = value;
    }

    public static void setAllowOther(boolean value) {
        CONFIG.allowOther = value;
    }

    public static void setShowTitleOnPlayerConnected(boolean value) {
        CONFIG.showTitleOnPlayerConnected = value;
    }

    public static void setIgnoreShape(boolean value) {
        CONFIG.ignoreShape = value;
    }

    public static void setPlantDelay(int plantDelay) {
        CONFIG.plantDelay = plantDelay;
    }

    public static void setAvoidDense(int avoidDense) {
        CONFIG.avoidDense = avoidDense;
    }

    public static void setPlayerAround(int playerAround) {
        CONFIG.playerAround = playerAround;
    }

    private void loadFromFile() {
        if (!Files.exists(CONFIG_PATH)) {
            Saplanting.LOGGER.info(String.format("\"saplanting.json\" does not present in target path %s, generating new one.", CONFIG_PATH));
            try {
                Files.createFile(CONFIG_PATH);
            } catch (Exception e) {
                Saplanting.LOGGER.info("Error(s) occurred during creating \"saplanting.json\"! Do I have permission?");
                e.printStackTrace();
            }
        } else {
            try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
                JsonObject jsonObject = (new Gson()).fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);

                initPlantEnable(jsonObject);
                initPlantLarge(jsonObject);
                initBlackListEnable(jsonObject);
                initAllowSapling(jsonObject);
                initAllowCrop(jsonObject);
                initAllowMushroom(jsonObject);
                initAllowFungus(jsonObject);
                initAllowFlower(jsonObject);
                initAllowOther(jsonObject);
                initShowTitleOnPlayerConnected(jsonObject);
                initIgnoreShape(jsonObject);
                initPlantDelay(jsonObject);
                initAvoidDense(jsonObject);
                initPlayerAround(jsonObject);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean loadFromFile(String name) {
        if (Files.exists(CONFIG_PATH)) {
            // try to read properties from file
            try (InputStream inputStream = Files.newInputStream(CONFIG_PATH)) {
                JsonObject jsonObject = (new Gson()).fromJson(IOUtils.toString(inputStream, StandardCharsets.UTF_8), JsonObject.class);

                switch (name) {
                    case "plantEnable":
                        return initPlantEnable(jsonObject);
                    case "plantLarge":
                        return initPlantLarge(jsonObject);
                    case "blackListEnable":
                        return initBlackListEnable(jsonObject);
                    case "allowSapling":
                        return initAllowSapling(jsonObject);
                    case "allowCrop":
                        return initAllowCrop(jsonObject);
                    case "allowMushroom":
                        return initAllowMushroom(jsonObject);
                    case "allowFungus":
                        return initAllowFungus(jsonObject);
                    case "allowFlower":
                        return initAllowFlower(jsonObject);
                    case "allowOther":
                        return initAllowOther(jsonObject);
                    case "showTitleOnPlayerConnected":
                        return initShowTitleOnPlayerConnected(jsonObject);
                    case "ignoreShape":
                        return initIgnoreShape(jsonObject);
                    case "plantDelay":
                        return initPlantDelay(jsonObject);
                    case "avoidDense":
                        return initAvoidDense(jsonObject);
                    case "playerAround":
                        return initPlayerAround(jsonObject);
                    case "blackList":
                        return initBlackList(jsonObject);
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private String stringJSON() {
        StringBuilder output = new StringBuilder();
        String indent = "    ";

        output.append('{').append('\n');
        output.append(indent).append("\"plantEnable\": ").append(plantEnable).append(",\n");
        output.append(indent).append("\"plantLarge\": ").append(plantLarge).append(",\n");
        output.append(indent).append("\"blackListEnable\": ").append(blackListEnable).append(",\n");
        output.append(indent).append("\"allowSapling\": ").append(allowSapling).append(",\n");
        output.append(indent).append("\"allowCrop\": ").append(allowCrop).append(",\n");
        output.append(indent).append("\"allowMushroom\": ").append(allowMushroom).append(",\n");
        output.append(indent).append("\"allowFungus\": ").append(allowFungus).append(",\n");
        output.append(indent).append("\"allowFlower\": ").append(allowFlower).append(",\n");
        output.append(indent).append("\"allowOther\": ").append(allowOther).append(",\n");
        output.append(indent).append("\"showTitleOnPlayerConnected\": ").append(showTitleOnPlayerConnected).append(",\n");
//        output.append(indent).append("\"ignoreShape\": ").append(ignoreShape).append(",\n");
        output.append(indent).append("\"plantDelay\": ").append(plantDelay).append(",\n");
        output.append(indent).append("\"avoidDense\": ").append(avoidDense).append(",\n");
        output.append(indent).append("\"playerAround\": ").append(playerAround).append(",\n");

        output.append(indent).append("\"blackList\": [");
        if (blackList.isEmpty()) {
            output.append("]\n");
        } else {
            boolean flag = false;
            for (Item item : blackList) {
                if (!flag) {
                    flag = true;
                } else {
                    output.append(',');
                }

                output.append('\n').append(indent).append(indent).append('\"')
                        .append(Registry.ITEM.getKey(item).getNamespace())
                        .append(':')
                        .append(Registry.ITEM.getKey(item).getPath())
                        .append('\"');
            }
            output.append('\n').append(indent).append("]\n");
        }

        output.append('}');

        return output.toString();
    }

    private void saveToFile() {
        // in case config file disappear
        if (!Files.exists(CONFIG_PATH)) {
            try {
                Files.createFile(CONFIG_PATH);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // try to save
        try (OutputStream outputStream = Files.newOutputStream(CONFIG_PATH)) {
            outputStream.write(stringJSON().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean initPlantEnable(JsonObject jsonObject) {
        try {
            this.plantEnable = jsonObject.get("plantEnable").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initPlantLarge(JsonObject jsonObject) {
        try {
            this.plantLarge = jsonObject.get("plantLarge").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initBlackListEnable(JsonObject jsonObject) {
        try {
            this.blackListEnable = jsonObject.get("blackListEnable").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowSapling(JsonObject jsonObject) {
        try {
            this.allowSapling = jsonObject.get("allowSapling").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowCrop(JsonObject jsonObject) {
        try {
            this.allowCrop = jsonObject.get("allowCrop").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowMushroom(JsonObject jsonObject) {
        try {
            this.allowMushroom = jsonObject.get("allowMushroom").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowFungus(JsonObject jsonObject) {
        try {
            this.allowFungus = jsonObject.get("allowFungus").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowFlower(JsonObject jsonObject) {
        try {
            this.allowFlower = jsonObject.get("allowFlower").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAllowOther(JsonObject jsonObject) {
        try {
            this.allowOther = jsonObject.get("allowOther").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initShowTitleOnPlayerConnected(JsonObject jsonObject) {
        try {
            this.showTitleOnPlayerConnected = jsonObject.get("showTitleOnPlayerConnected").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initIgnoreShape(JsonObject jsonObject) {
        try {
            this.ignoreShape = jsonObject.get("showTitleOnPlayerConnected").getAsBoolean();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initPlantDelay(JsonObject jsonObject) {
        try {
            this.plantDelay = jsonObject.get("plantDelay").getAsInt();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initAvoidDense(JsonObject jsonObject) {
        try {
            this.avoidDense = jsonObject.get("avoidDense").getAsInt();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initPlayerAround(JsonObject jsonObject) {
        try {
            this.playerAround = jsonObject.get("playerAround").getAsInt();
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    private boolean initBlackList(JsonObject jsonObject) {
        blackList.clear();
        JsonArray jsonArray;

        try {
            jsonArray = jsonObject.get("blackList").getAsJsonArray();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        if (jsonArray == null) {
            return true;
        }

        JsonArray finalJsonArray = jsonArray;
        new Thread(() -> {
            for (int i = 0; i < finalJsonArray.size(); i++) {
                try {
                    Item item = Registry.ITEM.get(new ResourceLocation(finalJsonArray.get(i).getAsString()));
                    if (item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof BushBlock) {
                        blackList.add(((BlockItem) item));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        return true;
    }
}
