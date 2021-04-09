package com.freeranger.hardermobs;

import com.freeranger.hardermobs.ai.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.SleepingLocationCheckEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.Set;

@Mod(HarderMobs.MOD_ID)
public class HarderMobs {
    boolean isDebugging = true;

    public static final String MOD_ID = "hardermobs";

    AttributeModifier velocityModifier = new AttributeModifier("velocity_modifier", 0.13f,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    AttributeModifier healthModifier = new AttributeModifier("health_modifier", 0.6f,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    AttributeModifier damageModifier = new AttributeModifier("damage_modifier", 0.4f,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    AttributeModifier fireSpeedModifier = new AttributeModifier("fire_speed_modifier", 0.4f,
            AttributeModifier.Operation.MULTIPLY_TOTAL);
    AttributeModifier knockbackResistance = new AttributeModifier("knockback_resistance", 1.5f,
            AttributeModifier.Operation.MULTIPLY_TOTAL);

    public HarderMobs() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityDamaged);
    }

    private void setup(final FMLCommonSetupEvent event){

    }
    @SubscribeEvent
    public void onEntitySpawn(EntityJoinWorldEvent event){
        if(event.getWorld().isRemote())
            return;

        if(event.getEntity() instanceof MonsterEntity){
            ((MonsterEntity) event.getEntity()).getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(32);

            Set<PrioritizedGoal> goals = getMonsterGoals((MonsterEntity)event.getEntity());
            Set<PrioritizedGoal> targets = getMonsterTargets((MonsterEntity)event.getEntity());
            if(goals != null) {
                ((MonsterEntity) event.getEntity()).goalSelector.removeGoal(((PrioritizedGoal) targets.toArray()[0]).getGoal());
                ((MonsterEntity) event.getEntity()).goalSelector.addGoal(1,
                        new CustomNearestAttackableTargetGoal((MonsterEntity) event.getEntity(), PlayerEntity.class, false));
                ((MonsterEntity) event.getEntity()).goalSelector.addGoal(1,
                        new CustomNearestAttackableTargetGoal((MonsterEntity) event.getEntity(), IronGolemEntity.class, false));

                ((MonsterEntity) event.getEntity()).goalSelector.addGoal(10, new ExplosionAvoid((MonsterEntity) event.getEntity(), 1.1D));

                if(!(event.getEntity() instanceof ZombieEntity)) {
                    ((MonsterEntity) event.getEntity()).goalSelector.addGoal(1,
                            new CustomNearestAttackableTargetGoal((MonsterEntity) event.getEntity(), AbstractVillagerEntity.class, false));
                }
            }
        }

        if(event.getEntity() instanceof ZombieEntity){
            ((ZombieEntity) event.getEntity()).goalSelector.addGoal(1, new BreakBlockGoal((ZombieEntity)event.getEntity()));
        }
        if(event.getEntity() instanceof PillagerEntity){
            ((PillagerEntity) event.getEntity()).goalSelector.addGoal(1, new ReloadRetreat((PillagerEntity) event.getEntity(), .9D));
        }

        if(event.getEntity() instanceof CreeperEntity){
            Set<PrioritizedGoal> goals = getCreeperGoals((CreeperEntity)event.getEntity());
            if(goals != null) {
                ((CreeperEntity) event.getEntity()).goalSelector.removeGoal(((PrioritizedGoal) goals.toArray()[1]).getGoal());
                ((CreeperEntity) event.getEntity()).goalSelector.addGoal(2,
                        new CustomCreeperSwell((CreeperEntity) event.getEntity()));
            }
        }

        modifyAttributes(event);
        equipArmor();
    }

    Set<PrioritizedGoal> getCreeperGoals(CreeperEntity entity) {
        Field f_goals = null;
        try {
            f_goals = GoalSelector.class.getDeclaredField(isDebugging ? "goals" : "field_220892_d");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if(f_goals == null) return null;
        f_goals.setAccessible(true);

        Set<PrioritizedGoal> goals = null;
        try {
            goals = (Set<PrioritizedGoal>)f_goals.get(entity.goalSelector);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return goals;
    }

    Set<PrioritizedGoal> getMonsterGoals(MonsterEntity entity) {
        Field f_goals = null;
        try {
            f_goals = GoalSelector.class.getDeclaredField(isDebugging ? "goals" : "field_220892_d");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if(f_goals == null) return null;
        f_goals.setAccessible(true);

        Set<PrioritizedGoal> goals = null;
        try {
            goals = (Set<PrioritizedGoal>)f_goals.get(entity.goalSelector);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return goals;
    }

    Set<PrioritizedGoal> getMonsterTargets(MonsterEntity entity) {
        Field f_goals = null;
        try {
            f_goals = GoalSelector.class.getDeclaredField(isDebugging ? "goals" : "field_220892_d");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if(f_goals == null) return null;
        f_goals.setAccessible(true);

        Set<PrioritizedGoal> targets = null;
        try {
            targets = (Set<PrioritizedGoal>)f_goals.get(entity.targetSelector);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return targets;
    }

    void modifyAttributes(EntityJoinWorldEvent event){
        if(!(event.getEntity() instanceof LivingEntity))
            return;

        if(event.getEntity() instanceof MonsterEntity && !(event.getEntity() instanceof WitherEntity)) {
            if(((LivingEntity)event.getEntity()).getAttribute(Attributes.MOVEMENT_SPEED) != null){ // TODO DONT MOD BABIES
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.MOVEMENT_SPEED).removeAllModifiers();
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.MOVEMENT_SPEED).applyPersistentModifier(velocityModifier);
            }

            if (((LivingEntity) event.getEntity()).getAttribute(Attributes.MAX_HEALTH) != null) {
                if (((LivingEntity) event.getEntity()).getAttribute(Attributes.MAX_HEALTH).getModifier(healthModifier.getID()) == null) {
                    ((LivingEntity) event.getEntity()).getAttribute(Attributes.MAX_HEALTH).applyPersistentModifier(healthModifier);
                    if(!event.getEntity().getPersistentData().contains("modified_health")){
                        event.getEntity().getPersistentData().putInt("modified_health", 1);
                        ((MonsterEntity) event.getEntity()).setHealth(((MonsterEntity) event.getEntity()).getMaxHealth());
                    }
                }
            }

            if (((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_DAMAGE).removeAllModifiers();
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_DAMAGE).applyPersistentModifier(damageModifier);
            }
        }
        if(event.getEntity() instanceof BlazeEntity){
            if (((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_SPEED) != null) {
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_SPEED).removeAllModifiers();
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.ATTACK_SPEED).applyPersistentModifier(fireSpeedModifier);
            }
        }

        if(event.getEntity() instanceof AbstractIllagerEntity){
            if (((LivingEntity) event.getEntity()).getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeAllModifiers();
                ((LivingEntity) event.getEntity()).getAttribute(Attributes.KNOCKBACK_RESISTANCE).applyPersistentModifier(knockbackResistance);
            }
        }

        if(event.getEntity() instanceof ZombieEntity){
            if(((ZombieEntity) event.getEntity()).getAttribute(Attributes.FOLLOW_RANGE) != null) {
                ((ZombieEntity) event.getEntity()).getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64);
            }
        }
    }

    void equipArmor(){
        // TODO add armor equipment based on difficulty
    }

    @SubscribeEvent
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event){
        if(event.getEntityLiving() instanceof WitherEntity){
            if(new Random().nextFloat() < 0.04f && new Random().nextFloat() < 0.04f){
                for(int i = 0; i < 3; i++){
                    WitherSkeletonEntity entity = new WitherSkeletonEntity(EntityType.WITHER_SKELETON, event.getEntity().world);
                    entity.setPosition(
                            event.getEntity().getPosition().getX() + event.getEntity().world.rand.nextDouble(),
                            event.getEntity().getPosition().getY() + event.getEntity().world.rand.nextDouble(),
                            event.getEntity().getPosition().getZ() + event.getEntity().world.rand.nextDouble()
                    );
                    entity.addPotionEffect(new EffectInstance(Effects.SLOW_FALLING, 15 * 20));
                    entity.setHeldItem(Hand.MAIN_HAND, new ItemStack(Items.STONE_SWORD));
                    entity.setAttackTarget(entity.getEntityWorld().getClosestPlayer(entity, 64));
                    entity.serializeNBT().putInt("targetsPlayer", 1);
                    event.getEntity().world.addEntity(entity);
                }
            }
        }
        if(event.getEntityLiving() instanceof WitherSkeletonEntity && event.getEntityLiving().serializeNBT().contains("targetsPlayer")){
            ((WitherSkeletonEntity)event.getEntityLiving()).setAttackTarget(event.getEntity().getEntityWorld().getClosestPlayer(event.getEntity(), 64));
        }
    }

    private void onEntityDamaged(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof MobEntity) {
            MobEntity entity = (MobEntity) event.getEntityLiving();
            Entity source = event.getSource().getTrueSource();

            if (source instanceof LivingEntity) {
                int range = 20;
                for (final MobEntity nearbyEntity : entity.world.getEntitiesWithinAABB(entity.getClass(),
                        new AxisAlignedBB(entity.getPosition().add(-range, -range, -range), entity.getPosition().add(range + 1, range + 1, range + 1)))) {
                    nearbyEntity.setRevengeTarget((LivingEntity) source);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityInit(EntityEvent.EnteringChunk event){
        if(event.getEntity() instanceof AbstractArrowEntity){
            if(((AbstractArrowEntity)event.getEntity()).func_234616_v_() != null){
                if(((AbstractArrowEntity)event.getEntity()).func_234616_v_() instanceof AbstractSkeletonEntity){
                    Vector3d direction = ((AbstractArrowEntity)event.getEntity()).func_234616_v_().getLookVec();
                    float speedMod = 2.1f;
                    event.getEntity().setVelocity(direction.x * speedMod, direction.y * speedMod, direction.z * speedMod);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityAttack(LivingAttackEvent event){
        if(event.getSource().getTrueSource() instanceof SpiderEntity){
            Entity target = event.getEntity();
            World world = target.world;
            BlockPos pos = new BlockPos(target.getPosition().getX(), target.getPosition().getY(), target.getPosition().getZ());
            BlockState state = world.getBlockState(pos);

            if(!state.getMaterial().isReplaceable())
                return;

            if(.3f <= world.rand.nextDouble())
                return;

            world.setBlockState(pos, Blocks.COBWEB.getDefaultState());
        }
        if(event.getEntity() instanceof SilverfishEntity){
            if(new Random().nextFloat() < .1f){
                SilverfishEntity entity = new SilverfishEntity(EntityType.SILVERFISH, event.getEntity().world);
                entity.setPosition(
                        event.getEntity().getPosition().getX() + event.getEntity().world.rand.nextDouble(),
                        event.getEntity().getPosition().getY() + event.getEntity().world.rand.nextDouble(),
                        event.getEntity().getPosition().getZ() + event.getEntity().world.rand.nextDouble()
                );
                event.getEntity().world.addEntity(entity);
            }
        }

        if(event.getSource().getTrueSource() instanceof ZombieEntity && event.getEntity() instanceof PlayerEntity){
            if(((PlayerEntity)event.getEntity()).getActivePotionEffect(Effects.HUNGER) != null) {
                if (((PlayerEntity) event.getEntity()).getActivePotionEffect(Effects.HUNGER).getAmplifier() == 0) {
                    ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(Effects.HUNGER, 15*20, 1));
                } else if (((PlayerEntity) event.getEntity()).getActivePotionEffect(Effects.HUNGER).getAmplifier() == 1) {
                    ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(Effects.HUNGER, 15*20, 2));
                } else if (((PlayerEntity) event.getEntity()).getActivePotionEffect(Effects.HUNGER).getAmplifier() == 2) {
                    ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(Effects.HUNGER, 15*20, 3));
                } else {
                    int amplifier = ((PlayerEntity)event.getEntity()).getActivePotionEffect(Effects.HUNGER).getAmplifier();
                    ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(Effects.HUNGER, amplifier));
                }
            }else {
                ((PlayerEntity) event.getEntity()).addPotionEffect(new EffectInstance(Effects.HUNGER, 15*20));
            }
        }

        if(event.getEntity() instanceof VindicatorEntity){
            event.getEntityLiving().addPotionEffect(new EffectInstance(Effects.STRENGTH, 5*20, 1));
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event){
        if(event.getEntity() instanceof WitchEntity){
            double x = event.getEntity().getPosition().getX() ;
            double y = event.getEntity().getPosition().getY();
            double z = event.getEntity().getPosition().getZ();

            for(int i = 0; i < 5; i++){
                BatEntity entity = new BatEntity(EntityType.BAT, event.getEntity().world);
                entity.setPosition(
                        x + event.getEntity().world.rand.nextDouble(),
                        y + event.getEntity().world.rand.nextDouble(),
                        z + event.getEntity().world.rand.nextDouble()
                );
                event.getEntity().world.addEntity(entity);
            }
        }
    }

    @SubscribeEvent
    public void preventSleep(SleepingLocationCheckEvent event){
        if(event.getEntity() instanceof PlayerEntity && !((PlayerEntity)event.getEntity()).isCreative()){
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Start event) {
        if(event.getExplosion().getExplosivePlacedBy() != null && event.getExplosion().getExplosivePlacedBy() instanceof CreeperEntity) {
            try {
                Field explodesInFire = Explosion.class.getDeclaredField(isDebugging ? "causesFire" : "field_77286_a");
                explodesInFire.setAccessible(true);
                explodesInFire.set(event.getExplosion(), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

