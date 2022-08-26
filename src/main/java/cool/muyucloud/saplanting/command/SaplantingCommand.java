package cool.muyucloud.saplanting.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import cool.muyucloud.saplanting.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.w3c.dom.Text;

public class SaplantingCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /saplanting
        final LiteralArgumentBuilder<CommandSourceStack> root = (Commands.literal("saplanting")
                .requires(source -> source.hasPermission(2)));

        // /saplanting <Integer>/NULL
        root.executes(context -> showAll(context.getSource(), 1));
        root.then(Commands.argument("page", IntegerArgumentType.integer()).executes(
                context -> showAll(context.getSource(), IntegerArgumentType.getInteger(context, "page"))
        ));

        // /saplanting plantEnable
        root.then(Commands.literal("plantEnable").executes(context -> getPlantEnable(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setPlantEnable(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // /saplanting plantEnable
        root.then(Commands.literal("plantLarge").executes(context -> getPlantLarge(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setPlantLarge(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowSapling
        root.then(Commands.literal("allowSapling").executes(context -> getAllowSapling(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowSapling(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowCrop
        root.then(Commands.literal("allowCrop").executes(context -> getAllowCrop(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowCrop(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowMushroom
        root.then(Commands.literal("allowMushroom").executes(context -> getAllowMushroom(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowMushroom(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowFungus
        root.then(Commands.literal("allowFungus").executes(context -> getAllowFungus(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowFungus(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowFlower
        root.then(Commands.literal("allowFlower").executes(context -> getAllowFlower(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowFlower(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting allowOther
        root.then(Commands.literal("allowOther").executes(context -> getAllowOther(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setAllowOther(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // saplanting showTitleOnPlayerConnected
        root.then(Commands.literal("showTitleOnPlayerConnected").executes(context -> getShowTitleOnPlayerConnected(context.getSource()))
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> setShowTitleOnPlayerConnected(context.getSource(), BoolArgumentType.getBool(context, "value")))));

//        // saplanting ignoreShape
//        root.then(Commands.literal("ignoreShape").executes(context -> getIgnoreShape(context.getSource()))
//                .then(Commands.argument("value", BoolArgumentType.bool())
//                        .executes(context -> setIgnoreShape(context.getSource(), BoolArgumentType.getBool(context, "value")))));

        // /saplanting plantDelay
        root.then(Commands.literal("plantDelay").executes(context -> getPlantDelay(context.getSource()))
                .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(context -> setPlantDelay(context.getSource(), IntegerArgumentType.getInteger(context, "value")))));

        // /saplanting PlayerAround
        root.then(Commands.literal("playerAround").executes(context -> getPlayerAround(context.getSource()))
                .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(context -> setPlayerAround(context.getSource(), IntegerArgumentType.getInteger(context, "value")))));

        // /saplanting AvoidDense
        root.then(Commands.literal("avoidDense").executes(context -> getAvoidDense(context.getSource()))
                .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(context -> setAvoidDense(context.getSource(), IntegerArgumentType.getInteger(context, "value")))));

        // /saplanting blackList
        root.then(Commands.literal("blackList").executes(context -> getBlackListEnable(context.getSource()))
                .then(Commands.literal("enable").executes(context -> setBlackListEnable(context.getSource(), true)))
                .then(Commands.literal("disable").executes(context -> setBlackListEnable(context.getSource(), false)))
                .then(Commands.literal("add")
                        .then(Commands.argument("itemName", ItemArgument.item())
                                .executes(context -> addBlackList(context.getSource(), ItemArgument.getItem(context, "itemName").getItem()))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ItemArgument.item())
                                .executes(context -> removeBlackList(context.getSource()
                                        , ItemArgument.getItem(context, "item").getItem()))))
                .then(Commands.literal("list")
                        .executes(context -> showBlackList(context.getSource())))
                .then(Commands.literal("clear")
                        .executes(context -> clearBlackList(context.getSource()))));

        // /saplanting load <Property>
        LiteralArgumentBuilder<CommandSourceStack> load = Commands.literal("load").executes(context -> loadProperty(context.getSource()));
        load.then(Commands.literal("plantEnable")
                .executes(context -> loadProperty(context.getSource(), "plantEnable")));
        load.then(Commands.literal("plantLarge")
                .executes(context -> loadProperty(context.getSource(), "plantLarge")));
        load.then(Commands.literal("blackListEnable")
                .executes(context -> loadProperty(context.getSource(), "blackListEnable")));
        load.then(Commands.literal("allowSapling")
                .executes(context -> loadProperty(context.getSource(), "allowSapling")));
        load.then(Commands.literal("allowCrop")
                .executes(context -> loadProperty(context.getSource(), "allowCrop")));
        load.then(Commands.literal("allowMushroom")
                .executes(context -> loadProperty(context.getSource(), "allowMushroom")));
        load.then(Commands.literal("allowFungus")
                .executes(context -> loadProperty(context.getSource(), "allowFungus")));
        load.then(Commands.literal("allowFlower")
                .executes(context -> loadProperty(context.getSource(), "allowFlower")));
        load.then(Commands.literal("allowOther")
                .executes(context -> loadProperty(context.getSource(), "allowOther")));
        load.then(Commands.literal("showTitleOnPlayerConnected")
                .executes(context -> loadProperty(context.getSource(), "showTitleOnPlayerConnected")));
//        load.then(Commands.literal("ignoreShape")
//                .executes(context -> loadProperty(context.getSource(), "ignoreShape")));
        load.then(Commands.literal("plantDelay")
                .executes(context -> loadProperty(context.getSource(), "plantDelay")));
        load.then(Commands.literal("avoidDense")
                .executes(context -> loadProperty(context.getSource(), "avoidDense")));
        load.then(Commands.literal("playerAround")
                .executes(context -> loadProperty(context.getSource(), "playerAround")));

        // /saplanting load
        root.then(load);

        // /saplanting save
        root.then(Commands.literal("save").executes(context -> saveProperty(context.getSource())));

        // register
        dispatcher.register(root);
    }

    public static int showBlackList(CommandSourceStack source) {
        if (Config.blackListSize() == 0) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.display.empty"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.display"), false);
            source.sendSuccess(new TextComponent(Config.strBlackList()), false);
        }
        return Config.blackListSize();
    }

    public static int addBlackList(CommandSourceStack source, Item item) {
        if (!(item instanceof BlockItem) || !Config.isItemAllowed(((BlockItem) item))) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.add.notPlantable")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        }
        
        if (Config.inBlackList(((BlockItem) item))) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.add.inBlackList")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.add.success"), false);
            Config.addToBlackList(((BlockItem) item));
            return 1;
        }
    }

    public static int removeBlackList(CommandSourceStack source, Item item) {
        if (item instanceof BlockItem && Config.inBlackList(((BlockItem) item))) {
            Config.rmFromBlackList(((BlockItem) item));
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.remove.success"), false);
            return 1;
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.remove.notInBlackList")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        }
    }

    public static int clearBlackList(CommandSourceStack source) {
        int output = Config.blackListSize();
        Config.clearBlackList();
        source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.clear"), false);
        return output;
    }

    public static int showAll(CommandSourceStack target, int page) {
        target.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.showAll")
                        .setStyle(Style.EMPTY.withColor(TextColor.parseColor("gold"))), false);

        if (page < 0 || page > 3) {
            page = 1;
        }

        switch (page) {
            case 1:
                target.sendSuccess(new TextComponent(" - plantEnable:   " + Config.getPlantEnable()), false);
                target.sendSuccess(new TextComponent(" - plantLarge:    " + Config.getPlantLarge()), false);
                target.sendSuccess(new TextComponent(" - blackList:     " + Config.getBlackListEnable()), false);
                target.sendSuccess(new TextComponent(" - allowSapling:  " + Config.getAllowSapling()), false);
                target.sendSuccess(new TextComponent(" - allowCrop:     " + Config.getAllowCrop()), false);
                target.sendSuccess(new TextComponent(" - allowMushroom: " + Config.getAllowMushroom()), false);
                target.sendSuccess(new TextComponent(" - allowFungus:   " + Config.getAllowFungus()), false);
                target.sendSuccess(new TextComponent(" - allowFlower:   " + Config.getAllowFlower()), false);
                target.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.showAll.nextPage").setStyle(Style.EMPTY
                        .withColor(TextColor.parseColor("green"))
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page + 1)))), false);
                break;
            case 2:
                target.sendSuccess(new TextComponent(" - allowOther:    " + Config.getAllowOther()), false);
                target.sendSuccess(new TextComponent(" - showTitle... : " + Config.getAllowOther()).setStyle(Style.EMPTY
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("showTitleOnPlayerConnected")))),
                        false);
//                target.sendSuccess(new TextComponent(" - ignoreShape:   " + Config.getIgnoreShape()), false);
                target.sendSuccess(new TextComponent(" - plantDelay:    " + Config.getPlantDelay()), false);
                target.sendSuccess(new TextComponent(" - avoidDense:    " + Config.getAvoidDense()), false);
                target.sendSuccess(new TextComponent(" - playerAround:  " + Config.getPlayerAround()), false);
                target.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.showAll.formerPage").setStyle(Style.EMPTY
                        .withColor(TextColor.parseColor("green"))
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/saplanting " + (page - 1)))), false);
                break;
        }

        return 1;
    }

    public static int setPlantEnable(CommandSourceStack source, boolean value) {
        Config.setPlantEnable(value);
        source.sendSuccess(new TextComponent("plantEnable")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(new TextComponent(Boolean.toString(value))), false);
        return value ? 1 : 0;
    }

    public static int setPlantLarge(CommandSourceStack source, boolean value) {
        Config.setPlantLarge(value);
        source.sendSuccess(new TextComponent("plantLarge")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setBlackListEnable(CommandSourceStack source, boolean value) {
        Config.setBlackListEnable(value);
        if (value) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.enable"), false);
            return 1;
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.disable"), false);
            return 0;
        }
    }

    public static int setAllowSapling(CommandSourceStack source, boolean value) {
        Config.setAllowSapling(value);
        source.sendSuccess(new TextComponent("allowSapling")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setAllowCrop(CommandSourceStack source, boolean value) {
        Config.setAllowCrop(value);
        source.sendSuccess(new TextComponent("allowCrop")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setAllowMushroom(CommandSourceStack source, boolean value) {
        Config.setAllowMushroom(value);
        source.sendSuccess(new TextComponent("allowMushroom")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setAllowFungus(CommandSourceStack source, boolean value) {
        Config.setAllowFungus(value);
        source.sendSuccess(new TextComponent("allowFungus")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setAllowFlower(CommandSourceStack source, boolean value) {
        Config.setAllowFlower(value);
        source.sendSuccess(new TextComponent("allowFlower")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setAllowOther(CommandSourceStack source, boolean value) {
        Config.setAllowOther(value);
        source.sendSuccess(new TextComponent("allowOther")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setShowTitleOnPlayerConnected(CommandSourceStack source, boolean value) {
        Config.setShowTitleOnPlayerConnected(value);
        source.sendSuccess(new TextComponent("showTitleOnPlayerConnected")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setIgnoreShape(CommandSourceStack source, boolean value) {
        Config.setIgnoreShape(value);
        source.sendSuccess(new TextComponent("ignoreShape")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Boolean.toString(value)), false);
        return value ? 1 : 0;
    }

    public static int setPlantDelay(CommandSourceStack source, int value) {
        Config.setPlantDelay(value);
        source.sendSuccess(new TextComponent("plantDelay")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Integer.toString(value)), false);
        return value;
    }

    public static int setAvoidDense(CommandSourceStack source, int value) {
        Config.setAvoidDense(value);
        source.sendSuccess(new TextComponent("avoidDense")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Integer.toString(value)), false);
        return value;
    }

    public static int setPlayerAround(CommandSourceStack source, int value) {
        Config.setPlayerAround(value);
        source.sendSuccess(new TextComponent("playerAround")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.set"))
                .append(Integer.toString(value)), false);
        return value;
    }

    public static int getPlantEnable(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("plantEnable")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getPlantEnable())), false);
        return Config.getPlantEnable() ? 1 : 0;
    }

    public static int getPlantLarge(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("plantLarge")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getPlantLarge())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getBlackListEnable(CommandSourceStack source) {
        if (Config.getBlackListEnable()) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.enable"), false);
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.blackList.disable"), false);
        }
        return Config.getBlackListEnable() ? 1 : 0;
    }

    public static int getAllowSapling(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowSapling")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowSapling())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getAllowCrop(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowCrop")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowCrop())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getAllowMushroom(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowMushroom")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowMushroom())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getAllowFungus(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowFungus")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowFungus())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getAllowFlower(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowFlower")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowFlower())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getAllowOther(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("allowOther")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getAllowOther())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getShowTitleOnPlayerConnected(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("showTitleOnPlayerConnected")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getShowTitleOnPlayerConnected())), false);
        return Config.getPlantLarge() ? 1 : 0;
    }

    public static int getIgnoreShape(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("ignoreShape")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Boolean.toString(Config.getIgnoreShape())), false);
        return Config.getIgnoreShape() ? 1 : 0;
    }

    public static int getPlantDelay(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("plantDelay")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Integer.toString(Config.getPlantDelay())), false);
        return Config.getPlantDelay();
    }

    public static int getAvoidDense(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("avoidDense")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Integer.toString(Config.getAvoidDense())), false);
        return Config.getAvoidDense();
    }

    public static int getPlayerAround(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("playerAround")
                .append(new TranslatableComponent("saplanting.commands.saplanting.property.show"))
                .append(Integer.toString(Config.getPlayerAround())), false);
        return Config.getPlayerAround();
    }

    public static int loadProperty(CommandSourceStack source) {
        try {
            Config.load();
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.load.success.head")
                    .append(new TranslatableComponent("saplanting.commands.saplanting.load.success.suggestCommand")
                            .setStyle(Style.EMPTY
                                    .withUnderlined(true).withColor(TextColor.parseColor("green"))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/saplanting"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            new TranslatableComponent("saplanting.commands.saplanting.load.success.suggestEvent"))))
                    ).append(new TranslatableComponent("saplanting.commands.saplanting.load.success.tail")
                    ), false);
        } catch (Exception e) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.load.fail")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        }
        return 1;
    }

    public static int loadProperty(CommandSourceStack source, String name) {
        if (Config.load(name)) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.load.property.success")
                    .append(new TextComponent(name).setStyle(Style.EMPTY
                            .withUnderlined(true).withColor(TextColor.parseColor("green"))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/saplanting " + name))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new TranslatableComponent("saplanting.commands.saplanting.load.property.success.suggestEvent"))))
                    ), false);
            return 1;
        } else {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.load.property.fail")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        }
    }

    public static int saveProperty(CommandSourceStack source) {
        try {
            Config.save();
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.save.success")
                    .append(new TranslatableComponent(Config.stringPath()).setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, Config.stringPath()))
                            .withUnderlined(true)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                    , new TranslatableComponent("saplanting.commands.saplanting.save.path")))
                    )), false);
        } catch (Exception e) {
            source.sendSuccess(new TranslatableComponent("saplanting.commands.saplanting.save.fail")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("red"))), false);
            return 0;
        }

        return 1;
    }
}
