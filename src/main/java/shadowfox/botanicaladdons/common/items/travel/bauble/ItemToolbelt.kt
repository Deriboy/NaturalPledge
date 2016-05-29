package shadowfox.botanicaladdons.common.items.travel.bauble

import baubles.api.BaubleType
import baubles.common.lib.PlayerHandler
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11
import shadowfox.botanicaladdons.api.item.IToolbeltBlacklisted
import shadowfox.botanicaladdons.api.lib.LibMisc
import shadowfox.botanicaladdons.client.core.BAClientMethodHandles
import shadowfox.botanicaladdons.common.BotanicalAddons
import shadowfox.botanicaladdons.common.items.base.ItemModBauble
import shadowfox.botanicaladdons.common.network.PlayerItemMessage
import vazkii.botania.api.item.IBaubleRender
import vazkii.botania.api.item.IBlockProvider
import vazkii.botania.client.core.handler.ClientMethodHandles
import vazkii.botania.client.core.handler.ClientTickHandler
import vazkii.botania.common.core.handler.MethodHandles
import vazkii.botania.common.core.helper.ItemNBTHelper
import vazkii.botania.common.item.ItemBaubleBox
import java.util.*

/**
 * @author WireSegal
 * Created at 10:01 AM on 5/5/16.
 */
class ItemToolbelt(name: String) : ItemModBauble(name), IBaubleRender, IBlockProvider, IToolbeltBlacklisted {

