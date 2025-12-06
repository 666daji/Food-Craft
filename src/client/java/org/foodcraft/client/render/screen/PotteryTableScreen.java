package org.foodcraft.client.render.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.foodcraft.FoodCraft;
import org.foodcraft.recipe.PotteryRecipe;
import org.foodcraft.screen.PotteryTableScreenHandler;

import java.util.List;

@Environment(EnvType.CLIENT)
public class PotteryTableScreen extends HandledScreen<PotteryTableScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(FoodCraft.MOD_ID, "textures/gui/container/pottery_table.png");

    /** 滚动条宽度 */
    private static final int SCROLLBAR_WIDTH = 12;
    /** 滚动条高度 */
    private static final int SCROLLBAR_HEIGHT = 15;
    /** 配方列表列数 */
    private static final int RECIPE_LIST_COLUMNS = 4;
    /** 配方列表行数 */
    private static final int RECIPE_LIST_ROWS = 3;
    /** 配方条目宽度 */
    private static final int RECIPE_ENTRY_WIDTH = 16;
    /** 配方条目高度 */
    private static final int RECIPE_ENTRY_HEIGHT = 18;
    /** 滚动条区域高度 */
    private static final int SCROLLBAR_AREA_HEIGHT = 54;
    /** 配方列表X偏移 */
    private static final int RECIPE_LIST_OFFSET_X = 52;
    /** 配方列表Y偏移 */
    private static final int RECIPE_LIST_OFFSET_Y = 14;

    /** 进度条位置和尺寸 */
    private static final int PROGRESS_BAR_X = 140;  // 进度条在GUI中的X坐标
    private static final int PROGRESS_BAR_Y = 57;   // 进度条在GUI中的Y坐标
    private static final int PROGRESS_BAR_WIDTH = 21;  // 进度条宽度 (161-140)
    private static final int PROGRESS_BAR_HEIGHT = 4; // 进度条高度 (67-60+1)

    /** 进度条覆盖纹理位置 */
    private static final int PROGRESS_OVERLAY_U = 176;  // 覆盖纹理的U坐标
    private static final int PROGRESS_OVERLAY_V = 24;   // 覆盖纹理的V坐标
    private static final int PROGRESS_OVERLAY_WIDTH = 21;  // 覆盖纹理宽度 (197-176)
    private static final int PROGRESS_OVERLAY_HEIGHT = 7; // 覆盖纹理高度 (31-14+1)

    /** 开始按钮位置和尺寸 */
    private static final int START_BUTTON_X = 23;    // 开始按钮在GUI中的X坐标
    private static final int START_BUTTON_Y = 55;    // 开始按钮在GUI中的Y坐标
    private static final int START_BUTTON_WIDTH = 10;  // 开始按钮宽度
    private static final int START_BUTTON_HEIGHT = 9; // 开始按钮高度

    /** 开始按钮覆盖纹理位置 */
    private static final int BUTTON_OVERLAY_U = 176;  // 按钮覆盖纹理的U坐标
    private static final int BUTTON_OVERLAY_V = 15;   // 按钮覆盖纹理的V坐标
    private static final int BUTTON_OVERLAY_WIDTH = 9;   // 按钮覆盖纹理宽度
    private static final int BUTTON_OVERLAY_HEIGHT = 8;  // 按钮覆盖纹理高度

    /** 输入物品图标缩放比例 */
    private static final float INPUT_ITEM_SCALE = 0.5f;

    /** 当前滚动位置 */
    private float scrollAmount;
    /** 鼠标是否点击了滚动条 */
    private boolean mouseClicked;
    /** 滚动偏移量 */
    private int scrollOffset;
    /** 是否可以制作 */
    private boolean canCraft;
    /** 开始制作按钮是否被按下 */
    private boolean startButtonPressed;

    public PotteryTableScreen(PotteryTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        handler.setContentsChangedListener(this::onInventoryChange);
        this.titleY--;
    }

    /**
     * 渲染屏幕。
     *
     * @param context 绘制上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param delta 帧间隔时间
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    /**
     * 绘制背景。
     *
     * @param context 绘制上下文
     * @param delta 帧间隔时间
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     */
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        this.renderBackground(context);
        int guiLeft = this.x;
        int guiTop = this.y;

        // 绘制主背景 (0,0) 到 (175,165)
        context.drawTexture(TEXTURE, guiLeft, guiTop, 0, 0, 176, 166);

        // 绘制滚动条
        int scrollBarY = (int)(41.0F * this.scrollAmount);
        context.drawTexture(TEXTURE, guiLeft + 119, guiTop + 15 + scrollBarY,
                176 + (this.shouldScroll() ? 0 : 12), 0, SCROLLBAR_WIDTH, SCROLLBAR_HEIGHT);

        // 绘制配方背景和图标
        int recipeListX = guiLeft + RECIPE_LIST_OFFSET_X;
        int recipeListY = guiTop + RECIPE_LIST_OFFSET_Y;
        int maxRecipes = this.scrollOffset + 12;
        this.renderRecipeBackground(context, mouseX, mouseY, recipeListX, recipeListY, maxRecipes);
        this.renderRecipeIcons(context, recipeListX, recipeListY, maxRecipes);

        // 根据制作状态绘制进度条覆盖或开始按钮
        if (this.handler.isCrafting()) {
            // 绘制制作进度条覆盖
            float progressRatio = this.handler.getCraftProgressRatio();
            if (progressRatio > 0) {
                int progressWidth = (int)(PROGRESS_OVERLAY_WIDTH * progressRatio);

                // 计算覆盖纹理的中间4像素区域
                int overlayStartY = PROGRESS_OVERLAY_V + 2; // 从中间开始，上下各去掉1像素
                int overlayHeight = PROGRESS_OVERLAY_HEIGHT - 4; // 总高度减去上下各2像素

                // 绘制进度条覆盖，使用中间4像素高度
                context.drawTexture(TEXTURE,
                        guiLeft + PROGRESS_BAR_X, guiTop + PROGRESS_BAR_Y, // Y坐标偏移2像素以居中
                        PROGRESS_OVERLAY_U, overlayStartY,
                        progressWidth, overlayHeight);
            }
        } else if (canCraft && this.handler.canStartCrafting()){
            // 绘制开始按钮背景
            context.drawTexture(TEXTURE,
                    guiLeft + START_BUTTON_X, guiTop + START_BUTTON_Y,
                    186, 15, START_BUTTON_WIDTH, START_BUTTON_HEIGHT);

            // 检查鼠标是否悬停在开始按钮上
            boolean isHovered = this.isPointWithinBounds(START_BUTTON_X, START_BUTTON_Y,
                    START_BUTTON_WIDTH, START_BUTTON_HEIGHT, mouseX, mouseY);

            // 如果按钮被按下或者鼠标悬停在按钮上，并且可以开始制作，绘制按钮覆盖
            if ((this.startButtonPressed && isHovered) ||
                    (isHovered && this.handler.canStartCrafting())) {
                context.drawTexture(TEXTURE,
                        guiLeft + START_BUTTON_X, guiTop + START_BUTTON_Y,
                        BUTTON_OVERLAY_U, BUTTON_OVERLAY_V,
                        BUTTON_OVERLAY_WIDTH, BUTTON_OVERLAY_HEIGHT);
            }
        }
    }

    /**
     * 绘制鼠标悬停提示。
     *
     * @param context 绘制上下文
     * @param x 鼠标X坐标
     * @param y 鼠标Y坐标
     */
    @Override
    protected void drawMouseoverTooltip(DrawContext context, int x, int y) {
        super.drawMouseoverTooltip(context, x, y);

        // 检查是否悬停在开始按钮上
        if (!this.handler.isCrafting() && this.handler.canStartCrafting() &&
                this.isPointWithinBounds(START_BUTTON_X, START_BUTTON_Y,
                        START_BUTTON_WIDTH, START_BUTTON_HEIGHT, x, y)) {
            context.drawTooltip(this.textRenderer, Text.translatable("tooltip.pottery_table.start_crafting"), x, y);
        }

        // 检查是否悬停在输出槽预览物品上
        if (this.handler.getSlot(1).getStack().isEmpty()) {
            PotteryRecipe selectedRecipe = this.handler.getSelectedRecipeInstance();
            if (selectedRecipe != null &&
                    this.isPointWithinBounds(143, 33, 16, 16, x, y)) {
                ItemStack previewStack = selectedRecipe.getOutput(this.client.world.getRegistryManager());
                if (!previewStack.isEmpty()) {
                    context.drawTooltip(this.textRenderer,
                            Text.translatable("tooltip.pottery_table.preview"), x, y);
                }
            }
        }

        if (this.canCraft) {
            int i = this.x + RECIPE_LIST_OFFSET_X;
            int j = this.y + RECIPE_LIST_OFFSET_Y;
            int k = this.scrollOffset + 12;
            List<PotteryRecipe> list = this.handler.getAvailableRecipes();

            for (int l = this.scrollOffset; l < k && l < this.handler.getAvailableRecipeCount(); l++) {
                int m = l - this.scrollOffset;
                int n = i + m % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
                int o = j + m / RECIPE_LIST_COLUMNS * RECIPE_ENTRY_HEIGHT + 2;
                if (x >= n && x < n + RECIPE_ENTRY_WIDTH && y >= o && y < o + RECIPE_ENTRY_HEIGHT) {
                    context.drawItemTooltip(this.textRenderer, list.get(l).getOutput(this.client.world.getRegistryManager()), x, y);
                }
            }
        }
    }

    /**
     * 绘制配方背景。
     *
     * @param context 绘制上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param scrollOffset 滚动偏移量
     */
    private void renderRecipeBackground(DrawContext context, int mouseX, int mouseY, int x, int y, int scrollOffset) {
        for (int i = this.scrollOffset; i < scrollOffset && i < this.handler.getAvailableRecipeCount(); i++) {
            int j = i - this.scrollOffset;
            int k = x + j % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
            int l = j / RECIPE_LIST_COLUMNS;
            int m = y + l * RECIPE_ENTRY_HEIGHT + 2;
            int n = this.backgroundHeight;
            if (i == this.handler.getSelectedRecipe()) {
                n += RECIPE_ENTRY_HEIGHT;
            } else if (mouseX >= k && mouseY >= m && mouseX < k + RECIPE_ENTRY_WIDTH && mouseY < m + RECIPE_ENTRY_HEIGHT) {
                n += 36;
            }

            context.drawTexture(TEXTURE, k, m - 1, 0, n, RECIPE_ENTRY_WIDTH, RECIPE_ENTRY_HEIGHT);
        }
    }

    /**
     * 绘制配方图标。
     *
     * @param context 绘制上下文
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param scrollOffset 滚动偏移量
     */
    private void renderRecipeIcons(DrawContext context, int x, int y, int scrollOffset) {
        List<PotteryRecipe> list = this.handler.getAvailableRecipes();

        // 获取输入槽的物品
        ItemStack inputStack = this.handler.getSlot(PotteryTableScreenHandler.INPUT_SLOT_INDEX).getStack();

        for (int i = this.scrollOffset; i < scrollOffset && i < this.handler.getAvailableRecipeCount(); i++) {
            int j = i - this.scrollOffset;
            int k = x + j % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH;
            int l = j / RECIPE_LIST_COLUMNS;
            int m = y + l * RECIPE_ENTRY_HEIGHT + 2;

            // 绘制配方输出图标
            context.drawItem(list.get(i).getOutput(null), k, m);

            // 在右下角绘制输入物品和数量
            if (!inputStack.isEmpty()) {
                PotteryRecipe recipe = list.get(i);
                if (recipe.getInput().test(inputStack)) {
                    drawInputRequirement(context, inputStack, recipe.getInputCount(), k, m);
                }
            }
        }
    }

    /**
     * 在配方图标右下角绘制输入物品和数量。
     *
     * @param context 绘制上下文
     * @param inputStack 输入物品堆栈
     * @param requiredCount 所需数量
     * @param iconX 图标左上角X坐标
     * @param iconY 图标左上角Y坐标
     */
    private void drawInputRequirement(DrawContext context, ItemStack inputStack, int requiredCount, int iconX, int iconY) {
        // 绘制背景框 - 缩小尺寸
        int bgX = iconX + 8;  // 从图标中间开始
        int bgy = iconY + 8;  // 从图标中间开始

        // 绘制输入物品图标（缩小版）
        int itemIconX = bgX - 2;
        int itemIconY = bgy + 2;

        // 保存当前变换
        context.getMatrices().push();
        // 缩小物品图标
        context.getMatrices().translate(itemIconX, itemIconY, 100);
        context.getMatrices().scale(0.4f, 0.4f, 1.0f);

        // 绘制物品图标（不使用计数，只显示图标）
        ItemStack displayStack = inputStack.copy();
        displayStack.setCount(1); // 只显示一个，数字另外绘制
        context.drawItemWithoutEntity(displayStack, 0, 0);

        // 恢复变换
        context.getMatrices().pop();

        // 绘制所需数量（使用缩小的文本）
        String countText = String.valueOf(requiredCount);
        int textX = bgX + 4; // 调整文本位置
        int textY = bgy + 2;

        // 保存当前变换
        context.getMatrices().push();

        // 缩小文本
        float scale = 0.5f;
        context.getMatrices().translate(textX, textY + (1 - scale) * 4, 101); // 调整位置以居中
        context.getMatrices().scale(scale, scale, 1.0f);

        // 绘制文本（白色）
        context.drawText(this.textRenderer, countText, 0, 0, 0xFFFFFF, false);

        // 恢复变换
        context.getMatrices().pop();
    }

    /**
     * 处理鼠标点击事件。
     *
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param button 鼠标按钮
     * @return 是否处理了点击事件
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.mouseClicked = false;
        if (this.client == null || this.client.interactionManager == null){
            return false;
        }

        // 检查是否点击了开始制作按钮
        if (!this.handler.isCrafting() && this.handler.canStartCrafting() &&
                this.isPointWithinBounds(START_BUTTON_X, START_BUTTON_Y,
                        START_BUTTON_WIDTH, START_BUTTON_HEIGHT, mouseX, mouseY)) {
            this.startButtonPressed = true;

            this.client.interactionManager.clickButton(this.handler.syncId, PotteryTableScreenHandler.START_CRAFTING_BUTTON_ID);

            MinecraftClient.getInstance().getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
            return true;
        } else {
            this.startButtonPressed = false;
        }

        if (this.canCraft) {
            int i = this.x + RECIPE_LIST_OFFSET_X;
            int j = this.y + RECIPE_LIST_OFFSET_Y;
            int k = this.scrollOffset + 12;

            for (int l = this.scrollOffset; l < k; l++) {
                int m = l - this.scrollOffset;
                double d = mouseX - (i + m % RECIPE_LIST_COLUMNS * RECIPE_ENTRY_WIDTH);
                double e = mouseY - (j + m / RECIPE_LIST_COLUMNS * RECIPE_ENTRY_HEIGHT);
                if (d >= 0.0 && e >= 0.0 && d < RECIPE_ENTRY_WIDTH && e < RECIPE_ENTRY_HEIGHT) {
                    this.client.interactionManager.clickButton(this.handler.syncId, l);
                    MinecraftClient.getInstance().getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                    return true;
                }
            }

            i = this.x + 119;
            j = this.y + 9;
            if (mouseX >= i && mouseX < i + SCROLLBAR_WIDTH && mouseY >= j && mouseY < j + SCROLLBAR_AREA_HEIGHT) {
                this.mouseClicked = true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 处理鼠标释放事件。
     *
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param button 鼠标按钮
     * @return 是否处理了释放事件
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.startButtonPressed = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * 处理鼠标拖拽事件。
     *
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param button 鼠标按钮
     * @param deltaX X轴变化量
     * @param deltaY Y轴变化量
     * @return 是否处理了拖拽事件
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.mouseClicked && this.shouldScroll()) {
            int i = this.y + 14;
            int j = i + SCROLLBAR_AREA_HEIGHT;
            this.scrollAmount = ((float)mouseY - i - 7.5F) / (j - i - 15.0F);
            this.scrollAmount = MathHelper.clamp(this.scrollAmount, 0.0F, 1.0F);
            this.scrollOffset = (int)(this.scrollAmount * this.getMaxScroll() + 0.5) * 4;
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }

    /**
     * 处理鼠标滚轮事件。
     *
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param amount 滚轮滚动量
     * @return 是否处理了滚轮事件
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (this.shouldScroll()) {
            int i = this.getMaxScroll();
            float f = (float)amount / i;
            this.scrollAmount = MathHelper.clamp(this.scrollAmount - f, 0.0F, 1.0F);
            this.scrollOffset = (int)(this.scrollAmount * i + 0.5) * 4;
        }
        return true;
    }

    /**
     * 检查是否应该显示滚动条。
     *
     * @return 如果需要滚动条返回true，否则返回false
     */
    private boolean shouldScroll() {
        return this.canCraft && this.handler.getAvailableRecipeCount() > 12;
    }

    /**
     * 获取最大滚动量。
     *
     * @return 最大滚动量
     */
    protected int getMaxScroll() {
        return (this.handler.getAvailableRecipeCount() + 4 - 1) / 4 - 3;
    }

    /**
     * 当物品栏内容变化时调用，更新制作状态。
     */
    private void onInventoryChange() {
        this.canCraft = this.handler.canCraft();
        if (!this.canCraft) {
            this.scrollAmount = 0.0F;
            this.scrollOffset = 0;
        }
    }
}