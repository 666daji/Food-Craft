package org.foodcraft.contentsystem.container;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.registry.ContainerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 容器类型抽象基类。
 * <p>
 * 表示一种可以承载内容物的容器类型，如碗、瓶、桶等。
 * 每个容器类型定义了容器的基本属性（空容器物品、容量、音效等）
 * 以及如何检查物品是否匹配该容器类型、如何提取内容物等。
 * </p>
 */
public abstract class ContainerType {
    private final Identifier id;
    private final Item emptyItem;
    private final int baseCapacity;
    private final SoundEvent useSound;

    /**
     * 创建一个容器类型实例。
     *
     * @param id 容器类型的唯一标识符
     * @param settings 容器设置
     * @throws NullPointerException 如果id或settings为null
     */
    protected ContainerType(Identifier id, ContainerSettings settings) {
        this.id = Objects.requireNonNull(id, "Container type ID cannot be null");
        Objects.requireNonNull(settings, "Container settings cannot be null");

        this.emptyItem = Objects.requireNonNull(settings.emptyItem, "Empty container item cannot be null");
        this.baseCapacity = settings.baseCapacity;
        this.useSound = Objects.requireNonNull(settings.useSound, "Use sound cannot be null");

        try {
            ContainerRegistry.register(this);
        } catch (Exception e) {
            FoodCraft.LOGGER.error("Container type registration failed: {}", id);
        }
    }

    /**
     * 容器设置类，用于传递初始化参数给ContainerType
     */
    public static class ContainerSettings {
        private final Item emptyItem;
        private int baseCapacity = 1;
        private SoundEvent useSound = SoundEvents.ITEM_BOTTLE_FILL;

        /**
         * 创建容器设置
         *
         * @param emptyItem 空容器物品（必填）
         */
        public ContainerSettings(Item emptyItem) {
            this.emptyItem = Objects.requireNonNull(emptyItem, "Empty container item cannot be null");
        }

        /**
         * 设置容器的基本容量（单位数）
         *
         * @param baseCapacity 基本容量，必须大于0
         * @return 当前容器设置实例，用于链式调用
         * @throws IllegalArgumentException 如果baseCapacity小于等于0
         */
        public ContainerSettings setBaseCapacity(int baseCapacity) {
            if (baseCapacity <= 0) {
                throw new IllegalArgumentException("Base capacity must be greater than 0");
            }
            this.baseCapacity = baseCapacity;
            return this;
        }

        /**
         * 设置使用容器时的音效
         *
         * @param useSound 使用音效
         * @return 当前容器设置实例，用于链式调用
         * @throws NullPointerException 如果useSound为null
         */
        public ContainerSettings setUseSound(@NotNull SoundEvent useSound) {
            this.useSound = Objects.requireNonNull(useSound, "Use sound cannot be null");
            return this;
        }
    }

    /**
     * 获取容器类型的唯一标识符。
     */
    @NotNull
    public final Identifier getId() {
        return id;
    }

    /**
     * 获取空容器对应的物品。
     */
    @NotNull
    public Item getEmptyItem() {
        return emptyItem;
    }

    /**
     * 获取容器的基本容量（单位数）。
     */
    public int getBaseCapacity() {
        return baseCapacity;
    }

    /**
     * 获取使用容器时的音效。
     */
    @NotNull
    public SoundEvent getUseSound() {
        return useSound;
    }

    /**
     * 获取容器的使用剩余
     * @return 使用剩余，默认为对应的空容器
     */
    public ItemStack remainder() {
        return createEmptyItemStack(1);
    }

    /**
     * 判断一个物品堆栈是否属于该容器类型。
     * <p>
     * 子类必须实现此方法，根据物品的ID、NBT数据等判断。
     * </p>
     *
     * @param stack 要检查的物品堆栈
     * @return 如果物品堆栈属于该容器类型则返回true
     */
    public abstract boolean matches(ItemStack stack);

    /**
     * 判断该容器类型是否可以装入指定的内容物类型。
     * <p>
     * 子类必须实现此方法，定义容器可以承载哪些内容物。
     * </p>
     *
     * @param content 要检查的内容物类型
     * @return 如果容器可以装入该内容物则返回true
     */
    public abstract boolean canContain(AbstractContent content);

    /**
     * 从物品堆栈中提取内容物类型。
     * <p>
     * 只有当物品堆栈属于该容器类型时，才会尝试提取内容物。
     * 如果容器是空的，应返回null。
     * </p>
     *
     * @param stack 要提取内容物的物品堆栈
     * @return 提取到的内容物类型，如果不匹配或为空则返回null
     */
    @Nullable
    public abstract AbstractContent extractContent(ItemStack stack);

    /**
     * 替换或填充容器中的内容物。
     * <p>
     * 此方法会修改给定的物品堆栈，将容器的内容物替换为指定的内容物，或者清空容器。
     * </p>
     *
     * @param stack 要修改的容器物品堆栈（必须是空容器或已装有内容物的容器）
     * @param content 要装入的新内容物类型，为null时清空容器
     * @return 填充后的物品堆栈。
     * @throws IllegalArgumentException 如果容器不能装入该内容物或stack不是有效的容器则返回原堆栈并抛出警告
     */
    @NotNull
    public abstract ItemStack replaceContent(@NotNull ItemStack stack, @Nullable AbstractContent content);

    /**
     * 抛出无效容器异常。
     * @param stack 无效的容器堆栈
     */
    protected void invalidContainer(ItemStack stack) {
        throw new IllegalArgumentException("Attempted to replace content on invalid container stack: " + stack);
    }

    /**
     * 抛出无效内容物异常。
     * @param content 无效的内容物
     */
    protected void invalidContent(AbstractContent content) {
        throw new IllegalArgumentException("Container " + getId() +" cannot contain content: " + content.getId());
    }

    /**
     * 创建一个空的该容器类型的物品堆栈。
     *
     * @param amount 物品数量
     * @return 空的容器物品堆栈
     */
    @NotNull
    public ItemStack createEmptyItemStack(int amount) {
        return new ItemStack(emptyItem, amount);
    }

    /**
     * 创建装有指定内容物的物品堆栈。
     * <p>默认实现：创建一个空容器，然后用指定内容物填充</p>
     *
     * @param content 要装入的内容物类型
     * @param amount 物品数量
     * @return 装有内容物的物品堆栈
     */
    @NotNull
    public ItemStack createItemStack(AbstractContent content, int amount) {
        ItemStack stack = createEmptyItemStack(amount);
        return replaceContent(stack, content);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ContainerType that = (ContainerType) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}