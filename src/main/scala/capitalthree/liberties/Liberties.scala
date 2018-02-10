package capitalthree.liberties

import net.minecraft.block.state.IBlockState
import net.minecraft.block.BlockSlab
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{World, WorldServer}
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.BlockEvent.PlaceEvent
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import net.minecraftforge.fml.common.Mod
import org.apache.logging.log4j.Logger

import scala.collection.mutable

@Mod(modid = "liberties", version = "1", name = "Liberties", modLanguage = "scala", acceptableRemoteVersions="*")
object Liberties {
  var logger:Logger = _

  @EventHandler
  def init(e: FMLPreInitializationEvent): Unit = {
    logger = e.getModLog

    val handler = new Object {
      @SubscribeEvent(priority = EventPriority.HIGHEST)
      def onPlace(event: PlaceEvent): Unit =  {
        val blockPlaced = event.getPlacedBlock
        if (isGoStone(blockPlaced)) {
          var captured = false

          neighbors(event.getPos).foreach { pos =>
            if (computeDanger(event.getWorld, blockPlaced, pos) == 1) {
              surroundedGroup(event.getWorld, pos).foreach { dead =>
                captured = true
                event.getWorld match {
                  case world: WorldServer => InvisiblePickaxeMan.destroy(world, event.getPlayer, dead)
                  case _ =>
                }
              }
            }
          }

          if (!captured && surroundedGroup(event.getWorld, event.getPos).isDefined)
            event.setCanceled(true)
        }
      }
    }

    MinecraftForge.EVENT_BUS.register(handler)
    MinecraftForge.EVENT_BUS.register(InvisiblePickaxeMan) // for capturing drops
  }

  def isGoStone(state: IBlockState): Boolean = state.getBlock match{
      // only bottom single slabs are go stones
    case slab: BlockSlab => !slab.isDouble && (state.getValue(BlockSlab.HALF) eq BlockSlab.EnumBlockHalf.BOTTOM)
    case _ => false
  }

  private def neighbors(pos: BlockPos): Seq[BlockPos] = Seq(
    pos.add(0, 0,  1), pos.add( 1, 0, 0),
    pos.add(0, 0, -1), pos.add(-1, 0, 0)
  )

  private def surroundedGroup(world: World, startPos: BlockPos): Option[List[BlockPos]] =
    surroundedGroup(world, world.getBlockState(startPos), startPos)

  private def surroundedGroup(world: World, startBlock: IBlockState, startPos: BlockPos): Option[List[BlockPos]] = {
    val group = mutable.Set(startPos)
    val explore = mutable.Stack(startPos)

    while (explore.nonEmpty) {
      neighbors(explore.pop()).filterNot(group).foreach{pos =>
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
  private def computeDanger(world: World, startBlock: IBlockState, pos: BlockPos): Int = {
    val state = world.getBlockState(pos)
    if (state.getBlockHardness(world, pos) == 0) return -1 // any insta-break block is counted as a liberty
    state match {
      case Blocks.AIR => -1
      case `startBlock` => 0
      case block => if (isGoStone(block)) 1 else 2
    }
  }
}
