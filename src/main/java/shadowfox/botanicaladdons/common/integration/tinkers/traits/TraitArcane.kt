package shadowfox.botanicaladdons.common.integration.tinkers.traits

import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import shadowfox.botanicaladdons.common.integration.tinkers.TinkersIntegration
import slimeknights.tconstruct.library.traits.AbstractTrait
import slimeknights.tconstruct.library.utils.ToolHelper
import vazkii.botania.api.mana.ManaItemHandler

class TraitArcane : AbstractTrait("arcane", TinkersIntegration.MANASTEEL_COLOR) {
    val manaPer = 30

    override fun onUpdate(tool: ItemStack, world: World, entity: Entity, itemSlot: Int, isSelected: Boolean) {
        if (entity is EntityPlayer && ManaItemHandler.requestManaExactForTool(tool, entity, 3 * manaPer, false)) {
            if(ToolHelper.getCurrentDurability(tool) < ToolHelper.getMaxDurability(tool)) {
                ToolHelper.healTool(tool, 1, entity)
                ManaItemHandler.requestManaExactForTool(tool, entity, 3 * manaPer, true)
            }
        }
    }

    override fun onToolDamage(tool: ItemStack?, damage: Int, newDamage: Int, entity: EntityLivingBase?): Int {
        if (entity is EntityPlayer && ManaItemHandler.requestManaExactForTool(tool, entity, newDamage * manaPer, true))
            return 0
        return super.onToolDamage(tool, damage, newDamage, entity)
    }
}
