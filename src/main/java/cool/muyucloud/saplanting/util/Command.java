package cool.muyucloud.saplanting.util;

import com.google.gson.JsonArray;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.saplanting.Saplanting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.Item;

import java.util.List;

public class Command {
    private static final Config CONFIG = Saplanting.getConfig();
    private static final Config DEFAULT_CONFIG = Saplanting.getDefaultConfig();
    private static final Style CLICKABLE_COMMAND = Style.EMPTY
        .withColor(TextColor.parseColor("green"))
        .withUnderlined(true);
    private static final Style CLICKABLE_FILE = Style.EMPTY
        .withUnderlined(true);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
        /* /saplanting <PAGE> */
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("saplanting");
        root.requires(source -> source.hasPermission(2));
        root.executes(context -> displayAll(1, context.getSource()));
        root.then(Commands.argument("page", IntegerArgumentType.integer())
            .executes(context -> displayAll(IntegerArgumentType.getInteger(context, "page"), context.getSource())));

        /* /saplanting property <KEY> <VALUE> */
        LiteralArgumentBuilder<CommandSourceStack> property = Commands.literal("property");
        for (String key : CONFIG.getKeySet()) {
            LiteralArgumentBuilder<CommandSourceStack> propertyE = Commands.literal(key);
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
        LiteralArgumentBuilder<CommandSourceStack> language = Commands.literal("language");
        language.executes(context -> queryLanguage(context.getSource()));
        // /saplanting language switch <LANG>
        LiteralArgumentBuilder<CommandSourceStack> change = Commands.literal("switch");
        for (String name : CONFIG.getValidLangs()) {
            change.then(Commands.literal(name).executes(context -> updateLanguage(name, context.getSource())));
        }
        // /saplanting language switch default
        change.then(Commands.literal("default")
            .executes(context -> updateLanguage("en_us", context.getSource())));
        language.then(change);
        root.then(language);

        /* /saplanting file <OPERATION> */
        LiteralArgumentBuilder<CommandSourceStack> file = Commands.literal("file");
        file.then(Commands.literal("load").executes(context -> load(context.getSource(), dedicated)));
        file.then(Commands.literal("save").executes(context -> save(context.getSource(), dedicated)));
        root.then(file);

        /* /saplanting blackList <OPERATION> [ARG] */
        LiteralArgumentBuilder<CommandSourceStack> blackList = Commands.literal("blackList");
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

    private static int setProperty(String key, boolean value, CommandSourceStack source) {
        if (CONFIG.set(key, value)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.property.set.success")
                .formatted(key, Boolean.toString(value)));
            MutableComponent hover = new TextComponent(Translation.translate("config.saplanting.property.%s".formatted(key)));
            text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendSuccess(text, false);
        } else {
            TextComponent text = new TextComponent(Translation.translate("command.saplanting.property.set.already")
                .formatted(key, Boolean.toString(value)));
            source.sendFailure(text);
        }
        return value ? 1 : 0;
    }

