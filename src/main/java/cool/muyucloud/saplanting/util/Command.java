package cool.muyucloud.saplanting.util;

import com.google.gson.JsonArray;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.saplanting.Saplanting;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ItemArgument;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;

public class Command {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Config DEFAULT_CONFIG = Saplanting.getDefaultConfig();
    private static final Style CLICKABLE_COMMAND = Style.EMPTY
        .withColor(Color.parseColor("green"))
        .withUnderlined(true);
    private static final Style CLICKABLE_FILE = Style.EMPTY
        .withUnderlined(true);

    public static void register(CommandDispatcher<CommandSource> dispatcher, boolean dedicated) {
        /* /saplanting <PAGE> */
        LiteralArgumentBuilder<CommandSource> root = Commands.literal("saplanting");
        root.requires(source -> source.hasPermission(2));
        root.executes(context -> displayAll(1, context.getSource()));
        root.then(Commands.argument("page", IntegerArgumentType.integer())
            .executes(context -> displayAll(IntegerArgumentType.getInteger(context, "page"), context.getSource())));

        /* /saplanting property <KEY> <VALUE> */
        LiteralArgumentBuilder<CommandSource> property = Commands.literal("property");
        for (String key : CONFIG.getKeySet()) {
            LiteralArgumentBuilder<CommandSource> propertyE = Commands.literal(key);
            propertyE.executes((context) -> getProperty(key, context.getSource()));
            if (CONFIG.getType(key) == Boolean.class) {
                propertyE.then(Commands.argument("value", BoolArgumentType.bool())
                    .executes((context -> setProperty(key, BoolArgumentType.getBool(context, "value"), context.getSource()))));
                propertyE.then(Commands.literal("default")
                    .executes(context -> setProperty(key, DEFAULT_CONFIG.getAsBoolean(key), context.getSource())));
            } else if (CONFIG.getType(key) == Integer.class) {
                propertyE.then(Commands.argument("value", IntegerArgumentType.integer())
                    .executes((context -> setProperty(key, IntegerArgumentType.getInteger(context, "value"), context.getSource()))));
                propertyE.then(Commands.literal("default")
                    .executes(context -> setProperty(key, DEFAULT_CONFIG.getAsInt(key), context.getSource())));
            }
            property.then(propertyE);
        }
        root.then(property);

        /* /saplanting language <OPERATION> [ARG] */
        // /saplanting language
        LiteralArgumentBuilder<CommandSource> language = Commands.literal("language");
        language.executes(context -> queryLanguage(context.getSource()));
        // /saplanting language switch <LANG>
        LiteralArgumentBuilder<CommandSource> change = Commands.literal("switch");
        for (String name : CONFIG.getValidLangs()) {
            change.then(Commands.literal(name).executes(context -> updateLanguage(name, context.getSource())));
        }
        // /saplanting language switch default
        change.then(Commands.literal("default")
            .executes(context -> updateLanguage("en_us", context.getSource())));
        language.then(change);
        root.then(language);

        /* /saplanting file <OPERATION> */
        LiteralArgumentBuilder<CommandSource> file = Commands.literal("file");
        file.then(Commands.literal("load").executes(context -> load(context.getSource(), dedicated)));
        file.then(Commands.literal("save").executes(context -> save(context.getSource(), dedicated)));
        root.then(file);

        /* /saplanting blackList <OPERATION> [ARG] */
        LiteralArgumentBuilder<CommandSource> blackList = Commands.literal("blackList");
        blackList.executes(context -> displayBlackList(1, context.getSource()));
        // saplanting blackList <page>
        blackList.then(Commands.argument("page", IntegerArgumentType.integer())
            .executes(context ->
                displayBlackList(IntegerArgumentType.getInteger(context, "page"), context.getSource())));
        // saplanting blackList add <item>
        blackList.then(Commands.literal("add")
            .then(Commands.argument("item", ItemArgument.item())
                .executes(context ->
                    addToBlackList(ItemArgument.getItem(context, "item").getItem(),
                        context.getSource()))));
        // saplanting blackList remove <item>
        blackList.then(Commands.literal("remove")
            .then(Commands.argument("item", ItemArgument.item())
                .executes(context ->
                    removeFromBlackList(ItemArgument.getItem(context, "item").getItem(),
                        context.getSource()))));
        // saplanting blackList clear
        blackList.then(Commands.literal("clear")
            .executes(context -> clearBlackList(context.getSource())));
        root.then(blackList);

        dispatcher.register(root);
    }

