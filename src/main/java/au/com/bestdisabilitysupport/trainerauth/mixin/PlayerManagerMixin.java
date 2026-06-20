package au.com.bestdisabilitysupport.trainerauth.mixin;

import au.com.bestdisabilitysupport.trainerauth.BestTrainerAuthMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @Inject(method = "loadPlayerData", at = @At("HEAD"))
    private void bestTrainerAuth$prepareTrainerData(ServerPlayerEntity player, CallbackInfoReturnable<Optional<NbtCompound>> cir) {
        BestTrainerAuthMod.LOGGER.info("[BestTrainerAuth] PlayerManagerMixin fired for {}", player.getName().getString());

        if (BestTrainerAuthMod.trainerBridge() != null) {
            Optional<String> result = BestTrainerAuthMod.trainerBridge().prepareForJoin(player.getUuid());
            if (result.isEmpty()) {
                BestTrainerAuthMod.trainerBridge().clearStaleLiveSessionIfNeeded(player.getUuid());
            } else {
                result.ifPresent(trainer ->
                        BestTrainerAuthMod.LOGGER.info("[BestTrainerAuth] Prepared trainer {} for {}", trainer, player.getName().getString())
                );
            }
        }
    }
}
