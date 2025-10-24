package org.foodcraft.client.render.gui.tooltip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.foodcraft.FoodCraft;
import org.foodcraft.item.FlourSackItem;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class FlourSackTooltipComponent implements TooltipComponent {
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID,"textures/gui/bundle.png");
    private static final int SLOT_SIZE = 18;
    private static final int BORDER_SIZE = 1;
    private static final int TOTAL_SIZE = SLOT_SIZE + 2 * BORDER_SIZE; // 20x20

    private final Optional<ItemStack> content;
    private final int occupancy;
    private final int maxStorage;

    public FlourSackTooltipComponent(FlourSackItem.FlourSackTooltipData data) {
        this.content = data.content();
        this.occupancy = data.occupancy();
        this.maxStorage = data.maxStorage();
    }

    @Override
    public int getHeight() {
        return TOTAL_SIZE; // 固定高度，单格
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return TOTAL_SIZE; // 固定宽度，单格
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        // 绘制单个槽位
        boolean isFull = this.occupancy >= this.maxStorage;

        // 绘制槽位背景
        this.drawSlot(x, y, isFull, context);

        // 如果有内容物，绘制物品
        if (this.content.isPresent()) {
            ItemStack itemStack = this.content.get();
            int itemX = x + BORDER_SIZE + 1; // 居中放置物品
            int itemY = y + BORDER_SIZE + 1;

            context.drawItem(itemStack, itemX, itemY, 0);
            context.drawItemInSlot(textRenderer, itemStack, itemX, itemY);
        }
    }

    private void drawSlot(int x, int y, boolean isBlocked, DrawContext context) {
        // 绘制槽位边框
        this.drawBorder(x, y, context);

        // 绘制槽位内部
        Sprite sprite = isBlocked ? Sprite.BLOCKED_SLOT : Sprite.SLOT;
        context.drawTexture(TEXTURE, x + BORDER_SIZE, y + BORDER_SIZE, 0,
                sprite.u, sprite.v, sprite.width, sprite.height, 128, 128);
    }

    private void drawBorder(int x, int y, DrawContext context) {
        // 绘制四个角的边框
        this.draw(context, x, y, Sprite.BORDER_CORNER_TOP_LEFT);
        this.draw(context, x + SLOT_SIZE + BORDER_SIZE, y, Sprite.BORDER_CORNER_TOP_RIGHT);
        this.draw(context, x, y + SLOT_SIZE + BORDER_SIZE, Sprite.BORDER_CORNER_BOTTOM_LEFT);
        this.draw(context, x + SLOT_SIZE + BORDER_SIZE, y + SLOT_SIZE + BORDER_SIZE, Sprite.BORDER_CORNER_BOTTOM_RIGHT);

        // 绘制上下边框
        for (int i = 0; i < SLOT_SIZE; i++) {
            this.draw(context, x + BORDER_SIZE + i, y, Sprite.BORDER_HORIZONTAL_TOP);
            this.draw(context, x + BORDER_SIZE + i, y + SLOT_SIZE + BORDER_SIZE, Sprite.BORDER_HORIZONTAL_BOTTOM);
        }

        // 绘制侧边框
        for (int i = 0; i < SLOT_SIZE; i++) {
            this.draw(context, x, y + BORDER_SIZE + i, Sprite.BORDER_VERTICAL_LEFT);
            this.draw(context, x + SLOT_SIZE + BORDER_SIZE, y + BORDER_SIZE + i, Sprite.BORDER_VERTICAL_RIGHT);
        }
    }

    private void draw(DrawContext context, int x, int y, Sprite sprite) {
        context.drawTexture(TEXTURE, x, y, 0, sprite.u, sprite.v, sprite.width, sprite.height, 128, 128);
    }

    @Environment(EnvType.CLIENT)
    private enum Sprite {
        // 槽位背景
        SLOT(0, 0, 18, 18),
        BLOCKED_SLOT(0, 40, 18, 18),

        // 边框部分
        BORDER_HORIZONTAL_TOP(0, 20, 1, 1),
        BORDER_HORIZONTAL_BOTTOM(0, 60, 1, 1),
        BORDER_VERTICAL_LEFT(0, 18, 1, 1),
        BORDER_VERTICAL_RIGHT(19, 18, 1, 1),
        BORDER_CORNER_TOP_LEFT(0, 20, 1, 1),
        BORDER_CORNER_TOP_RIGHT(19, 20, 1, 1),
        BORDER_CORNER_BOTTOM_LEFT(0, 60, 1, 1),
        BORDER_CORNER_BOTTOM_RIGHT(19, 60, 1, 1);

        public final int u;
        public final int v;
        public final int width;
        public final int height;

        Sprite(int u, int v, int width, int height) {
            this.u = u;
            this.v = v;
            this.width = width;
            this.height = height;
        }
    }
}