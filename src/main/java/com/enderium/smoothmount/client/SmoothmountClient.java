package com.enderium.smoothmount.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SmoothmountClient {
    private static SmoothmountClient instance;

    public static void init() {
        new SmoothmountClient();
    }

    private SmoothmountClient() {
        instance = this;
        MinecraftForge.EVENT_BUS.register(this);
    }

    private Vec3 lastPlayerPos;
    private Vec3 lastPlayerPosStart;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (event.phase == TickEvent.Phase.START) {
            lastPlayerPosStart = mc.player.position();
        } else {
            lastPlayerPos = mc.player.oldPosition();
        }
    }

    public Vec3 lastPlayerPos() {
        return lastPlayerPos;
    }

    public Vec3 lastPlayerPosStart() {
        return lastPlayerPosStart;
    }

    public static SmoothmountClient instance() {
        return instance;
    }
}
