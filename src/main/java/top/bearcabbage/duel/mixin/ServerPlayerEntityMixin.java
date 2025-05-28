package top.bearcabbage.duel.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.bearcabbage.duel.ServerPlayerEntityAccessor;

import static top.bearcabbage.duel.Duel.wearNecklace;
import static top.bearcabbage.duel.Duel.LOGGER;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements ServerPlayerEntityAccessor {
    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    public ServerPlayerEntity oldAttacker = null;

    public ServerPlayerEntity getOldAttacker() {
        return this.oldAttacker;
    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void duel$onDeath(DamageSource damageSource, CallbackInfo ci) {
        if (damageSource.getSource()!=null && damageSource.getSource() instanceof ServerPlayerEntity player) this.oldAttacker = player;
        else this.oldAttacker = null;
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void duel$copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if(!alive && !this.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) && !oldPlayer.isSpectator() && wearNecklace(oldPlayer) && wearNecklace(((ServerPlayerEntityAccessor)oldPlayer).getOldAttacker())) {
            this.getInventory().clone(oldPlayer.getInventory());
            this.experienceLevel = oldPlayer.experienceLevel;
            this.totalExperience = oldPlayer.totalExperience;
            this.experienceProgress = oldPlayer.experienceProgress;
            this.setScore(oldPlayer.getScore());
            this.oldAttacker = ((ServerPlayerEntityAccessor)oldPlayer).getOldAttacker();
        }
    }
}