    private static int setProperty(String key, boolean value, CommandSource source) {
        if (CONFIG.set(key, value)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.property.set.success")
                , key, value));
            IFormattableTextComponent hover = new StringTextComponent(Translation.translate(String.format("config.saplanting.property.%s", key)));
            text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendSuccess(text, false);
        } else {
            IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.property.set.already"), key, value));
            source.sendFailure(text);
        }
        return value ? 1 : 0;
    }

    private static int setProperty(String key, int value, CommandSource source) {
        if (CONFIG.set(key, value)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.property.set.success"), key, value));
            IFormattableTextComponent hover = new StringTextComponent(Translation.translate(String.format("config.saplanting.property.%s", key)));
            text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendSuccess(text, false);
        } else {
            IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.property.set.already"), key, value));
            source.sendFailure(text);
        }
        return value;
    }

    private static int getProperty(String key, CommandSource source) {
        IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.property.get"), key, CONFIG.getAsString(key)));
        IFormattableTextComponent hover = new StringTextComponent(Translation.translate(String.format("config.saplanting.property.%s", key)));
        text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        source.sendSuccess(text, false);
        return 1;
    }

    private static int updateLanguage(String name, CommandSource source) {
        if (!CONFIG.set("language", name)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.language.already"), name));
            source.sendFailure(text);
            return 0;
        }
        Translation.updateLanguage(name);
        IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.language.success"), name));
        source.sendSuccess(text, false);
        return 1;
    }

    private static int queryLanguage(CommandSource source) {
        IFormattableTextComponent text = new StringTextComponent(String.format(Translation.translate("command.saplanting.language.query"), CONFIG.getAsString("language")));
        IFormattableTextComponent change = new StringTextComponent(String.format(Translation.translate("command.saplanting.language.switch")))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/saplanting language switch ")));
        source.sendSuccess(text.append(change), false);
        return 1;
    }

    private static int load(CommandSource source, boolean dedicated) {
        IFormattableTextComponent text;
        IFormattableTextComponent hover = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.open")));
        IFormattableTextComponent file = new StringTextComponent(CONFIG.stringConfigPath())
            .setStyle(CLICKABLE_FILE
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        IFormattableTextComponent query = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.load.query")))
            .setStyle(CLICKABLE_COMMAND.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting")));
        if (CONFIG.load()) {
            text = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.load.success")));
            if (!dedicated) {
                text.append(file);
            }
            text.append(" ").append(query);
            source.sendSuccess(text, false);
            return 1;
        } else {
            text = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.load.fail")));
            if (!dedicated) {
                text.append(file);
            }
            source.sendFailure(text);
            return 0;
        }
    }

    private static int save(CommandSource source, boolean dedicated) {
        IFormattableTextComponent text;
        IFormattableTextComponent hover = new StringTextComponent(String.format(Translation.translate("commands.saplanting.file.open")));
        IFormattableTextComponent file = new StringTextComponent(CONFIG.stringConfigPath())
            .setStyle(CLICKABLE_FILE
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        if (CONFIG.save()) {
            text = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.save.success")));
            if (!dedicated) {
                text.append(file);
            }
            source.sendSuccess(text, false);
            return 1;
        } else {
            text = new StringTextComponent(String.format(Translation.translate("command.saplanting.file.save.fail")));
            if (!dedicated) {
                text.append(file);
            }
            source.sendFailure(text);
            return 0;
        }
    }

    private static int displayAll(int page, CommandSource source) {
        List<String> arr = CONFIG.getKeySet();
        if ((page - 1) * 8 > arr.size() || page < 1) {
            IFormattableTextComponent pageError = new StringTextComponent(String.format(Translation.translate("command.saplanting.page404")));
            source.sendFailure(pageError);
            return 0;
        }

        /* ======TITLE====== */
        IFormattableTextComponent title = new StringTextComponent(String.format(Translation.translate("command.saplanting.title"))).setStyle(Style.EMPTY
            .withColor(Color.parseColor("gold")));
        source.sendSuccess(title, false);
        /* - KEY : VALUE */
        for (int i = (page - 1) * 8; i < page * 8 && i < arr.size(); ++i) {
            String key = arr.get(i);
            IFormattableTextComponent head = new StringTextComponent("- ");
            IFormattableTextComponent reset = new StringTextComponent(String.format(Translation.translate("command.saplanting.reset")))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/saplanting property %s default", key)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new StringTextComponent(String.format(Translation.translate("command.saplanting.reset.hover"), DEFAULT_CONFIG.getAsString(key))))));
            IFormattableTextComponent hover = new StringTextComponent(Translation.translate(String.format("config.saplanting.property.%s", key)));
            IFormattableTextComponent property = new StringTextComponent(key).setStyle(CLICKABLE_COMMAND
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    String.format("/saplanting property %s ", key))));
            IFormattableTextComponent value = new StringTextComponent(": " + CONFIG.getAsString(key));
            head.append(reset).append(" ").append(property).append(value);

            source.sendSuccess(head, false);
        }

        /* [FORMER] PAGE [NEXT] */
        IFormattableTextComponent next = new StringTextComponent(String.format(Translation.translate("command.saplanting.next")))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page + 1))));
        IFormattableTextComponent former = new StringTextComponent(String.format(Translation.translate("command.saplanting.former")))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page - 1))));
        IFormattableTextComponent foot;
        if (arr.size() <= 8) {
            foot = new StringTextComponent(" 1 ");
        } else if (page == 1) {
            foot = new StringTextComponent(" 1 >> ").append(next);
        } else if ((page * 8) >= arr.size()) {
            foot = new StringTextComponent(" ").append(former).append(String.format(" << %d ", page));
        } else {
            foot = new StringTextComponent(" ").append(former).append(String.format(" << %d >> ", page)).append(next);
        }
        source.sendSuccess(foot, false);

        return page;
    }

    private static int addToBlackList(Item item, CommandSource source) {
        String id = Registry.ITEM.getKey(item).toString();

        if (!Saplanting.isPlantItem(item)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.add.notPlant"), id));
            source.sendFailure(text);
            return 0;
        }

        if (CONFIG.addToBlackList(item)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.add.success"), id));
            IFormattableTextComponent undo = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.add.undo")))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/saplanting blackList remove %s", id))));
            source.sendSuccess(text.append(" ").append(undo), false);
            return 1;
        }

        IFormattableTextComponent text = new StringTextComponent(String.format(
            Translation.translate("command.saplanting.blackList.add.inBlackList"), id));
        source.sendFailure(text);
        return 0;
    }

    private static int removeFromBlackList(Item item, CommandSource source) {
        String id = Registry.ITEM.getKey(item).toString();
        if (CONFIG.removeFromBlackList(item)) {
            IFormattableTextComponent text = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.remove.success"), id));
            IFormattableTextComponent undo = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.remove.undo")))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/saplanting blackList add %s", id))));
            source.sendSuccess(text.append(" ").append(undo), false);
            return 1;
        }

        IFormattableTextComponent text = new StringTextComponent(String.format(
            Translation.translate("command.saplanting.blackList.remove.notInBlackList"), id));
        source.sendFailure(text);
        return 0;
    }

    private static int displayBlackList(int page, CommandSource source) {
        if (CONFIG.blackListSize() == 0) {
            source.sendFailure(new StringTextComponent(String.format(
                Translation.translate("command.saplanting.blackList.empty"))));
            return 0;
        }

        /* Page validation */
        JsonArray blackList = CONFIG.getBlackList();
        if ((page - 1) * 8 > blackList.size() || page < 1) {
            IFormattableTextComponent pageError = new StringTextComponent(String.format(
                Translation.translate("command.saplanting.page404")));
            source.sendFailure(pageError);
            return 0;
        }

        /* TITLE: */
        IFormattableTextComponent title = new StringTextComponent(String.format(
            Translation.translate("command.saplanting.blackList.title")));
        source.sendSuccess(title, false);

        /* - ITEM */
        for (int i = (page - 1) * 8; (i < (page * 8)) && (i < blackList.size()); ++i) {
            String id = blackList.get(i).getAsString();
            IFormattableTextComponent head = new StringTextComponent("- ");
            IFormattableTextComponent remove = new StringTextComponent(Translation.translate("command.saplanting.blackList.click.remove"));
            remove.setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    String.format("/saplanting blackList remove %s", id))));
            IFormattableTextComponent item = new StringTextComponent(id);
            head.append(item);
            source.sendSuccess(head, false);
        }

        /* [FORMER] << PAGE >> [NEXT] */
        IFormattableTextComponent next = new StringTextComponent(String.format(Translation.translate("command.saplanting.next")))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page + 1))));
        IFormattableTextComponent former = new StringTextComponent(String.format(Translation.translate("command.saplanting.former")))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page - 1))));
        IFormattableTextComponent foot;
        if (blackList.size() <= 8) {
            foot = new StringTextComponent(" 1 ");
        } else if (page == 1) {
            foot = new StringTextComponent(" 1 >> ").append(next);
        } else if ((page * 8) >= blackList.size()) {
            foot = new StringTextComponent(" ").append(former).append(String.format(" << %d ", page));
        } else {
            foot = new StringTextComponent(" ").append(former).append(String.format(" << %d >> ", page)).append(next);
        }
        source.sendSuccess(foot, false);
        return CONFIG.blackListSize();
    }

    private static int clearBlackList(CommandSource source) {
        if (CONFIG.blackListSize() == 0) {
            source.sendFailure(new StringTextComponent(Translation.translate("command.saplanting.blackList.empty")));
            return 0;
        }
        int i = CONFIG.blackListSize();
        IFormattableTextComponent text = new StringTextComponent(Translation.translate("command.saplanting.blackList.clear"));
        source.sendSuccess(text, false);
        CONFIG.clearBlackList();
        return i;
    }
}
