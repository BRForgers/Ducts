package br.com.brforgers.mods.ducts.blocks

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.*
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Material
import net.minecraft.world.level.material.MaterialColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraftforge.network.NetworkHooks


class DuctBlock : BaseEntityBlock(Properties.of(Material.METAL, MaterialColor.COLOR_GRAY).strength(1.0F, 6.0F).sound(SoundType.METAL).noOcclusion()) {
    private val shapeCache = hashMapOf<BlockState, VoxelShape>()

    init {
        registerDefaultState(Props.input.values
                .fold(stateDefinition.any()) { state, prop -> state.setValue(prop, false) }
                .setValue(Props.output, Direction.DOWN)
                .setValue(Props.powered, false))
    }

    override fun createBlockStateDefinition(propContainerBuilder: StateDefinition.Builder<Block, BlockState>) {
        propContainerBuilder.add(
            Props.output,
            Props.powered,
            *Props.input.values.toTypedArray()
        )
    }

    override fun newBlockEntity(pos: BlockPos?, state: BlockState?) = DuctBlockEntity(pos!!, state!!)

    override fun getShape(
        state: BlockState,
        view: BlockGetter,
        blockPos: BlockPos,
        verticalEntityPosition: CollisionContext
    ): VoxelShape {
        return shapeCache.computeIfAbsent(state) {
            val core = Shapes.coreCube
            val output = Shapes.outputCubes[state.getValue(Props.output)]
            val inputs = Props.input.keys
                .filter { state.getValue(Props.input.getValue(it)) }
                .map(Shapes.inputCubes::getValue)
                .toTypedArray()
            net.minecraft.world.phys.shapes.Shapes.or(core, output, *inputs)
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

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
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

    override fun updateShape(state: BlockState, direction: Direction, neighborState: BlockState, world: LevelAccessor, pos: BlockPos, neighborPos: BlockPos): BlockState? {
        return state.setValue(Props.input.getValue(direction), canConnect(neighborState, direction)).setValue(Props.powered, (world as? Level)!!.hasNeighborSignal(pos))
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
        world: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        blockHitPos: BlockHitResult
    ): InteractionResult {
        if (player.getItemInHand(hand).item == this.asItem() && player.isShiftKeyDown)
            return InteractionResult.PASS

        if (!world.isClientSide) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity) {
                NetworkHooks.openGui(player as ServerPlayer, entity as DuctBlockEntity?, pos)
            }
        }

        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(
        world: Level,
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
        world: Level,
        pos: BlockPos,
        state2: BlockState,
        someBool: Boolean
    ) {
        if (state1.block !== state2.block) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity) {
                Containers.dropContents(world, pos, entity)
                world.updateNeighbourForOutputSignal(pos, this)
            }
        }
        @Suppress("DEPRECATION")
        super.onRemove(state1, world, pos, state2, someBool)
    }

    override fun hasAnalogOutputSignal(state: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(state: BlockState, world: Level, pos: BlockPos): Int {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(world.getBlockEntity(pos))
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
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

    override fun <T : BlockEntity?> getTicker(
        world: Level?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ): BlockEntityTicker<T>? {
        return if (world!!.isClientSide) null else createTickerHelper(type, DuctBlockEntity.type, DuctBlockEntity.instance::tick)
    }
}