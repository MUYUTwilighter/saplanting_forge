package cool.muyucloud.saplanting;

import cool.muyucloud.saplanting.command.SaplantingCommand;
import cool.muyucloud.saplanting.events.ItemEntityEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TranslationTextComponent;
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
public class Saplanting
{
    public static final Logger LOGGER = LogManager.getLogger();
    public static final IEventBus EVENT_BUS = MinecraftForge.EVENT_BUS;

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
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayerEntity player = ((ServerPlayerEntity) event.getPlayer());
        PlayerList playerList = Objects.requireNonNull(player.getServer()).getPlayerList();
        if (Config.getShowTitleOnPlayerConnected() && playerList.isOp(player.getGameProfile()) && !Config.getPlantEnable()) {
            player.displayClientMessage(new TranslationTextComponent("saplanting.info.chat.onPlayerConnected.disabled"), false);
        }
    }

    private static void onServerStarted(FMLServerStartedEvent event) {
        if (Config.getPlantEnable()) {
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
        Config.save();
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering command \"/saplanting\".");
        SaplantingCommand.register(event.getDispatcher());
    }
}
