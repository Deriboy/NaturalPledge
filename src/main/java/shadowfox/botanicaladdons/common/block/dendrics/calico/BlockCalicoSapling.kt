package shadowfox.botanicaladdons.common.block.dendrics.calico

import com.teamwizardry.librarianlib.features.base.block.BlockModSapling
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.Explosion
import net.minecraft.world.World
import net.minecraft.world.gen.feature.WorldGenTrees
import net.minecraftforge.event.terraingen.TerrainGen
import shadowfox.botanicaladdons.common.block.ModBlocks
import shadowfox.botanicaladdons.common.lexicon.LexiconEntries
import vazkii.botania.api.lexicon.ILexiconable
import vazkii.botania.api.lexicon.LexiconEntry
import java.util.*

/**
 * @author WireSegal
 * Created at 10:35 PM on 5/27/16.
 */
class BlockCalicoSapling(name: String) : BlockModSapling(name), IExplosionDampener, ILexiconable {

    override fun onBlockExploded(world: World?, pos: BlockPos?, explosion: Explosion?) {
        //NO-OP
    }

    override fun generateTree(worldIn: World, pos: BlockPos, state: IBlockState, rand: Random) {
        defaultSaplingBehavior(worldIn, pos, state, rand, ModBlocks.calicoLog, ModBlocks.calicoLeaves)
    }

    override fun getEntry(p0: World?, p1: BlockPos?, p2: EntityPlayer?, p3: ItemStack): LexiconEntry? {
        return LexiconEntries.calicoTree
    }
}
