package top.bearcabbage.donquixote;

import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.fabricmc.api.ModInitializer;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.item.BundleItem.getAmountFilled;
import static net.minecraft.item.Items.BONE;
import static top.bearcabbage.mirrortree.MirrorTree.FOX_TAIL_ITEM;

public class DonQuixote implements ModInitializer {
	public static final String MOD_ID = "don-quixote";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Item WINDMILL = register("windmill", new WindmillItem(new Item.Settings().maxCount(1).component(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)));
	public static final int MAX_FOXES = 10;

	public static <T extends Item> T register(String path, T item) {
		return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, path), item);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
			if (stack.getItem() == WINDMILL) {
				return TrinketEnums.DropRule.KEEP;
			}
			return rule;
		});

		LOGGER.info("To dream the impossible dream, that is my quest.");
	}

	public static boolean wearWindmill(ServerPlayerEntity player) {
		if (player == null) return false;
		AtomicBoolean re = new AtomicBoolean(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> trinkets.forEach((slot, stack) -> {
			if (slot.getId().contains("chest/necklace")) {
				if (stack.isOf(WINDMILL)) {
					BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
					BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
					ItemStack itemStack = builder.removeFirst();
					if (itemStack!=null && itemStack.getCount()>0) {
						re.set(true);
						if(!itemStack.isOf(BONE)) {
							builder.add(itemStack);
						}
					}
					stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
				}
			}
		}));
		return re.get();
	}
}