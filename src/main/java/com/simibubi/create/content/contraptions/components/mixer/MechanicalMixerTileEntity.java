package com.simibubi.create.content.contraptions.components.mixer;

import java.util.Optional;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.contraptions.components.press.MechanicalPressTileEntity;
import com.simibubi.create.content.contraptions.processing.BasinOperatingTileEntity;
import com.simibubi.create.content.contraptions.processing.BasinTileEntity;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.advancement.ITriggerable;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class MechanicalMixerTileEntity extends BasinOperatingTileEntity {

	private static final Object shapelessOrMixingRecipesKey = new Object();

	public int runningTicks;
	public int processingTicks;
	public boolean running;

	public MechanicalMixerTileEntity(TileEntityType<? extends MechanicalMixerTileEntity> type) {
		super(type);
	}

	public float getRenderedHeadOffset(float partialTicks) {
		int localTick;
		float offset = 0;
		if (running) {
			if (runningTicks < 20) {
				localTick = runningTicks;
				float num = (localTick + partialTicks) / 20f;
				num = ((2 - MathHelper.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			} else if (runningTicks <= 20) {
				offset = 1;
			} else {
				localTick = 40 - runningTicks;
				float num = (localTick - partialTicks) / 20f;
				num = ((2 - MathHelper.cos((float) (num * Math.PI))) / 2);
				offset = num - .5f;
			}
		}
		return offset + 7 / 16f;
	}

	public float getRenderedHeadRotationSpeed(float partialTicks) {
		float speed = getSpeed();
		if (running) {
			if (runningTicks < 15) {
				return speed;
			}
			if (runningTicks <= 20) {
				return speed * 2;
			}
			return speed;
		}
		return speed / 2;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos).expand(0, -1.5, 0);
	}

	@Override
	protected void read(CompoundNBT compound, boolean clientPacket) {
		running = compound.getBoolean("Running");
		runningTicks = compound.getInt("Ticks");
		super.read(compound, clientPacket);
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		compound.putBoolean("Running", running);
		compound.putInt("Ticks", runningTicks);
		super.write(compound, clientPacket);
	}

	@Override
	public void tick() {
		super.tick();

		if (runningTicks >= 40) {
			running = false;
			runningTicks = 0;
			return;
		}

		float speed = Math.abs(getSpeed());
		if (running && world != null) {
			if (world.isRemote && runningTicks == 20)
				renderParticles();

			if (!world.isRemote && runningTicks == 20) {
				if (processingTicks < 0) {
					processingTicks = MathHelper.clamp((MathHelper.log2((int) (512 / speed))) * 15 + 1, 1, 512);
				} else {
					processingTicks--;
					if (processingTicks == 0) {
						runningTicks++;
						processingTicks = -1;
						applyBasinRecipe();
						sendData();
					}
				}
			}

			if (runningTicks != 20)
				runningTicks++;
		}

	}

	public void renderParticles() {
		Optional<BasinTileEntity> basin = getBasin();
		if (!basin.isPresent() || world == null)
			return;

		SmartInventory inputs = basin.get()
			.getInputInventory();
		for (int slot = 0; slot < inputs.getSlots(); slot++) {
			ItemStack stackInSlot = inputs.getStackInSlot(slot);
			if (stackInSlot.isEmpty())
				continue;

			ItemParticleData data = new ItemParticleData(ParticleTypes.ITEM, stackInSlot);
			float angle = world.rand.nextFloat() * 360;
			Vec3d offset = new Vec3d(0, 0, 0.25f);
			offset = VecHelper.rotate(offset, angle, Axis.Y);
			Vec3d target = VecHelper.rotate(offset, getSpeed() > 0 ? 25 : -25, Axis.Y)
				.add(0, .25f, 0);

			Vec3d center = offset.add(VecHelper.getCenterOf(pos));
			target = VecHelper.offsetRandomly(target.subtract(offset), world.rand, 1 / 128f);
			world.addParticle(data, center.x, center.y - 2, center.z, target.x, target.y, target.z);
		}
	}

	@Override
	protected <C extends IInventory> boolean matchStaticFilters(IRecipe<C> r) {
		return ((r.getSerializer() == IRecipeSerializer.CRAFTING_SHAPELESS
			&& AllConfigs.SERVER.recipes.allowShapelessInMixer.get()) || r.getType() == AllRecipeTypes.MIXING.type)
			&& !MechanicalPressTileEntity.canCompress(r.getIngredients());
	}

	@Override
	public void startProcessingBasin() {
		if (running && runningTicks <= 20)
			return;
		super.startProcessingBasin();
		running = true;
		runningTicks = 0;
	}

	@Override
	public boolean continueWithPreviousRecipe() {
		runningTicks = 20;
		return true;
	}

	@Override
	protected void onBasinRemoved() {
		if (!running)
			return;
		runningTicks = 40;
		running = false;
	}

	@Override
	protected Object getRecipeCacheKey() {
		return shapelessOrMixingRecipesKey;
	}

	@Override
	protected boolean isRunning() {
		return running;
	}
	
	@Override
	protected Optional<ITriggerable> getProcessedRecipeTrigger() {
		return Optional.of(AllTriggers.MIXER_MIX);
	}

}
