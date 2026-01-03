package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.dfood.block.entity.ComplexFoodBlockEntity;
import org.foodcraft.item.FlourItem;
import org.foodcraft.item.FlourSackItem;
import org.foodcraft.registry.ModBlockEntityTypes;

import java.util.Optional;

/**
 * 粉尘袋方块实体，专注于粉尘袋特殊方法
 */
public class FlourSackBlockEntity extends ComplexFoodBlockEntity {
    public static final int DEFAULT_FLOUR_COLOR = 0xFFFFFF; // 默认白色

    public FlourSackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.FLOUR_SACK, pos, state);
    }

    /**
     * 获取指定位置的粉尘颜色
     */
    public int getFlourColor(int index) {
        NbtCompound nbt = getNbtAt(index);
        if (nbt != null && !nbt.isEmpty()) {
            Optional<ItemStack> sack = FlourSackItem.getBundledStack(getSackStack(index));
            if (sack.isPresent() && sack.get().getItem() instanceof FlourItem flourItem) {
                return flourItem.getColor();
            }
        }
        return DEFAULT_FLOUR_COLOR;
    }

    /**
     * 获取所有粉尘袋堆叠的颜色数组
     */
    public int[] getAllFlourColors() {
        int[] colors = new int[getNbtCount()];
        for (int i = 0; i < getNbtCount(); i++) {
            colors[i] = getFlourColor(i);
        }
        return colors;
    }

    /**
     * 获取指定位置的粉尘袋物品堆栈
     */
    public ItemStack getSackStack(int index) {
        NbtCompound nbt = getNbtAt(index);
        if (nbt != null && !nbt.isEmpty()) {
            // 从存储的NBT创建完整的粉尘袋物品
            ItemStack sackItem = new ItemStack(getCachedState().getBlock().asItem());

            // 复制原始NBT数据
            sackItem.setNbt(nbt.copy());
            return sackItem;
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取所有内容物
     */
    public DefaultedList<ItemStack> getAllContents() {
        DefaultedList<ItemStack> contents = DefaultedList.of();
        for (int i = 0; i < getNbtCount(); i++) {
            NbtCompound nbt = getNbtAt(i);
            if (nbt != null && !nbt.isEmpty()) {
                contents.add(ItemStack.fromNbt(nbt));
            }
        }
        return contents;
    }

    /**
     * 检查指定索引是否有效
     */
    public boolean isValidSackIndex(int index) {
        return index >= 0 && index < getNbtCount();
    }

    /**
     * 获取粉尘袋总占用空间
     */
    public int getTotalOccupancy() {
        int total = 0;
        for (int i = 0; i < getNbtCount(); i++) {
            NbtCompound nbt = getNbtAt(i);
            if (nbt != null && !nbt.isEmpty()) {
                ItemStack stack = ItemStack.fromNbt(nbt);
                if (!stack.isEmpty()) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    /**
     * 获取最大容量（每个粉尘袋最多16个粉尘）
     */
    public int getMaxCapacity() {
        return 16 * getNbtCount();
    }
}