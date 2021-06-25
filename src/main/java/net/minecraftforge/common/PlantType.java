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

package net.minecraftforge.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class PlantType
{
    private static final Pattern INVALID_CHARACTERS = Pattern.compile("[^a-z_]"); //Only a-z and _ are allowed, meaning names must be lower case. And use _ to separate words.
    private static final Map<String, PlantType> VALUES = new ConcurrentHashMap<>();

    public static final PlantType PLAINS = get("plains", x -> x.getState().is(Blocks.SAND) || x.getState().is(Blocks.TERRACOTTA) || x.getState().getBlock() instanceof GlazedTerracottaBlock);
    public static final PlantType DESERT = get("desert", x -> x.getState().is(Blocks.GRASS_BLOCK) || Tags.Blocks.DIRT.contains(x.getState().getBlock()) || x.getState().is(Blocks.FARMLAND));
    public static final PlantType BEACH = get("beach", x -> {
        boolean isBeach = x.getState().is(Blocks.GRASS_BLOCK) || net.minecraftforge.common.Tags.Blocks.DIRT.contains(x.getState().getBlock()) || x.getState().is(Blocks.SAND) || x.getState().is(Blocks.RED_SAND);
        boolean hasWater = false;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockState blockState = x.getWorld().getBlockState(x.getPos().relative(face));
            net.minecraft.fluid.FluidState fluidState = x.getWorld().getFluidState(x.getPos().relative(face));
            hasWater |= blockState.is(Blocks.FROSTED_ICE);
            hasWater |= fluidState.is(net.minecraft.tags.FluidTags.WATER);
            if (hasWater)
                break; //No point continuing.
        }
        return isBeach && hasWater;
    });
    public static final PlantType CAVE = get("cave", x -> x.getState().isFaceSturdy(x.getWorld(), x.getPos(), Direction.UP));
    public static final PlantType WATER = get("water", x -> x.getState().getMaterial() == Material.WATER);
    public static final PlantType NETHER = get("nether", x -> x.getState().is(Blocks.SOUL_SAND));
    public static final PlantType CROP = get("crop", x -> x.getState().is(Blocks.FARMLAND));
    public static final PlantType CACTUS = get("cactus", x -> x.getState().is(Blocks.CACTUS) || x.getState().is(Blocks.SAND) || x.getState().is(Blocks.RED_SAND));
    public static final PlantType SUGARCANE = get("sugarcane", BEACH.getBlockPredicate().or(x -> x.getState().is(Blocks.SUGAR_CANE)));

    /**
     * Getting a custom {@link PlantType}, or an existing one if it has the same name as that one. Your plant should implement {@link IPlantable}
     * and return this custom type in {@link IPlantable#getPlantType(IBlockReader, BlockPos)}.
     *
     * <p>If your new plant grows on blocks like any one of them above, never create a new {@link PlantType}.
     * This Type is only functioning in
     * {@link net.minecraft.block.Block#canSustainPlant(BlockState, IBlockReader, BlockPos, Direction, IPlantable)},
     * which you are supposed to override this function in your new block and create a new plant type to grow on that block.
     *
     * This method can be called during parallel loading
     * @param name the name of the type of plant, you had better follow the style above
     * @return the acquired {@link PlantType}, a new one if not found.
     */
    public static PlantType get(String name, Predicate<PlantInfo> blockPredicate)
    {
        return VALUES.computeIfAbsent(name, e ->
        {
            if (INVALID_CHARACTERS.matcher(e).find())
                throw new IllegalArgumentException("PlantType.get() called with invalid name: " + name);
            return new PlantType(e, blockPredicate);
        });
    }

    private final String name;
    private Predicate<PlantInfo> blockPredicate;

    private PlantType(String name, Predicate<PlantInfo> blockPredicate)
    {
        this.name = name;
        this.blockPredicate = blockPredicate;
    }

    public String getName()
    {
        return name;
    }

    public Predicate<PlantInfo> getBlockPredicate()
    {
        return blockPredicate;
    }

    public void setBlockPredicate(Predicate<PlantInfo> blockPredicate)
    {
        this.blockPredicate = blockPredicate;
    }

    public void and(Predicate<PlantInfo> newBlockPredicate) {
        blockPredicate = blockPredicate.and(newBlockPredicate);
    }

    public void or(Predicate<PlantInfo> newBlockPredicate) {
        blockPredicate = blockPredicate.or(newBlockPredicate);
    }

    public boolean test(IBlockReader world, BlockPos pos, BlockState state, Direction facing) {
        return blockPredicate.test(new PlantInfo(world, pos, state, facing));
    }

    public static class PlantInfo {
        public final IBlockReader world;
        public final BlockPos pos;
        public final BlockState state;
        public final Direction facing;

        private PlantInfo(IBlockReader world, BlockPos pos, BlockState state, Direction facing) {
            this.world = world;
            this.pos = pos;
            this.state = state;
            this.facing = facing;
        }

        public BlockState getState()
        {
            return state;
        }

        public BlockPos getPos()
        {
            return pos;
        }

        public Direction getFacing()
        {
            return facing;
        }

        public IBlockReader getWorld()
        {
            return world;
        }
    }
}

