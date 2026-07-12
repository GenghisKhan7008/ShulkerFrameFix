package com.example.shulkerframefix.mixin;

import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameEntity.class)
public class ItemFrameEntityMixin {

    @Redirect(
        method = "canStayAttached",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;isSpaceEmpty(Lnet/minecraft/entity/Entity;)Z"
        )
    )
    private boolean ignoreShulkerLidOverlap(World world, net.minecraft.entity.Entity entity) {
        // Get the item frame's bounding box
        Box frameBox = entity.getBoundingBox();

        // Check if the only thing overlapping is a shulker box lid
        // by seeing if space is empty when we exclude shulker box block shapes
        boolean hasNonShulkerOverlap = world.getBlockCollisions(entity, frameBox)
            .findAny()
            .map(shape -> {
                // Check each block position that might overlap
                net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(
                    (int) Math.floor(frameBox.getCenter().x),
                    (int) Math.floor(frameBox.getCenter().y),
                    (int) Math.floor(frameBox.getCenter().z)
                );
                // Check surrounding positions for shulker boxes
                for (net.minecraft.util.math.BlockPos checkPos : net.minecraft.util.math.BlockPos.iterateOutwards(pos, 1, 1, 1)) {
                    net.minecraft.block.BlockState state = world.getBlockState(checkPos);
                    if (state.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock) {
                        net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(checkPos);
                        if (be instanceof ShulkerBoxBlockEntity shulker && shulker.getAnimationProgress(0.0F) > 0.0F) {
                            return false; // It's a shulker overlapping, not a solid block
                        }
                    }
                }
                return true; // Real block overlap
            })
            .orElse(false);

        return !hasNonShulkerOverlap;
    }
}
