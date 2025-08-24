/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2019
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.carts;

import mods.railcraft.client.render.carts.LocomotiveRenderType;
import mods.railcraft.common.blocks.RailcraftBlocks;
import mods.railcraft.common.items.ItemGear;
import mods.railcraft.common.items.Metal;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.plugins.color.EnumColor;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import net.minecraft.init.Blocks;


public class ItemLocoDiesel extends ItemLocomotive {
    public ItemLocoDiesel(IRailcraftCartContainer cart) {
        super(cart, LocomotiveRenderType.DIESEL, EnumColor.RED, EnumColor.BLACK);
    }

    @Override
    public void defineRecipes() {
        super.defineRecipes();
        CraftingPlugin.addShapedRecipe(getStack(),
                "LTT",
                "PFP",
                "GMG",
                'L', Blocks.REDSTONE_LAMP,
                'P', Blocks.PISTON,
                'F', RailcraftBlocks.BOILER_FIREBOX_FLUID,
                'M', RailcraftCarts.TANK,
                'G', RailcraftItems.GEAR, ItemGear.EnumGear.STEEL,
                'T', RailcraftItems.PLATE, Metal.STEEL);
    }
}
