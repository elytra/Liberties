package capitalthree.liberties

import java.util.UUID

import com.mojang.authlib.GameProfile
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.network._
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayer

class InvisibleDiamondPickaxeMan(world: WorldServer)
    extends FakePlayer(world, InvisibleDiamondPickaxeMan.PROFILE)
{
  {new NetHandlerPlayServer(null, FakeNetworkManager, this)}
  val notTheHammer = new ItemStack(Items.DIAMOND_PICKAXE)

  override def getHeldItem(hand: EnumHand): ItemStack = hand match {
    case EnumHand.MAIN_HAND => getHeldItemMainhand
    case _ => super.getHeldItem(hand)
  }

  override def getHeldItemMainhand: ItemStack = {
    notTheHammer.stackSize = 1; notTheHammer.setItemDamage(0)
    notTheHammer
  }
}

object InvisibleDiamondPickaxeMan {
  val PROFILE = new GameProfile(UUID.randomUUID(), "Suffocation")

  def destroy(world: WorldServer, blocks: Iterable[BlockPos]): Unit = {
    val man = new InvisibleDiamondPickaxeMan(world)
    blocks.foreach(man.interactionManager.tryHarvestBlock)
  }
}

object FakeNetworkManager extends NetworkManager(EnumPacketDirection.CLIENTBOUND) {
  override def isChannelOpen(): Boolean = true
  override def sendPacket(packetIn: Packet[_]): Unit = {}
}