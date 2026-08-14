package org.m9mx.cactus.glowberry.util.waila;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Predicate;

/**
 * Everything a Waila provider needs to decide what to show. Providers should
 * return early when the current target isn't relevant to them.
 */
public class WailaInfoContext {
    public final Minecraft mc;
    public final HitResult hit;
    public final boolean inEditor;
    private final Predicate<WailaFeature> enabled;

    public WailaInfoContext(Minecraft mc, HitResult hit, boolean inEditor, Predicate<WailaFeature> enabled) {
        this.mc = mc;
        this.hit = hit;
        this.inEditor = inEditor;
        this.enabled = enabled;
    }

    /** Whether the given feature is toggled on in the HUD settings. */
    public boolean enabled(WailaFeature feature) {
        return enabled.test(feature);
    }

    public boolean isEntity() {
        return hit != null && hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult;
    }

    public Entity entity() {
        return hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
    }

    public boolean isBlock() {
        return hit != null && hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult && mc.level != null;
    }

    public BlockPos blockPos() {
        return hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
    }

    public BlockState blockState() {
        BlockPos pos = blockPos();
        return pos != null ? mc.level.getBlockState(pos) : null;
    }

    public BlockEntity blockEntity() {
        BlockPos pos = blockPos();
        return pos != null ? mc.level.getBlockEntity(pos) : null;
    }
}
