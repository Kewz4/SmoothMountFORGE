package com.enderium.smoothmount.client.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public class VehicleTypes {

    public static boolean isCorrectVehicle(Entity entity) {
        if (entity instanceof Camel) return false;
        if (entity instanceof AbstractBoat) return false;
        if (entity instanceof AbstractMinecart) return false;
        if (entity instanceof AbstractHorse) return true;
        return entity instanceof Pig;
    }
}
