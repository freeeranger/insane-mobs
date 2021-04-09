package com.freeranger.hardermobs.util;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class RayTraceUtil {

    public static BlockState getTargetBlock(LivingEntity player,World world, int maxdistance, int eyeOffset){
        BlockPos blockpos = getTargetBlockPos(player,world, maxdistance, eyeOffset);
        if(blockpos == null) {
            return null;
        }
        BlockState blockstate= world.getBlockState(blockpos);
        return blockstate;
    }

    public static BlockPos getTargetBlockPos(LivingEntity player,World world, int maxdistance, int eyeOffset){
        BlockRayTraceResult rayTraceResult = getTargetBlockResult(player,world, maxdistance, eyeOffset);
        if(rayTraceResult!=null){
            return new BlockPos(rayTraceResult.getHitVec().getX(),rayTraceResult.getHitVec().getY(),rayTraceResult.getHitVec().getZ());
        }
        return null;
    }

    public static BlockRayTraceResult getTargetBlockResult(LivingEntity player, World world, int maxdistance, int eyeOffset){
        Vector3d vec = player.getPositionVec();
        Vector3d vec3 = new Vector3d(vec.x,vec.y+player.getEyeHeight()+eyeOffset,vec.z);
        Vector3d vec3a = player.getLook(1.0F);
        Vector3d vec3b = vec3.add(vec3a.getX() * maxdistance, vec3a.getY()*  maxdistance, vec3a.getZ()*  maxdistance);

        BlockRayTraceResult rayTraceResult = world.rayTraceBlocks(new RayTraceContext(vec3, vec3b,RayTraceContext.BlockMode.OUTLINE,  RayTraceContext.FluidMode.ANY, player));

        if(rayTraceResult!=null)
        {
            double xm=rayTraceResult.getHitVec().getX();
            double ym=rayTraceResult.getHitVec().getY();
            double zm=rayTraceResult.getHitVec().getZ();


            //playerIn.sendMessage(new StringTextComponent(rayTraceResult.getFace().toString()));
            if(rayTraceResult.getFace() == Direction.SOUTH) {
                zm--;
            }
            if(rayTraceResult.getFace() == Direction.EAST) {
                xm--;
            }
            if(rayTraceResult.getFace() == Direction.UP) {
                ym--;
            }

            return new BlockRayTraceResult(rayTraceResult.getHitVec(), rayTraceResult.getFace(), new BlockPos(xm,ym,zm), false);
        }
        return null;
    }

}
