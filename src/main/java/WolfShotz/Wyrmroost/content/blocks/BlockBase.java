package WolfShotz.Wyrmroost.content.blocks;

import WolfShotz.Wyrmroost.content.items.ItemList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

/**
 * Blockbase - Helper Class allowing for easier block registration
 */
//TODO: USE MANUAL TOOLTYPE AND HARVESTLEVEL UNTIL FORGE RE-EVALUATES
class BlockBase extends Block
{
    private boolean isBeaconBase = false;
    private ToolType tool;
    private int harvestLevel;

    /**
     * @param name The Resource Location
     * @param material Material the block is
     * @param toughness How hard and how resistant the block is
     * @param sound the sound the block makes
     */
    BlockBase(String name, ItemGroup group, Material material, ToolType tool, int harvestLevel, float toughness, SoundType sound) {
        super(Block.Properties.create(material).hardnessAndResistance(toughness).sound(sound));
        setRegistryName(name);
        this.tool = tool;
        this.harvestLevel = harvestLevel;

        BlockList.BLOCKS.add(this);
        ItemList.ITEMS.add(new BlockItem(this, new Item.Properties().group(group)).setRegistryName(name));
    }

    /**
     * Constructor used for more advanced block properties
     * @param name The Resource Location
     * @param material Material the block is
     * @param hardness How hard the block is (i.e how fast it takes to break it)
     * @param resistance How resistant the block is to explosives
     * @param light The light level the block produces
     * @param sound The sound the block makes
     * @param isBeaconBase Does this block work with beacons?
     */
    BlockBase(String name, ItemGroup group, Material material, ToolType tool, int harvestLevel, float hardness, float resistance, int light, SoundType sound, boolean isBeaconBase) {
        super(Block.Properties.create(material).hardnessAndResistance(hardness, resistance).lightValue(light).sound(sound));
        setRegistryName(name);
        this.tool = tool;
        this.harvestLevel = harvestLevel;
        this.isBeaconBase = isBeaconBase;

        BlockList.BLOCKS.add(this);
        ItemList.ITEMS.add(new BlockItem(this, new Item.Properties().group(group)).setRegistryName(name));
    }

    @Nullable
    @Override
    public ToolType getHarvestTool(BlockState state) { return this.tool; }

    @Override
    public int getHarvestLevel(BlockState state) { return this.harvestLevel; }

    @Override
    public boolean isBeaconBase(BlockState state, IWorldReader world, BlockPos pos, BlockPos beacon) { return isBeaconBase; }
}