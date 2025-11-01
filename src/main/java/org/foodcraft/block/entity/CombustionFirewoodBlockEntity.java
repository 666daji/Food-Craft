package org.foodcraft.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.foodcraft.block.CombustionFirewoodBlock;
import org.foodcraft.block.FirewoodBlock;
import org.foodcraft.registry.ModBlockEntityTypes;

public class CombustionFirewoodBlockEntity extends BlockEntity {
    protected int energy;
    protected int cycleCount; // 循环次数
    protected boolean isFirstCycle = true; // 是否是首次循环
    /** 热量等级，0-无热量，1-低热量，2-高热量 */
    protected int heatLevel = 0;

    static final int MAX_ENERGY = 12000;
    static final int HALF_ENERGY = MAX_ENERGY / 2; // 50%能量阈值
    static final int FIREWOOD_ENERGY = HALF_ENERGY; // 每次添柴增加50%能量

    public CombustionFirewoodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.COMBUSTION_FIREWOOD, pos, state);
        // 首次点燃时设置满能量
        if (state.get(CombustionFirewoodBlock.COMBUSTION_STATE) == CombustionFirewoodBlock.CombustionState.FIRST_IGNITED) {
            this.energy = MAX_ENERGY;
            this.isFirstCycle = true;
            this.cycleCount = 0;
        }
        updateHeatLevel(); // 初始化热量等级
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Energy", energy);
        nbt.putInt("CycleCount", cycleCount);
        nbt.putBoolean("IsFirstCycle", isFirstCycle);
        nbt.putInt("HeatLevel", heatLevel); // 保存热量等级
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energy = nbt.getInt("Energy");
        cycleCount = nbt.getInt("CycleCount");
        isFirstCycle = nbt.getBoolean("IsFirstCycle");
        heatLevel = nbt.getInt("HeatLevel"); // 读取热量等级
        // 如果NBT中没有热量等级，根据能量计算
        if (!nbt.contains("HeatLevel")) {
            updateHeatLevel();
        }
    }

    /**
     * 更新热量等级
     */
    public void updateHeatLevel() {
        int oldHeatLevel = heatLevel;

        if (energy <= 0) {
            heatLevel = 0; // 未燃烧或能量耗尽，无热量
        } else if (energy > HALF_ENERGY) {
            heatLevel = 2; // 能量大于50%，高热量
        } else {
            heatLevel = 1; // 能量小于等于50%，低热量
        }

        // 如果热量等级改变，标记需要同步
        if (oldHeatLevel != heatLevel) {
            markDirty();
        }
    }

    /**
     * 获取热量等级
     */
    public int getHeatLevel() {
        return heatLevel;
    }

    /**
     * 检查是否有热量
     */
    public boolean hasHeat() {
        return heatLevel > 0;
    }

    /**
     * 更新燃烧状态
     */
    public void updateCombustionState() {
        if (world == null || world.isClient()) return;

        CombustionFirewoodBlock.CombustionState currentState = getCachedState().get(CombustionFirewoodBlock.COMBUSTION_STATE);
        CombustionFirewoodBlock.CombustionState newState = currentState;

        // 根据当前状态和能量值决定下一个状态
        if (energy > 0) {
            // 燃烧状态转换
            switch (currentState) {
                case FIRST_IGNITED:
                    if (energy <= HALF_ENERGY) {
                        newState = CombustionFirewoodBlock.CombustionState.FIRST_HALF;
                    }
                    break;

                case FIRST_HALF:
                    // 保持在FIRST_HALF直到能量为0
                    break;

                case AGAIN_IGNITED:
                    if (energy <= HALF_ENERGY) {
                        newState = CombustionFirewoodBlock.CombustionState.AGAIN_HALF;
                    }
                    break;

                case AGAIN_HALF:
                    // 保持在AGAIN_HALF直到能量为0
                    break;

                case REIGNITED:
                    if (energy <= HALF_ENERGY) {
                        newState = CombustionFirewoodBlock.CombustionState.FIRST_HALF;
                        isFirstCycle = false; // 首次循环结束
                        cycleCount++;
                    }
                    break;

                default:
                    break;
            }
        } else {
            // 能量为0时的燃尽状态
            switch (currentState) {
                case FIRST_HALF:
                    newState = CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED;
                    break;

                case AGAIN_HALF:
                    newState = CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED;
                    break;

                default:
                    break;
            }
        }

        // 只有当状态确实改变时才更新，避免不必要的方块更新
        if (currentState != newState) {
            world.setBlockState(pos, getCachedState().with(CombustionFirewoodBlock.COMBUSTION_STATE, newState));
        }

        // 更新热量等级
        updateHeatLevel();

        markDirty();
    }

    /**
     * 强制熄灭当前燃烧的柴火堆
     * 根据当前方块状态决定熄灭后的状态
     * @return 是否成功熄灭（如果已经熄灭则返回false）
     */
    public boolean extinguish() {
        if (world == null || world.isClient()) {
            return false;
        }

        BlockState currentState = getCachedState();
        CombustionFirewoodBlock.CombustionState currentCombustionState =
                currentState.get(CombustionFirewoodBlock.COMBUSTION_STATE);

        // 如果已经处于熄灭状态，不需要再次熄灭
        if (!currentCombustionState.isBurning()) {
            return false;
        }

        CombustionFirewoodBlock.CombustionState extinguishedState = switch (currentCombustionState) {
            case FIRST_IGNITED, FIRST_HALF -> CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED;
            case AGAIN_IGNITED, AGAIN_HALF, REIGNITED -> CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED;
            default ->
                // 默认情况下，如果是首次燃烧则熄灭为首次燃尽，否则为非首次燃尽
                    isFirstCycle ?
                            CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED :
                            CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED;
        };

        this.energy = 0;
        world.setBlockState(pos, currentState.with(CombustionFirewoodBlock.COMBUSTION_STATE, extinguishedState));
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5f, 1.0f);
        spawnExtinguishParticles(world, pos);

        markDirty();
        return true;
    }

    /**
     * 生成熄灭粒子效果
     */
    private void spawnExtinguishParticles(World world, BlockPos pos) {
        if (world.isClient()) {
            // 客户端生成烟雾粒子
            Random random = world.random;
            for (int i = 0; i < 5; i++) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
                double y = pos.getY() + 0.3 + random.nextDouble() * 0.2;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

                world.addParticle(ParticleTypes.SMOKE,
                        x, y, z,
                        (random.nextDouble() - 0.5) * 0.05,
                        0.05,
                        (random.nextDouble() - 0.5) * 0.05);
            }
        }
    }

    public static void tick(World world, BlockPos pos, BlockState state, CombustionFirewoodBlockEntity blockEntity) {
        // 每tick消耗1点能量
        blockEntity.consumeEnergy();
        blockEntity.checkClearSpaceAbove(world, pos, state);

        // 如果能量耗尽且处于燃烧状态，更新到燃尽状态
        if (blockEntity.energy <= 0 && state.get(CombustionFirewoodBlock.COMBUSTION_STATE).isBurning()) {
            blockEntity.updateCombustionState();
        }
    }

    /**
     * 检查上方空间，如果不满足条件则强制熄灭
     */
    private void checkClearSpaceAbove(World world, BlockPos pos, BlockState state) {
        if (world.isClient()) return;

        // 只有在燃烧状态下才需要检查
        CombustionFirewoodBlock.CombustionState currentState = state.get(CombustionFirewoodBlock.COMBUSTION_STATE);
        if (!currentState.isBurning()) return;

        // 检查上方空间
        if (!FirewoodBlock.hasClearSpaceAbove(world, pos)) {
            // 上方空间被阻塞，强制熄灭
            extinguishDueToObstruction(world, pos);
        }
    }

    /**
     * 由于上方阻塞而强制熄灭
     */
    private void extinguishDueToObstruction(World world, BlockPos pos) {
        BlockState currentState = getCachedState();
        CombustionFirewoodBlock.CombustionState currentCombustionState =
                currentState.get(CombustionFirewoodBlock.COMBUSTION_STATE);

        CombustionFirewoodBlock.CombustionState extinguishedState = switch (currentCombustionState) {
            case FIRST_IGNITED, FIRST_HALF -> CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED;
            case AGAIN_IGNITED, AGAIN_HALF, REIGNITED -> CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED;
            default -> isFirstCycle ?
                    CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED :
                    CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED;
        };
        // 设置能量为0
        this.energy = 0;

        // 更新方块状态
        world.setBlockState(pos, currentState.with(CombustionFirewoodBlock.COMBUSTION_STATE, extinguishedState));

        // 播放特殊的阻塞熄灭音效
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.7f, 0.8f);

        // 生成更多的烟雾粒子，表示因阻塞而熄灭
        spawnObstructionExtinguishParticles(world, pos);

        markDirty();
    }

    /**
     * 生成因阻塞而熄灭的粒子效果
     */
    private void spawnObstructionExtinguishParticles(World world, BlockPos pos) {
        if (world.isClient()) {
            Random random = world.random;
            // 生成更多的烟雾粒子
            for (int i = 0; i < 10; i++) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
                double y = pos.getY() + 0.5 + random.nextDouble() * 0.5;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

                world.addParticle(ParticleTypes.SMOKE,
                        x, y, z,
                        (random.nextDouble() - 0.5) * 0.1,
                        0.05 + random.nextDouble() * 0.1,
                        (random.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    /**
     * 添柴操作 - 任何能量不满的状态都可以添柴
     * @return 是否成功添柴
     */
    public boolean addFirewood() {
        if (energy >= MAX_ENERGY || isCompletelyExtinguished()) {
            return false; // 能量已满或完全燃尽，无法添柴
        }

        CombustionFirewoodBlock.CombustionState currentState = getCachedState().get(CombustionFirewoodBlock.COMBUSTION_STATE);
        CombustionFirewoodBlock.CombustionState newState;

        // 根据当前状态决定添柴后的状态
        switch (currentState) {
            case FIRST_IGNITED:
            case FIRST_HALF:
            case FIRST_EXTINGUISHED, REIGNITED, AGAIN_EXTINGUISHED:
                newState = CombustionFirewoodBlock.CombustionState.AGAIN_IGNITED;
                break;

            case AGAIN_IGNITED:
            case AGAIN_HALF:
                newState = CombustionFirewoodBlock.CombustionState.REIGNITED;
                break;

            default:
                return false;
        }

        // 增加50%能量
        addEnergy(FIREWOOD_ENERGY);

        // 更新方块状态
        if (world != null) {
            world.setBlockState(pos, getCachedState().with(CombustionFirewoodBlock.COMBUSTION_STATE, newState));
        }
        markDirty();
        return true;
    }

    /**
     * 检查柴火堆上方6格内是否为空气
     * @param world 世界
     * @param pos 柴火堆位置
     * @return 上方6格内是否全部为空气
     */
    public static boolean hasClearSpaceAbove(World world, BlockPos pos) {
        for (int i = 1; i <= 6; i++) {
            BlockPos checkPos = pos.up(i);
            BlockState state = world.getBlockState(checkPos);

            if (!state.isAir()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否可以点燃（包括上方空间检查）
     */
    public boolean canIgnite(World world, BlockPos pos) {
        return hasClearSpaceAbove(world, pos);
    }


    /**
     * 检查是否可以添柴
     */
    public boolean canAddFirewood() {
        return energy < MAX_ENERGY && !isCompletelyExtinguished();
    }

    /**
     * 检查是否正在燃烧
     */
    public boolean isCombusting() {
        return this.heatLevel > 0;
    }

    public void addEnergy(int energy) {
        this.energy = Math.min(this.energy + energy, MAX_ENERGY);
        updateHeatLevel();
        updateCombustionState();
    }

    public boolean consumeEnergy() {
        return consumeEnergy(1);
    }

    /**
     * 消耗指定数量的能量
     * @param amount 消耗的能量数量
     * @return 是否成功消耗了能量
     */
    public boolean consumeEnergy(int amount) {
        if (energy <= 0 || amount <= 0) {
            return false;
        }

        int oldEnergy = this.energy;
        this.energy = Math.max(0, this.energy - amount);

        // 只有当能量确实改变时才更新状态
        if (oldEnergy != this.energy) {
            updateHeatLevel();
            updateCombustionState();
            return true;
        }

        return false;
    }

    public void setEnergy(int energy) {
        this.energy = Math.min(energy, MAX_ENERGY);
        updateHeatLevel();
        updateCombustionState();
    }


    /**
     * 检查能量是否已经耗尽
     * @return 能量是否<=0
     */
    public boolean isEnergyDepleted() {
        return energy <= 0;
    }

    /**
     * 获取当前能量值
     * @return 当前能量值
     */
    public int getCurrentEnergy() {
        return energy;
    }

    /**
     * 获取能量消耗进度（0.0到1.0）
     * @return 能量消耗进度，0表示满能量，1表示能量耗尽
     */
    public float getEnergyConsumptionProgress() {
        return 1.0f - ((float) energy / MAX_ENERGY);
    }

    public int getEnergy() {
        return energy;
    }

    public static int getMaxEnergy() {
        return MAX_ENERGY;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(int count) {
        this.cycleCount = count;
        markDirty();
    }

    public boolean isFirstCycle() {
        return isFirstCycle;
    }

    public void setFirstCycle(boolean firstCycle) {
        this.isFirstCycle = firstCycle;
        markDirty();
    }

    /**
     * 获取当前能量百分比
     */
    public float getEnergyRatio() {
        return (float) energy / MAX_ENERGY;
    }

    /**
     * 检查是否完全燃尽（不能再燃烧）
     */
    public boolean isCompletelyExtinguished() {
        CombustionFirewoodBlock.CombustionState currentState = getCachedState().get(CombustionFirewoodBlock.COMBUSTION_STATE);
        return (currentState == CombustionFirewoodBlock.CombustionState.FIRST_EXTINGUISHED ||
                currentState == CombustionFirewoodBlock.CombustionState.AGAIN_EXTINGUISHED) &&
                energy <= 0;
    }

    /**
     * 获取半能量值（50%）
     */
    public int getHalfEnergy() {
        return HALF_ENERGY;
    }
}