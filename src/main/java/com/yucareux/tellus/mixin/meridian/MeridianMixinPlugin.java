package com.yucareux.tellus.mixin.meridian;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Disables every mixin in this config unless Meridian is actually installed, since they all target
 * Meridian's own classes.
 */
public final class MeridianMixinPlugin implements IMixinConfigPlugin {
	private static final String MERIDIAN_MOD_ID = "meridian";

	private boolean meridianPresent;

	@Override
	public void onLoad(final String mixinPackage) {
		this.meridianPresent = FabricLoader.getInstance().isModLoaded(MERIDIAN_MOD_ID);
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
		return this.meridianPresent;
	}

	@Override
	public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(
			final String targetClassName, final ClassNode targetClass, final String mixinClassName,
			final IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(
			final String targetClassName, final ClassNode targetClass, final String mixinClassName,
			final IMixinInfo mixinInfo) {
	}
}
