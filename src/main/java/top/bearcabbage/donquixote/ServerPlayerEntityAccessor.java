package top.bearcabbage.donquixote;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ServerPlayerEntityAccessor {
    public ServerPlayerEntity getOldAttacker();
}
