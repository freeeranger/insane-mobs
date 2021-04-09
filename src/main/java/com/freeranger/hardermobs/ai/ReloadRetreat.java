package com.freeranger.hardermobs.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.entity.monster.PillagerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class ReloadRetreat extends RandomWalkingGoal {

    public ReloadRetreat(CreatureEntity creatureEntity, double retreatSpeed) {
        super(creatureEntity, retreatSpeed);
    }

    @Override
    public boolean shouldExecute() {
        return  creature.isHandActive() &&
                creature.getActiveItemStack().getItem() instanceof CrossbowItem &&
                creature.getAttackTarget() != null &&
                this.findPosition() &&
                !CrossbowItem.isCharged(creature.getActiveItemStack());
    }

    public boolean findPosition() {
        Vector3d pos = this.getPosition();
        if (pos != null){
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;
            return true;
        }
        return false;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        if (creature.getAttackTarget() != null) {
            creature.getLookController().setLookPositionWithEntity(creature.getAttackTarget(), 30f, 30f);
            creature.faceEntity(creature.getAttackTarget(), 30f, 30f);
        }
        ((PillagerEntity) creature).setCharging(true);
        this.creature.setActiveHand(ProjectileHelper.getHandWith(this.creature, Items.CROSSBOW));

    }

    @Override
    public void resetTask() {
        super.resetTask();
        ((PillagerEntity) creature).setCharging(false);
        this.creature.stopActiveHand();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return  creature.isHandActive() &&
                !CrossbowItem.isCharged(creature.getActiveItemStack()) &&
                creature.getActiveItemStack().getItem() instanceof CrossbowItem &&
                super.shouldContinueExecuting();
    }

    @Override
    public void tick() {
        int maxUses = this.creature.getItemInUseMaxCount();
        ItemStack activeItem = this.creature.getActiveItemStack();
        if (maxUses >= CrossbowItem.getChargeTime(activeItem)) {
            this.creature.stopActiveHand();
            ((PillagerEntity) creature).setCharging(false);
        }
    }

    @Override
    @Nullable
    protected Vector3d getPosition() {
        return RandomPositionGenerator.findRandomTargetBlockAwayFrom(
                this.creature,
                20,
                6,
                this.creature.getAttackTarget().getPositionVec()
        );
    }
}