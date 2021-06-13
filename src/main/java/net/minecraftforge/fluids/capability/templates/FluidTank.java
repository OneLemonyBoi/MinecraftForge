/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fluids.capability.templates;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.FluidResult;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Flexible implementation of a Fluid Storage object. NOT REQUIRED.
 *
 * @author King Lemming and OneLemonyBoi
 */
public class FluidTank implements IFluidHandler {

    protected Predicate<FluidStack> validator;
    @Nonnull
    protected FluidStack fluid = FluidStack.EMPTY;
    protected int capacity;
    protected HandlerType handlerType;
    protected Function<FluidResult, ItemStack> stackFunction;

    public FluidTank(int capacity)
    {
        this(capacity, e -> true, HandlerType.BLOCK);
    }

    public FluidTank(int capacity, HandlerType handlerType)
    {
        this(capacity, e -> true, handlerType);
    }

    public FluidTank(int capacity, Predicate<FluidStack> validator, HandlerType handlerType)
    {
        this.capacity = capacity;
        this.validator = validator;
        this.handlerType = handlerType;
        this.stackFunction = FluidResult::getItemStack;
    }

    public FluidTank(int capacity, Predicate<FluidStack> validator, Function<FluidResult, ItemStack> stackFunction, HandlerType handlerType)
    {
        this.capacity = capacity;
        this.validator = validator;
        this.handlerType = handlerType;
        this.stackFunction = stackFunction;
    }

    public FluidTank(int capacity, Function<FluidResult, ItemStack> stackFunction)
    {
        this(capacity, e -> true, stackFunction, HandlerType.ITEM);
    }

    public FluidTank setCapacity(int capacity)
    {
        this.capacity = capacity;
        return this;
    }

    public FluidTank setValidator(Predicate<FluidStack> validator)
    {
        if (validator != null) {
            this.validator = validator;
        }
        return this;
    }

    public boolean isFluidValid(FluidStack stack)
    {
        return validator.test(stack);
    }

    public int getCapacity()
    {
        return capacity;
    }

    @Nonnull
    public FluidStack getFluid()
    {
        return fluid;
    }

    public int getFluidAmount()
    {
        return fluid.getAmount();
    }

    public FluidTank readFromNBT(CompoundNBT nbt) {

        FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
        setFluid(fluid);
        return this;
    }

    public CompoundNBT writeToNBT(CompoundNBT nbt) {

        fluid.writeToNBT(nbt);

        return nbt;
    }

    @Override
    public HandlerType getType()
    {
        return HandlerType.BLOCK;
    }

    @Override
    public int getTanks() {

        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {

        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {

        return getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {

        return isFluidValid(stack);
    }

    @Override
    public int fillBlock(FluidStack resource, FluidAction action)
    {
        if (resource.isEmpty() || !isFluidValid(resource))
        {
            return 0;
        }
        if (action.simulate())
        {
            if (fluid.isEmpty())
            {
                return Math.min(capacity, resource.getAmount());
            }
            if (!fluid.isFluidEqual(resource))
            {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty())
        {
            fluid = new FluidStack(resource, Math.min(capacity, resource.getAmount()));
            onContentsChanged();
            return fluid.getAmount();
        }
        if (!fluid.isFluidEqual(resource))
        {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled)
        {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        }
        else
        {
            fluid.setAmount(capacity);
        }
        if (filled > 0)
            onContentsChanged();
        return filled;
    }

    @Override
    public FluidResult fillItem(FluidStack resource, FluidAction action)
    {
        return FluidResult.of(new FluidStack(resource.getFluid(), fillBlock(resource, action)));
    }

    @Nonnull
    @Override
    public FluidStack drainBlock(FluidStack resource, FluidAction action)
    {
        if (resource.isEmpty() || !resource.isFluidEqual(fluid))
        {
            return FluidStack.EMPTY;
        }
        return drainBlock(resource.getAmount(), action);
    }

    @Nonnull
    @Override
    public FluidResult drainItem(FluidStack resource, FluidAction action)
    {
        return FluidResult.of(drainBlock(resource, action));
    }

    @Nonnull
    @Override
    public FluidStack drainBlock(int maxDrain, FluidAction action)
    {
        int drained = maxDrain;
        if (fluid.getAmount() < drained)
        {
            drained = fluid.getAmount();
        }
        FluidStack stack = new FluidStack(fluid, drained);
        if (action.execute() && drained > 0)
        {
            fluid.shrink(drained);
            onContentsChanged();
        }
        return stack;
    }

    @Nonnull
    @Override
    public FluidResult drainItem(int maxDrain, FluidAction action)
    {
        return FluidResult.of(drainBlock(maxDrain, action));
    }

    protected void onContentsChanged()
    {

    }

    public void setFluid(FluidStack stack)
    {
        this.fluid = stack;
    }

    public boolean isEmpty()
    {
        return fluid.isEmpty();
    }

    public int getSpace()
    {
        return Math.max(0, capacity - fluid.getAmount());
    }
}
