package pl.asie.computronics.integration.forestry.nanomachines;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeHousing;
import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import pl.asie.computronics.integration.forestry.IntegrationForestry;
import pl.asie.lib.util.RayTracer;

import java.util.Collections;

/**
 * @author Vexatos
 */
public class SwarmProvider extends AbstractProvider {

	public SwarmProvider() {
		super("computronics:forestry-swarmprovider");
	}

	@Override
	protected Behavior readBehaviorFromNBT(EntityPlayer player, NBTTagCompound nbt) {
		//SwarmBehavior behavior = new SwarmBehavior(player);
		//behavior.readFromNBT(nbt);
		return new SwarmBehavior(player);
	}

	@Override
	protected void writeBehaviorToNBT(Behavior behavior, NBTTagCompound nbt) {
		/*if(behavior instanceof SwarmBehavior) {
			((SwarmBehavior) behavior).writeToNBT(nbt);
		}*/
	}

	@Override
	public Iterable<Behavior> createBehaviors(EntityPlayer player) {
		return Collections.<Behavior>singletonList(new SwarmBehavior(player));
	}

	private void findTarget(EntityPlayer player) {
		SwarmBehavior behavior = getSwarmBehavior(player);
		if(behavior != null) {
			if(behavior.entity != null) {
				RayTracer.instance().fire(player, 30);
				MovingObjectPosition target = RayTracer.instance().getTarget();
				if((target != null) && (target.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY)) {
					Entity entity = target.entityHit;
					if(entity != null && entity instanceof EntityLivingBase && entity != behavior.entity) {
						behavior.entity.setAttackTarget((EntityLivingBase) entity);
						swingItem(player);
					}
				} else if(behavior.entity.getAttackTarget() != null) {
					behavior.entity.setAttackTarget(null);
					swingItem(player);
				}
			} else if(player.capabilities != null && player.capabilities.isCreativeMode) {
				behavior.spawnNewEntity(player.posX, player.posY + 2f, player.posZ);
				swingItem(player);
			}
		} /*else {
			Controller controller = Nanomachines.installController(player);
			boolean win = false;
			while(!win) {
				Iterator<Behavior> behaviorSet = ((ControllerImpl) controller).configuration().behaviorMap().keySet().iterator();
				while(behaviorSet.hasNext()) {
					Behavior next = behaviorSet.next();
					if(next instanceof SwarmBehavior) {
						win = true;
						break;
					}
				}
				controller.reconfigure();
			}
		}*/
	}

	private void makeSwarm(PlayerInteractEvent e, EntityPlayer player, IBeeHousing tile) {
		if(tile.getBeekeepingLogic() != null && tile.getBeeInventory() != null && tile.getBeekeepingLogic().canDoBeeFX()) {
			IBee member = BeeManager.beeRoot.getMember(tile.getBeeInventory().getQueen());
			if(member != null) {
				SwarmBehavior behavior = getSwarmBehavior(player);
				if(behavior != null) {
					if(behavior.entity != null) {
						behavior.entity.setDead();
					}
					Controller controller = Nanomachines.getController(player);
					if(controller != null) {
						controller.changeBuffer(-10);
					}
					behavior.spawnNewEntity(e.x + 0.5, e.y + 0.5, e.z + 0.5,
						member.getGenome().getPrimary().getIconColour(0),
						member.getGenome().getTolerantFlyer());
					swingItem(player);
				}
			}
		}
	}

	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent e) {
		EntityPlayer player = e.entityPlayer;
		if(player != null && !player.worldObj.isRemote) {
			ItemStack heldItem = player.getHeldItem();
			if(heldItem != null && heldItem.getItem() == IntegrationForestry.itemStickImpregnated) {
				if(e.action == Action.RIGHT_CLICK_AIR) {
					findTarget(player);
				} else if(e.action == Action.RIGHT_CLICK_BLOCK) {
					if(player.isSneaking() && e.world.blockExists(e.x, e.y, e.z)) {
						TileEntity te = e.world.getTileEntity(e.x, e.y, e.z);
						if(te instanceof IBeeHousing) {
							makeSwarm(e, player, ((IBeeHousing) te));
						} else {
							findTarget(player);
						}
					} else {
						findTarget(player);
					}
				}
			} else if(heldItem == null && e.action == Action.RIGHT_CLICK_BLOCK) {
				if(player.isSneaking() && e.world.blockExists(e.x, e.y, e.z)) {
					TileEntity te = e.world.getTileEntity(e.x, e.y, e.z);
					if(te instanceof IBeeHousing) {
						makeSwarm(e, player, ((IBeeHousing) te));
					}
				}
			}
		}
	}

	public static void swingItem(EntityPlayer player) {
		player.swingItem();
		if(player instanceof EntityPlayerMP && ((EntityPlayerMP) player).playerNetServerHandler != null) {
			((EntityPlayerMP) player).playerNetServerHandler.sendPacket(new S0BPacketAnimation(player, 0));
		}
	}

	/*@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent e) {
		SwarmBehavior behavior = getSwarmBehavior(e.player);
		if(behavior != null && !e.player.worldObj.isRemote && e.phase == TickEvent.PlayerTickEvent.Phase.START) {
			behavior.update();
		}
	}*/

	//private final HashMap<String, SwarmBehavior> behaviors = new HashMap<String, SwarmBehavior>();

	private SwarmBehavior getSwarmBehavior(EntityPlayer player) {
		Controller controller = Nanomachines.getController(player);
		if(controller != null) {
			Iterable<Behavior> behaviors = controller.getActiveBehaviors();
			for(Behavior behavior : behaviors) {
				if(behavior instanceof SwarmBehavior) {
					return (SwarmBehavior) behavior;
				}
			}
		}
		return null;

		/*SwarmBehavior behavior = behaviors.get(player.getCommandSenderName());
		if(behavior == null) {
			behavior = new SwarmBehavior(player);
			behavior.onEnable();
			behaviors.put(player.getCommandSenderName(), behavior);
		}
		return behavior;*/
	}

}
