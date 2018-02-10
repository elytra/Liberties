package capitalthree.liberties

import java.util.UUID

import com.mojang.authlib.GameProfile
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.{Blocks, Items}
import net.minecraft.item.ItemStack
import net.minecraft.network._
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.{EventPriority, SubscribeEvent}

class InvisiblePickaxeMan(world: WorldServer)
    extends FakePlayer(world, InvisiblePickaxeMan.PROFILE)
{
  private val notTheHammer = new ItemStack(Items.DIAMOND_PICKAXE)

  {
    new NetHandlerPlayServer(null, FakeNetworkManager, this)
    inventory.mainInventory.set(0, notTheHammer)
    setHeldItem(EnumHand.MAIN_HAND, notTheHammer)
  }
}

object InvisiblePickaxeMan {
  val PROFILE = new GameProfile(UUID.randomUUID(), "Suffocation")
  val FIRESTATE = Blocks.FIRE.getDefaultState

  def destroy(world: WorldServer, player: EntityPlayer, blocks: Iterable[BlockPos]): Unit = {
    val man = new InvisiblePickaxeMan(world)
    saving = Some(player)
    blocks.foreach(man.interactionManager.tryHarvestBlock)
    saving = None
    blocks.foreach(world.setBlockState(_, FIRESTATE))
  }

  var saving: Option[EntityPlayer] = None
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def onSpawn(event: EntityJoinWorldEvent): Unit = saving match {
    case Some(player) =>
      event.getEntity match {
        case item: EntityItem =>
          item.setEntityInvulnerable(true)
          item.setPosition(player.posX, player.posY, player.posZ)
          item.setNoPickupDelay()
        case _ =>
      }
    case _ =>
  }
}

object FakeNetworkManager extends NetworkManager(EnumPacketDirection.CLIENTBOUND) {
  override def isChannelOpen(): Boolean = true
  override def sendPacket(packetIn: Packet[_]): Unit = {}
}