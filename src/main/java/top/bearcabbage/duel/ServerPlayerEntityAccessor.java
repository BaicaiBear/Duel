package top.bearcabbage.duel;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ServerPlayerEntityAccessor {
    public ServerPlayerEntity getOldAttacker();
}
