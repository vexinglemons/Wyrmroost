package WolfShotz.Wyrmroost.items;

import WolfShotz.Wyrmroost.entities.dragon.AbstractDragonEntity;
import WolfShotz.Wyrmroost.registry.WRItems;
import WolfShotz.Wyrmroost.util.Mafs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class SoulCrystalItem extends Item
{
    public static final String DATA_DRAGON = "DragonData";

    public SoulCrystalItem() { super(WRItems.builder().maxStackSize(1)); }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand)
    {
        World world = player.world;
        if (containsDragon(stack)) return ActionResultType.PASS;
        if (!isSuitableEntity(target)) return ActionResultType.PASS;
        TameableEntity dragon = (TameableEntity) target;
        if (dragon.getOwner() != player) return ActionResultType.PASS;

        if (!dragon.getPassengers().isEmpty()) dragon.removePassengers();
        if (!world.isRemote)
        {
            CompoundNBT tag = stack.getOrCreateTag();
            tag.put(DATA_DRAGON, dragon.serializeNBT()); // Serializing the dragons data, including its id.
            stack.setTag(tag);
            dragon.remove();
            player.setHeldItem(hand, stack);
        }
        else // Client side Aesthetics
        {
            for (int i = 0; i <= dragon.getWidth() * 25; ++i)
            {
                double calcX = MathHelper.cos(i + 360 / Mafs.PI * 360f) * (dragon.getWidth() * 1.5d);
                double calcZ = MathHelper.sin(i + 360 / Mafs.PI * 360f) * (dragon.getWidth() * 1.5d);
                double x = dragon.getPosX() + calcX;
                double y = dragon.getPosY() + (dragon.getHeight() * 1.8f);
                double z = dragon.getPosZ() + calcZ;
                double xMot = -calcX / 5f;
                double yMot = -(dragon.getHeight() / 8);
                double zMot = -calcZ / 5f;

                world.addParticle(ParticleTypes.END_ROD, x, y, z, xMot, yMot, zMot);
            }
        }
        world.playSound(null, player.getPosition(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.AMBIENT, 1, 1);
        return ActionResultType.func_233537_a_(world.isRemote);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        ItemStack stack = context.getItem();
        if (!containsDragon(stack)) return ActionResultType.PASS;
        World world = context.getWorld();
        TameableEntity dragon = getContained(stack, world);
        BlockPos pos = context.getPos().offset(context.getFace());
        PlayerEntity player = context.getPlayer();

        dragon.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5); // update the position now for collision checking
        if (!world.hasNoCollisions(dragon, dragon.getBoundingBox()))
        {
            player.sendStatusMessage(new TranslationTextComponent("item.wyrmroost.soul_crystal.fail").mergeStyle(TextFormatting.RED), true);
            return ActionResultType.FAIL;
        }

        if (!world.isRemote) // Spawn the entity on the server side only
        {
            stack.removeChildTag(DATA_DRAGON);
            world.addEntity(dragon);
        }
        else // Client Side Aesthetics
        {
            EntitySize size = dragon.getSize(dragon.getPose());

            double posX = pos.getX() + 0.5d;
            double posY = pos.getY() + (size.height / 2);
            double posZ = pos.getZ() + 0.5d;
            for (int i = 0; i < dragon.getWidth() * 25; ++i)
            {
                double x = MathHelper.cos(i + 360 / Mafs.PI * 360f) * (dragon.getWidth() * 1.5d);
                double z = MathHelper.sin(i + 360 / Mafs.PI * 360f) * (dragon.getWidth() * 1.5d);
                double xMot = x / 10f;
                double yMot = dragon.getHeight() / 18f;
                double zMot = z / 10f;

                world.addParticle(ParticleTypes.END_ROD, posX, posY, posZ, xMot, yMot, zMot);
                world.addParticle(ParticleTypes.CLOUD, posX, posY + (i * 0.25), posZ, 0, 0, 0);

            }
        }
        world.playSound(null, dragon.getPosition(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.AMBIENT, 1, 1);

        return ActionResultType.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn)
    {
        TameableEntity dragon = getContained(stack, world);
        if (dragon != null)
        {
            tooltip.add(new StringTextComponent(dragon.getName().getUnformattedComponentText()));
            tooltip.add(new StringTextComponent(Character.toString('\u2764')).mergeStyle(TextFormatting.RED)
                    .append(new StringTextComponent(String.format(" %s / %s", (int) (dragon.getHealth() / 2), (int) dragon.getMaxHealth() / 2))));
            LivingEntity owner = dragon.getOwner();
            if (owner != null)
                tooltip.add(new StringTextComponent("Tamed by " + owner.getName().getUnformattedComponentText()));
        }
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack)
    {
        TranslationTextComponent name = (TranslationTextComponent) super.getDisplayName(stack);
        if (containsDragon(stack)) name.mergeStyle(TextFormatting.AQUA).mergeStyle(TextFormatting.ITALIC);
        return name;
    }

    @Override
    public boolean hasEffect(ItemStack stack) { return containsDragon(stack); }

    private static boolean containsDragon(ItemStack stack)
    {
        return stack.hasTag() && stack.getTag().contains(DATA_DRAGON);
    }

    @Nullable
    private static TameableEntity getContained(ItemStack stack, World world)
    {
        if (!containsDragon(stack)) return null;
        CompoundNBT tag = stack.getTag().getCompound(DATA_DRAGON);
        EntityType<?> type = EntityType.byKey(tag.getString("id")).orElse(null);
        if (type == null) return null;
        TameableEntity dragon = (TameableEntity) type.create(world);
        dragon.deserializeNBT(tag);
        return dragon;
    }

    private static boolean isSuitableEntity(LivingEntity entity)
    {
        if (entity instanceof TameableEntity)
        {
            if (entity instanceof AbstractDragonEntity) return true;
            String modID = entity.getType().getRegistryName().getNamespace();
            switch (modID)
            {
                case "dragonmounts":
                case "wings":
                    return true;
                default:
                    break;
            }
        }
        return false;
    }
}
