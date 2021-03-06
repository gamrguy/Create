package com.simibubi.create.compat.jei.category;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.compat.jei.category.animations.AnimatedMixer;
import com.simibubi.create.content.contraptions.processing.BasinRecipe;
import com.simibubi.create.content.contraptions.processing.HeatCondition;

public class MixingCategory extends BasinCategory {

	private final AnimatedMixer mixer = new AnimatedMixer();
	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

	public MixingCategory(boolean isForShapelessCraftingOnly) {
		super(
				isForShapelessCraftingOnly ? "shapeless_mixing" : "mixing",
				doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), AllBlocks.BASIN.get()),
				emptyBackground(177, 103));
	}

	@Override
	public void draw(BasinRecipe recipe, double mouseX, double mouseY) {
		super.draw(recipe, mouseX, mouseY);
		HeatCondition requiredHeat = recipe.getRequiredHeat();
		if (requiredHeat != HeatCondition.NONE)
			heater.withHeat(requiredHeat.visualizeAsBlazeBurner())
					.draw(getBackground().getWidth() / 2 + 3, 55);
		mixer.draw(getBackground().getWidth() / 2 + 3, 34);
	}

}
