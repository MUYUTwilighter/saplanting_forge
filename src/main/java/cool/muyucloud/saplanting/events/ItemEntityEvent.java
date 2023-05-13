package cool.muyucloud.saplanting.events;

import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.Saplanting;
import net.minecraft.block.*;
import net.minecraft.block.trees.BigTree;
import net.minecraft.block.trees.Tree;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.AirItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Random;

public class ItemEntityEvent {
    public static int expireTime = 6000;

    private static final LinkedList<ItemEntity> TASKS_1 = new LinkedList<>();
    private static final LinkedList<ItemEntity> TASKS_2 = new LinkedList<>();
    private static boolean SWITCH = true;
    private static boolean THREAD_ALIVE = false;
    private static final Logger LOGGER = Saplanting.getLogger();
    private static final Config CONFIG = Saplanting.getConfig();

    public static void onItemDrop(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity)) {  // is entity an itemEntity?
            return;
        }
        ItemEntity itemEntity = ((ItemEntity) entity);
        Item item = itemEntity.getItem().getItem();
        if (!Saplanting.isPlantItem(item)) {
            return;
        }
        itemEntity.lifespan = itemEntity.getAge() + 1;
    }

    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntityItem();
        Item item = entity.getItem().getItem();
        if (!Saplanting.isPlantItem(item)) { // is item placeable?
            return;
        }

        if (entity.getAge() > expireTime) { // should it be expired?
            return;
        }
        event.setExtraLife(1);
        event.setCanceled(true);

        /* Kill if multi thread disabled, and deal with item here */
        if (!CONFIG.getAsBoolean("multiThread")) {
            THREAD_ALIVE = false;
            run(entity);
            return;
        }

        /* Run Thread if thread not alive */
        if (!THREAD_ALIVE) {
            THREAD_ALIVE = true;
            LOGGER.info("Launching Saplanting core thread.");
            Thread THREAD = new Thread(ItemEntityEvent::multiThreadRun);
            THREAD.setName("SaplantingCoreThread");
            THREAD.start();
        }

        /* Add item entity as tasks for multi thread run */
        addToQueue(entity);
    }

    public static void run(ItemEntity entity) {
        if (!tickPlantCheck(entity)) { // tick-check
            entity.tickCount = -1;
            return;
        }

        if (entity.tickCount > CONFIG.getAsInt("plantDelay") && roundPlantCheck(entity)) { // round-check
            plant(entity);
            entity.tickCount = -1;
        }
    }

    public static void multiThreadRun() {
        try {
            while (THREAD_ALIVE && CONFIG.getAsBoolean("plantEnable") && CONFIG.getAsBoolean("multiThread")) {
                LinkedList<ItemEntity> TASKS;
                if (SWITCH) {
                    TASKS = TASKS_2;
                } else {
                    TASKS = TASKS_1;
                }

                while (!TASKS.isEmpty() && CONFIG.getAsBoolean("plantEnable") && THREAD_ALIVE && CONFIG.getAsBoolean("multiThread")) {
                    ItemEntity task = TASKS.removeFirst();
                    Item item = task.getItem().getItem();
                    if (item instanceof AirItem) { // In case item was removed mill-secs ago
                        continue;
                    }
                    run(task);
                }

                SWITCH = !SWITCH;

                Thread.sleep(20);
            }
            LOGGER.info("Saplanting core thread exiting.");
        } catch (Exception e) {
            LOGGER.info("Saplanting core thread exited unexpectedly!");
            e.printStackTrace();
        }
        THREAD_ALIVE = false;
    }

    public static void stop() {
        THREAD_ALIVE = false;
    }

    /**
     * This check validation of plant-operation every time when reaching plant-delay,
     * it goes after tick-check
     * Including:
     * 1. are players nearby?
     * 2. are there other blocks nearby representing there might be trees?
     */
    public static boolean roundPlantCheck(ItemEntity entity) {
        BushBlock block = ((BushBlock) ((BlockItem) entity.getItem().getItem()).getBlock());
        BlockPos blockPos = entity.blockPosition();
        if (entity.getY() % 1 != 0) {
            blockPos = blockPos.above();
        }
        World world = entity.level;

        /* Player Around Check */
        if (CONFIG.getAsInt("playerAround") > 0 &&
            world.hasNearbyAlivePlayer(entity.getX(), entity.getY(), entity.getZ(), CONFIG.getAsInt("playerAround"))) {
            return false;
        }

        if (block instanceof SaplingBlock) {
            /* Avoid Dense Check */
            if (CONFIG.getAsInt("avoidDense") > 0) {
                for (BlockPos pos : BlockPos.betweenClosed(
                    blockPos.offset(-CONFIG.getAsInt("avoidDense"), -CONFIG.getAsInt("avoidDense"), -CONFIG.getAsInt("avoidDense")),
                    blockPos.offset(CONFIG.getAsInt("avoidDense"), CONFIG.getAsInt("avoidDense"), CONFIG.getAsInt("avoidDense"))
                )) {
                    Block tmpBlock = world.getBlockState(pos).getBlock();
                    if (tmpBlock instanceof LeavesBlock
                        || tmpBlock instanceof SaplingBlock
                        || tmpBlock.defaultBlockState().is(BlockTags.LOGS)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * This check validation of plant-operation every single game-tick,
     * it goes before round-check
     * Including:
     * 1. item is on ground
     * 2. saplanting is enabled
     * 3. is not a water-oriented plant
     * 4. block BELOW the itemEntity allows the plant to grow
     * 5. block AT the itemEntity is replaceable
     */
    public static boolean tickPlantCheck(ItemEntity entity) {
        BlockItem item = ((BlockItem) entity.getItem().getItem());
        if (!entity.isOnGround() || !CONFIG.getAsBoolean("plantEnable") || !Saplanting.isPlantAllowed(item)) {
            return false;
        }

        BlockPos pos = entity.blockPosition();
        if (entity.getY() % 1 != 0) {
            pos = pos.above();
        }

        BushBlock block = ((BushBlock) item.getBlock());
        World world = entity.level;

        if (!block.defaultBlockState().getFluidState().isEmpty()) {
            return false;
        }

        return block.canSurvive(block.defaultBlockState(), world, pos) &&
            world.getBlockState(pos).getMaterial().isReplaceable();
    }

    /**
     * Plant operation, but also combines some checks.
     * Including:
     * 1. is planting large tree allowed? is it a large tree? So then do planting
     * 2. is it available to grow a small tree? So then do planting
     * Both of above involve environment check
     */
    public static void plant(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        World world = entity.level;
        BushBlock block = ((BushBlock) ((BlockItem) entity.getItem().getItem()).getBlock());
        BlockState state = block.defaultBlockState();
        BlockPos pos = entity.blockPosition();
        if (entity.getY() % 1 != 0) {
            pos = pos.above();
        }

        if (block instanceof SaplingBlock) {
            Tree tree = ((SaplingBlock) block).treeGrower;
            /* Plant Large Tree */
            if (CONFIG.getAsBoolean("plantLarge") && stack.getCount() >= 4 && tree instanceof BigTree) {
                for (BlockPos tmpPos : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos)) {
                    if (block.canSurvive(state, world, tmpPos) && world.getBlockState(tmpPos).getMaterial().isReplaceable()
                        && block.canSurvive(state, world, tmpPos.offset(1, 0, 0)) && world.getBlockState(tmpPos.offset(1, 0, 0)).getMaterial().isReplaceable()
                        && block.canSurvive(state, world, tmpPos.offset(1, 0, 1)) && world.getBlockState(tmpPos.offset(1, 0, 1)).getMaterial().isReplaceable()
                        && block.canSurvive(state, world, tmpPos.offset(0, 0, 1)) && world.getBlockState(tmpPos.offset(0, 0, 1)).getMaterial().isReplaceable()) {
                        world.setBlock(tmpPos, state, 3);
                        world.setBlock(tmpPos.offset(1, 0, 0), state, 3);
                        world.setBlock(tmpPos.offset(0, 0, 1), state, 3);
                        world.setBlock(tmpPos.offset(1, 0, 1), state, 3);
                        stack.setCount(stack.getCount() - 4);
                        return;
                    }
                }
            }
            // At this time, 2x2 planting failed
            // If want to plant anyway, which means ignoring shape, go to Plant Small
            // If shape should be concerned, and sapling cannot grow by 1x1, stop planting
            // Meanwhile, if sapling can, continue planting
            if (!CONFIG.getAsBoolean("ignoreShape") && !canPlant1x1(tree, world.random)) {
                return;
            }
        }

        /* Plant Small Objects(including sapling) */
        world.setBlock(pos, state, 3);
        stack.setCount(stack.getCount() - 1);
    }

    private static boolean canPlant1x1(Tree tree, Random random) {
        Object result = null;
        try {
            Class<? extends Tree> cl = tree.getClass();
            Method method = cl.getDeclaredMethod("getConfiguredFeature", Random.class, boolean.class);
            method.setAccessible(true);
            result = method.invoke(tree, random, false);
        } catch (Exception ignored) {
        }
        return result != null;
    }

    /**
     * To visit task queue shared by saplanting-core-thread and MC server thread safely,
     * use this method to add items as tasks.
     * This should only be used by MC server thread.
     * */
    private static void addToQueue(ItemEntity item) {
        LinkedList<ItemEntity> queue;
        if (SWITCH) {
            queue = TASKS_1;
        } else {
            queue = TASKS_2;
        }

        int size = queue.size();
        if (size > CONFIG.getAsInt("maxTask")) {
            queue.clear();
            if (CONFIG.getAsBoolean("warnTaskQueue")) {
                LOGGER.warn(String.format("Too many items! Cleared %s tasks.", size));
            }
        }

        queue.add(item);
    }
}
