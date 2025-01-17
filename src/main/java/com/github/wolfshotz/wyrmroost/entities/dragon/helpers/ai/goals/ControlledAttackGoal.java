package com.github.wolfshotz.wyrmroost.entities.dragon.helpers.ai.goals;

import com.github.wolfshotz.wyrmroost.entities.dragon.TameableDragonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class ControlledAttackGoal extends MeleeAttackGoal
{
    private final TameableDragonEntity dragon;

    public ControlledAttackGoal(TameableDragonEntity dragon, double speed, boolean longMemory)
    {
        super(dragon, speed, longMemory);
        this.dragon = dragon;
    }

    @Override
    public boolean canUse()
    {
        return super.canUse() && !dragon.isVehicle();
    }

    @Override
    public boolean canContinueToUse()
    {
        LivingEntity target = dragon.getTarget();
        if (target == null) return false;
        return !dragon.isVehicle() && dragon.wantsToAttack(target, dragon.getOwner()) && super.canContinueToUse();
    }

    @Override
    public void start()
    {
        dragon.setAggressive(true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr)
    {
        double reach = getAttackReachSqr(enemy);
        if (distToEnemySqr <= reach && isTimeToAttack()) {
            //attack.run();
            dragon.swing(InteractionHand.MAIN_HAND);
            dragon.doHurtTarget(enemy);
            resetAttackCooldown();
        }

    }

    @Override
    protected double getAttackReachSqr(LivingEntity attackTarget)
    {
        return dragon.getBbWidth() * 2 * dragon.getBbWidth() * 2 + attackTarget.getBbWidth();
    }
}
