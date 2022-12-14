/*
 *     ____           _       __    __     _____ __            ___
 *    / __ \_      __(_)___ _/ /_  / /_   / ___// /___  ______/ (_)___
 *   / / / / | /| / / / __ `/ __ \/ __/   \__ \/ __/ / / / __  / / __ \
 *  / /_/ /| |/ |/ / / /_/ / / / / /_    ___/ / /_/ /_/ / /_/ / / /_/ /
 * /_____/ |__/|__/_/\__, /_/ /_/\__/   /____/\__/\__,_/\__,_/_/\____/
 *                  /____/
 * Copyright (c) 2022-2022 Dwight Studio's Team <support@dwight-studio.fr>
 *
 * This Source Code From is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/ .
 */

package fr.dwightstudio.instantstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class InstantComparatorBlock extends InstantDiodeBlock implements EntityBlock {
    public static final EnumProperty<ComparatorMode> MODE = BlockStateProperties.MODE_COMPARATOR;

    public InstantComparatorBlock() {
        super(BlockBehaviour.Properties.of(Material.DECORATION).instabreak().sound(SoundType.WOOD));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.FALSE).setValue(MODE, ComparatorMode.COMPARE));
    }

    protected int getOutputSignal(BlockGetter p_51892_, @NotNull BlockPos p_51893_, @NotNull BlockState p_51894_) {
        BlockEntity blockentity = p_51892_.getBlockEntity(p_51893_);
        return blockentity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockentity).getOutputSignal() : 0;
    }

    private int calculateOutputSignal(Level p_51904_, BlockPos p_51905_, BlockState p_51906_) {
        int i = this.getInputSignal(p_51904_, p_51905_, p_51906_);
        if (i == 0) {
            return 0;
        } else {
            int j = this.getAlternateSignal(p_51904_, p_51905_, p_51906_);
            if (j > i) {
                return 0;
            } else {
                return p_51906_.getValue(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
            }
        }
    }

    protected boolean shouldTurnOn(@NotNull Level p_51861_, @NotNull BlockPos p_51862_, @NotNull BlockState p_51863_) {
        int i = this.getInputSignal(p_51861_, p_51862_, p_51863_);
        if (i == 0) {
            return false;
        } else {
            int j = this.getAlternateSignal(p_51861_, p_51862_, p_51863_);
            if (i > j) {
                return true;
            } else {
                return i == j && p_51863_.getValue(MODE) == ComparatorMode.COMPARE;
            }
        }
    }

    protected int getInputSignal(@NotNull Level p_51896_, @NotNull BlockPos p_51897_, @NotNull BlockState p_51898_) {
        int i = super.getInputSignal(p_51896_, p_51897_, p_51898_);
        Direction direction = p_51898_.getValue(FACING);
        BlockPos blockpos = p_51897_.relative(direction);
        BlockState blockstate = p_51896_.getBlockState(blockpos);
        if (blockstate.hasAnalogOutputSignal()) {
            i = blockstate.getAnalogOutputSignal(p_51896_, blockpos);
        } else if (i < 15 && blockstate.isRedstoneConductor(p_51896_, blockpos)) {
            blockpos = blockpos.relative(direction);
            blockstate = p_51896_.getBlockState(blockpos);
            ItemFrame itemframe = this.getItemFrame(p_51896_, direction, blockpos);
            int j = Math.max(itemframe == null ? Integer.MIN_VALUE : itemframe.getAnalogOutput(), blockstate.hasAnalogOutputSignal() ? blockstate.getAnalogOutputSignal(p_51896_, blockpos) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }

        return i;
    }

    @Nullable
    private ItemFrame getItemFrame(Level p_51865_, Direction p_51866_, BlockPos p_51867_) {
        List<ItemFrame> list = p_51865_.getEntitiesOfClass(ItemFrame.class, new AABB(p_51867_.getX(), p_51867_.getY(), p_51867_.getZ(), p_51867_.getX() + 1, p_51867_.getY() + 1, p_51867_.getZ() + 1), (p_51890_) -> p_51890_ != null && p_51890_.getDirection() == p_51866_);
        return list.size() == 1 ? list.get(0) : null;
    }

    public @NotNull InteractionResult use(@NotNull BlockState p_51880_, @NotNull Level p_51881_, @NotNull BlockPos p_51882_, Player p_51883_, @NotNull InteractionHand p_51884_, @NotNull BlockHitResult p_51885_) {
        if (!p_51883_.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            p_51880_ = p_51880_.cycle(MODE);
            float f = p_51880_.getValue(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
            p_51881_.playSound(p_51883_, p_51882_, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, f);
            p_51881_.setBlock(p_51882_, p_51880_, 2);
            this.refreshOutputState(p_51881_, p_51882_, p_51880_);
            return InteractionResult.sidedSuccess(p_51881_.isClientSide);
        }
    }

    protected void checkTickOnNeighbor(@NotNull Level p_51900_, @NotNull BlockPos p_51901_, @NotNull BlockState p_51902_) {
        int i = this.calculateOutputSignal(p_51900_, p_51901_, p_51902_);
        BlockEntity blockentity = p_51900_.getBlockEntity(p_51901_);
        int j = blockentity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockentity).getOutputSignal() : 0;
        if (i != j || p_51902_.getValue(POWERED) != this.shouldTurnOn(p_51900_, p_51901_, p_51902_)) {
            this.refreshOutputState(p_51900_, p_51901_, p_51902_);
        }
    }

    private void refreshOutputState(Level p_51908_, BlockPos p_51909_, BlockState p_51910_) {
        int i = this.calculateOutputSignal(p_51908_, p_51909_, p_51910_);
        BlockEntity blockentity = p_51908_.getBlockEntity(p_51909_);
        int j = 0;
        if (blockentity instanceof ComparatorBlockEntity comparatorblockentity) {
            j = comparatorblockentity.getOutputSignal();
            comparatorblockentity.setOutputSignal(i);
        }

        if (j != i || p_51910_.getValue(MODE) == ComparatorMode.COMPARE) {
            boolean flag1 = this.shouldTurnOn(p_51908_, p_51909_, p_51910_);
            boolean flag = p_51910_.getValue(POWERED);
            if (flag && !flag1) {
                p_51908_.setBlock(p_51909_, p_51910_.setValue(POWERED, Boolean.FALSE), 2);
            } else if (!flag && flag1) {
                p_51908_.setBlock(p_51909_, p_51910_.setValue(POWERED, Boolean.TRUE), 2);
            }

            this.updateNeighborsInFront(p_51908_, p_51909_, p_51910_);
        }

    }

    public void tick(@NotNull BlockState p_221010_, @NotNull ServerLevel p_221011_, @NotNull BlockPos p_221012_, @NotNull RandomSource p_221013_) {
        this.sTick(p_221010_, p_221011_, p_221012_);
    }

    @Override
    public void sTick(BlockState p_221065_, Level p_221066_, BlockPos p_221067_) {
        super.sTick(p_221065_, p_221066_, p_221067_);
        this.refreshOutputState(p_221066_, p_221067_, p_221065_);
    }

    public boolean triggerEvent(@NotNull BlockState p_51874_, @NotNull Level p_51875_, @NotNull BlockPos p_51876_, int p_51877_, int p_51878_) {
        super.triggerEvent(p_51874_, p_51875_, p_51876_, p_51877_, p_51878_);
        BlockEntity blockentity = p_51875_.getBlockEntity(p_51876_);
        return blockentity != null && blockentity.triggerEvent(p_51877_, p_51878_);
    }

    public BlockEntity newBlockEntity(@NotNull BlockPos p_153086_, @NotNull BlockState p_153087_) {
        return new ComparatorBlockEntity(p_153086_, p_153087_);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_51887_) {
        p_51887_.add(FACING, MODE, POWERED);
    }

    @Override
    public boolean getWeakChanges(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos) {
        return state.is(Blocks.COMPARATOR);
    }

    @Override
    public void onNeighborChange(BlockState state, net.minecraft.world.level.LevelReader world, BlockPos pos, BlockPos neighbor) {
        if (pos.getY() == neighbor.getY() && world instanceof Level && !world.isClientSide()) {
            state.neighborChanged((Level)world, pos, world.getBlockState(neighbor).getBlock(), neighbor, false);
        }
    }
}
