package shadowfox.botanicaladdons.api;

import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author WireSegal
 *         Created at 11:57 AM on 4/24/16.
 */
public interface IPriestlyEmblem {
    boolean isAwakened(@Nonnull ItemStack stack);

    void setAwakened(@Nonnull ItemStack stack, boolean awakened);

    @Nullable
    IFaithVariant getVariant(@Nonnull ItemStack stack);
}
