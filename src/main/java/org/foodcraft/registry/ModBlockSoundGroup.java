package org.foodcraft.registry;

import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvents;

public class ModBlockSoundGroup {
    public static final BlockSoundGroup KITCHEN_KNIFE = new BlockSoundGroup(
            1.0F,
            1.0F,
            ModSounds.KITCHEN_KNIFE_FETCH,
            SoundEvents.BLOCK_METAL_STEP,
            ModSounds.KITCHEN_KNIFE_PLACE,
            SoundEvents.BLOCK_METAL_HIT,
            SoundEvents.BLOCK_METAL_FALL
    );
}
