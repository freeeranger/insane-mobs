package com.freeranger.hardermobs.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.pathfinding.Path;

public class CustomCreeperSwell extends Goal{

    private final CreeperEntity swellingCreeper;
    private LivingEntity creeperAttackTarget;

    private boolean regularExplosion;

    public CustomCreeperSwell(CreeperEntity entitycreeperIn) {
        swellingCreeper = entitycreeperIn;
        regularExplosion = false;
    }

    public boolean shouldExecute() {
        LivingEntity livingentity = swellingCreeper.getAttackTarget();

        if(swellingCreeper.getCreeperState() > 0){
            return true;
        } else if(livingentity != null) {
            regularExplosion = swellingCreeper.getDistanceSq(livingentity) < 9.0D;
            return (regularExplosion || shouldBreakWall(livingentity));
        }
        return false;
    }

    private boolean shouldBreakWall(LivingEntity target){
        boolean breakWall = swellingCreeper.ticksExisted > 60 && !swellingCreeper.hasPath() && swellingCreeper.getDistance(target) < 10D;

        if (breakWall){
            Path pathToTarget = swellingCreeper.getNavigator().getPathToEntity(target, 0);
            if(pathToTarget != null && pathToTarget.getCurrentPathLength() > 6){
                swellingCreeper.getNavigator().setPath(pathToTarget, 1D);
                return false;
            }
            return true;
        }
        return false;
    }

    public void startExecuting() {
        swellingCreeper.getNavigator().clearPath();
        creeperAttackTarget = swellingCreeper.getAttackTarget();
    }

    public void resetTask() {
        creeperAttackTarget = null;
    }

    public void tick() {
        if (creeperAttackTarget == null) {
            swellingCreeper.setCreeperState(-1);
        } else if (regularExplosion && swellingCreeper.getDistanceSq(creeperAttackTarget) > 49.0D) {
            swellingCreeper.setCreeperState(-1);
        }   else {
            swellingCreeper.setCreeperState(1);
        }
    }
}
