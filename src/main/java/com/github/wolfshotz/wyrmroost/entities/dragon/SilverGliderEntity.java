package com.github.wolfshotz.wyrmroost.entities.dragon;

import com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai.goals.DragonBreedGoal;
import com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai.goals.FlyerWanderGoal;
import com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai.goals.WRAvoidEntityGoal;
import com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai.goals.WRFollowOwnerGoal;
import com.github.wolfshotz.wyrmroost.entities.util.EntitySerializer;
import com.github.wolfshotz.wyrmroost.network.packets.SGGlidePacket;
import com.github.wolfshotz.wyrmroost.registry.WRSounds;
import com.github.wolfshotz.wyrmroost.util.LerpedFloat;
import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

import static net.minecraft.world.entity.ai.attributes.Attributes.*;


public class SilverGliderEntity extends TameableDragonEntity
{
    private static final EntitySerializer<SilverGliderEntity> SERIALIZER = TameableDragonEntity.SERIALIZER.concat(b -> b
            .track(EntitySerializer.BOOL, "Gender", TameableDragonEntity::isMale, TameableDragonEntity::setGender)
            .track(EntitySerializer.INT, "Variant", TameableDragonEntity::getVariant, TameableDragonEntity::setVariant)
            .track(EntitySerializer.BOOL, "Sleeping", TameableDragonEntity::isSleeping, TameableDragonEntity::setSleeping));

    public final LerpedFloat sitTimer = LerpedFloat.unit();
    public final LerpedFloat flightTimer = LerpedFloat.unit();

    public TemptGoal temptGoal;
    public boolean isGliding; // controlled by player-gliding.

    public SilverGliderEntity(EntityType<? extends TameableDragonEntity> dragon, Level level)
    {
        super(dragon, level);
    }

    @Override
    public EntitySerializer<SilverGliderEntity> getSerializer()
    {
        return SERIALIZER;
    }

    @Override
    protected void defineSynchedData()
    {
        super.defineSynchedData();
        entityData.define(FLYING, false);
        entityData.define(GENDER, false);
        entityData.define(VARIANT, 0);
        entityData.define(SLEEPING, false);
    }

