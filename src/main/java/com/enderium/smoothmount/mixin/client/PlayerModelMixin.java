package com.enderium.smoothmount.mixin.client;

import com.enderium.smoothmount.client.animation.definition.ModelAnimation;
import com.enderium.smoothmount.client.model.ModelPartChildren;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimation;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimationState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PlayerModel.class, priority = 5000)
public class PlayerModelMixin extends HumanoidModel<AbstractClientPlayer> {

    @Unique
    private ModelAnimation mountBackAnimation;
    @Unique
    private ModelAnimation mountRightAnimation;
    @Unique
    private ModelAnimation mountLeftAnimation;
    @Unique
    private ModelAnimation dismountBackAnimation;
    @Unique
    private ModelAnimation dismountForwardAnimation;

    @Unique
    private final List<ModelPart> bodyParts = List.of(leftLeg, rightLeg, leftArm, rightArm, head);
    @Unique
    private final List<ModelPart> bodyPartsResetMount = List.of(leftLeg, rightLeg, leftArm, rightArm);

    public PlayerModelMixin(ModelPart modelPart) {
        super(modelPart);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    public void init(ModelPart modelPart, boolean slim, CallbackInfo ci) {
        mountBackAnimation = PlayerAnimation.Mount.MOUNT_BACK_M.bake(modelPart);
        mountRightAnimation = PlayerAnimation.Mount.MOUNT_RIGHT_M.bake(modelPart);
        mountLeftAnimation = PlayerAnimation.Mount.MOUNT_LEFT_M.bake(modelPart);
        dismountBackAnimation = PlayerAnimation.Dismount.DISMOUNT_BACK_M.bake(modelPart);
        dismountForwardAnimation = PlayerAnimation.Dismount.DISMOUNT_FORWARD_M.bake(modelPart);
    }

    @Inject(
            method = "setupAnim",
            at = @At("TAIL")
    )
    public void setAnim(LivingEntity entity, float limbSwing, float limbSwingAmount,
                        float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer player)) return;
        PlayerAnimationState pState = (PlayerAnimationState) player;

        boolean stopped = pState.smoothMount$IsStopped();
        if (stopped) return;

        if (pState.dismountAnimation().isStarted()) {
            ((ModelPartChildren)(Object)leftLeg).resetPoseAndChildren();
            ((ModelPartChildren)(Object)rightLeg).resetPoseAndChildren();
            ((ModelPartChildren)(Object)leftArm).resetPoseOnlyChildren();
            ((ModelPartChildren)(Object)rightArm).resetPoseOnlyChildren();
        } else {
            for (ModelPart part : bodyPartsResetMount) {
                ((ModelPartChildren)(Object)part).resetPoseAndChildren();
            }
        }

        if (pState.mountState().isEnabled()) {
            switch (pState.mountState()) {
                case BACK  -> pState.mountAnimation().update(0, mountBackAnimation,    ageInTicks);
                case LEFT  -> pState.mountAnimation().update(2, mountLeftAnimation,    ageInTicks);
                case RIGHT -> pState.mountAnimation().update(1, mountRightAnimation,   ageInTicks);
                default    -> throw new IllegalStateException("Unexpected: " + pState.mountState());
            }
        }

        if (pState.dismountState().isEnabled()) {
            switch (pState.dismountState()) {
                case BACK    -> pState.dismountAnimation().update(0, dismountBackAnimation,    ageInTicks, 1.2f);
                case FORWARD -> pState.dismountAnimation().update(1, dismountForwardAnimation, ageInTicks, 1.2f);
                default      -> throw new IllegalStateException("Unexpected: " + pState.dismountState());
            }
        }

        Vector3f offset    = new Vector3f(body.x,    body.y,    body.z);
        Vector3f offsetRot = new Vector3f(body.xRot, body.yRot, body.zRot);
        for (ModelPart part : bodyParts) {
            part.offsetPos(offset);
            part.offsetRotation(offsetRot);
        }
    }
}
