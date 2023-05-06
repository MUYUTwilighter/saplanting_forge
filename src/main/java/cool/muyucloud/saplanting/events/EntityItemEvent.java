package cool.muyucloud.saplanting.events;

import cool.muyucloud.saplanting.util.Config;
import cool.muyucloud.saplanting.Saplanting;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;

public class EntityItemEvent {
    public static int expireTime = 6000;
    private static final LinkedList<EntityItem> TASKS_1 = new LinkedList<>();
    private static final LinkedList<EntityItem> TASKS_2 = new LinkedList<>();
    private static boolean SWITCH = true;
    private static boolean THREAD_ALIVE = false;
    private static final Logger LOGGER = Saplanting.getLogger();
    private static final Config CONFIG = Saplanting.getConfig();

    @SubscribeEvent
    public void onItemDrop(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EntityItem)) {  // is entity an EntityItem?
            return;
        }
        EntityItem EntityItem = ((EntityItem) entity);
        Item item = EntityItem.getItem().getItem();
        if (!Saplanting.isPlantItem(item)) {
            return;
        }
        EntityItem.lifespan = EntityItem.getAge() + 1;
    }

    @SubscribeEvent
    public void onItemExpire(ItemExpireEvent event) {
        EntityItem entity = event.getEntityItem();
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
            Thread THREAD = new Thread(EntityItemEvent::multiThreadRun);
            THREAD.setName("SaplantingCoreThread");
            THREAD.start();
        }

        /* Add item entity as tasks for multi thread run */
        addToQueue(entity);
    }

    public static void run(EntityItem entity) {
        if (!tickPlantCheck(entity)) { // tick-check
            entity.ticksExisted = -1;
            return;
        }

        if (entity.ticksExisted > CONFIG.getAsInt("plantDelay") && roundPlantCheck(entity)) { // round-check
            plant(entity);
            entity.ticksExisted = -1;
        }
    }

    public static void multiThreadRun() {
        try {
            while (THREAD_ALIVE && CONFIG.getAsBoolean("plantEnable") && CONFIG.getAsBoolean("multiThread")) {
                LinkedList<EntityItem> TASKS;
                if (SWITCH) {
                    TASKS = TASKS_2;
                } else {
                    TASKS = TASKS_1;
                }

                while (!TASKS.isEmpty() && CONFIG.getAsBoolean("plantEnable") && THREAD_ALIVE && CONFIG.getAsBoolean("multiThread")) {
                    EntityItem task = TASKS.removeFirst();
                    Item item = task.getItem().getItem();
                    if (item instanceof ItemAir) { // In case item was removed mill-secs ago
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
    public static boolean roundPlantCheck(EntityItem entity) {
        BlockBush block = ((BlockBush) ((ItemBlock) entity.getItem().getItem()).getBlock());
        BlockPos blockPos = entity.getPosition();
        if (entity.posY % 1 != 0) {
            blockPos = blockPos.up();
        }
        World world = entity.world;

        /* Player Around Check */
        if (CONFIG.getAsInt("playerAround") > 0 &&
            world.isAnyPlayerWithinRangeAt(blockPos.getX(), blockPos.getY(), blockPos.getZ(), CONFIG.getAsInt("playerAround"))) {
            return false;
        }

        if (block instanceof BlockSapling) {
            /* Avoid Dense Check */
            if (CONFIG.getAsInt("avoidDense") > 0) {
                BlockPos start = blockPos.add(-CONFIG.getAsInt("avoidDense"), -CONFIG.getAsInt("avoidDense"), -CONFIG.getAsInt("avoidDense"));
                BlockPos end = blockPos.add(CONFIG.getAsInt("avoidDense"), CONFIG.getAsInt("avoidDense"), CONFIG.getAsInt("avoidDense"));
                for (BlockPos pos : BlockPos.getAllInBox(start, end)) {
                    Block tmpBlock = world.getBlockState(pos).getBlock();
                    if (tmpBlock instanceof BlockLeaves
                        || tmpBlock instanceof BlockSapling
                        || tmpBlock instanceof BlockLog) {
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
     * 4. block BELOW the EntityItem allows the plant to grow
     * 5. block AT the EntityItem is replaceable
     */
    public static boolean tickPlantCheck(EntityItem entity) {
        ItemBlock item = ((ItemBlock) entity.getItem().getItem());
        if (!entity.onGround || !CONFIG.getAsBoolean("plantEnable") || !Saplanting.isPlantAllowed(item)) {
            return false;
        }

        BlockPos pos = entity.getPosition();
        if (entity.posY % 1 != 0) {
            pos = pos.up();
        }

        BlockBush block = ((BlockBush) item.getBlock());
        World world = entity.world;

        return block.canPlaceBlockAt(world, pos) &&
            world.getBlockState(pos).getMaterial().isReplaceable();
    }

    /**
     * Plant operation, but also combines some checks.
     * Including:
     * 1. is planting large tree allowed? is it a large tree? So then do planting
     * 2. is it available to grow a small tree? So then do planting
     * Both of above involve environment check
     */
    public static void plant(EntityItem entity) {
        ItemStack stack = entity.getItem();
        World world = entity.world;
        BlockBush block = ((BlockBush) ((ItemBlock) entity.getItem().getItem()).getBlock());
        IBlockState state = block.getDefaultState();
        BlockPos pos = entity.getPosition();
        if (entity.posY % 1 != 0) {
            pos = pos.up();
        }

        if (block instanceof BlockSapling) {
//            // This is disabled because TreeGenerator is not a member of BlockSapling,
//            // we cannot classify which sapling can only grow in shape of 2x2
//            /* Plant Large Tree */
//            if (CONFIG.getAsBoolean("plantLarge") && stack.getCount() >= 4 && ((BlockSapling) block). instanceof BigTree) {
//                for (BlockPos tmpPos : BlockPos.betweenClosed(pos.add(-1, 0, -1), pos)) {
//                    if (block.canBlockStay(world, tmpPos, state) && world.getBlockState(tmpPos).getMaterial().isReplaceable()
//                        && block.canBlockStay(world, tmpPos.add(1, 0, 0), state) && world.getBlockState(tmpPos.add(1, 0, 0)).getMaterial().isReplaceable()
//                        && block.canBlockStay(world, tmpPos.add(1, 0, 1), state) && world.getBlockState(tmpPos.add(1, 0, 1)).getMaterial().isReplaceable()
//                        && block.canBlockStay(world, tmpPos.add(0, 0, 1), state) && world.getBlockState(tmpPos.add(0, 0, 1)).getMaterial().isReplaceable()) {
//                        world.setBlock(tmpPos, state, 3);
//                        world.setBlock(tmpPos.offset(1, 0, 0), state, 3);
//                        world.setBlock(tmpPos.offset(0, 0, 1), state, 3);
//                        world.setBlock(tmpPos.offset(1, 0, 1), state, 3);
//                        stack.setCount(stack.getCount() - 4);
//                        return;
//                    }
//                }
//            }
//            // This is disabled because AT cannot be applied to net.minecraft.block.trees.Tree#getConfiguredFeature(Random, boolean)
//            /* Ignore Shape */
//            if (CONFIG.getIgnoreShape() && ((BlockSapling) block).treeGrower.getConfiguredFeature(new Random(), true) == null) {
//                return;
//            }
        }

        /* Plant Small Objects(including sapling) */
        world.setBlockState(pos, state, 3);
        stack.setCount(stack.getCount() - 1);
    }

    /**
     * To visit task queue shared by saplanting-core-thread and MC server thread safely,
     * use this method to add items as tasks.
     * This should only be used by MC server thread.
     * */
    private static void addToQueue(EntityItem item) {
        LinkedList<EntityItem> queue;
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