    @Override
    protected void registerGoals()
    {
        super.registerGoals();

        goalSelector.addGoal(3, temptGoal = new TemptGoal(this, 0.8d,  Ingredient.of(ItemTags.FISHES), true));
        goalSelector.addGoal(4, new WRAvoidEntityGoal<>(this, Player.class, 10f, 0.8));
        goalSelector.addGoal(5, new DragonBreedGoal(this));
        goalSelector.addGoal(6, new WRFollowOwnerGoal(this));
        goalSelector.addGoal(7, new SwoopGoal());
        goalSelector.addGoal(8, new FlyerWanderGoal(this, 1));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, LivingEntity.class, 7f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    /*
    @Override
    public <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        return null; // Todo Implement animations
    }
    */

    @Override
    public void registerControllers(AnimationData data) {

    }

    @Override
    public void aiStep()
    {
        super.aiStep();

        if (isGliding && !isRiding()) isGliding = false;

        sitTimer.add((isInSittingPose() || isSleeping())? 0.2f : -0.2f);
        sleepTimer.add(isSleeping()? 0.05f : -0.1f);
        flightTimer.add(isFlying() || isGliding()? 0.1f : -0.1f);
    }

    @Override
    public void rideTick()
    {
        super.rideTick();

        if (!(getVehicle() instanceof Player)) return;
        Player player = (Player) getVehicle();
        final boolean FLAG = shouldGlide(player);

        if (level.isClientSide && isGliding != FLAG)
        {
            SGGlidePacket.send(FLAG);
            isGliding = FLAG;
        }

        if (isGliding)
        {
            Vec3 vec3d = player.getLookAngle().scale(0.3);
            player.setDeltaMovement(player.getDeltaMovement().scale(0.6).add(vec3d.x, Math.min(vec3d.y * 2, 0), vec3d.z));
            player.fallDistance = 0;
        }
    }

    @Override
    public void travel(Vec3 vec3d)
    {
        Vec3 look = getLookAngle();
        if (isFlying() && look.y < 0) setDeltaMovement(getDeltaMovement().add(0, look.y * 0.25, 0));

        super.travel(vec3d);
    }

    @Override
    public InteractionResult playerInteraction(Player player, InteractionHand hand, ItemStack stack)
    {
        InteractionResult result = super.playerInteraction(player, hand, stack);
        if (result.consumesAction()) return result;

        if (!isTame() && isFood(stack))
        {
            if (!level.isClientSide && (temptGoal.isRunning() || player.isCreative()))
            {
                tame(getRandom().nextDouble() < 0.333, player);
                eat(stack);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }

        if (isOwnedBy(player) && player.getPassengers().isEmpty() && !player.isShiftKeyDown() && !isFood(stack) && !isLeashed())
        {
            startRiding(player, true);
            setOrderedToSit(false);
            clearAI();
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    public boolean shouldGlide(Player player)
    {
        if (isBaby()) return false;
        if (!player.jumping) return false;
        if (player.getAbilities().flying) return false;
        if (player.isFallFlying()) return false;
        if (player.isInWater()) return false;
        if (player.getDeltaMovement().y > 0) return false;
        if (isGliding() && !player.isOnGround()) return true;
        return getAltitude() - 1.8 > 4;
    }

    @Override
    public void doSpecialEffects()
    {
        if (getVariant() == -1 && tickCount % 5 == 0)
        {
            double x = getX() + getRandom().nextGaussian();
            double y = getY() + getRandom().nextDouble();
            double z = getZ() + getRandom().nextGaussian();
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.8f, 0), 1f), x, y, z, 0, 0.2f, 0);
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose)
    {
        EntityDimensions size = getType().getDimensions().scale(getScale());
        if (isInSittingPose() || isSleeping()) size = size.scale(1, 0.87f);
        return size;
    }

    @Override
    public int determineVariant()
    {
        if (getRandom().nextDouble() < 0.002) return -1;
        return getRandom().nextInt(3);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound()
    {
        return WRSounds.ENTITY_SILVERGLIDER_IDLE.get();
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn)
    {
        return WRSounds.ENTITY_SILVERGLIDER_HURT.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound()
    {
        return WRSounds.ENTITY_SILVERGLIDER_DEATH.get();
    }

    @Override
    public Vec3 getRidingPosOffset(int passengerIndex)
    {
        return new Vec3(0, 1.81, 0.5d);
    }

    @Override
    public boolean shouldFly()
    {
        return isRiding()? isGliding() : super.shouldFly();
    }

    @Override
    public int getHeadRotSpeed()
    {
        return 30;
    }

    @Override
    public int getYawRotationSpeed()
    {
        return isFlying()? 5 : 75;
    }

    public boolean isGliding()
    {
        return isGliding;
    }

    @Override
    public boolean isFood(ItemStack stack)
    {
        return stack.is(ItemTags.FISHES);
    }

    public static boolean getSpawnPlacement(EntityType<SilverGliderEntity> fEntityType, ServerLevelAccessor level, MobSpawnType spawnReason, BlockPos blockPos, Random random)
    {
        if (spawnReason == MobSpawnType.SPAWNER) return true;
        Block block = level.getBlockState(blockPos.below()).getBlock();
        return block == Blocks.AIR || block == Blocks.SAND && level.getRawBrightness(blockPos, 0) > 8;
    }

    @Override
    public Attribute[] getScaledAttributes()
    {
        return new Attribute[] {MAX_HEALTH};
    }

    public static AttributeSupplier.Builder getAttributeSupplier()
    {
        return Mob.createMobAttributes()
                .add(MAX_HEALTH, 20)
                .add(MOVEMENT_SPEED, 0.23)
                .add(FLYING_SPEED, 0.12);
    }

    /*@Nullable
    @Override
    public AbstractContainerMenu createMenu(int p_39954_, Inventory p_39955_, Player p_39956_) {
        return null;
    }*/


    public class SwoopGoal extends Goal
    {
        private BlockPos pos;

        public SwoopGoal()
        {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse()
        {
            if (!isFlying()) return false;
            if (isRiding()) return false;
            if (getRandom().nextDouble() > 0.001) return false;
            if (level.getFluidState(this.pos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockPosition()).below()).isEmpty())
                return false;
            return getY() - pos.getY() > 8;
        }

        @Override
        public boolean canContinueToUse()
        {
            return blockPosition().distSqr(pos) > 8;
        }

        @Override
        public void tick()
        {
            if (getNavigation().isDone()) getNavigation().moveTo(pos.getX(), pos.getY() + 2, pos.getZ(), 1);
            getLookControl().setLookAt(pos.getX(), pos.getY() + 2, pos.getZ());
        }
    }
}
