package com.enderium.smoothmount.mixin.client;

import com.enderium.smoothmount.client.animation.TransformChannel;
import com.enderium.smoothmount.client.animation.definition.EntityAnimation;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimation;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimationState;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimationState.DismountType;
import com.enderium.smoothmount.state.MountState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {

    @Unique private EntityAnimation mountBackAnimation;
    @Unique private EntityAnimation mountRightAnimation;
    @Unique private EntityAnimation mountLeftAnimation;
    @Unique private EntityAnimation dismountBackAnimation;
    @Unique private EntityAnimation dismountForwardAnimation;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        mountBackAnimation    = PlayerAnimation.Mount.MOUNT_BACK_E.bake();
        mountRightAnimation   = PlayerAnimation.Mount.MOUNT_RIGHT_E.bake();
        mountLeftAnimation    = PlayerAnimation.Mount.MOUNT_LEFT_E.bake();
        dismountBackAnimation    = PlayerAnimation.Dismount.DISMOUNT_BACK_E.bake();
        dismountForwardAnimation = PlayerAnimation.Dismount.DISMOUNT_FORWARD_E.bake();
    }

    /**
     * Applies the entity-offset animation (o_transform) as a PoseStack translation,
     * and updates mountYaw from the live vehicle rotation.
     */
    @Inject(method = "setupRotations", at = @At("HEAD"))
    public void applyOffset(AbstractClientPlayer entity, PoseStack poseStack,
                            float ageInTicks, float rotationYaw, float partialTick, CallbackInfo ci) {
        PlayerAnimationState pState = (PlayerAnimationState) entity;
        MountState mState = (MountState) entity;

        pState.smoothMount$checkAndStop(ageInTicks);

        // Keep mountYaw in sync with the vehicle's live rotation
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            mState.setMountYaw(vehicle instanceof net.minecraft.world.entity.LivingEntity le
                    ? le.yBodyRot : vehicle.getYRot());
        }

        Vector3f rotateOffset = new Vector3f();

        if (pState.mountState().isEnabled()) {
            EntityAnimation animation;
            switch (pState.mountState()) {
                case BACK  -> pState.mountAnimation().update(3, animation = mountBackAnimation,  ageInTicks);
                case RIGHT -> pState.mountAnimation().update(4, animation = mountRightAnimation, ageInTicks);
                case LEFT  -> pState.mountAnimation().update(5, animation = mountLeftAnimation,  ageInTicks);
                default    -> throw new IllegalStateException("Unexpected: " + pState.mountState());
            }
            TransformChannel channel = animation.offsetTransform();
            channel.position().currentValue.div(16, rotateOffset)
                    .rotateY(org.joml.Math.toRadians(mState.getMountYaw()));
        }

        DismountType dismountType = pState.dismountState();
        if (dismountType.isEnabled()) {
            EntityAnimation animation;
            switch (dismountType) {
                case BACK    -> pState.dismountAnimation().update(2, animation = dismountBackAnimation,    ageInTicks, 1.2f);
                case FORWARD -> pState.dismountAnimation().update(3, animation = dismountForwardAnimation, ageInTicks, 1.2f);
                default      -> throw new IllegalStateException("Unexpected: " + dismountType);
            }

            Optional<Entity> lastVehicle = mState.getLastVehicle();
            if (lastVehicle.isPresent()) {
                Entity mount = lastVehicle.get();
                Vec3 mountPos = mount.getPosition(partialTick);
                Vec3 entityPos = entity.getPosition(partialTick);

                // Compute yaw from entity position relative to mount
                float computedYaw = (float) Math.toDegrees(
                        Math.atan2(entityPos.z - mountPos.z, entityPos.x - mountPos.x)) + 90f + dismountType.yawOffset;
                mState.setMountYaw(computedYaw);

                TransformChannel channel = animation.offsetTransform();
                channel.position().currentValue.div(16, rotateOffset);

                float x = (float) (entityPos.x - mountPos.x);
                float z = (float) (entityPos.z - mountPos.z);
                float off = org.joml.Math.sqrt(x * x + z * z);
                if (off > 0) rotateOffset.z *= off;

                TransformChannel mountMotionChannel = animation.get("mount_motion");
                float y = mountMotionChannel != null ? mountMotionChannel.getPosition().y : 0f;
                rotateOffset.y += y * 0.01f;

                rotateOffset.rotateY(org.joml.Math.toRadians(mState.getMountYaw()));

                float rOffset = Mth.wrapDegrees(entity.yBodyRot - mState.getMountYaw());
                mState.setMountYaw(mState.getMountYaw() + rOffset * -y);
            }
        }

        if (rotateOffset.x != 0 || rotateOffset.y != 0 || rotateOffset.z != 0) {
            poseStack.translate(rotateOffset.x, rotateOffset.y, -rotateOffset.z);
        }
    }

    /**
     * Overrides body yaw to mountYaw while a mount/dismount animation is active.
     * rotationYaw is the 2nd float arg (ordinal 1 among floats) in setupRotations.
     */
    @ModifyVariable(method = "setupRotations", ordinal = 1, at = @At("HEAD"), argsOnly = true)
    public float modifyRotationYaw(float value, AbstractClientPlayer entity) {
        PlayerAnimationState pState = (PlayerAnimationState) entity;
        MountState mState = (MountState) entity;
        if (entity.isPassenger() && !pState.smoothMount$IsStopped()) return mState.getMountYaw();
        if (pState.dismountState().isEnabled()) return mState.getMountYaw();
        return value;
    }
}
