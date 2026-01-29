package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.content.ShapedDoughContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;
import org.foodcraft.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

public class MoldBlockEntity extends BlockEntity{
    private static final String CONTENT_KEY = "shaped_dough";

    /** 当前模具中的定型面团 */
    @Nullable protected ShapedDoughContent shapedDough;

    public MoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.MOLD, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        if (shapedDough != null) {
            nbt.putString(CONTENT_KEY, shapedDough.getId().toString());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        if (nbt.contains(CONTENT_KEY, NbtElement.STRING_TYPE)) {
            AbstractContent content = ContentRegistry.get(Identifier.tryParse(nbt.getString(CONTENT_KEY)));
            if (content instanceof ShapedDoughContent dough) {
                this.shapedDough = dough;
            }
        }
    }

    /**
     * 检查物品是否能作为配方输入
     * @param stack 要输入的物品堆栈
     * @return 是否可以作为配方输入
     */
    private boolean isValidMoldInput(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return ShapedDoughContent.fromBaseGet(stack, getCachedState()) != null;
    }

    /**
     * 尝试向模具中添加一个面团。
     *
     * @param stack 要添加的堆栈
     * @return 是否添加成功，堆栈不是合适的面团或者模具已经拥有定型面团时则失败。
     */
    public boolean addDough(ItemStack stack) {
        if (shapedDough != null) {
            return false;
        }

        ShapedDoughContent content = ShapedDoughContent.fromBaseGet(stack, getCachedState());
        if (content == null) {
            return false;
        }

        setShapedDough(content);
        return true;
    }

    /**
     * 清空当前模具中的面团并返回原始物品。
     *
     * @return 当前定型面团的原始堆栈，当不存在定型面团时返回空物品堆栈
     */
    public ItemStack getAndClearResultStack() {
        if (shapedDough != null) {
            ItemStack res = shapedDough.getOriginalDough().getDefaultStack();
            shapedDough = null;
            return res;
        }

        return ItemStack.EMPTY;
    }

    public void setShapedDough(@Nullable ShapedDoughContent shapedDough) {
        this.shapedDough = shapedDough;
    }

    public @Nullable ShapedDoughContent getShapedDough() {
        return shapedDough;
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
