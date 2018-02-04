package capitalthree.liberties

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.BlockEvent.PlaceEvent
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.Logger

import scala.collection.mutable

@Mod(modid = "liberties", version = "1", name = "Liberties", modLanguage = "scala", acceptableRemoteVersions="*")
object Liberties {
  var logger:Logger = null
  val goStones: Set[Block] = Set(Blocks.WOODEN_SLAB, Blocks.STONE_SLAB, Blocks.STONE_SLAB2, Blocks.PURPUR_SLAB)

  @EventHandler
  def init(e: FMLPreInitializationEvent): Unit = {
    logger = e.getModLog

    val handler = new Object {
      @SubscribeEvent
      def onPlace(event: PlaceEvent): Unit = event.getWorld match {
        case world: WorldServer =>
          val blockPlaced = event.getPlacedBlock.getBlock
          if (goStones.contains(blockPlaced)) {
            neighbors(event.getPos).foreach(pos =>
              computeDanger(world, blockPlaced, pos) match {
                case 2 =>
                  surroundedGroup(world, pos).foreach { dead => InvisibleDiamondPickaxeMan.destroy(world, dead) }
              }
            )
            if (surroundedGroup(world, event.getPos).isDefined)
              event.setCanceled(true)
          }
        case _ =>
      }
    }

    MinecraftForge.EVENT_BUS.register(handler)
  }

  private def neighbors(pos: BlockPos): Seq[BlockPos] = Seq(
    pos.add(0, 0,  1), pos.add( 1, 0, 0),
    pos.add(0, 0, -1), pos.add(-1, 0, 0)
  )

  private def surroundedGroup(world: World, startPos: BlockPos): Option[List[BlockPos]] =
    surroundedGroup(world, world.getBlockState(startPos).getBlock, startPos)

  private def surroundedGroup(world: World, startBlock: Block, startPos: BlockPos): Option[List[BlockPos]] = {
    val group = mutable.Set(startPos)
    val explore = mutable.Stack(startPos)

    while (explore.nonEmpty) {
      neighbors(explore.pop()).filterNot(group)
        .foreach{pos =>
          computeDanger(world, startBlock, pos) match {
            case -1 => return None
            case 0 =>
              group.add(pos)
              explore.push(pos)
            case _ =>
          }
        }
    }

    Some(group.toList)
  }

  /**
    * -1 = liberty
    * 0 = same block
    * 1 = other stone
    * 2 = wall
    */
  private def computeDanger(world: World, startBlock: Block, pos: BlockPos): Int = {
    val state = world.getBlockState(pos)
    if (state.getBlockHardness(world, pos) == 0) return -1 // any insta-break block is counted as a liberty
    state.getBlock match {
      case Blocks.AIR => -1
      case `startBlock` => 0
      case block => if (goStones.contains(block)) 1 else 2
    }
  }
}
