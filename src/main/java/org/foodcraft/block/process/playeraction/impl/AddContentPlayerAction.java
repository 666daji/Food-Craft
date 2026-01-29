package org.foodcraft.block.process.playeraction.impl;

import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.foodcraft.block.process.playeraction.PlayerAction;
import org.foodcraft.block.process.step.StepExecutionContext;
import org.foodcraft.contentsystem.api.ContainerContentBinding;
import org.foodcraft.contentsystem.api.ContainerUtil;
import org.foodcraft.contentsystem.content.AbstractContent;
import org.foodcraft.contentsystem.registry.ContentRegistry;

import java.util.Optional;

/**
 * 添加内容物操作。
 */
public class AddContentPlayerAction extends PlayerAction {
    public static final String TYPE = "add_content";

    private final AbstractContent content;
    private final int count;

    /**
     * 从字符串参数创建添加内容物操作。
     *
     * @param params 参数字符串数组
     * @return 添加内容物操作实例
     */
    public static AddContentPlayerAction fromParams(String[] params) {
        if (params.length < 1) {
            throw new IllegalArgumentException("The Add Content parameter requires the Content ID parameter");
        }

        Identifier contentId = Identifier.tryParse(params[0]);
        if (contentId == null) {
            throw new IllegalArgumentException("Invalid Content ID: " + params[0]);
        }

        AbstractContent content = ContentRegistry.get(contentId);
        if (content == null) {
            throw new IllegalArgumentException("No Content found: " + contentId);
        }

        int count = 1;
        if (params.length >= 2) {
            try {
                count = Integer.parseInt(params[1]);
                if (count <= 0) {
                    throw new IllegalArgumentException("The quantity must be greater than 0: " + count);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid quantity: " + params[1]);
            }
        }

        return new AddContentPlayerAction(content, count);
    }

    /**
     * 从上下文创建添加内容物操作。
     *
     * @param context 步骤执行上下文
     * @return 如果玩家手持正确的非空容器，则返回对应的添加内容物操作
     */
    public static Optional<PlayerAction> fromContext(StepExecutionContext<?> context) {
        ItemStack heldItem = context.getHeldItemStack();
        Optional<ContainerContentBinding> binding = ContainerUtil.analyze(heldItem);

        if (binding.isPresent() && binding.get().content() != null) {
            return Optional.of(new AddContentPlayerAction(binding.get().content(), binding.get().container().getBaseCapacity()));
        }
        return Optional.empty();
    }

    public AddContentPlayerAction(AbstractContent content, int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("The quantity must be greater than 0");
        }

        this.content = content;
        this.count = count;
    }

    public AddContentPlayerAction(AbstractContent content) {
        this(content, 1);
    }

    @Override
    public String toString() {
        Identifier contentId = content.getId();
        return String.format("add_content|%s|%d", contentId, count);
    }

    @Override
    public ItemStack toItemStack() {
        return ItemStack.EMPTY;
    }

    @Override
    public void consume(StepExecutionContext<?> context) {
        // 播放操作音效
        context.playSound(context.getItemSounds().getPlaceSound());

        if (context.isCreateMode()) {
            return;
        }

        // 如果不是创造模式，消耗玩家手持物品
        ItemStack heldItem = context.getHeldItemStack();
        Optional<ContainerContentBinding> binding = ContainerUtil.analyze(heldItem);

        if (!heldItem.isEmpty() && binding.isPresent() && binding.get().content() != null) {
            // 获取容器容量
            int capacity = binding.get().container().getBaseCapacity();

            // 计算需要消耗多少个容器（向上取整）
            int containersNeeded = (int) Math.ceil((double) count / capacity);

            // 消耗对应数量的容器
            ItemStack stack = heldItem.copyWithCount(containersNeeded);
            heldItem.decrement(containersNeeded);

            // 清空被消耗的容器中的内容物
            context.giveStack(ContainerUtil.replaceContent(stack, (AbstractContent) null));
        }
    }

    @Override
    public boolean matches(PlayerAction other) {
        if (!(other instanceof AddContentPlayerAction otherAction)) {
            return false;
        }

        // 匹配内容物类型和数量
        return this.content == otherAction.content && this.count <= otherAction.count;
    }

    @Override
    public String getCode() {
        Identifier itemId = content.getId();

        // 1. 操作类型固定位: "c" 表示添加物品
        StringBuilder code = new StringBuilder("c");

        // 2. 命名空间前2位（不足补'_'）
        String namespace = itemId.getNamespace();
        if (namespace.length() >= 2) {
            code.append(namespace, 0, 2);
        } else {
            code.append(namespace).append("_".repeat(2 - namespace.length()));
        }

        code.append("_");

        // 3. 物品路径前3位（不足补'_'）
        String path = itemId.getPath();
        if (path.length() >= 3) {
            code.append(path, 0, 3);
        } else {
            code.append(path).append("_".repeat(3 - path.length()));
        }

        // 4. 数量（如果大于1则添加，否则省略）
        if (count > 1) {
            code.append(count);
        }

        return code.toString();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("player_action.add_content", content.getDisplayName(), count);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
