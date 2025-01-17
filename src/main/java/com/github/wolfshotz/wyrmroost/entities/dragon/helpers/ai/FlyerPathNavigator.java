package com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai;

import com.github.wolfshotz.wyrmroost.entities.dragon.TameableDragonEntity;
import com.github.wolfshotz.wyrmroost.entities.dragon.WRDragonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * todo: I guess make our own node processor. Derivative of WalkAndSwim, just ditch all the water related shit.
 */
public class FlyerPathNavigator extends FlyingPathNavigation
{
    public FlyerPathNavigator(WRDragonEntity entity)
    {
        super(entity, entity.level);
    }

    @Override
    @SuppressWarnings("ConstantConditions") // IT CAN BE NULL DAMNIT
    public void tick()
    {
        if (!isDone() && canUpdatePath())
        {
            WRDragonEntity dragon = ((WRDragonEntity) mob);
            BlockPos target = getTargetPos();
            if (target != null)
            {
                mob.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), speedModifier);
                maxDistanceToWaypoint = mob.getBbWidth() * mob.getBbWidth() * dragon.getYawRotationSpeed() * dragon.getYawRotationSpeed();
                Vec3 entityPos = getTempMobPos();
                if (target.distSqr(new Vec3i(entityPos.x, entityPos.y, entityPos.z)) <= maxDistanceToWaypoint)
                    path = null;
            }
        }
    }

    @Override
    public boolean isStableDestination(BlockPos pos)
    {
        return true;
    }
}
