/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2022
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.carts;

import mods.railcraft.api.carts.CartToolsAPI;
import mods.railcraft.api.carts.IFluidCart;
import mods.railcraft.client.render.carts.LocomotiveRenderType;
import mods.railcraft.client.util.effects.ClientEffects;
import mods.railcraft.common.blocks.logic.*;
import mods.railcraft.common.blocks.logic.Logic.Adapter;
import mods.railcraft.common.fluids.FluidItemHelper;
import mods.railcraft.common.fluids.FluidTools;
import mods.railcraft.common.fluids.FluidTools.ProcessState;
import mods.railcraft.common.fluids.FluidTools.ProcessType;
import mods.railcraft.common.fluids.TankManager;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.items.ItemTicket;
import mods.railcraft.common.plugins.buildcraft.triggers.ITemperature;
import mods.railcraft.common.plugins.forge.DataManagerPlugin;
import mods.railcraft.common.plugins.forge.NBTPlugin;
import mods.railcraft.common.util.inventory.InvTools;
import mods.railcraft.common.util.inventory.wrappers.InventoryMapper;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.sounds.RailcraftSoundEvents;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

import java.util.EnumSet;
import java.util.Optional;

public class EntityLocomotiveDiesel extends EntityLocomotive implements IFluidCart, ISidedInventory, ITemperature {

    public static final int SLOT_DIESEL_INPUT = 0;
    public static final int SLOT_DIESEL_PROCESSING = 1;
    public static final int SLOT_DIESEL_OUTPUT = 2;
    private static final int SLOT_TICKET = 3;
    private static final int[] SLOTS = InvTools.buildSlotArray(0, 3);
    private final IInventory invTicket = new InventoryMapper(this, SLOT_TICKET, 2).ignoreItemChecks();
    private static final DataParameter<Boolean> SMOKE = DataManagerPlugin.create(DataSerializers.BOOLEAN);
    private ProcessState processState = ProcessState.RESET;
    public final DieselMotorLogic engine;


    @SuppressWarnings("unused")
    public EntityLocomotiveDiesel(World world) {
        super(world);
    }

    @SuppressWarnings("unused")
    public EntityLocomotiveDiesel(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    {
        setAllowedModes(EnumSet.of(LocoMode.RUNNING, LocoMode.IDLE, LocoMode.SHUTDOWN));

        Adapter adapter = Adapter.of(this);

        engine = new DieselMotorLogic(adapter);
        logic = engine;

        //engine.addLogic(new BucketProcessorLogic(adapter, SLOT_DIESEL_INPUT, ProcessType.DRAIN_ONLY));
    }

    @Override
    public IRailcraftCartContainer getCartType() {
        return RailcraftCarts.LOCO_DIESEL;
    }

    @Override
    public LocomotiveRenderType getRenderType() {
        return LocomotiveRenderType.DIESEL;
    }

    @Override
    public float getOptimalDistance(EntityMinecart cart) {
        return 0.92f;
    }

    @Override
    protected IInventory getTicketInventory() {
        return invTicket;
    }

    @Override
    public int getSizeInventory() {
        return 5;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
        return slot < SLOT_TICKET;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        switch (slot) {
            case SLOT_DIESEL_INPUT:
                FluidStack fluidStack = FluidItemHelper.getFluidStackInContainer(stack);
                if (FluidItemHelper.isEmptyContainer(stack) ||
                    fluidStack != null && engine.canAcceptFluid(fluidStack.getFluid())) {
                    return true;
                }
                return false;
            case SLOT_TICKET:
                return ItemTicket.FILTER.test(stack);
            default:
                return false;
        }
    }

    @Override
    protected Optional<EnumGui> getGuiType() {
        return EnumGui.LOCO_DIESEL.op();
    }

    @Override
    protected void entityInit() {
        super.entityInit();

        dataManager.register(SMOKE, false);
    }

    @Override
    public SoundEvent getWhistle() {
        return RailcraftSoundEvents.ENTITY_LOCOMOTIVE_ELECTRIC_WHISTLE.getSoundEvent();
    }

    @Override
    protected ItemStack getCartItemBase() {
        return RailcraftCarts.LOCO_DIESEL.getStack();
    }

    public TankManager getTankManager() {
        return engine.getLogic(FluidLogic.class).map(FluidLogic::getTankManager).orElse(TankManager.NIL);
    }

    @Override
    public void setMode(LocoMode mode) {
        engine.setRunning(mode != LocoMode.SHUTDOWN);
        super.setMode(mode);
    }

    @Override
    public void setSpeed(LocoSpeed speed) {
        //TODO: Change motor sound
        super.setSpeed(speed);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (Game.isHost(world)) {
            if(!engine.isRunning() && getMode() != LocoMode.SHUTDOWN){
                setMode(LocoMode.SHUTDOWN);
            }

            setSmoking(engine.isRunning());

            processState = FluidTools.processContainer(this, getTankManager(), ProcessType.DRAIN_ONLY, processState);

            if (engine.getDieselTank().getFluidAmount() < engine.getDieselTank().getCapacity() / 2) {
                FluidStack fuelType = engine.getDieselTank().getFluid();
                if (fuelType != null) {
                    FluidStack pulled = CartToolsAPI.transferHelper().pullFluid(this, new FluidStack(fuelType.getFluid(), 1000));
                    if (pulled != null) {
                        engine.getDieselTank().fill(pulled, true);
                    }
                }
            }
        } else {
            if (isSmoking()) {
                double rads = renderYaw * Math.PI / 180;
                double sin = Math.sin(rads);
                double cos = Math.cos(rads);
                float offsetX = 0.55f;
                float offsetY = 1.35f;
                float offsetZ = 0.30f;

                //TODO: get pitch and fix rendering on slopes
                //the pitch cannot be obtained here, it would have to be added to IDirectionalCart
                ClientEffects.INSTANCE.dieselSmokeEffect(
                    world,
                    posX + cos * offsetX + sin * offsetZ,
                    posY + offsetY,
                    posZ + sin * offsetX - cos * offsetZ //aaaah!! this was so painful to determine correctly!
                ); //at least it looks really cool...

                ClientEffects.INSTANCE.dieselSmokeEffect(
                    world,
                    posX + cos * offsetX - sin * offsetZ,
                    posY + offsetY,
                    posZ + sin * offsetX + cos * offsetZ
                );
            }
        }
    }

    // @SuppressWarnings("WeakerAccess")
    public boolean isSmoking() {
        return dataManager.get(SMOKE);
    }

    private void setSmoking(boolean smoke) {
        dataManager.set(SMOKE, smoke);
    }

    @Override
    public double getTemp() {
        return engine.getTemp();
    }

    @Override
    protected int getIdleFuelUse() {
        // Idle fuel usage is implemented directly in DieselMotorLogic
        return 0;
    }

    @Override
    public int getMoreGoJuice() {
        return engine.burnFuel(1);
    }

    @Override
    public double getHorsepower() {
        return super.getHorsepower() * engine.getTemperaturePowerCoef();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        NBTPlugin.writeEnumOrdinal(data, "processState", processState);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        processState = NBTPlugin.readEnumOrdinal(data, "processState", ProcessState.values(), ProcessState.RESET);
    }

    @Override
    public boolean canPassFluidRequests(FluidStack fluid) {
        return engine.canAcceptFluid(fluid.getFluid());
    }

    @Override
    public boolean canAcceptPushedFluid(EntityMinecart requester, FluidStack fluid) {
        return engine.canAcceptFluid(fluid.getFluid());
    }

    @Override
    public boolean canProvidePulledFluid(EntityMinecart requester, FluidStack fluid) {
        return false;
    }
}