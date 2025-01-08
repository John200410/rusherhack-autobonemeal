package org.rusherhack.bonemeal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.render.EventRender3D;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.render.IRenderer3D;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.client.api.utils.RotationUtils;
import org.rusherhack.client.api.utils.WorldUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.NullSetting;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.utils.ColorUtils;
import org.rusherhack.core.utils.Timer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author john@rusherhack.org 1/8/2025
 */
public class AutoBoneMealModule extends ToggleableModule {
	
	/**
	 * Settings
	 */
	private final NumberSetting<Integer> delay = new NumberSetting<>("Delay", 1, 0, 20);
	
	private final NullSetting crops = new NullSetting("Crops");
	private final BooleanSetting saplings = new BooleanSetting("Saplings", true);
	private final BooleanSetting wheat = new BooleanSetting("Wheat", true);
	private final BooleanSetting carrots = new BooleanSetting("Carrots", true);
	private final BooleanSetting potatoes = new BooleanSetting("Potatoes", true);
	
	private final BooleanSetting render = new BooleanSetting("Render", true);
	private final ColorSetting color = new ColorSetting("Color", ColorUtils.transparency(Color.RED, 0.25f)).setThemeSync(true);
	
	/**
	 * Variables
	 */
	private final List<BlockPos> targetBlocks = new ArrayList<>();
	private final Timer delayTimer = new Timer();
	private BlockPos currentTarget = null;
	
	public AutoBoneMealModule() {
		super("AutoBoneMeal", "Automatically use bonemeal on crops", ModuleCategory.PLAYER);
		
		this.crops.addSubSettings(saplings, wheat, carrots, potatoes);
		this.render.addSubSettings(color);
		this.registerSettings(delay, crops, render);
	}
	
	@Subscribe
	private void onUpdate(EventUpdate event) {
		
		this.targetBlocks.clear();
		this.targetBlocks.addAll(WorldUtils.getSphere(mc.player.blockPosition(), 6, this::canUseBonemeal));
		
		//check if holding bonemeal
		if(!mc.player.getMainHandItem().is(Items.BONE_MEAL)) {
			this.currentTarget = null;
			return;
		}
		
		this.currentTarget = this.getBestTarget();
		
		if(this.currentTarget == null) {
			return;
		}
		
		RusherHackAPI.getRotationManager().updateRotation(this.currentTarget);
		
		final BlockHitResult hitResult = RusherHackAPI.getRotationManager().getLookRaycast(this.currentTarget);
		if(hitResult == null || hitResult.getType().equals(BlockHitResult.Type.MISS)) {
			return;
		}
		
		if(this.delayTimer.ticksPassed(this.delay.getValue())) {
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
			this.delayTimer.reset();
		}
		
	}
	
	@Subscribe
	private void onRender3D(EventRender3D event) {
		
		if(!this.render.getValue() || this.targetBlocks.isEmpty()) {
			return;
		}
		
		final IRenderer3D renderer = event.getRenderer();
		
		renderer.begin(event.getMatrixStack());
		
		for(BlockPos targetBlock : this.targetBlocks) {
			
			renderer.drawBox(targetBlock, targetBlock == this.currentTarget, true, this.color.getValueRGB());
			
		}
		
		renderer.end();
	}
	
	private BlockPos getBestTarget() {
		BlockPos bestTarget = null;
		double dist = 999d;
		
		for(BlockPos targetBlock : this.targetBlocks) {
			
			final double d = mc.player.distanceToSqr(targetBlock.getCenter());
			
			if(d < dist) {
				bestTarget = targetBlock;
				dist = d;
			}
		}
		
		return bestTarget;
	}
	
	private boolean canUseBonemeal(BlockPos pos) {
		final BlockState blockState = mc.level.getBlockState(pos);
		final Block block = blockState.getBlock();
		
		if(!(block instanceof BonemealableBlock bonemealable)) {
			return false;
		}
		
		if(!bonemealable.isValidBonemealTarget(mc.level, pos, blockState)) {
			return false;
		}
		
		if(block instanceof SaplingBlock) {
			return this.saplings.getValue();
		}
		
		if(block instanceof CropBlock crop) {
			
			if(crop.isMaxAge(blockState)) {
				return false;
			}
			
			if(block.equals(Blocks.WHEAT)) {
				return this.wheat.getValue();
			}
			
			if(block instanceof CarrotBlock) {
				return this.carrots.getValue();
			}
			
			if(block instanceof PotatoBlock) {
				return this.potatoes.getValue();
			}
		}
		
		return false;
	}
	
}
