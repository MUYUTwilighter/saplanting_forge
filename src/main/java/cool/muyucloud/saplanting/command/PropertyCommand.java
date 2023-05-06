package cool.muyucloud.saplanting.command;

import cool.muyucloud.saplanting.Saplanting;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PropertyCommand extends CommandBase {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Config DEFAULT_CONFIG= Saplanting.getDefaultConfig();
    private static final Style FAILURE = new Style()
        .setColor(TextFormatting.RED);

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "property";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new CommandNotFoundException();
        } else if (args.length == 1) {
            String property = args[0];
            if (!CONFIG.getKeySet().contains(property)) {
                throw new CommandNotFoundException();
            }
            this.getProperty(property, sender);
        } else if (args.length == 2) {
            String property = args[0];
            if (!CONFIG.getKeySet().contains(property)) {
                throw new CommandNotFoundException();
            }
            String arg = args[1];
            Class<? extends Serializable> type = CONFIG.getType(property);
            if (type == Boolean.class) {
                if (Objects.equals(arg, "default")) {
                    arg = DEFAULT_CONFIG.getAsString(property);
                }
                boolean value = parseBoolean(arg);
                this.setProperty(property, value, sender);
            } else if (type == Integer.class) {
                if (Objects.equals(arg, "default")) {
                    arg = DEFAULT_CONFIG.getAsString(property);
                }
                int value = parseInt(arg);
                this.setProperty(property, value, sender);
            }
        } else {
            throw new CommandNotFoundException();
        }
    }

    private void setProperty(String key, boolean value, ICommandSender source) {
        if (CONFIG.set(key, value)) {
            ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.property.set.success")
                , key, value));
            ITextComponent hover = new TextComponentString(Translation.translate(String.format("config.saplanting.property.%s", key)));
            text.setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendMessage(text);
        } else {
            ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.property.set.already"), key, value));
            text.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(text);
        }
    }

    private void setProperty(String key, int value, ICommandSender source) {
        if (CONFIG.set(key, value)) {
            ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.property.set.success"), key, value));
            ITextComponent hover = new TextComponentString(Translation.translate(String.format("config.saplanting.property.%s", key)));
            text.setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendMessage(text);
        } else {
            ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.property.set.already"), key, value));
            text.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(text);
        }
    }

    private void getProperty(String key, ICommandSender source) {
        ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.property.get"), key, CONFIG.getAsString(key)));
        ITextComponent hover = new TextComponentString(Translation.translate(String.format("config.saplanting.property.%s", key)));
        text.setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        source.sendMessage(text);
    }

    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, CONFIG.getKeySet());
        } else if (args.length == 2) {
            if (!CONFIG.getKeySet().contains(args[0])) {
                return Collections.emptyList();
            }
            Class<? extends Serializable> type = CONFIG.getType(args[0]);
            if (type == Boolean.class) {
                return getListOfStringsMatchingLastWord(args, "true", "false");
            } else if (type == Integer.class) {
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}
