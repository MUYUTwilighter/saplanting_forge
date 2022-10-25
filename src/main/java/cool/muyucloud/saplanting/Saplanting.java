package cool.muyucloud.saplanting;

import cool.muyucloud.saplanting.events.ItemEntityEvent;
import cool.muyucloud.saplanting.util.Command;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.block.*;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod("saplanting")
public class Saplanting {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final IEventBus EVENT_BUS = MinecraftForge.EVENT_BUS;
    private static final Config CONFIG = new Config();
    private static final Config DEFAULT_CONFIG = new Config();

    public Saplanting() {
        LOGGER.info("Initializing.");
        LOGGER.info("Saplanting waking up! OwO");

        LOGGER.info("Registering events.");
        EVENT_BUS.addListener(ItemEntityEvent::onItemDrop);
        EVENT_BUS.addListener(ItemEntityEvent::onItemExpire);
        EVENT_BUS.addListener(Saplanting::onPlayerLoggedIn);
        EVENT_BUS.addListener(Saplanting::onServerStarted);
        EVENT_BUS.addListener(Saplanting::onServerStopped);
        EVENT_BUS.addListener(Saplanting::onRegisterCommands);

        LOGGER.info("Loading config.");
        CONFIG.load();
        CONFIG.save();

        LOGGER.info("Updating language.");
        Translation.updateLanguage(CONFIG.getAsString("language"));
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayerEntity player = ((ServerPlayerEntity) event.getPlayer());
        PlayerList playerList = Objects.requireNonNull(player.getServer()).getPlayerList();
        if (CONFIG.getAsBoolean("showTitleOnOpConnected")
            && playerList.isOp(player.getGameProfile())
            && !CONFIG.getAsBoolean("plantEnable")) {
            player.displayClientMessage(new StringTextComponent(String.format(Translation.translate("saplanting.onPlayerConnected.plantDisable")))
                    .append(new StringTextComponent(String.format(Translation.translate("saplanting.onPlayerConnected.plantDisable.click")))
                        .setStyle(Style.EMPTY
                            .withColor(Color.parseColor("green"))
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                "/saplanting property plantEnable true"))))
                , false);
        }
    }

    private static void onServerStarted(FMLServerStartedEvent event) {
        if (CONFIG.getAsBoolean("plantEnable")) {
            LOGGER.info("Saplanting is enabled now \\^o^/");
        } else {
            LOGGER.info("Saplanting is disabled QAQ");
            LOGGER.info("Use command \"/saplanting plantEnable true\" to enable saplanting");
        }
    }

    private static void onServerStopped(FMLServerStoppedEvent event) {
        LOGGER.info("Stopping item entity thread.");
        ItemEntityEvent.stop();
        LOGGER.info("Dumping current properties into file.");
        CONFIG.save();
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering command \"/saplanting\".");
        Command.register(event.getDispatcher(), event.getEnvironment().equals(Commands.EnvironmentType.DEDICATED));
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
        return item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof BushBlock;
    }

    public static boolean isPlantAllowed(Item item) {
        if (!isPlantItem(item) || (CONFIG.getAsBoolean("blackListEnable") && CONFIG.inBlackList(item))) {
            return false;
        }

        BushBlock block = ((BushBlock) ((BlockItem) item).getBlock());
        if (block instanceof SaplingBlock) {
            return CONFIG.getAsBoolean("allowSapling");
        } else if (block instanceof CropsBlock) {
            return CONFIG.getAsBoolean("allowCrop");
        } else if (block instanceof FungusBlock) {
            return CONFIG.getAsBoolean("allowFungus");
        } else if (block instanceof FlowerBlock) {
            return CONFIG.getAsBoolean("allowFlower");
        } else if (block instanceof MushroomBlock) {
            return CONFIG.getAsBoolean("allowMushroom");
        } else {
            return CONFIG.getAsBoolean("allowOther");
        }
    }
}
