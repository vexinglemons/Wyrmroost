package com.github.wolfshotz.wyrmroost.containers;

import com.github.wolfshotz.wyrmroost.Wyrmroost;
import com.github.wolfshotz.wyrmroost.entities.dragon.AbstractDragonEntity;
import com.github.wolfshotz.wyrmroost.entities.dragon.helpers.DragonInvHandler;
import com.github.wolfshotz.wyrmroost.registry.WRIO;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class DragonInvContainer extends Container
{
    public static final int MAX_PLAYER_SLOTS = 36;

    public final DragonInvHandler inventory;
    public final PlayerInventory playerInv;

    public DragonInvContainer(DragonInvHandler inv, PlayerInventory playerInv, int windowID)
    {
        super(WRIO.DRAGON_INVENTORY.get(), windowID);
        this.inventory = inv;
        this.playerInv = playerInv;
        inv.dragon.addContainerInfo(this);
    }

    // override method for public exposure
    @Override
    public Slot addSlot(Slot slotIn)
    {
        return super.addSlot(slotIn);
    }

    @Override
    public boolean canUse(PlayerEntity playerIn)
    {
        return inventory.dragon.getOwner() == playerIn;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity playerIn, int index)
    {
        Slot slot = slots.get(index);
        if (slot != null && slot.hasStack())
        {
            ItemStack transferring = slot.getStack();
            if ((index < MAX_PLAYER_SLOTS && insertItem(transferring, MAX_PLAYER_SLOTS, slots.size(), false))
                    || (index >= MAX_PLAYER_SLOTS && insertItem(transferring, 0, MAX_PLAYER_SLOTS, true)))
            {
                if (transferring.isEmpty()) slot.setStack(ItemStack.EMPTY);
                else slot.markDirty();
                return transferring.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    public void makeSlots(int firstIndex, int firstX, int firstY, int length, int height, ISlotArea slot)
    {
        for (int y = 0; y < height; ++y)
        {
            for (int x = 0; x < length; ++x)
            {
                if (inventory.getSlots() <= firstIndex)
                {
                    Wyrmroost.LOG.error("TOO MANY SLOTS! ABORTING THE REST! Ended Index: {}, Supposed to be: {}", firstIndex, length * height);
                    return;
                }
                addSlot(slot.get(firstIndex++, firstX + x * 18, firstY + y * 18));
            }
        }
    }

    public void makeSlots(IInventory inventory, int index, int initialX, int initialY, int length, int height)
    {
        for (int y = 0; y < height; ++y)
        {
            for (int x = 0; x < length; ++x)
            {
                if (inventory.size() <= index)
                {
                    Wyrmroost.LOG.error("TOO MANY SLOTS! ABORTING THE REST!");
                    return;
                }
                addSlot(new Slot(inventory, index++, initialX + x * 18, initialY + y * 18));
            }
        }
    }

    public void makePlayerSlots(PlayerInventory playerInv, int initialX, int initialY)
    {
        makeSlots(playerInv, 9, initialX, initialY, 9, 3); // Player inv
        makeSlots(playerInv, 0, initialX, initialY + 58, 9, 1); // Hotbar
    }

    public static INamedContainerProvider getProvider(AbstractDragonEntity dragon)
    {
        return new INamedContainerProvider()
        {
            @Override
            public ITextComponent getDisplayName()
            {
                return new StringTextComponent("Dragon Inventory");
            }

            @Override
            public Container createMenu(int id, PlayerInventory playersInv, PlayerEntity player)
            {
                return new DragonInvContainer(dragon.getInvHandler(), playersInv, id);
            }
        };
    }

    public interface ISlotArea
    {
        Slot get(int index, int posX, int posY);
    }
}
