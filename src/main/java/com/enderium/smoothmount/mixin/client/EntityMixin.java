package com.enderium.smoothmount.mixin.client;

import com.enderium.smoothmount.client.SmoothmountClient;
import com.enderium.smoothmount.client.entity.VehicleTypes;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimationState;
import com.enderium.smoothmount.client.model.animation.entity.PlayerAnimationState.MountType;
import com.enderium.smoothmount.client.util.CameraLerpUtils;
import com.enderium.smoothmount.state.CameraModifier;
import com.enderium.smoothmount.state.MountState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements MountState {

    @Shadow
    private @Nullable Entity vehicle;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract void setPos(double x, double y, double z);

    @Shadow
    public abstract Vec3 getPosition(float partialTick);

    @Unique
    private Vec3 lastMountPos = Vec3.ZERO;

    @Unique
    private float mountYaw = 0;

    @Unique
    private WeakReference<Entity> lastVehicle = new WeakReference<>(null);

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("TAIL"))
    public void startRide(Entity entity, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (!player.level().isClientSide()) return;

        lastVehicle = new WeakReference<>(entity);
        PlayerAnimationState state = (PlayerAnimationState) player;
        state.smoothMount$stopDismount();

        if (VehicleTypes.isCorrectVehicle(entity)) {
            float yR = Mth.wrapDegrees(player.getYRot() - entity.getYRot());
            if (Math.abs(yR) < 35) state.startMount(MountType.BACK, player.tickCount);
            else if (yR < 0) state.startMount(MountType.RIGHT, player.tickCount);
            else state.startMount(MountType.LEFT, player.tickCount);
        }

        Minecraft instance = Minecraft.getInstance();
        if (player != instance.player) {
            lastMountPos = new Vec3(player.xOld, player.yOld, player.zOld);
            return;
        }

        lastMountPos = SmoothmountClient.instance().lastPlayerPos();

        Camera camera = instance.gameRenderer.getMainCamera();
        CameraModifier modifier = (CameraModifier) camera;
        long time = instance.options.getCameraType().isFirstPerson() ? 300 : 450;
        modifier.smoothMount$setSupplierPosition(CameraLerpUtils.smoothToPosition(modifier, modifier.smoothMount$lastPosition(), time));
        modifier.smoothMount$setSupplierRotation(CameraLerpUtils.smoothToRotation(modifier, new Quaternionf(modifier.smoothMount$lastRotation()), time));
    }

    @Inject(method = "stopRiding", at = @At("HEAD"))
    public void stopRide(CallbackInfo ci) {
        if (!(((Object) this) instanceof Player player)) return;
        if (!player.level().isClientSide()) return;

        lastMountPos = new Vec3(player.xOld, player.yOld, player.zOld);
        PlayerAnimationState state = (PlayerAnimationState) player;
        state.smoothMount$stopMount();

        if (VehicleTypes.isCorrectVehicle(vehicle)) {
            Vec3 pos = player.position();
            Vec3 findPos = findPosition(vehicle, player);
            float angleTo = (float) Math.toDegrees(Math.atan2(pos.z - findPos.z, pos.x - findPos.x)) + 90f;
            float angleCross = Mth.wrapDegrees(player.yBodyRot - angleTo);
            if (Math.abs(angleCross) < 89) {
                state.startDismount(PlayerAnimationState.DismountType.FORWARD, player.tickCount);
            } else state.startDismount(PlayerAnimationState.DismountType.BACK, player.tickCount);
        }

        if (player != Minecraft.getInstance().player) return;
        long time = 250;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        CameraModifier modifier = (CameraModifier) camera;
        modifier.smoothMount$setSupplierPosition(CameraLerpUtils.smoothToPosition(modifier, modifier.smoothMount$lastPosition(), time));
        modifier.smoothMount$setSupplierRotation(CameraLerpUtils.smoothToRotation(modifier, new Quaternionf(modifier.smoothMount$lastRotation()), time));
    }

    @Override
    public Vec3 getPlayerPos(float delta) {
        return getPosition(delta);
    }

    @Override
    public void setPlayerPos(Vec3 vec) {
        setPos(vec.x, vec.y, vec.z);
    }

    @Override
    public Vec3 getLastPosition() {
        return lastMountPos;
    }

    @Override
    public void setLastPosition(Vec3 vec) {
        lastMountPos = vec;
    }

    @Override
    public float getMountYaw() {
        return mountYaw;
    }

    @Override
    public void setMountYaw(float yaw) {
        this.mountYaw = yaw;
    }

    @Override
    public Optional<Entity> getLastVehicle() {
        return Optional.ofNullable(lastVehicle.get());
    }

    @Override
    public void setLastVehicle(Entity vehicle) {
        lastVehicle = new WeakReference<>(vehicle);
    }

    @Unique
    private static Vec3 findPosition(Entity entity, LivingEntity self) {
        if (self.isRemoved() || entity.isRemoved()
                || self.level().getBlockState(entity.blockPosition()).is(BlockTags.PORTALS)) {
            double d = Math.max(self.getY(), entity.getY());
            return new Vec3(self.getX(), d, self.getZ());
        }
        return entity.getDismountLocationForPassenger(self);
    }
}
