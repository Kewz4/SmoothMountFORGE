package com.enderium.smoothmount.mixin.client;

import com.enderium.smoothmount.state.CameraModifier;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraModifier {

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private Vec3 position;

    @Shadow
    protected abstract void setPosition(Vec3 vec3);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Unique
    private Vec3 lastPos = Vec3.ZERO;

    @Unique
    private Quaternionf lastRotation = new Quaternionf();

    @Unique
    private Supplier<Vec3> positionSupplier = null;

    @Unique
    private Supplier<Quaternionf> rotationSupplier = null;

    @Inject(method = "setup", at = @At("TAIL"))
    public void lerpPos(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        lastPos = position;
        lastRotation.set(rotation);
        if (positionSupplier != null) setPosition(positionSupplier.get());
        if (rotationSupplier != null) smoothMount$setRotation(rotationSupplier.get());
    }

    @Unique
    private final Vector3f eulerAngles = new Vector3f();

    @Unique
    public void smoothMount$setRotation(Quaternionf rotation) {
        rotation.getEulerAnglesYXZ(eulerAngles);
        setRotation(-(180 + eulerAngles.y * 57.29577951308232f), -eulerAngles.x * 57.29577951308232f);
    }

    @Override
    public Vec3 smoothMount$realPosition() {
        return position;
    }

    @Override
    public Quaternionf smoothMount$realRotation() {
        return rotation;
    }

    @Override
    public Supplier<Vec3> smoothMount$getSupplierPosition() {
        return positionSupplier;
    }

    @Override
    public void smoothMount$setSupplierPosition(@Nullable Supplier<@NotNull Vec3> supplier) {
        positionSupplier = supplier;
    }

    @Override
    public Supplier<Quaternionf> smoothMount$getSupplierRotation() {
        return rotationSupplier;
    }

    @Override
    public void smoothMount$setSupplierRotation(@Nullable Supplier<@NotNull Quaternionf> supplier) {
        rotationSupplier = supplier;
    }

    @Override
    public Vec3 smoothMount$lastPosition() {
        return lastPos;
    }

    @Override
    public Quaternionf smoothMount$lastRotation() {
        return lastRotation;
    }
}
