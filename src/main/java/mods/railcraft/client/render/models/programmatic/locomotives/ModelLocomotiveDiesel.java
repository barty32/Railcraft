/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2019
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.client.render.models.programmatic.locomotives;

import mods.railcraft.client.render.models.programmatic.ModelSimple;
import net.minecraft.client.model.ModelRenderer;

public class ModelLocomotiveDiesel extends ModelSimple {

    public ModelLocomotiveDiesel() {
        this(0f);
    }

    public ModelLocomotiveDiesel(float scale) {
        super("loco");

        renderer.setTextureSize(128, 64);

        ModelRenderer loco = renderer;
        loco.setTextureOffset(1, 25).addBox(-20.0F, -5.0F, -16.0F, 23, 2, 16, scale);
        loco.setTextureOffset(1, 1).addBox(-21.0F, -8.0F, -17.0F, 25, 3, 18, scale);
        loco.setTextureOffset(74, 9).addBox(-6.0F, -21.0F, -16.0F, 6, 13, 16, scale);
        loco.setTextureOffset(32, 46).addBox(-20.0F, -19.0F, -11.0F, 14, 11, 6, scale);
        loco.setTextureOffset(1, 55).addBox(-21.0F, -20.0F, -10.0F, 6, 4, 4, scale);
        loco.setTextureOffset(73, 41).addBox(-19.0F, -18.0F, -14.0F, 13, 10, 12, scale);
        loco.setTextureOffset(107, 3).addBox(0.0F, -19.0F, -11.0F, 3, 11, 6, scale);
        loco.setTextureOffset(23, 50).addBox(0.0F, -21.0F, -14.0F, 2, 11, 2, scale);
        loco.setTextureOffset(25, 46).addBox(0.5F, -23.0F, -13.5F, 1, 2, 1, scale);
        loco.setTextureOffset(18, 47).addBox(-0.5F, -10.0F, -13.5F, 2, 1, 1, scale);
        loco.setTextureOffset(18, 47).addBox(-0.5F, -10.0F, -3.5F, 2, 1, 1, scale);
        loco.setTextureOffset(23, 50).addBox(0.0F, -21.0F, -4.0F, 2, 11, 2, scale);
        loco.setTextureOffset(25, 46).addBox(0.5F, -23.0F, -3.5F, 1, 2, 1, scale);

        renderer.rotationPointX = 8F;
        renderer.rotationPointY = 8F;
        renderer.rotationPointZ = 8F;
    }

}
