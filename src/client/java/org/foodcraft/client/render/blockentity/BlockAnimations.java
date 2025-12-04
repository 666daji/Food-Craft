package org.foodcraft.client.render.blockentity;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;

public class BlockAnimations {
    public static final Animation GRINDING_STONE_SPIN = Animation.Builder.create(10.0F).looping()
            .addBoneAnimation("top", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(10.0F, AnimationHelper.createRotationalVector(0.0F, 360.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("top", new Transformation(Transformation.Targets.TRANSLATE,
                    new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation POTTERY_TABLE_CLAY_SPIN = Animation.Builder.create(5.0F).looping()
            .addBoneAnimation("clay_ball", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(5.0F, AnimationHelper.createRotationalVector(0.0F, -360.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("clay_ball", new Transformation(Transformation.Targets.TRANSLATE,
                    new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createTranslationalVector(0.0F, 1.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(2.0F, AnimationHelper.createTranslationalVector(0.0F, -1.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(5.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("clay_ball", new Transformation(Transformation.Targets.SCALE,
                    new Keyframe(0.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(1.0F, AnimationHelper.createScalingVector(0.6F, 2.0F, 0.6F), Transformation.Interpolations.LINEAR),
                    new Keyframe(2.0F, AnimationHelper.createScalingVector(1.6F, 0.95F, 1.6F), Transformation.Interpolations.LINEAR),
                    new Keyframe(2.7083F, AnimationHelper.createScalingVector(1.7585F, 0.6618F, 1.7585F), Transformation.Interpolations.LINEAR),
                    new Keyframe(4.0F, AnimationHelper.createScalingVector(0.731F, 1.7524F, 0.731F), Transformation.Interpolations.LINEAR),
                    new Keyframe(5.0F, AnimationHelper.createScalingVector(1.0F, 1.0F, 1.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();

    public static final Animation POTTERY_TABLE_WORK_SURFACE_SPIN = Animation.Builder.create(5.0F).looping()
            .addBoneAnimation("work_surface", new Transformation(Transformation.Targets.ROTATE,
                    new Keyframe(0.0F, AnimationHelper.createRotationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR),
                    new Keyframe(5.0F, AnimationHelper.createRotationalVector(0.0F, -360.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .addBoneAnimation("work_surface", new Transformation(Transformation.Targets.TRANSLATE,
                    new Keyframe(0.0F, AnimationHelper.createTranslationalVector(0.0F, 0.0F, 0.0F), Transformation.Interpolations.LINEAR)
            ))
            .build();
}