package cool.muyucloud.saplanting;

import cool.muyucloud.saplanting.events.EntityItemEvent;
import cool.muyucloud.saplanting.command.SaplantingCommand;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.block.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.beans.Beans;
import java.util.Objects;

@Mod(
    modid = Saplanting.MOD_ID,
    name = Saplanting.MOD_NAME,
    version = Saplanting.VERSION
)
public class Saplanting {
    public static final String MOD_ID = "saplanting";
    public static final String MOD_NAME = "Saplanting";
    public static final String VERSION = "0.2.0";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final EventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    private static final Config CONFIG = new Config();
    private static final Config DEFAULT_CONFIG = new Config();

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CONFIG.setPath(event.getModConfigurationDirectory().toPath().resolve("saplanting.json"));
    }

    /**
     * This is the second initialization event. Register custom recipes
     */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Initializing.");
        LOGGER.info("Saplanting waking up! OwO");

        LOGGER.info("Registering events.");
        EVENT_BUS.register(this);
        EVENT_BUS.register(new EntityItemEvent());

        LOGGER.info("Loading config.");
        CONFIG.load();
        CONFIG.save();
    }

    /**
     * This is the final initialization event. Register actions from other mods here
     */
    @Mod.EventHandler
    public void postinit(FMLPostInitializationEvent event) {
        LOGGER.info("Updating language.");
        Translation.updateLanguage(CONFIG.getAsString("language"));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayerMP player = ((EntityPlayerMP) event.player);
        PlayerList playerList = Objects.requireNonNull(player.getServer()).getPlayerList();
        boolean isOp = playerList.getOppedPlayers().getGameProfileFromName(player.getName()) != null;   // TO TEST
        if (CONFIG.getAsBoolean("showTitleOnOpConnected")
            && isOp && !CONFIG.getAsBoolean("plantEnable")) {
            Style style = new Style();
            style.setColor(TextFormatting.GREEN);
            style.setUnderlined(true);
            style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                "/saplanting property plantEnable true"));
            ITextComponent text = new TextComponentString(String.format(Translation.translate("saplanting.onPlayerConnected.plantDisable")))
                .appendSibling(new TextComponentString(String.format(Translation.translate("saplanting.onPlayerConnected.plantDisable.click")))
                    .setStyle(style));
            player.sendMessage(text);
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        if (CONFIG.getAsBoolean("plantEnable")) {
            LOGGER.info("Saplanting is enabled now \\^o^/");
        } else {
            LOGGER.info("Saplanting is disabled QAQ");
            LOGGER.info("Use command \"/saplanting plantEnable true\" to enable saplanting");
        }
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent event) {
        LOGGER.info("Stopping item entity thread.");
        EntityItemEvent.stop();
        LOGGER.info("Dumping current properties into file.");
        CONFIG.save();
    }

    @Mod.EventHandler
    public void onRegisterCommands(FMLServerStartingEvent event) {
        LOGGER.info("Registering command \"/saplanting\".");
        event.registerServerCommand(new SaplantingCommand());
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Config getConfig() {
        return CONFIG;
    }

    public static Config getDefaultConfig() {
        return DEFAULT_CONFIG;
    }

    public static boolean isPlantItem(Item item) {
        return item instanceof ItemBlock && ((ItemBlock) item).getBlock() instanceof BlockBush;
    }

    public static boolean isPlantAllowed(Item item) {
        if (!isPlantItem(item) || (CONFIG.getAsBoolean("blackListEnable") && CONFIG.inBlackList(item))) {
            return false;
        }

        BlockBush block = ((BlockBush) ((ItemBlock) item).getBlock());
        if (block instanceof BlockSapling) {
            return CONFIG.getAsBoolean("allowSapling");
        } else if (block instanceof BlockCrops) {
            return CONFIG.getAsBoolean("allowCrop");
        } else if (block instanceof BlockFlower) {
            return CONFIG.getAsBoolean("allowFlower");
        } else if (block instanceof BlockMushroom) {
            return CONFIG.getAsBoolean("allowMushroom");
        } else {
            return CONFIG.getAsBoolean("allowOther");
        }
    }
}
