package cool.muyucloud.saplanting.command;

import cool.muyucloud.saplanting.Saplanting;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.server.command.CommandTreeBase;

public class FileCommand extends CommandTreeBase {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Style CLICKABLE_COMMAND = new Style()
        .setColor(TextFormatting.GREEN)
        .setUnderlined(true);
    private static final Style CLICKABLE_FILE = new Style()
        .setUnderlined(true);
    private static final Style FAILURE = new Style()
        .setColor(TextFormatting.RED);

    public FileCommand() {
        this.addSubcommand(new FileSaveCommand());
        this.addSubcommand(new FileLoadCommand());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return null;
    }

    private String[] getSubArgs(String[] args) {
        String[] sub = new String[args.length - 1];
        System.arraycopy(args, 1, sub, 0, sub.length);
        return sub;
    }

    private boolean gotoSubCommand(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 0) {
            ICommand sub = this.getSubCommand(args[0]);
            if (sub != null) {
                sub.execute(server, sender, this.getSubArgs(args));
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (gotoSubCommand(server, sender, args)) return;

        throw new CommandNotFoundException();
    }

    public static class FileSaveCommand extends CommandBase {
        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getName() {
            return "save";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return null;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 0) {
                throw new CommandNotFoundException();
            }
            this.save(sender, sender.getEntityWorld().isRemote);
        }

        private void save(ICommandSender source, boolean dedicated) {
            ITextComponent text;
            ITextComponent hover = new TextComponentString(String.format(Translation.translate("commands.saplanting.file.open")));
            ITextComponent file = new TextComponentString(CONFIG.stringConfigPath())
                .setStyle(CLICKABLE_FILE.createDeepCopy()
                    .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            if (CONFIG.save()) {
                text = new TextComponentString(String.format(Translation.translate("command.saplanting.file.save.success")));
                if (!dedicated) {
                    text.appendSibling(file);
                }
                source.sendMessage(text);
            } else {
                text = new TextComponentString(String.format(Translation.translate("command.saplanting.file.save.fail")));
                if (!dedicated) {
                    text.appendSibling(file);
                }
                text.setStyle(FAILURE.createDeepCopy());
                source.sendMessage(text);
            }
        }
    }

    public static class FileLoadCommand extends CommandBase {
        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getName() {
            return "load";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return null;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

            if (args.length != 0) {
                throw new CommandNotFoundException();
            }
            this.load(sender, sender.getEntityWorld().isRemote);
        }

        private void load(ICommandSender source, boolean dedicated) {
            ITextComponent text;
            ITextComponent hover = new TextComponentString(String.format(Translation.translate("command.saplanting.file.open")));
            ITextComponent file = new TextComponentString(CONFIG.stringConfigPath())
                .setStyle(CLICKABLE_FILE.createDeepCopy()
                    .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            ITextComponent query = new TextComponentString(String.format(Translation.translate("command.saplanting.file.load.query")))
                .setStyle(CLICKABLE_COMMAND.createDeepCopy().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting")));
            if (CONFIG.load()) {
                text = new TextComponentString(String.format(Translation.translate("command.saplanting.file.load.success")));
                if (!dedicated) {
                    text.appendSibling(file);
                }
                text.appendText(" ").appendSibling(query);
                source.sendMessage(text);
            } else {
                text = new TextComponentString(String.format(Translation.translate("command.saplanting.file.load.fail")));
                if (!dedicated) {
                    text.appendSibling(file);
                }
                text.setStyle(FAILURE.createDeepCopy());
                source.sendMessage(text);
            }
        }
    }
}
