package cool.muyucloud.saplanting.command;

import cool.muyucloud.saplanting.Saplanting;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.server.command.CommandTreeBase;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class LanguageCommand extends CommandTreeBase {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Style CLICKABLE_COMMAND = new Style()
        .setColor(TextFormatting.GREEN)
        .setUnderlined(true);
    private static final Style FAILURE = new Style()
        .setColor(TextFormatting.RED);

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "language";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return null;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, CONFIG.getValidLangs());
        }
        return Collections.emptyList();
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

        if (args.length == 0) {
            this.queryLanguage(sender);
        } else if (args.length == 1) {
            this.updateLanguage(args[0], sender);
        } else {
            throw new CommandNotFoundException();
        }
    }

    private void queryLanguage(ICommandSender source) {
        ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.language.query"), CONFIG.getAsString("language")));
        ITextComponent change = new TextComponentString(String.format(Translation.translate("command.saplanting.language.switch")))
            .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/saplanting language switch ")));
        source.sendMessage(text.appendSibling(change));
    }

    private void updateLanguage(String name, ICommandSender source) {
        if (!CONFIG.set("language", name)) {
            ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.language.already"), name));
            text.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(text);
            return;
        }
        Translation.updateLanguage(name);
        ITextComponent text = new TextComponentString(String.format(Translation.translate("command.saplanting.language.success"), name));
        source.sendMessage(text);
    }
}
