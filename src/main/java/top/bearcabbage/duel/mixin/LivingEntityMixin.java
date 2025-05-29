package top.bearcabbage.duel.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.Attackable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.item.BundleItem.getAmountFilled;
import static top.bearcabbage.duel.Duel.*;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Attackable {
	public LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}

	@Inject(method = "drop", at = @At("HEAD"), cancellable = true)
	private void drop(ServerWorld world, DamageSource damageSource, CallbackInfo ci) {
		// Prevent LivingEntity from dropping items
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof ServerPlayerEntity player && player.getPrimeAdversary() instanceof ServerPlayerEntity attacker && wearNecklace(attacker) && player!=attacker) {
			TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> trinkets.forEach((slot, stack) -> {
				if (slot.getId().contains("chest/necklace")) {
					if (stack.isOf(NECKLACE)) {
						if (getAmountFilled(stack) > 0) {
							int ran = (int) (Math.random()* MAX_WAGER);
							BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
							BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
							ItemStack itemStack = builder.removeFirst();
                            if (itemStack != null) {
                                ItemStack dropStack = new ItemStack(itemStack.getItem(), itemStack.getCount());
								if (itemStack.getCount() > ran) {
									itemStack.setCount(itemStack.getCount() - ran);
									dropStack.setCount(ran);
									builder.add(itemStack);
								} else builder.add(new ItemStack(EXHAUSTION,1));
								player.dropItem(dropStack, true, false);
								stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
								ci.cancel();
                            }

						}
					}
				}
			}));
		}
	}
}