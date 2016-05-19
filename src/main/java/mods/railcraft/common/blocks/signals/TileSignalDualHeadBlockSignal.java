/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.blocks.signals;

import mods.railcraft.api.signals.IReceiverTile;
import mods.railcraft.api.signals.SignalAspect;
import mods.railcraft.api.signals.SignalController;
import mods.railcraft.api.signals.SimpleSignalReceiver;
import mods.railcraft.common.util.misc.AABBFactory;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TileSignalDualHeadBlockSignal extends TileSignalBlockSignal implements IReceiverTile, IDualHeadSignal {

    private final SimpleSignalReceiver receiver = new SimpleSignalReceiver(getLocalizationTag(), this);

    @Override
    public EnumSignal getSignalType() {
        return EnumSignal.DUAL_HEAD_BLOCK_SIGNAL;
    }

    public int getLightValue() {
        return Math.max(getSignalAspect().getLightValue(), getBottomAspect().getLightValue());
    }

    @Override
    public void update() {
        super.update();
        if (Game.isNotHost(worldObj)) {
            receiver.tickClient();
            return;
        }
        receiver.tickServer();
        SignalAspect prevAspect = receiver.getAspect();
        if (receiver.isBeingPaired()) {
            receiver.setAspect(SignalAspect.BLINK_YELLOW);
        } else if (!receiver.isPaired()) {
            receiver.setAspect(SignalAspect.BLINK_RED);
        }
        if (prevAspect != receiver.getAspect()) {
            sendUpdateToClient();
        }
    }

    @Override
    public void onControllerAspectChange(SignalController con, SignalAspect aspect) {
        sendUpdateToClient();
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
        getBlockType().setBlockBounds(BOUNDS, 0, BOUNDS, 1 - BOUNDS, 1f, 1 - BOUNDS);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World world, BlockPos pos) {
        return AABBFactory.start().createBoxForTileAt(pos).expandHorizontally(-BOUNDS).build();
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        receiver.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        receiver.readFromNBT(data);
    }

    @Override
    public void writePacketData(DataOutputStream data) throws IOException {
        super.writePacketData(data);

        receiver.writePacketData(data);
    }

    @Override
    public void readPacketData(DataInputStream data) throws IOException {
        super.readPacketData(data);

        receiver.readPacketData(data);
    }

    @Override
    public SimpleSignalReceiver getReceiver() {
        return receiver;
    }

    @Override
    public SignalAspect getTopAspect() {
        return getSignalAspect();
    }

    @Override
    public SignalAspect getBottomAspect() {
        return receiver.getAspect();
    }
}
