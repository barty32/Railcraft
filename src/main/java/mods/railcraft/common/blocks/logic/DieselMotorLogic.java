/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2022
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.blocks.logic;

import mods.railcraft.common.fluids.*;
import mods.railcraft.common.fluids.tanks.StandardTank;
import mods.railcraft.common.gui.widgets.IIndicatorController;
import mods.railcraft.common.gui.widgets.IndicatorController;
import mods.railcraft.common.modules.ModuleLocomotives;
import mods.railcraft.common.plugins.buildcraft.triggers.ITemperature;
import mods.railcraft.common.util.sounds.SoundHelper;
import mods.railcraft.common.util.steam.SteamConstants;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class DieselMotorLogic extends Logic implements ITemperature, IFluidHandlerImplementor {

    private static final int TANK_CAPACITY = ModuleLocomotives.config.dieselLocomotiveTankCapacity * FluidTools.BUCKET_VOLUME;
    private static final int TICKS_PER_CYCLE = 40;
    private static final int ENGINE_STARTUP_CONSUMPTION = 10;
    private static final double TARGET_TEMPERATURE = 105.0;
    private static final double MAX_TEMPERATURE = 200.0;
    public final IIndicatorController heatIndicator = new HeatIndicator();
    private final StandardTank tankDiesel;
    private double temp = SteamConstants.COLD_TEMP;
    private boolean running;
    protected byte burnCycle;

    public DieselMotorLogic(Adapter adapter) {
        super(adapter);

        tankDiesel = new StandardTank(TANK_CAPACITY);

        addLogic(new FluidLogic(adapter).addTank(tankDiesel));
        addLogic(new BucketInteractionLogic(adapter));
    }

    @Override
    protected void updateServer() {
        if (isRunning()) {
            burnCycle++;

            // Idle mode should use 1 mB per 2 seconds
            if (burnCycle >= TICKS_PER_CYCLE) {
                burnCycle = 0;
                if(burnFuel(1) == 0){
                    //shut down engine
                    setRunning(false);
                    return;
                }
            }
            // Idle engine temperature increase
            increaseTemp(0.5);
        }
        else {
            reduceTemp();
        }
    }

    public boolean isFuelValid(Fluid fluid) {
        //TODO: maybe move this to config?
        Fluids[] validFuels = {
            Fluids.FUEL,
            Fluids.BIODIESEL,
            Fluids.DIESEL,
            Fluids.FUEL_LIGHT,
            Fluids.FUEL_MIXED_LIGHT,
        };
        return Arrays.stream(validFuels).anyMatch(fuel -> Fluids.areEqual(fluid, fuel.get(1)));
    }

    public boolean canAcceptFluid(@Nullable Fluid fluid) {
        FluidStack fluidInTank = tankDiesel.getFluid();
        return fluid != null && isFuelValid(fluid) &&
            (fluidInTank == null || fluidInTank.getFluid() == fluid);
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if(!isFuelValid(resource.getFluid()))
            return 0;
        return getTankManager().fill(resource, doFill);
    }

    @Override
    public TankManager getTankManager() {
        return getLogic(FluidLogic.class).map(FluidLogic::getTankManager).orElse(TankManager.NIL);
    }

    public StandardTank getDieselTank() {
        return tankDiesel;
    }

    public double getMaxTemp() {
        return MAX_TEMPERATURE;
    }

    @Override
    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
        if (this.temp < SteamConstants.COLD_TEMP)
            this.temp = SteamConstants.COLD_TEMP;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean state) {
        if(running == state)
            return;

        if(state) {
            // Start engine
            tankDiesel.drainInternal(ENGINE_STARTUP_CONSUMPTION, true);
            if(tankDiesel.isEmpty())
                return;
            //SoundHelper.playSound(theWorldAsserted(), null, getPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
        else {
            // Stop engine
            //SoundHelper.playSound(theWorldAsserted(), null, getPos(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        }
        running = state;
    }

    public int burnFuel(int amount) {
        if(isRunning()){
            FluidStack fluid = tankDiesel.drainInternal((int)Math.ceil(amount * ModuleLocomotives.config.dieselMotorConsumptionMultiplier), true);
            if(fluid != null) {
                increaseTemp(amount);
                return fluid.amount;
            }
        }
        return 0;
    }

    public double getHeatLevel() {
        return temp / getMaxTemp();
    }

    public void increaseTemp(double coef) {
        double max = getMaxTemp();
        if (temp == max)
            return;
        double percentHot = ((TARGET_TEMPERATURE - temp) / TARGET_TEMPERATURE);
        temp += percentHot * coef * 0.05;// This was chosen to not heat up too quickly
        temp = Math.min(temp, max);
    }

    public void reduceTemp() {
        if (temp == SteamConstants.COLD_TEMP)
            return;
        double step = SteamConstants.HEAT_STEP * 0.1;
        temp -= step + ((temp / getMaxTemp()) * step * 3);
        temp = Math.max(temp, SteamConstants.COLD_TEMP);
    }

    public double getTemperaturePowerCoef() {
        // Basically a Gaussian curve
        return 1.1 / Math.exp(Math.pow((temp - TARGET_TEMPERATURE) / 100.0, 2));
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setBoolean("motorRunning", running);
        data.setDouble("heat", (float) temp);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        running = data.getBoolean("motorRunning");
        temp = data.getDouble("heat");
    }

    private class HeatIndicator extends IndicatorController {

        @Override
        protected void refreshToolTip() {
            tip.text = String.format("%.0f°C", getTemp());
        }

        @Override
        public double getMeasurement() {
            return getTemp() / getMaxTemp();
        }

        @Override
        public double getServerValue() {
            return getTemp();
        }

        @Override
        public double getClientValue() {
            return getTemp();
        }

        @Override
        public void setClientValue(double value) {
            setTemp(value);
        }
    }
}