/*
 * Copyright (c) 2017, 2018, 2019, 2021 Adrian Siekierka
 *
 * This file is part of Unlimited Chisel Works.
 *
 * Unlimited Chisel Works is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unlimited Chisel Works is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Unlimited Chisel Works.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.ucw.util;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class BlockStateUtil {
    private BlockStateUtil() {

    }

    public static String getVariantString(IBlockState state) {
        BlockStateContainer baseState = state.getBlock().getBlockState();
        final StringBuilder variant = new StringBuilder();
        baseState.getProperties().stream()
                .sorted(Comparator.comparing(IProperty::getName))
                .forEachOrdered(p -> {
                    //noinspection unchecked
                    variant.append(p.getName()).append('=').append(((IProperty) p).getName((Comparable) state.getValue(p))).append(',');
                });
        variant.setLength(variant.length() - 1);
        return variant.toString();
    }

    public static IBlockState applyProperties(Block block, IBlockState state) {
        IBlockState toState = block.getDefaultState();
        for (IProperty property : state.getPropertyKeys()) {
            toState = toState.withProperty(property, state.getValue(property));
        }
        return toState;
    }
}
