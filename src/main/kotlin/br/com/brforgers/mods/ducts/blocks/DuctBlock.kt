package br.com.brforgers.mods.ducts.blocks

import br.com.brforgers.mods.ducts.blockentities.DuctBlockEntity
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.sound.BlockSoundGroup
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.*
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

class DuctBlock(
        private val screenHandler: ((Int, PlayerInventory, ScreenHandlerContext) -> ScreenHandler)?

) : BlockWithEntity(Settings.of(Material.METAL, Blocks.IRON_BLOCK.defaultMapColor).requiresTool().strength(3.0F, 4.8F).sounds(BlockSoundGroup.METAL).nonOpaque()) {
    private val shapeCache = hashMapOf<BlockState, VoxelShape>()

    init {
        defaultState = Props.input.values
                .fold(stateManager.defaultState) { state, prop -> state.with(prop, false) }
                .with(Props.output, Direction.DOWN)
                .with(Props.powered, false)
    }

    override fun appendProperties(propContainerBuilder: StateManager.Builder<Block, BlockState>) {
        propContainerBuilder.add(
                Props.output,
                Props.powered,
                *Props.input.values.toTypedArray()
        )
    }

    override fun createBlockEntity(pos: BlockPos?, state: BlockState?) = DuctBlockEntity(pos!!, state!!)

    override fun getOutlineShape(
            state: BlockState,
            view: BlockView,
            blockPos: BlockPos,
            verticalEntityPosition: ShapeContext
    ): VoxelShape {
        return shapeCache.computeIfAbsent(state) {
            val core = Shapes.coreCube
            val output = Shapes.outputCubes[state[Props.output]]
            val inputs = Props.input.keys
                    .filter { state[Props.input.getValue(it)] }
                    .map(Shapes.inputCubes::getValue)
                    .toTypedArray()
            VoxelShapes.union(core, output, *inputs)
        }
    }

    override fun rotate(state: BlockState, rotation: BlockRotation): BlockState {
        return state.with(
                Props.output,
                rotation.rotate(state.get(Props.output) as Direction)
        )
    }

    override fun mirror(state: BlockState, mirror: BlockMirror): BlockState {
        return state.with(
                Props.output,
                mirror.apply(state.get(Props.output) as Direction)
        )
    }

    override fun getPlacementState(context: ItemPlacementContext): BlockState {
        return Props.input.entries
                .map { (dir, prop) ->
                    prop to canConnect(
                            context.world.getBlockState(context.blockPos.offset(dir)),
                            dir
                    )
                }
                .fold(defaultState) { state, (prop, connect) -> state.with(prop, connect) }
                .with(Props.output, context.side.opposite)
                .with(Props.powered, context.world.isReceivingRedstonePower(context.blockPos))
    }

    override fun getStateForNeighborUpdate(state: BlockState, direction: Direction, neighborState: BlockState, world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState? {
        return state.with(Props.input[direction], canConnect(neighborState, direction)).with(Props.powered, (world as? World)!!.isReceivingRedstonePower(pos))
    }

    private fun canConnect(other: BlockState, dirToOther: Direction): Boolean {
        return other.block in setOf(Blocks.HOPPER, this) &&
                listOf(
                        Properties.FACING,
                        Properties.HORIZONTAL_FACING,
                        Properties.HOPPER_FACING
                ).any { facingProp ->
                    other.contains(facingProp) && other.get(facingProp) == dirToOther.opposite
                }
    }


    override fun onUse(
            state: BlockState,
            world: World,
            pos: BlockPos,
            player: PlayerEntity,
            hand: Hand,
            blockHitPos: BlockHitResult
    ): ActionResult {
        if (player.getStackInHand(hand).item == this.asItem() && player.isSneaking)
            return ActionResult.PASS

        if (!world.isClient) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity && screenHandler != null) {
                player.openHandledScreen(state.createScreenHandlerFactory(world, pos))
            }
        }

        return ActionResult.SUCCESS
    }

    override fun onPlaced(
            world: World,
            pos: BlockPos,
            state: BlockState,
            placer: LivingEntity?,
            stack: ItemStack
    ) {
        if (stack.hasCustomName()) {
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is DuctBlockEntity) {
                blockEntity.customName = stack.name
            }
        }
    }

    override fun onStateReplaced(
            state1: BlockState,
            world: World,
            pos: BlockPos,
            state2: BlockState,
            someBool: Boolean
    ) {
        if (state1.block !== state2.block) {
            val entity = world.getBlockEntity(pos)
            if (entity is DuctBlockEntity) {
                ItemScatterer.spawn(world, pos, entity)
                world.updateComparators(pos, this)
            }
        }
        @Suppress("DEPRECATION")
        super.onStateReplaced(state1, world, pos, state2, someBool)
    }

    override fun hasComparatorOutput(state: BlockState): Boolean {
        return true
    }

    override fun getComparatorOutput(state: BlockState, world: World, pos: BlockPos): Int {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos))
    }

    override fun getRenderType(state: BlockState): BlockRenderType {
        return BlockRenderType.MODEL
    }

    object Props {
        val output = Properties.FACING!!
        val powered = Properties.POWERED!!
        val input = mapOf(
                Direction.NORTH to Properties.NORTH!!,
                Direction.EAST to Properties.EAST!!,
                Direction.SOUTH to Properties.SOUTH!!,
                Direction.WEST to Properties.WEST!!,
                Direction.UP to Properties.UP!!,
                Direction.DOWN to Properties.DOWN!!
        )
    }

    private object Shapes {
        val coreCube = createCuboidShape(4.0, 4.0, 4.0, 12.0, 12.0, 12.0)!!
        val outputCubes = mapOf(
                Direction.NORTH to createCuboidShape(6.0, 6.0, 0.0, 10.0, 10.0, 4.0)!!, //Z
                Direction.EAST to createCuboidShape(12.0, 6.0, 6.0, 16.0, 10.0, 10.0)!!, //X
                Direction.SOUTH to createCuboidShape(6.0, 6.0, 12.0, 10.0, 10.0, 16.0)!!, //Z
                Direction.WEST to createCuboidShape(0.0, 6.0, 6.0, 4.0, 10.0, 10.0)!!, //X
                Direction.DOWN to createCuboidShape(6.0, 0.0, 6.0, 10.0, 4.0, 10.0)!!, //Y
                Direction.UP to createCuboidShape(6.0, 12.0, 6.0, 10.0, 16.0, 10.0)!! //Y NEW
        )
        val inputCubes = mapOf(
                Direction.NORTH to createCuboidShape(5.0, 5.0, 0.0, 11.0, 11.0, 4.0)!!,
                Direction.EAST to createCuboidShape(12.0, 5.0, 5.0, 16.0, 11.0, 11.0)!!,
                Direction.SOUTH to createCuboidShape(5.0, 5.0, 12.0, 11.0, 11.0, 16.0)!!,
                Direction.WEST to createCuboidShape(0.0, 5.0, 5.0, 4.0, 11.0, 11.0)!!,
                Direction.DOWN to createCuboidShape(5.0, 0.0, 5.0, 11.0, 4.0, 11.0)!!, //NEW
                Direction.UP to createCuboidShape(5.0, 12.0, 5.0, 11.0, 16.0, 11.0)!!
        )
    }

    override fun <T : BlockEntity?> getTicker(
        world: World?,
        state: BlockState?,
        type: BlockEntityType<T>?
    ): BlockEntityTicker<T>? {
        return checkType(type, DuctBlockEntity.type, DuctBlockEntity.instance::tick)
    }
}
