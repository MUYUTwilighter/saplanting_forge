package cool.muyucloud.saplanting.events;

import cool.muyucloud.saplanting.Config;
import cool.muyucloud.saplanting.Saplanting;
import net.minecraft.block.*;
import net.minecraft.block.trees.BigTree;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;

import java.util.LinkedList;

public class ItemEntityEvent {
    public static int expireTime = 6000;
    private static Thread THREAD = null;
    private static final LinkedList<ItemEntity> TASKS = new LinkedList<>();
    private static boolean loop = false;

    public static void onItemDrop(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity)) {  // is entity an itemEntity?
            return;
        }
        ItemEntity itemEntity = ((ItemEntity) entity);
        Item item = ((ItemEntity) entity).getItem().getItem();
        if (!isPlant(item)) {
            return;
        }
        itemEntity.lifespan = itemEntity.getAge() + 1;
    }

    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntityItem();
        Item item = entity.getItem().getItem();
        if (!isPlant(item)) { // is item placeable?
            return;
        }

        if (entity.getAge() > expireTime) { // should it be expired?
            return;
        }
        event.setExtraLife(1);
        event.setCanceled(true);

//        if (!tickPlantCheck(entity)) { // tick-check
//            entity.tickCount = 0;
//            return;
//        }
//
//        if (entity.tickCount > Config.getPlantDelay() && roundPlantCheck(entity)) { // round-check
//            plant(entity);
//            entity.tickCount = 0;
//        }

        /* Thread Call */
        if (!loop) {
            loop = true;
            Saplanting.LOGGER.info("Launching Saplanting core thread.");
            THREAD = new Thread(ItemEntityEvent::run);
            THREAD.setName("SaplantingCoreThread");
            THREAD.start();
        }

        /* Add to Task Queue */
        TASKS.add(entity);
    }

    public static void run() {
        try {
            while (loop) {
                if (TASKS.isEmpty()) {
                    Thread.sleep(50);
                    continue;
                }

                while (!TASKS.isEmpty()) {
                    ItemEntity entity = TASKS.removeFirst();
                    if (!tickPlantCheck(entity)) { // tick-check
                        entity.tickCount = 0;
                        continue;
                    }

                    if (entity.tickCount > Config.getPlantDelay() && roundPlantCheck(entity)) { // round-check
                        plant(entity);
                        entity.tickCount = 0;
                    }
                }
            }
        } catch (Exception e) {
            Saplanting.LOGGER.info("Saplanting core thread exited!");
            e.printStackTrace();
        }
        loop = false;
    }

    public static void stop() {
        loop = false;
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
        if (Config.getPlayerAround() > 0 &&
                world.hasNearbyAlivePlayer(entity.getX(), entity.getY(), entity.getZ(), Config.getPlayerAround())) {
            return false;
        }

        if (block instanceof SaplingBlock) {
            /* Avoid Dense Check */
            if (Config.getAvoidDense() > 0) {
                for (BlockPos pos : BlockPos.betweenClosed(
                        blockPos.offset(-Config.getAvoidDense(), -Config.getAvoidDense(), -Config.getAvoidDense()),
                        blockPos.offset(Config.getAvoidDense(), Config.getAvoidDense(), Config.getAvoidDense())
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
        if (!entity.isOnGround() || !Config.getPlantEnable() || !Config.isItemAllowed(item)) {
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
            /* Plant Large Tree */
            if (Config.getPlantLarge() && stack.getCount() >= 4 && ((SaplingBlock) block).treeGrower instanceof BigTree) {
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
//            // This is disabled because AT cannot be applied to net.minecraft.block.trees.Tree#getConfiguredFeature(Random, boolean)
//            /* Ignore Shape */
//            if (Config.getIgnoreShape() && ((SaplingBlock) block).treeGrower.getConfiguredFeature(new Random(), true) == null) {
//                return;
//            }
        }

        /* Plant Small Objects(including sapling) */
        world.setBlock(pos, state, 3);
        stack.setCount(stack.getCount() - 1);
    }

    public static boolean isPlant(Item item) {
        return item instanceof BlockItem && ((BlockItem) item).getBlock() instanceof BushBlock;
    }
}
