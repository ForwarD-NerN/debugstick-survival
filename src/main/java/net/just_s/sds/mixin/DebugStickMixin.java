package net.just_s.sds.mixin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.just_s.sds.SDSMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.Registries;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(DebugStickItem.class)
public class DebugStickMixin {
    @Shadow
    private static void sendMessage(PlayerEntity player, Text message) {}

    @Shadow
    private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {
        return null;
    }
    @Shadow
    private static <T extends Comparable<T>> BlockState cycle(BlockState state, Property<T> property, boolean inverse) {
        return null;
    }



    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void onUse(PlayerEntity player, BlockState state, WorldAccess world, BlockPos pos, boolean update, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        // if the player already does have the rights to use Debug Stick, the mod should not interfere
        if (player.isCreativeLevelTwoOp() || !Permissions.check(player, "survivaldebugstick.use", 0)) return;

        Block block = state.getBlock();
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> properties = stateManager.getProperties();

        // check if block is modifiable by the config
        if (!sds$isBlockStateAllowedToModify(state) || properties.isEmpty()) {
            sendMessage(player, Text.of(SDSMod.config.messages.nomodify));
            cir.setReturnValue(false);
            return;
        }

        // https://minecraft.fandom.com/wiki/Debug_Stick#Item_data
        // to remember the data of which property for which block is chosen,
        // Minecraft Devs decided to use NBT data for Debug Stick.
        // Who am I to disagree?
        NbtCompound nbtCompound = stack.getOrCreateSubNbt("DebugProperty");

        String blockName = Registries.BLOCK.getId(block).toString();
        String propertyName = nbtCompound.getString(blockName);

        Property<?> property = stateManager.getProperty(propertyName);
        if(property != null && !sds$isPropertyModifiable(property, block, state)) property = null;


        if (player.isSneaking()) {
            // select next property
            property = sds$getNextProperty(properties, property, block, state);

            if(property == null){
                sendMessage(player, Text.of(SDSMod.config.messages.notfound));
                return;
            }

            // save chosen property in the NBT data of Debug Stick
            nbtCompound.putString(blockName, property.getName());

            // send the player a message of successful selecting
            sendMessage(player, Text.of(
                            String.format(
                                    SDSMod.config.messages.select,
                                property.getName(),
                                getValueString(state, property)
                            )
                    )
            );
        } else {
            // change value of property
            property = property == null ? sds$getNextProperty(properties, null, block, state) : property;
            if(property == null){
                sendMessage(player, Text.of(SDSMod.config.messages.notfound));
                return;
            }


            // generate new state of chosen block with modified property
            BlockState newState = cycle(state, property, false);
            // update chosen block with its new state
            world.setBlockState(pos, newState, 18);
            // send the player a message of successful modifying
            sendMessage(player, Text.of(
                            String.format(
                                    SDSMod.config.messages.change,
                                    property.getName(),
                                    getValueString(newState, property)
                            )
                    )
            );
        }
        cir.setReturnValue(true);
    }



    /**
     * Choose next property that is appropriate for the configuration file
     * */
    @Unique
    private Property<?> sds$getNextProperty(Collection<Property<?>> properties, @Nullable Property<?> property, @Nullable Block block, BlockState state) {
        int len = properties.size();

        do { // simply scrolling through the list of properties until suitable is found
            property = Util.next(properties, property);
            len--;
        } while (len > 0 && !sds$isPropertyModifiable(property, block, state));

        //Fixes https://github.com/JustS-js/debugStickSurvival/issues/5 by returning null if the input property is the same as the next.
        return sds$isPropertyModifiable(property, block, state) ? property : null;
    }

    /**
     * Check via config if chosen blockstate is able to be modified in survival
     * */
    @Unique
    private boolean sds$isBlockStateAllowedToModify(BlockState state) {
        return SDSMod.isBlockStateAllowed(state);
    }

    /**
     * Check via config if chosen property is able to be modified in survival
     * */
    @Unique
    private boolean sds$isPropertyModifiable(Property<?> property, @Nullable Block block, BlockState state) {
        return SDSMod.isPropertyAllowed(property.getName(), block, state);
    }
}
