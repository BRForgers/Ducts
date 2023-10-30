package br.com.brforgers.ducts.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import br.com.brforgers.ducts.Ducts;
import br.com.brforgers.ducts.blockentities.DuctBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class DuctBlock extends BaseEntityBlock
{
	private static HashMap<BlockState, VoxelShape> shapeCache = new HashMap<BlockState, VoxelShape>();

	public DuctBlock() 
	{
		super(Properties.of(Material.METAL, MaterialColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion());
		
		registerDefaultState(DuctProperties.INPUT.values().stream()
				.reduce(stateDefinition.any(), (state, prop) -> state.setValue(prop, Boolean.valueOf(false)), DuctBlock::combiner)
				.setValue(DuctProperties.OUTPUT, Direction.DOWN)
				.setValue(DuctProperties.POWERED, Boolean.valueOf(false))
		);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) 
	{
		return new DuctBlockEntity(pPos, pState);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> propContainerBuilder) 
	{
		propContainerBuilder.add(DuctProperties.POWERED, DuctProperties.OUTPUT);
		propContainerBuilder.add(DuctProperties.INPUT.values().toArray(BooleanProperty[]::new));
    }
	
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos blockPos, CollisionContext verticalEntityPosition)
	{
		return shapeCache.computeIfAbsent(state, DuctShapes::get);
	}
	
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) 
	{
		return DuctProperties.INPUT.entrySet().stream()
		.map(entry -> Map.entry(
				entry.getValue(), 
				Boolean.valueOf(canConnect(context.getLevel().getBlockState(context.getClickedPos().relative(entry.getKey())), entry.getKey()))
			))
		.reduce(defaultBlockState(), (state, entry) -> state.setValue(entry.getKey(), entry.getValue()), DuctBlock::combiner)
		.setValue(DuctProperties.OUTPUT, context.getClickedFace().getOpposite())
		.setValue(DuctProperties.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

	@Override
	public RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.MODEL;
	}
	
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) 
	{
        return state.setValue(DuctProperties.INPUT.get(direction), canConnect(neighborState, direction)).setValue(DuctProperties.POWERED, ((Level)world).hasNeighborSignal(pos));
    }

	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
        return state.setValue(DuctProperties.OUTPUT, rotation.rotate(state.getValue(DuctProperties.OUTPUT)));
    }

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) 
	{
        return state.setValue(DuctProperties.OUTPUT, mirror.mirror(state.getValue(DuctProperties.OUTPUT)));
    }

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult blockHitPos) 
	{
		if (player.getItemInHand(hand).getItem() == this.asItem() && player.isShiftKeyDown())
			return InteractionResult.PASS;

        if (!world.isClientSide()) 
        {
            BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof DuctBlockEntity ductBlockEntity)
                NetworkHooks.openScreen((ServerPlayer)player, ductBlockEntity, pos);
        }

        return InteractionResult.SUCCESS;
    }
	
	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) 
	{
        if (stack.hasCustomHoverName()) 
        {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DuctBlockEntity ductBlockEntity)
                ductBlockEntity.setCustomName(stack.getHoverName());
        }
        
        this.onRemove(state, world, pos, state, dynamicShape);
    }

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state1, Level world, BlockPos pos, BlockState state2, boolean someBool)
	{
        if (state1.getBlock() != state2.getBlock()) 
        {
        	BlockEntity entity = world.getBlockEntity(pos);
        	if (entity instanceof DuctBlockEntity ductBlockEntity)
        	{
        		Containers.dropContents(world, pos, ductBlockEntity);
        		world.updateNeighbourForOutputSignal(pos, this);
        	}
        }
        
        super.onRemove(state1, world, pos, state2, someBool);
    }

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) 
	{
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos)
	{
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos));
	}
	
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type)
	{
		return world.isClientSide() ? null : createTickerHelper(type, Ducts.DUCT_BLOCKENTITY_TYPE.get(), DuctBlockEntity::tick);
	}
	
	private static boolean canConnect(BlockState other, Direction dirToOther)
	{
		return Set.of(Blocks.HOPPER, Ducts.DUCT_BLOCK.get()).contains(other.getBlock()) && 
				Stream.of(
						BlockStateProperties.FACING,
						BlockStateProperties.HORIZONTAL_FACING,
						BlockStateProperties.FACING_HOPPER
					).anyMatch(facingProp -> 
						other.hasProperty(facingProp) && 
						other.getValue(facingProp) == dirToOther.getOpposite()
					);
    }
	
	private static BlockState combiner(BlockState state1, BlockState state2) 
	{
		for (Property<?> prop : state2.getProperties())
			state1 = setProperty(state1, state2, prop);
		return state1;
	}
	
	private static <T extends Comparable<T>> BlockState setProperty(BlockState state, BlockState from, Property<T> prop)
	{
		return state.setValue(prop, from.getValue(prop));
	}
	
	public static class DuctShapes 
	{
		public static final VoxelShape CORESHAPE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);
		public static final Map<Direction, VoxelShape> OUTPUTSHAPES = Map.of(
				Direction.NORTH, Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 4.0), //Z
				Direction.EAST, Block.box(12.0, 6.0, 6.0, 16.0, 10.0, 10.0), //X
				Direction.SOUTH, Block.box(6.0, 6.0, 12.0, 10.0, 10.0, 16.0), //Z
				Direction.WEST, Block.box(0.0, 6.0, 6.0, 4.0, 10.0, 10.0), //X
				Direction.DOWN, Block.box(6.0, 0.0, 6.0, 10.0, 4.0, 10.0), //Y
				Direction.UP, Block.box(6.0, 12.0, 6.0, 10.0, 16.0, 10.0) //Y NEW
			);
		public static final Map<Direction, VoxelShape> INPUTSHAPES = Map.of(
				Direction.NORTH, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 4.0),
				Direction.EAST, Block.box(12.0, 5.0, 5.0, 16.0, 11.0, 11.0),
				Direction.SOUTH, Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 16.0),
				Direction.WEST, Block.box(0.0, 5.0, 5.0, 4.0, 11.0, 11.0),
				Direction.DOWN, Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0), //NEW
				Direction.UP, Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0)
		    );
		
		public static VoxelShape get(BlockState state) 
		{
			VoxelShape output = OUTPUTSHAPES.get(state.getValue(DuctProperties.OUTPUT));
			List<VoxelShape> inputs = DuctProperties.INPUT.keySet().stream()
					.filter(it -> state.getValue(DuctProperties.INPUT.get(it)))
					.map(INPUTSHAPES::get)
					.toList();
			
			List<VoxelShape> shapes = new ArrayList<>(Arrays.asList(output));
			shapes.addAll(inputs);
			
			return Shapes.or(CORESHAPE, shapes.toArray(VoxelShape[]::new));
		}
	}
	
	public static class DuctProperties
	{
		public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
		public static final DirectionProperty OUTPUT = BlockStateProperties.FACING;
		public static final Map<Direction, BooleanProperty> INPUT = Map.of(
				Direction.NORTH, BlockStateProperties.NORTH,
				Direction.EAST, BlockStateProperties.EAST,
				Direction.SOUTH, BlockStateProperties.SOUTH,
				Direction.WEST, BlockStateProperties.WEST,
				Direction.DOWN, BlockStateProperties.DOWN,
				Direction.UP, BlockStateProperties.UP
			);
	}
}
