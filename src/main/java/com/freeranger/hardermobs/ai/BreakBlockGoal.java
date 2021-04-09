package com.freeranger.hardermobs.ai;

import com.freeranger.hardermobs.util.RayTraceUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectUtils;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BreakBlockGoal extends Goal {

    MobEntity entity;
    LivingEntity target;
    BlockPos pos = null;
    int progress = 0;

    public BreakBlockGoal(MobEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean shouldExecute() {
        if(entity != null && entity.getAttackTarget() == null && !isCreative(target) && entity.ticksExisted % 10 == 0){
            if(!isCreative(entity.getEntityWorld().getClosestPlayer(entity, 10)))
                target = entity.getEntityWorld().getClosestPlayer(entity, 10);

            entity.setAttackTarget(target);
        }

        if(target != null && isCreative(target)){
            return false;
        }

        pos = RayTraceUtil.getTargetBlockPos(entity, entity.world, 3, 0);
        if(RayTraceUtil.getTargetBlock(entity, entity.world, 3, 0).getMaterial() == Material.AIR
        && entity.getHeight() > 1){
            pos = RayTraceUtil.getTargetBlockPos(entity, entity.world, 3, -1);
        }

        entity.world.getBlockState(pos).getBlock();
        if(entity.world.getBlockState(pos).getMaterial() != Material.AIR && pos != null){
            BlockState state = entity.world.getBlockState(pos);
            if(entity.getHeldItemMainhand().canHarvestBlock(state) || entity.getHeldItemOffhand().canHarvestBlock(state) || !state.getRequiresTool()) {
                return true;
            }
        }
        return false;
    }

    boolean isCreative(LivingEntity entity){
        if(entity instanceof PlayerEntity){
            if(((PlayerEntity)entity).isCreative()){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return  target != null &&
                entity != null &&
                target.isAlive() &&
                entity.isAlive() &&
                pos != null &&
                entity.world.getBlockState(pos).getBlock() != Blocks.AIR &&
                entity.getDistance(target) > 1D &&
                entity.getNavigator().getPathToEntity(target, 1) == null &&
                !entity.canEntityBeSeen(target) &&
                !isCreative(target) &&
                !isCreative(entity.getAttackTarget());
    }

    @Override
    public void resetTask() {
        pos = null;
        if(isCreative(target) && entity.getAttackTarget() == target){
            entity.setAttackTarget(null);
        }
    }

    @Override
    public void tick() {
        // TODO breaking takes time and requires proper tools
        if(!isCreative(target)) {
            BlockState state = entity.world.getBlockState(pos);

            if (target != null && pos != null && entity != null) {
                if (isCreative(target)) {
                    return;
                }

                SoundEvent breakSound = state.getBlock().getSoundType(state, entity.world, pos, entity).getBreakSound();
                entity.world.playSound(null, pos, breakSound, SoundCategory.BLOCKS, 2f, .6f);

                float str = getBlockStrength(entity, entity.world, pos);
                str = str/(1+str*6) * (progress+1);

                if (str >= 1f) {
                    entity.world.destroyBlock(pos, true);
                    pos = null;
                    progress = 0;

                    if (entity.getNavigator().getPathToEntity(target, 0) != null) {
                        entity.getNavigator().setPath(entity.getNavigator().getPathToEntity(target, 0), 1D);
                        entity.setAttackTarget(target);
                    }
                } else {
                    progress++;
                    if(progress % 5 == 0) {
                        entity.swingArm(Hand.MAIN_HAND);
                        entity.getLookController().setLookPosition(pos.getX(), pos.getY(), pos.getZ(), 0f, 0f);
                        entity.world.sendBlockBreakProgress(entity.getEntityId(), pos, progress / 5);
                        System.out.println(str + " | " + progress + " -> " + (int)(str)*progress*10);
                    }
                }
            }
        }
    }

    public float getBlockStrength(LivingEntity entity, World world, BlockPos pos) {
        float hardness = world.getBlockState(pos).getBlockHardness(world, pos);
        if (hardness == -1) {
            return 0.0F;
        }
        ItemStack mainHand = entity.getHeldItemMainhand();
        ItemStack offHand = entity.getHeldItemOffhand();

        if (mainHand.canHarvestBlock(world.getBlockState(pos))) {
            return getBlockBreakSpeed(entity, pos, mainHand) / hardness / 30F;
        } else if (offHand.canHarvestBlock(world.getBlockState(pos))) {
            return getBlockBreakSpeed(entity, pos, offHand) / hardness / 30F;
        } else {
            return getBlockBreakSpeed(entity, pos, mainHand) / hardness / 100F;
        }
    }

    float getBlockBreakSpeed(LivingEntity entity, BlockPos pos, ItemStack usedStack){
        BlockState state = entity.world.getBlockState(pos);
        float f = usedStack.getDestroySpeed(state);

        if (f > 1.0F) {
            int i = EnchantmentHelper.getEfficiencyModifier(entity);
            if (i > 0 && !entity.getHeldItemMainhand().isEmpty()) {
                f += (float)(i * i + 1);
            }
        }

        if (EffectUtils.hasMiningSpeedup(entity)) {
            f *= 1.0F + (float)(EffectUtils.getMiningSpeedup(entity) + 1) * 0.2F;
        }

        if (entity.isPotionActive(Effects.MINING_FATIGUE)) {
            float f1;
            switch(entity.getActivePotionEffect(Effects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    f1 = 0.3F;
                    break;
                case 1:
                    f1 = 0.09F;
                    break;
                case 2:
                    f1 = 0.0027F;
                    break;
                case 3:
                default:
                    f1 = 8.1E-4F;
            }
            f *= f1;
        }

        if (entity.areEyesInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(entity)) {
            f /= 5.0F;
        }

        if (!entity.isOnGround()) {
            f /= 5.0F;
        }

        return f;
    }
}