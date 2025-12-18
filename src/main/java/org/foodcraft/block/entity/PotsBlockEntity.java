package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.foodcraft.block.process.KneadingProcess;
import org.foodcraft.registry.ModBlockEntityTypes;

/**
 * 盆方块实体，支持揉面流程，实现Inventory接口
 */
public class PotsBlockEntity extends BlockEntity implements Inventory {

    /** 当前揉面流程 */
    private KneadingProcess<PotsBlockEntity> kneadingProcess;

    /** 存储最终产品的槽位 */
    private final DefaultedList<ItemStack> inventory;

    public PotsBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.POTS, pos, state);
        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
        this.kneadingProcess = new KneadingProcess<>();
    }

    /**
     * 尝试揉面交互。
     * @param state 方块状态
     * @param world 世界
     * @param pos 方块位置
     * @param player 交互的玩家
     * @param hand 交互的手
     * @param hit 交互的操作
     * @return 交互的结果
     */
    public ActionResult tryKnead(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack heldStack = player.getStackInHand(hand);

        // 如果没有流程，检查是否手持面粉开始新流程
        if (!kneadingProcess.isActive()) {
            if (KneadingProcess.isCanAddFlour(heldStack)) {
                kneadingProcess.start();
            } else {
                return ActionResult.PASS;
            }
        }

        // 执行流程步骤
        return kneadingProcess.executeStep(this, state, world, pos, player, hand, hit);
    }

    /**
     * 获取当前揉面流程
     */
    public KneadingProcess<PotsBlockEntity> getKneadingProcess() {
        return kneadingProcess;
    }

    /**
     * 检查是否正在进行揉面流程
     */
    public boolean isKneadingInProgress() {
        return kneadingProcess != null && kneadingProcess.isActive();
    }

    /**
     * 获取当前步骤
     */
    public String getCurrentStep() {
        return kneadingProcess != null ? kneadingProcess.getCurrentStepId() : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        // 保存库存
        Inventories.writeNbt(nbt, inventory);

        // 保存揉面流程
        if (kneadingProcess != null) {
            NbtCompound processNbt = new NbtCompound();
            kneadingProcess.writeToNbt(processNbt);
            nbt.put("kneading_process", processNbt);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        // 读取库存
        this.inventory.clear();
        Inventories.readNbt(nbt, inventory);

        // 读取揉面流程
        if (nbt.contains("kneading_process")) {
            kneadingProcess = new KneadingProcess<>();
            kneadingProcess.readFromNbt(nbt.getCompound("kneading_process"));
        }
    }

    // ============ Inventory接口实现 ============

    @Override
    public int size() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < 0 || slot >= inventory.size()) {
            return ItemStack.EMPTY;
        }
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(inventory, slot, amount);


        markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(inventory, slot);

        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
            markDirty();
        }
    }

    @Override
    public void markDirty() {
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }

        super.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        inventory.clear();
        markDirty();
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return this.createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}