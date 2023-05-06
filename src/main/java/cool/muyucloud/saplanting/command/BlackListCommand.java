package cool.muyucloud.saplanting.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import cool.muyucloud.saplanting.Saplanting;
import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.util.Translation;
import net.minecraft.command.*;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.server.command.CommandTreeBase;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class BlackListCommand extends CommandTreeBase {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Style CLICKABLE_COMMAND = new Style()
        .setColor(TextFormatting.GREEN)
        .setUnderlined(true);
    private static final Style FAILURE = new Style()
        .setColor(TextFormatting.RED);

    public BlackListCommand() {
        this.addSubcommand(new BlackListAddCommand());
        this.addSubcommand(new BlackListRemoveCommand());
        this.addSubcommand(new BlackListClearCommand());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getName() {
        return "blackList";
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
        if (args.length == 0) {
            this.displayBlackList(0, sender);
        } else if (args.length == 1) {
            int page = parseInt(args[0]);
            this.displayBlackList(page, sender);
        } else {
            throw new CommandNotFoundException();
        }
    }

    private void displayBlackList(int page, ICommandSender source) {
        if (CONFIG.blackListSize() == 0) {
            source.sendMessage(new TextComponentString(String.format(
                Translation.translate("command.saplanting.blackList.empty")))
                .setStyle(FAILURE.createDeepCopy()));
            return;
        }

        /* Page validation */
        JsonArray blackList = CONFIG.getBlackList();
        if ((page - 1) * 8 > blackList.size() || page < 1) {
            ITextComponent pageError = new TextComponentString(String.format(
                Translation.translate("command.saplanting.page404")));
            pageError.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(pageError);
            return;
        }

        /* TITLE: */
        ITextComponent title = new TextComponentString(String.format(
            Translation.translate("command.saplanting.blackList.title")));
        source.sendMessage(title);

        /* - ITEM */
        for (int i = (page - 1) * 8; (i < (page * 8)) && (i < blackList.size()); ++i) {
            String id = blackList.get(i).getAsString();
            ITextComponent head = new TextComponentString("- ");
            ITextComponent remove = new TextComponentString(Translation.translate("command.saplanting.blackList.click.remove"));
            remove.setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    String.format("/saplanting blackList remove %s", id))));
            ITextComponent item = new TextComponentString(id);
            head.appendSibling(item);
            source.sendMessage(head);
        }

        /* [FORMER] << PAGE >> [NEXT] */
        ITextComponent next = new TextComponentString(String.format(Translation.translate("command.saplanting.next")))
            .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page + 1))));
        ITextComponent former = new TextComponentString(String.format(Translation.translate("command.saplanting.former")))
            .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page - 1))));
        ITextComponent foot;
        if (blackList.size() <= 8) {
            foot = new TextComponentString(" 1 ");
        } else if (page == 1) {
            foot = new TextComponentString(" 1 >> ").appendSibling(next);
        } else if ((page * 8) >= blackList.size()) {
            foot = new TextComponentString(" ").appendSibling(former).appendText(String.format(" << %d ", page));
        } else {
            foot = new TextComponentString(" ").appendSibling(former).appendText(String.format(" << %d >> ", page)).appendSibling(next);
        }
        source.sendMessage(foot);
        CONFIG.blackListSize();
    }

    public static class BlackListAddCommand extends CommandBase {
        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getName() {
            return "add";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return null;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) {
                throw new CommandNotFoundException();
            }
            Item item = getItemByText(sender, args[0]);
            this.addToBlackList(item, sender);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
            if (args.length == 1) {
                return getListOfStringsMatchingLastWord(args, Item.REGISTRY.getKeys());
            }
            return Collections.emptyList();
        }

        private void addToBlackList(Item item, ICommandSender source) {
            String id = Item.REGISTRY.getNameForObject(item).toString();

            if (!Saplanting.isPlantItem(item)) {
                ITextComponent text = new TextComponentString(String.format(
                    Translation.translate("command.saplanting.blackList.add.notPlant"), id));
                text.setStyle(FAILURE.createDeepCopy());
                source.sendMessage(text);
                return;
            }

            if (CONFIG.addToBlackList(item)) {
                ITextComponent text = new TextComponentString(String.format(
                    Translation.translate("command.saplanting.blackList.add.success"), id));
                ITextComponent undo = new TextComponentString(String.format(
                    Translation.translate("command.saplanting.blackList.add.undo")))
                    .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            String.format("/saplanting blackList remove %s", id))));
                source.sendMessage(text.appendText(" ").appendSibling(undo));
                return;
            }

            ITextComponent text = new TextComponentString(String.format(
                Translation.translate("command.saplanting.blackList.add.inBlackList"), id));
            text.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(text);
        }
    }

    public static class BlackListRemoveCommand extends CommandBase {
        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getName() {
            return "remove";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return null;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (args.length != 1) {
                throw new CommandNotFoundException();
            }
            Item item = getItemByText(sender, args[0]);
            this.removeFromBlackList(item, sender);
        }

        @Override
        public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
            if (args.length == 1) {
                HashSet<String> blackList = new HashSet<>();
                for (JsonElement element : CONFIG.getBlackList()) {
                    blackList.add(element.getAsString());
                }
                return getListOfStringsMatchingLastWord(args, blackList);
            }
            return Collections.emptyList();
        }

        private void removeFromBlackList(Item item, ICommandSender source) {
            String id = Item.REGISTRY.getNameForObject(item).toString();
            if (CONFIG.removeFromBlackList(item)) {
                ITextComponent text = new TextComponentString(String.format(
                    Translation.translate("command.saplanting.blackList.remove.success"), id));
                ITextComponent undo = new TextComponentString(String.format(
                    Translation.translate("command.saplanting.blackList.remove.undo")))
                    .setStyle(CLICKABLE_COMMAND.createDeepCopy()
                        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            String.format("/saplanting blackList add %s", id))));
                source.sendMessage(text.appendText(" ").appendSibling(undo));
                return;
            }

            ITextComponent text = new TextComponentString(String.format(
                Translation.translate("command.saplanting.blackList.remove.notInBlackList"), id));
            text.setStyle(FAILURE.createDeepCopy());
            source.sendMessage(text);

        }
    }

    public static class BlackListClearCommand extends CommandBase {
        @Override
        public int getRequiredPermissionLevel() {
            return 2;
        }

        @Override
        public String getName() {
            return "clear";
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
            this.clearBlackList(sender);
        }

        private void clearBlackList(ICommandSender source) {
            if (CONFIG.blackListSize() == 0) {
                source.sendMessage(new TextComponentString(Translation.translate("command.saplanting.blackList.empty"))
                    .setStyle(FAILURE.createDeepCopy()));
                return;
            }
            int i = CONFIG.blackListSize();
            ITextComponent text = new TextComponentString(Translation.translate("command.saplanting.blackList.clear"));
            source.sendMessage(text);
            CONFIG.clearBlackList();
        }
    }
}