    private static int setProperty(String key, int value, CommandSourceStack source) {
        if (CONFIG.set(key, value)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.property.set.success")
                .formatted(key, Integer.toString(value)));
            MutableComponent hover = new TextComponent(Translation.translate("config.saplanting.property.%s".formatted(key)));
            text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
            source.sendSuccess(text, false);
        } else {
            TextComponent text = new TextComponent(Translation.translate("command.saplanting.property.set.already")
                .formatted(key, Integer.toString(value)));
            source.sendFailure(text);
        }
        return value;
    }

    private static int getProperty(String key, CommandSourceStack source) {
        MutableComponent hover = new TextComponent(Translation.translate("config.saplanting.property.%s".formatted(key)));
        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.property.get")
            .formatted(key, CONFIG.getAsString(key)));
        text.setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        source.sendSuccess(text, false);
        return 1;
    }

    private static int updateLanguage(String name, CommandSourceStack source) {
        if (!CONFIG.set("language", name)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.language.already").formatted(name));
            source.sendFailure(text);
            return 0;
        }
        Translation.updateLanguage(name);
        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.language.success").formatted(name));
        source.sendSuccess(text, false);
        return 1;
    }

    private static int queryLanguage(CommandSourceStack source) {
        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.language.query").formatted(CONFIG.getAsString("language")));
        MutableComponent change = new TextComponent(Translation.translate("command.saplanting.language.switch"))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/saplanting language switch ")));
        source.sendSuccess(text.append(change), false);
        return 1;
    }

    private static int load(CommandSourceStack source, boolean dedicated) {
        MutableComponent text;
        MutableComponent hover = new TextComponent(Translation.translate("command.saplanting.file.open"));
        MutableComponent file = new TextComponent(CONFIG.stringConfigPath())
            .setStyle(CLICKABLE_FILE
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        MutableComponent query = new TextComponent(Translation.translate("command.saplanting.file.load.query"))
            .setStyle(CLICKABLE_COMMAND.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting")));
        if (CONFIG.load()) {
            text = new TextComponent(Translation.translate("command.saplanting.file.load.success"));
            if (!dedicated) {
                text.append(file);
            }
            text.append(" ").append(query);
            source.sendSuccess(text, false);
            return 1;
        } else {
            text = new TextComponent(Translation.translate("command.saplanting.file.load.fail"));
            if (!dedicated) {
                text.append(file);
            }
            source.sendFailure(text);
            return 0;
        }
    }

    private static int save(CommandSourceStack source, boolean dedicated) {
        TextComponent text;
        TextComponent hover = new TextComponent(Translation.translate("commands.saplanting.file.open"));
        MutableComponent file = new TextComponent(CONFIG.stringConfigPath())
            .setStyle(CLICKABLE_FILE
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, CONFIG.stringConfigPath()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
        if (CONFIG.save()) {
            text = new TextComponent(Translation.translate("command.saplanting.file.save.success"));
            if (!dedicated) {
                text.append(file);
            }
            source.sendSuccess(text, false);
            return 1;
        } else {
            text = new TextComponent(Translation.translate("command.saplanting.file.save.fail"));
            if (!dedicated) {
                text.append(file);
            }
            source.sendFailure(text);
            return 0;
        }
    }

    private static int displayAll(int page, CommandSourceStack source) {
        List<String> arr = CONFIG.getKeySet();
        if ((page - 1) * 8 > arr.size() || page < 1) {
            MutableComponent pageError = new TextComponent(Translation.translate("command.saplanting.page404"));
            source.sendFailure(pageError);
            return 0;
        }

        /* ======TITLE====== */
        MutableComponent title = new TextComponent(Translation.translate("command.saplanting.title")).setStyle(Style.EMPTY
            .withColor(TextColor.parseColor("gold")));
        source.sendSuccess(title, false);
        /* - KEY : VALUE */
        for (int i = (page - 1) * 8; i < page * 8 && i < arr.size(); ++i) {
            String key = arr.get(i);
            TextComponent head = new TextComponent("- ");
            MutableComponent reset = new TextComponent(Translation.translate("command.saplanting.reset"))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/saplanting property %s default".formatted(key)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new TextComponent(Translation.translate("command.saplanting.reset.hover")
                            .formatted(DEFAULT_CONFIG.getAsString(key))))));
            TextComponent hover = new TextComponent(Translation.translate("config.saplanting.property.%s".formatted(key)));
            MutableComponent property = new TextComponent(key).setStyle(CLICKABLE_COMMAND
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    "/saplanting property %s ".formatted(key))));
            TextComponent value = new TextComponent(": " + CONFIG.getAsString(key));
            head.append(reset).append(" ").append(property).append(value);

            source.sendSuccess(head, false);
        }

        /* [FORMER] PAGE [NEXT] */
        MutableComponent next = new TextComponent(Translation.translate("command.saplanting.next"))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page + 1))));
        MutableComponent former = new TextComponent(Translation.translate("command.saplanting.former"))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page - 1))));
        MutableComponent foot;
        if (arr.size() <= 8) {
            foot = new TextComponent(" 1 ");
        } else if (page == 1) {
            foot = new TextComponent(" 1 >> ").append(next);
        } else if ((page * 8) >= arr.size()) {
            foot = new TextComponent(" ").append(former).append(" << %d ".formatted(page));
        } else {
            foot = new TextComponent(" ").append(former).append(" << %d >> ".formatted(page)).append(next);
        }
        source.sendSuccess(foot, false);

        return page;
    }

    private static int addToBlackList(Item item, CommandSourceStack source) {
        String id = Registry.ITEM.getKey(item).toString();

        if (!Saplanting.isPlantItem(item)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.add.notPlant")
                .formatted(id));
            source.sendFailure(text);
            return 0;
        }

        if (CONFIG.addToBlackList(item)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.add.success")
                .formatted(id));
            MutableComponent undo = new TextComponent(Translation.translate("command.saplanting.blackList.add.undo"))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/saplanting blackList remove %s".formatted(id))));
            source.sendSuccess(text.append(" ").append(undo), false);
            return 1;
        }

        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.add.inBlackList")
            .formatted(id));
        source.sendFailure(text);
        return 0;
    }

    private static int removeFromBlackList(Item item, CommandSourceStack source) {
        String id = Registry.ITEM.getKey(item).toString();
        if (CONFIG.removeFromBlackList(item)) {
            MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.remove.success")
                .formatted(id));
            MutableComponent undo = new TextComponent(Translation.translate("command.saplanting.blackList.remove.undo"))
                .setStyle(CLICKABLE_COMMAND
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/saplanting blackList add %s".formatted(id))));
            source.sendSuccess(text.append(" ").append(undo), false);
            return 1;
        }

        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.remove.notInBlackList")
            .formatted(id));
        source.sendFailure(text);
        return 0;
    }

    private static int displayBlackList(int page, CommandSourceStack source) {
        if (CONFIG.blackListSize() == 0) {
            source.sendFailure(new TextComponent(Translation.translate("command.saplanting.blackList.empty")));
            return 0;
        }

        /* Page validation */
        JsonArray blackList = CONFIG.getBlackList();
        if ((page - 1) * 8 > blackList.size() || page < 1) {
            MutableComponent pageError = new TextComponent(Translation.translate("command.saplanting.page404"));
            source.sendFailure(pageError);
            return 0;
        }

        /* TITLE: */
        MutableComponent title = new TextComponent(Translation.translate("command.saplanting.blackList.title"));
        source.sendSuccess(title, false);

        /* - ITEM */
        for (int i = (page - 1) * 8; (i < (page * 8)) && (i < blackList.size()); ++i) {
            String id = blackList.get(i).getAsString();
            MutableComponent head = new TextComponent("- ");
            MutableComponent remove = new TextComponent(Translation.translate("command.saplanting.blackList.click.remove"));
            remove.setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/saplanting blackList remove %s".formatted(id))));
            MutableComponent item = new TextComponent(id);
            head.append(item);
            source.sendSuccess(head, false);
        }

        /* [FORMER] << PAGE >> [NEXT] */
        MutableComponent next = new TextComponent(Translation.translate("command.saplanting.next"))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page + 1))));
        MutableComponent former = new TextComponent(Translation.translate("command.saplanting.former"))
            .setStyle(CLICKABLE_COMMAND
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting blackList " + (page - 1))));
        MutableComponent foot;
        if (blackList.size() <= 8) {
            foot = new TextComponent(" 1 ");
        } else if (page == 1) {
            foot = new TextComponent(" 1 >> ").append(next);
        } else if ((page * 8) >= blackList.size()) {
            foot = new TextComponent(" ").append(former).append(" << %d ".formatted(page));
        } else {
            foot = new TextComponent(" ").append(former).append(" << %d >> ".formatted(page)).append(next);
        }
        source.sendSuccess(foot, false);
        return CONFIG.blackListSize();
    }

    private static int clearBlackList(CommandSourceStack source) {
        if (CONFIG.blackListSize() == 0) {
            source.sendFailure(new TextComponent(Translation.translate("command.saplanting.blackList.empty")));
            return 0;
        }
        int i = CONFIG.blackListSize();
        MutableComponent text = new TextComponent(Translation.translate("command.saplanting.blackList.clear"));
        source.sendSuccess(text, false);
        CONFIG.clearBlackList();
        return i;
    }
}
