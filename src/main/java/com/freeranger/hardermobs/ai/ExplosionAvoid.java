package com.freeranger.hardermobs.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class ExplosionAvoid extends RandomWalkingGoal {

    CreeperEntity entityToAvoid = null;

    public ExplosionAvoid(CreatureEntity creatureEntity, double retreatSpeed) {
        super(creatureEntity, retreatSpeed);
    }

    @Override
    public boolean shouldExecute() {
        boolean isDetonating = isCreeperDetonating();
        return this.findPosition() && isDetonating && !(creature instanceof CreeperEntity);
    }

    boolean isCreeperDetonating(){
        int range = 10;

        AxisAlignedBB detectionArea = new AxisAlignedBB(creature.getPosition().add(-range, -range, -range),
                creature.getPosition().add(range + 1, range + 1, range + 1));

        EntityPredicate targetEntitySelector = (new EntityPredicate()).setDistance(range);

        CreeperEntity entityToAvoid = creature.world.getClosestEntityWithinAABB(
                CreeperEntity.class,
                targetEntitySelector,
                creature,
                creature.getPosX(),
                creature.getPosY(),
                creature.getPosZ(),
                detectionArea);
        if(entityToAvoid != null){
            // TODO check if trying to ignite or ignited
            return true;
        }
        return false;
    }

    public boolean findPosition() {
        if(entityToAvoid != null) {
            Vector3d pos = this.getPosition();
            if (pos != null) {
                this.x = pos.x;
                this.y = pos.y;
                this.z = pos.z;
                System.out.println("1");
                return true;
            }
            System.out.println("2");
        }
        System.out.println("3");
        return false;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
    }

    @Override
    public void resetTask() {
        super.resetTask();
        entityToAvoid = null;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return creature.getDistance(entityToAvoid) < 12 && creature.isAlive() && entityToAvoid.isAlive();
    }

    @Override
    public void tick() {

    }

    @Override
    @Nullable
    protected Vector3d getPosition() {
        return RandomPositionGenerator.findRandomTargetBlockAwayFrom(
                creature,
                12,
                6,
                entityToAvoid.getPositionVec()
        );
    }
}