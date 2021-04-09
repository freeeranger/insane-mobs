package com.freeranger.hardermobs.ai;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;

public class CustomNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
    protected final Class<T> targetClass;
    protected final int targetChance;
    protected LivingEntity nearestTarget;
    /** This filter is applied to the Entity search. Only matching entities will be targeted. */
    protected EntityPredicate targetEntitySelector;

    public CustomNearestAttackableTargetGoal(MobEntity goalOwnerIn, Class<T> targetClassIn, boolean checkSight) {
        this(goalOwnerIn, targetClassIn, checkSight, false);
    }

    public CustomNearestAttackableTargetGoal(MobEntity goalOwnerIn, Class<T> targetClassIn, boolean checkSight, boolean nearbyOnlyIn) {
        this(goalOwnerIn, targetClassIn, 10, checkSight, nearbyOnlyIn, null);
    }

    public CustomNearestAttackableTargetGoal(MobEntity goalOwnerIn, Class<T> targetClassIn, int targetChanceIn, boolean checkSight, boolean nearbyOnlyIn, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(goalOwnerIn, checkSight, nearbyOnlyIn);
        this.targetClass = targetClassIn;
        this.targetChance = targetChanceIn;
        this.setMutexFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetEntitySelector = (new EntityPredicate()).setDistance(this.getTargetDistance()).setCustomPredicate(targetPredicate).setLineOfSiteRequired();
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
     * method as well.
     */
    public boolean shouldExecute() {
        if (this.targetChance > 0 && this.goalOwner.getRNG().nextInt(this.targetChance) != 0) {
            return false;
        } else {
            this.findNearestTarget();
            return this.nearestTarget != null;
        }
    }

    protected AxisAlignedBB getTargetableArea(double targetDistance) {
        return this.goalOwner.getBoundingBox().grow(targetDistance, 4.0D, targetDistance);
    }

    protected void findNearestTarget() {
        if (this.targetClass != PlayerEntity.class && this.targetClass != ServerPlayerEntity.class) {
            this.nearestTarget = this.goalOwner.world.func_225318_b(this.targetClass, this.targetEntitySelector, this.goalOwner, this.goalOwner.getPosX(), this.goalOwner.getPosYEye(), this.goalOwner.getPosZ(), this.getTargetableArea(this.getTargetDistance()));
        } else {
            this.nearestTarget = this.goalOwner.world.getClosestPlayer(this.targetEntitySelector, this.goalOwner, this.goalOwner.getPosX(), this.goalOwner.getPosYEye(), this.goalOwner.getPosZ());
        }
    }

    public void resetTask() {
        this.goalOwner.setAttackTarget(null);
        nearestTarget = null;
        this.target = null;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting() {
        this.goalOwner.setAttackTarget(this.nearestTarget);
        super.startExecuting();
    }

    public void setNearestTarget(@Nullable LivingEntity target) {
        this.nearestTarget = target;
    }
}