    companion object {
        val glowTexture = ResourceLocation(LibMisc.MOD_ID, "textures/misc/toolbelt.png")
        val beltTexture = ResourceLocation(LibMisc.MOD_ID, "textures/model/toolbelt.png")

        @SideOnly(Side.CLIENT)
        var model: Any? = null

        val SEGMENTS = 12

        val TAG_ITEM_PREFIX = "item"
        val TAG_EQUIPPED = "equipped"
        val TAG_ROTATION_BASE = "rotationBase"

        fun isEquipped(stack: ItemStack): Boolean = ItemNBTHelper.getBoolean(stack, TAG_EQUIPPED, false)
        fun setEquipped(stack: ItemStack, equipped: Boolean) = ItemNBTHelper.setBoolean(stack, TAG_EQUIPPED, equipped)
        fun getRotationBase(stack: ItemStack): Float = ItemNBTHelper.getFloat(stack, TAG_ROTATION_BASE, 0F)
        fun setRotationBase(stack: ItemStack, rotation: Float) = ItemNBTHelper.setFloat(stack, TAG_ROTATION_BASE, rotation)

        fun getSegmentLookedAt(stack: ItemStack, player: EntityLivingBase): Int {
            val yaw = getCheckingAngle(player, getRotationBase(stack))

            val angles = 360
            val segAngles = angles / SEGMENTS
            for (seg in 0..SEGMENTS - 1) {
                val calcAngle = seg.toFloat() * segAngles
                if (yaw >= calcAngle && yaw < calcAngle + segAngles)
                    return seg
            }
            return -1
        }

        fun getCheckingAngle(player: EntityLivingBase): Float = getCheckingAngle(player, 0F)

        // Agreed, V, minecraft's rotation is shit. And no roll? Seriously?
        fun getCheckingAngle(player: EntityLivingBase, base: Float): Float {
            var yaw = MathHelper.wrapDegrees(player.rotationYaw) + 90F
            val angles = 360
            val segAngles = angles / SEGMENTS
            val shift = segAngles / 2

            if (yaw < 0)
                yaw = 180F + (180F + yaw)
            yaw -= 360F - base
            var angle = 360F - yaw + shift

            if (angle < 0)
                angle += 360F

            return angle
        }

        fun isLookingAtSegment(player: EntityLivingBase): Boolean {
            val pitch = player.rotationPitch

            return pitch > -33.75 && pitch < 45
        }

        fun getItemForSlot(stack: ItemStack, slot: Int): ItemStack? {
            if (slot >= SEGMENTS) return null
            else {
                val cmp = getStoredCompound(stack, slot) ?: return null
                return ItemStack.loadItemStackFromNBT(cmp)
            }
        }

        fun getStoredCompound(stack: ItemStack, slot: Int): NBTTagCompound? = ItemNBTHelper.getCompound(stack, TAG_ITEM_PREFIX + slot, true)
        fun setItem(beltStack: ItemStack, stack: ItemStack?, pos: Int) {
            if (stack == null) ItemNBTHelper.setCompound(beltStack, TAG_ITEM_PREFIX + pos, NBTTagCompound())
            else {
                val tag = NBTTagCompound()
                stack.writeToNBT(tag)
                ItemNBTHelper.setCompound(beltStack, TAG_ITEM_PREFIX + pos, tag)
            }
        }

        fun getEquippedBelt(player: EntityPlayer): ItemStack? {
            val inv = PlayerHandler.getPlayerBaubles(player)
            var beltStack: ItemStack? = null
            for (i in 0..inv.sizeInventory) {
                val stack = inv.getStackInSlot(i)
                if (stack != null && stack.item is ItemToolbelt) {
                    beltStack = stack
                }
            }
            return beltStack
        }


        object EventHandler {
            @SideOnly(Side.CLIENT)
            @SubscribeEvent
            fun onRenderWorldLast(event: RenderWorldLastEvent) {
                val player = Minecraft.getMinecraft().thePlayer
                val beltStack = getEquippedBelt(player)
                if (beltStack != null && isEquipped(beltStack)) {
                    render(beltStack, player, event.partialTicks)
                }
            }

            @SideOnly(Side.CLIENT)
            fun render(stack: ItemStack, player: EntityPlayer, partialTicks: Float) {
                val mc = Minecraft.getMinecraft()
                val tess = Tessellator.getInstance()

                val renderPosX = BAClientMethodHandles.getRenderPosX(mc.renderManager)
                val renderPosY = BAClientMethodHandles.getRenderPosY(mc.renderManager)
                val renderPosZ = BAClientMethodHandles.getRenderPosZ(mc.renderManager)

                GlStateManager.pushMatrix()
                GlStateManager.enableBlend()
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                val alpha = (Math.sin(((ClientTickHandler.ticksInGame + partialTicks) * 0.2f).toDouble()).toFloat() * 0.5f + 0.5f) * 0.4f + 0.3f

                val posX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks
                val posY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks
                val posZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks

                GlStateManager.translate(posX - renderPosX, posY - renderPosY + player.defaultEyeHeight, posZ - renderPosZ)


                val base = getRotationBase(stack)
                val angles = 360
                val segAngles = angles / SEGMENTS
                val shift = base - segAngles / 2

                val u = 1f
                val v = 0.25f

                val s = 3f
                val m = 0.8f
                val y = v * s * 2f
                var y0 = 0f

                val segmentLookedAt = getSegmentLookedAt(stack, player)

                for (seg in 0..SEGMENTS - 1) {
                    var inside = false
                    val rotationAngle = (seg + 0.5f) * segAngles + shift
                    GlStateManager.pushMatrix()
                    GlStateManager.rotate(rotationAngle, 0f, 1f, 0f)
                    GlStateManager.translate(s * m, -0.75f, 0f)

                    if (segmentLookedAt == seg)
                        inside = true

                    val slotStack = getItemForSlot(stack, seg)
                    if (slotStack != null) {
                        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
                        val scale = 0.6f
                        GlStateManager.scale(scale, scale, scale)
                        GlStateManager.rotate(180f, 0f, 1f, 0f)
                        GlStateManager.translate(0f, 0.6f, 0f)
                        GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f)
                        Minecraft.getMinecraft().renderItem.renderItem(slotStack, ItemCameraTransforms.TransformType.GUI)
                        GlStateManager.disableLighting()
                    }
                    GlStateManager.popMatrix()

                    GlStateManager.pushMatrix()
                    GlStateManager.rotate(180f, 1f, 0f, 0f)
                    var a = alpha
                    if (inside) {
                        a += 0.3f
                        y0 = -y
                    }

                    GlStateManager.enableBlend()
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

                    if (seg % 2 == 0)
                        GlStateManager.color(0.6f, 0.6f, 0.6f, a)
                    else
                        GlStateManager.color(1f, 1f, 1f, a)

                    GlStateManager.disableCull()
                    mc.renderEngine.bindTexture(glowTexture)
                    tess.buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
                    for (i in 0..segAngles - 1) {
                        val ang = i.toFloat() + (seg * segAngles).toFloat() + shift
                        var xp = Math.cos(ang * Math.PI / 180f) * s
                        var zp = Math.sin(ang * Math.PI / 180f) * s

                        tess.buffer.pos(xp * m, y.toDouble(), zp * m).tex(u.toDouble(), v.toDouble()).endVertex()
                        tess.buffer.pos(xp, y0.toDouble(), zp).tex(u.toDouble(), 0.0).endVertex()

                        xp = Math.cos((ang + 1) * Math.PI / 180f) * s
                        zp = Math.sin((ang + 1) * Math.PI / 180f) * s

                        tess.buffer.pos(xp, y0.toDouble(), zp).tex(0.0, 0.0).endVertex()
                        tess.buffer.pos(xp * m, y.toDouble(), zp * m).tex(0.0, v.toDouble()).endVertex()
                    }
                    y0 = 0f
                    tess.draw()
                    GlStateManager.enableCull()
                    GlStateManager.popMatrix()
                }
                GlStateManager.popMatrix()
            }

            @SubscribeEvent fun onPlayerInteract(event: PlayerInteractEvent.RightClickItem) {
                if (event.hand == EnumHand.MAIN_HAND)
                    firePlayerInteraction(event)
            }

            @SubscribeEvent fun onPlayerInteractEmpty(event: PlayerInteractEvent.RightClickEmpty) {
                if (event.entityPlayer.heldItemMainhand == null)
                    firePlayerInteraction(event)
            }

            fun firePlayerInteraction(event: PlayerInteractEvent) {
                val player = event.entityPlayer
                val beltStack = getEquippedBelt(player)

                val heldItem = event.itemStack
                if (beltStack != null && isEquipped(beltStack)) {
                    val segment = getSegmentLookedAt(beltStack, player)
                    val toolStack = getItemForSlot(beltStack, segment)
                    if (toolStack == null && heldItem != null) {
                        val heldItemObject = heldItem.item
                        if (!(heldItemObject is IToolbeltBlacklisted && !heldItemObject.allowedInToolbelt(heldItem)) && heldItemObject !is ItemBaubleBox) {
                            if (!event.world.isRemote) {
                                val item = heldItem.copy()

                                setItem(beltStack, item, segment)

                                player.inventory.decrStackSize(player.inventory.currentItem, 64)
                                player.inventory.markDirty()
                            }
                        }
                    } else if (toolStack != null) {
                        setItem(beltStack, null, segment)
                        if (!event.world.isRemote) {
                            if (player.heldItemMainhand == null) {
                                player.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, toolStack.copy())
                            } else if (!player.inventory.addItemStackToInventory(toolStack.copy())) {
                                player.dropItem(toolStack.copy(), false)
                            }
                        } else {
                            BotanicalAddons.network.sendToServer(PlayerItemMessage(toolStack))
                        }
                    }
                    if (event.isCancelable) event.isCanceled = true
                }
            }

        }
    }

    init {
        MinecraftForge.EVENT_BUS.register(EventHandler)
    }

    override fun getBaubleType(p0: ItemStack?) = BaubleType.BELT

    override fun onPlayerBaubleRender(stack: ItemStack, player: EntityPlayer, type: IBaubleRender.RenderType, partTicks: Float) {
        if (type == vazkii.botania.api.item.IBaubleRender.RenderType.BODY) {

            if (model == null)
                model = ModelBiped()

            Minecraft.getMinecraft().renderEngine.bindTexture(beltTexture)
            if (player.isSneaking)
                GlStateManager.translate(0f, 0.3f, 0f)
            vazkii.botania.api.item.IBaubleRender.Helper.rotateIfSneaking(player)

            if (!player.isSneaking)
                GlStateManager.translate(0F, 0.2F, 0F)

            val s = 1.05F / 16F
            GlStateManager.scale(s, s, s)

            (model as ModelBiped).bipedBody.render(1F)
        }
    }

    override fun getBlockCount(p0: EntityPlayer?, p1: ItemStack, p2: ItemStack, p3: Block, p4: Int): Int {
        var total = 0
        for (segment in 0..SEGMENTS - 1) {
            val slotStack = getItemForSlot(p2, segment)
            if (slotStack != null) {
                val slotItem = slotStack.item
                if (slotItem is IBlockProvider) {
                    val count = slotItem.getBlockCount(p0, p1, slotStack, p3, p4)
                    setItem(p2, slotStack, segment)
                    if (count == -1) return -1
                    total += count
                } else if (slotItem is ItemBlock && Block.getBlockFromItem(slotItem) == p3 && slotStack.itemDamage == p4) {
                    total += slotStack.stackSize
                }
            }
        }
        return total
    }

    override fun provideBlock(p0: EntityPlayer?, p1: ItemStack, p2: ItemStack, p3: Block, p4: Int, p5: Boolean): Boolean {
        for (segment in 0..SEGMENTS - 1) {
            val slotStack = getItemForSlot(p2, segment)
            if (slotStack != null) {
                val slotItem = slotStack.item
                if (slotItem is IBlockProvider) {
                    val provided = slotItem.provideBlock(p0, p1, slotStack, p3, p4, p5)
                    setItem(p2, slotStack, segment)
                    if (provided) return true
                } else if (slotItem is ItemBlock && Block.getBlockFromItem(slotItem) == p3 && slotStack.itemDamage == p4) {
                    if (p5) slotStack.stackSize--

                    if (slotStack.stackSize == 0) setItem(p2, null, segment)
                    else setItem(p2, slotStack, segment)
                    return true
                }
            }
        }
        return false
    }

    override fun addHiddenTooltip(stack: ItemStack, player: EntityPlayer?, tooltip: MutableList<String>, advanced: Boolean) {
        val map = HashMap<String, Int>()
        for (segment in 0..SEGMENTS - 1) {
            val slotStack = getItemForSlot(stack, segment)
            if (slotStack != null) {
                var base = 0
                val name = slotStack.displayName
                val node = map[name]
                if (node != null) base = node
                map.put(name, base + slotStack.stackSize)
            }
        }
        if (map.size > 0) addToTooltip(tooltip, "misc.${LibMisc.MOD_ID}.contains")
        else addToTooltip(tooltip, "misc.${LibMisc.MOD_ID}.containsNothing")

        for (key in map.keys.sorted()) {
            tooltip.add("${map[key]}x ${TextFormatting.WHITE}$key")
        }
        super.addHiddenTooltip(stack, player, tooltip, advanced)

    }


    override fun allowedInToolbelt(stack: ItemStack): Boolean = false

    override fun onWornTick(stack: ItemStack, player: EntityLivingBase) {
        if (player is EntityPlayer) {
            val eqLastTick = isEquipped(stack)
            val eq = player.isSneaking && isLookingAtSegment(player)
            if (eqLastTick != eq)
                setEquipped(stack, eq)

            if (!player.isSneaking) {
                val angles = 360
                val segAngles = angles / SEGMENTS
                val shift = segAngles / 2
                setRotationBase(stack, getCheckingAngle(player) - shift)
            }
        }
    }
}
