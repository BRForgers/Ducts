package br.com.brforgers.mods.ducts.blocks

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import net.minecraft.block.*
import net.minecraft.block.material.Material
import net.minecraft.block.material.MaterialColor
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.inventory.InventoryHelper
import net.minecraft.inventory.container.Container
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.ItemStack
import net.minecraft.state.StateContainer
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.world.*
import net.minecraftforge.fml.network.NetworkHooks


class DuctBlock :
    ContainerBlock(Properties.of(Material.METAL, MaterialColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()) {
    private val shapeCache = hashMapOf<BlockState, VoxelShape>()

    init {
        registerDefaultState(Props.input.values
                .fold(stateDefinition.any()) { state, prop -> state.setValue(prop, false) }
                .setValue(Props.output, Direction.DOWN)
                .setValue(Props.powered, false))
    }

    override fun createBlockStateDefinition(propContainerBuilder: StateContainer.Builder<Block, BlockState>) {
        propContainerBuilder.add(
            Props.output,
            Props.powered,
            *Props.input.values.toTypedArray()
        )
    }

    override fun newBlockEntity(blockreader: IBlockReader) = DuctBlockEntity()

    override fun getShape(
        state: BlockState,
        view: IBlockReader,
        blockPos: BlockPos,
        verticalEntityPosition: ISelectionContext
    ): VoxelShape {
        return shapeCache.computeIfAbsent(state) {
            val core = Shapes.coreCube
            val output = Shapes.outputCubes[state.getValue(Props.output)]
            val inputs = Props.input.keys
                .filter { state.getValue(Props.input.getValue(it)) }
                .map(Shapes.inputCubes::getValue)
                .toTypedArray()
            VoxelShapes.or(core, output, *inputs)
        }
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            Props.output,
            rotation.rotate(state.getValue(Props.output) as Direction)
        )
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.setValue(
            Props.output,
            mirror.mirror(state.getValue(Props.output) as Direction)
        )
    }

    override fun getStateForPlacement(context: BlockItemUseContext): BlockState {
        return Props.input.entries
            .map { (dir, prop) ->
                prop to canConnect(
                    context.level.getBlockState(context.clickedPos.relative(dir)),
                    dir
                )
            }
            .fold(defaultBlockState()) { state, (prop, connect) -> state.setValue(prop, connect) }
            .setValue(Props.output, context.clickedFace.opposite)
            .setValue(Props.powered, context.level.hasNeighborSignal(context.clickedPos))
    }

    override fun updateShape(state: BlockState, direction: Direction, neighborState: BlockState, world: IWorld, pos: BlockPos, neighborPos: BlockPos): BlockState? {
        return state.setValue(Props.input.getValue(direction), canConnect(neighborState, direction)).setValue(Props.powered, (world as? World)!!.hasNeighborSignal(pos))
    }

    private fun canConnect(other: BlockState, dirToOther: Direction): Boolean {
        return other.block in setOf(Blocks.HOPPER, this) &&
                listOf(
                    BlockStateProperties.FACING,
                    BlockStateProperties.HORIZONTAL_FACING,
                    BlockStateProperties.FACING_HOPPER
                ).any { facingProp ->
                    other.hasProperty(facingProp) && other.getValue(facingProp) == dirToOther.opposite
                }
    }


    override fun use(
        state: BlockState,
        world: World,
        pos: BlockPos,
        player: PlayerEntity,
        hand: Hand,
        blockHitPos: BlockRayTraceResult
    ): ActionResultType {
        if (player.getItemInHand(hand).item == this.asItem() && player.isShiftKeyDown)
            return ActionResultType.PASS

        if (!world.isClientSide) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity) {
                NetworkHooks.openGui(player as ServerPlayerEntity, entity as DuctBlockEntity?, pos)
            }
        }

        return ActionResultType.SUCCESS
    }

    override fun setPlacedBy(
        world: World,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        if (stack.hasCustomHoverName()) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is DuctBlockEntity) {
                blockEntity.customName = stack.hoverName
            }
        }
    }

    override fun onRemove(
        state1: BlockState,
        world: World,
        pos: BlockPos,
        state2: BlockState,
        someBool: Boolean
    ) {
        if (state1.block !== state2.block) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity) {
                InventoryHelper.dropContents(world, pos, entity)
                world.updateNeighbourForOutputSignal(pos, this)
            }
        }
        @Suppress("DEPRECATION")
        super.onRemove(state1, world, pos, state2, someBool)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(state: BlockState, world: World, pos: BlockPos): Int {
        return Container.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos))
    }

    override fun getRenderShape(state: BlockState): BlockRenderType {
        return BlockRenderType.MODEL
    }

    object Props {
        val output = BlockStateProperties.FACING
        val powered = BlockStateProperties.POWERED
        val input = mapOf(
            Direction.NORTH to BlockStateProperties.NORTH,
            Direction.EAST to BlockStateProperties.EAST,
            Direction.SOUTH to BlockStateProperties.SOUTH,
            Direction.WEST to BlockStateProperties.WEST,
            Direction.UP to BlockStateProperties.UP,
            Direction.DOWN to BlockStateProperties.DOWN
        )
    }

    private object Shapes {
        val coreCube = box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0)
        val outputCubes = mapOf(
            Direction.NORTH to box(6.0, 6.0, 0.0, 10.0, 10.0, 4.0), //Z
            Direction.EAST to box(12.0, 6.0, 6.0, 16.0, 10.0, 10.0), //X
            Direction.SOUTH to box(6.0, 6.0, 12.0, 10.0, 10.0, 16.0), //Z
            Direction.WEST to box(0.0, 6.0, 6.0, 4.0, 10.0, 10.0), //X
            Direction.DOWN to box(6.0, 0.0, 6.0, 10.0, 4.0, 10.0), //Y
            Direction.UP to box(6.0, 12.0, 6.0, 10.0, 16.0, 10.0) //Y NEW
        )
        val inputCubes = mapOf(
            Direction.NORTH to box(5.0, 5.0, 0.0, 11.0, 11.0, 4.0),
            Direction.EAST to box(12.0, 5.0, 5.0, 16.0, 11.0, 11.0),
            Direction.SOUTH to box(5.0, 5.0, 12.0, 11.0, 11.0, 16.0),
            Direction.WEST to box(0.0, 5.0, 5.0, 4.0, 11.0, 11.0),
            Direction.DOWN to box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0), //NEW
            Direction.UP to box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0)
        )
    }
}