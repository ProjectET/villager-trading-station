package me.ramidzkh.vts.block;

import com.google.common.base.Predicates;
import io.github.astrarre.gui.v1.api.component.ACenteringPanel;
import io.github.astrarre.gui.v1.api.component.AGrid;
import io.github.astrarre.gui.v1.api.component.AIcon;
import io.github.astrarre.gui.v1.api.component.AList;
import io.github.astrarre.gui.v1.api.component.slot.ASlot;
import io.github.astrarre.gui.v1.api.component.slot.SlotKey;
import io.github.astrarre.gui.v1.api.server.ServerPanel;
import io.github.astrarre.rendering.v1.api.plane.icon.backgrounds.ContainerBackgroundIcon;
import io.github.astrarre.rendering.v1.api.space.Transform3d;
import io.github.astrarre.rendering.v1.api.util.Axis2d;
import me.ramidzkh.vts.VillagerTradingStationFabric;
import me.ramidzkh.vts.gui.VillagerTradingServerPanel;
import me.ramidzkh.vts.gui.client.VillagerTradingClientPanel;
import me.ramidzkh.vts.util.GuiHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class TradingStationBlock extends BaseEntityBlock {

    private static final VoxelShape[] SHAPES = new VoxelShape[3];

    static {
        SHAPES[Direction.Axis.X.ordinal()] = makeShape();
        SHAPES[Direction.Axis.Z.ordinal()] = rotateShape(Direction.NORTH, Direction.WEST, makeShape());
    }

    boolean toggle;

    public TradingStationBlock(Properties properties) {
        super(properties);
    }

    public static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.4375, 0.21875, 0.4375, 0.5625, 0.84375, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.0625, 0, 0.0625, 0.9375, 0.0625, 0.9375), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.421875, 0.9375, 0.21875, 0.578125, 1, 0.78125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.59375, 0.84375, 0.8125, 0.65625, 0.90625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.15625, 0.34375, 0.84375, 0.21875, 0.59375, 0.90625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.78125, 0.34375, 0.84375, 0.84375, 0.59375, 0.90625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.15625, 0.28125, 0.71875, 0.84375, 0.34375, 1.03125), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.25, 0.0625, 0.25, 0.75, 0.125, 0.75), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.1875, 0.59375, 0.09375, 0.8125, 0.65625, 0.15625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.15625, 0.34375, 0.09375, 0.21875, 0.59375, 0.15625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.78125, 0.34375, 0.09375, 0.84375, 0.59375, 0.15625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.15625, 0.28125, -0.03125, 0.84375, 0.34375, 0.28125), BooleanOp.OR);

        return shape;
    }

    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[]{shape, Shapes.empty()};
        int times = (to.get2DDataValue() - from.get2DDataValue() + 4) % 4;

        for (int i = 0; i < times; i++) {
            buffer[0].forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = Shapes.or(buffer[1], Shapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = Shapes.empty();
        }

        return buffer[0];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_AXIS);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, blockPlaceContext.getHorizontalDirection().getAxis());
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPES[blockState.getValue(BlockStateProperties.HORIZONTAL_AXIS).ordinal()];
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        boolean success = false;

        if (level.getBlockEntity(blockPos) instanceof TradingStationBlockEntity tradingStation) {
            if (player.isShiftKeyDown()) {
                try (Transaction transaction = Transaction.openOuter()) {
                    success = StorageUtil.move(tradingStation.getQuotes(), PlayerInventoryStorage.of(player), Predicates.alwaysTrue(), 1, transaction) == 1;

                    if (success) {
                        transaction.commit();
                    }
                }
            } else {
                ItemStack hand = player.getItemInHand(interactionHand);

                try (Transaction transaction = Transaction.openOuter()) {
                    if (tradingStation.getQuotes().insert(ItemVariant.of(hand), 1, transaction) == 1) {
                        if (!player.getAbilities().instabuild) {
                            hand.shrink(1);
                        }

                        transaction.commit();
                        success = true;
                    } else {
                        List<SlotKey> playerKey = SlotKey.player(player, 0), quoteKey = SlotKey.inv(tradingStation.getQuoteContainer(), 1), inputKey = SlotKey.inv(tradingStation.getInputContainer(), 2), outputKey = SlotKey.inv(tradingStation.getOutputContainer(),
                                3);

                        playerKey.forEach(k -> k.linkAllPre(quoteKey));
                        quoteKey.forEach(k -> k.linkAll(playerKey));

                        playerKey.forEach(k -> k.linkAllPre(inputKey));
                        inputKey.forEach(k -> k.linkAll(playerKey));

                        outputKey.forEach(k -> k.linkAll(playerKey));
                        playerKey.forEach(k -> k.linkAll(outputKey));

                        ArrayList<List<SlotKey>> listKey = new ArrayList<>();
                        listKey.add(playerKey);
                        listKey.add(inputKey);
                        listKey.add(outputKey);
                        listKey.add(quoteKey);

                        ServerPanel.openHandled(player,
                                (communication, panel) -> new VillagerTradingClientPanel(communication, panel, listKey),
                                (communication, panel) -> new VillagerTradingServerPanel(communication, panel, listKey));
                        return InteractionResult.CONSUME;
                    }
                }
            }

        }

        return success ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (level.getBlockEntity(blockPos) instanceof TradingStationBlockEntity tradingStation) {
            tradingStation.drop(level, blockPos);
        }

        super.onRemove(blockState, level, blockPos, blockState2, bl);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return VillagerTradingStationFabric.BlockEntities.TRADING_STATION.create(blockPos, blockState);
    }
}
