package cool.muyucloud.saplanting.command;

import cool.muyucloud.saplanting.Saplanting;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandNotFoundException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.server.command.CommandTreeBase;
import scala.tools.nsc.doc.base.comment.Link;

import java.util.LinkedList;
import java.util.List;

public class SaplantingCommand extends CommandTreeBase {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Config DEFAULT_CONFIG = Saplanting.getDefaultConfig();
    private static final Style CLICKABLE_COMMAND = new Style()
        .setColor(TextFormatting.GREEN)
        .setUnderlined(true);
    private static final Style FAILURE = new Style()
        .setColor(TextFormatting.RED);

    public SaplantingCommand() {
        this.addSubcommand(new PropertyCommand());
        this.addSubcommand(new LanguageCommand());
        this.addSubcommand(new FileCommand());
        this.addSubcommand(new BlackListCommand());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
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
            displayAll(1, sender);
        } else if (args.length == 1) {
            int page = parseInt(args[0]);
            displayAll(page, sender);
        } else {
            throw new CommandNotFoundException();
        }
    }

    private static void displayAll(int page, ICommandSender source) {
        List<String> arr = CONFIG.getKeySet();
        if ((page - 1) * 8 > arr.size() || page < 1) {
            ITextComponent pageError = new TextComponentString(String.format(Translation.translate("command.saplanting.page404")));
            pageError.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(pageError);
            return;
        }

        /* ======TITLE====== */
        ITextComponent title = new TextComponentString(String.format(Translation.translate("command.saplanting.title")))
            .setStyle(new Style().setColor(TextFormatting.GOLD));
        source.sendMessage(title);
        /* - KEY : VALUE */
        for (int i = (page - 1) * 8; i < page * 8 && i < arr.size(); ++i) {
            String key = arr.get(i);
            ITextComponent head = new TextComponentString("- ");
            ITextComponent reset = new TextComponentString(String.format(Translation.translate("command.saplanting.reset")))
                .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                    .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/saplanting property %s default", key)))
                    .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponentString(String.format(Translation.translate("command.saplanting.reset.hover"), DEFAULT_CONFIG.getAsString(key))))));
            ITextComponent hover = new TextComponentString(Translation.translate(String.format("config.saplanting.property.%s", key)));
            ITextComponent property = new TextComponentString(key).setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    String.format("/saplanting property %s ", key))));
            ITextComponent value = new TextComponentString(": " + CONFIG.getAsString(key));
            head.appendSibling(reset).appendText(" ").appendSibling(property).appendSibling(value);

            source.sendMessage(head);
        }

        /* [FORMER] PAGE [NEXT] */
        ITextComponent next = new TextComponentString(String.format(Translation.translate("command.saplanting.next")))
            .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page + 1))));
        ITextComponent former = new TextComponentString(String.format(Translation.translate("command.saplanting.former")))
            .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page - 1))));
        ITextComponent foot;
        if (arr.size() <= 8) {
            foot = new TextComponentString(" 1 ");
        } else if (page == 1) {
            foot = new TextComponentString(" 1 >> ").appendSibling(next);
        } else if ((page * 8) >= arr.size()) {
            foot = new TextComponentString(" ").appendSibling(former).appendText(String.format(" << %d ", page));
        } else {
            foot = new TextComponentString(" ").appendSibling(former).appendText(String.format(" << %d >> ", page)).appendSibling(next);
        }
        source.sendMessage(foot);

    }

    @Override
    public String getName() {
        return "saplanting";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return null;
    }
}
