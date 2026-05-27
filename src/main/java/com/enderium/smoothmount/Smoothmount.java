package com.enderium.smoothmount;

import com.enderium.smoothmount.client.SmoothmountClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("smoothmount")
public class Smoothmount {

    public static final String MOD_ID = "smoothmount";

    public Smoothmount() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SmoothmountClient::init);
    }
}
