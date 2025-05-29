package top.bearcabbage.duel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.event.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;


public class Duel implements ModInitializer {
	public static final String MOD_ID = "duel";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final DConfig config = new DConfig(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("config.json"));

	public static final Item NECKLACE = register("necklace", new NecklaceItem(new Item.Settings().maxCount(1).component(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)));
	public static final Item EXHAUSTION = register("exhaustion", new Item(new Item.Settings()));
	public static int MAX_WAGER;
	public static Item WAGER = Items.DIAMOND;
	public static Team DUEL_TEAM;


	public static <T extends Item> T register(String path, T item) {
		return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, path), item);
	}

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		TrinketDropCallback.EVENT.register((rule, stack, ref, entity) -> {
			if (stack.getItem() == NECKLACE) {
				return TrinketEnums.DropRule.KEEP;
			}
			return rule;
		});

		TrinketEquipCallback.EVENT.register((stack, slot, entity) -> {
			if (slot.getId().contains("chest/necklace") && checkNecklace(stack) && entity instanceof ServerPlayerEntity player) {
				if (player.getScoreboardTeam()==null) player.getServerWorld().getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), DUEL_TEAM);
			}
		});

		TrinketUnequipCallback.EVENT.register((stack, slot, entity) -> {
			if (slot.getId().contains("chest/necklace") && entity instanceof ServerPlayerEntity player) {
				if (player.getScoreboardTeam() == DUEL_TEAM) {
					player.getServerWorld().getScoreboard().removeScoreHolderFromTeam(player.getNameForScoreboard(), DUEL_TEAM);
				}
			}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Initialize the bedroom coordinates
			String wager = config.getOrDefault("wager", "minecraft:diamond");
			MAX_WAGER = config.getOrDefault("max_wager", 10);
			try {
				WAGER = Registries.ITEM.get(Identifier.of(wager.split(":")[0], wager.split(":")[1]));
			} catch (Exception e) {
				LOGGER.error("Failed to load wager item: " + wager + ". Defaulting to diamond.");
				WAGER = Items.DIAMOND;
			}

			// Initialize the duel team
			DUEL_TEAM = server.getScoreboard().addTeam("Duel_Duelers");
			DUEL_TEAM.setShowFriendlyInvisibles(false);
			DUEL_TEAM.setPrefix(Text.of("§c⚔ "));
			DUEL_TEAM.setSuffix(Text.of(" §c⚔"));
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((player, oldPlayer, alive) -> {
			if (wearNecklace(oldPlayer)) if (wearNecklace(((ServerPlayerEntityAccessor)oldPlayer).getOldAttacker())) {
				ServerPlayerEntity oldAttacker = ((ServerPlayerEntityAccessor)oldPlayer).getOldAttacker();
				ServerWorld world = oldAttacker.getServerWorld();
				player.teleport(world, oldAttacker.getX(), oldAttacker.getY(), oldAttacker.getZ(), oldAttacker.getYaw(), oldAttacker.getPitch());
				// This part of code is modified from chorus fruit.
				for(int i = 0; i < 16; ++i) {
					double d = player.getX() + (player.getRandom().nextDouble() - (double)0.5F) * (double)16.0F;
					double e = MathHelper.clamp(player.getY() + (double)(player.getRandom().nextInt(16) - 8), world.getBottomY(), (world.getBottomY() + (world).getLogicalHeight() - 1));
					double f = player.getZ() + (player.getRandom().nextDouble() - (double)0.5F) * (double)16.0F;
					if (player.hasVehicle()) {
						((LivingEntity) player).stopRiding();
					}

					Vec3d vec3d = player.getPos();
					if (player.teleport(d, e, f, true)) {
						world.emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(player));
						SoundCategory soundCategory;
						SoundEvent soundEvent;
                        soundEvent = SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
                        soundCategory = SoundCategory.PLAYERS;

                        world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, soundCategory);
						((LivingEntity) player).onLanding();
						break;
					}
				}
			} else if (player.getScoreboardTeam() == DUEL_TEAM) {
				player.getServerWorld().getScoreboard().removeScoreHolderFromTeam(player.getNameForScoreboard(), DUEL_TEAM);
			}
		});

		LOGGER.info("To dream the impossible dream, that is my quest.");
	}

	public static boolean checkNecklace(ItemStack stack) {
		if (!stack.isOf(NECKLACE)) return false;
		BundleContentsComponent bundleContentsComponent = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
		if (bundleContentsComponent == null) return false;
		BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
		return builder.removeFirst() instanceof ItemStack itemStack && itemStack.getCount()>0;
	}

	public static boolean wearNecklace(ServerPlayerEntity player) {
		if (player == null) return false;
		AtomicBoolean re = new AtomicBoolean(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> trinkets.forEach((slot, stack) -> {
			if (slot.getId().contains("chest/necklace")) {
				if (stack.isOf(NECKLACE)) {
					BundleContentsComponent bundleContentsComponent = (BundleContentsComponent)stack.get(DataComponentTypes.BUNDLE_CONTENTS);
					BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
					ItemStack itemStack = builder.removeFirst();
					if (itemStack!=null && itemStack.getCount()>0) {
						re.set(true);
						if(!itemStack.isOf(EXHAUSTION)) {
							builder.add(itemStack);
						}
					}
					stack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
				}
			}
		}));
		return re.get();
	}

	private static class DConfig  {
		private final Path filePath;
		private JsonObject jsonObject;
		private final Gson gson;

		public DConfig(Path filePath) {
			this.filePath = filePath;
			this.gson = new GsonBuilder().setPrettyPrinting().create();
			try {
				if (Files.notExists(filePath.getParent())) {
					Files.createDirectories(filePath.getParent());
				}
				if (Files.notExists(filePath)) {
					Files.createFile(filePath);
					try (FileWriter writer = new FileWriter(filePath.toFile())) {
						writer.write("{}");
					}
				}

			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
			load();
		}

		public void load() {
			try (FileReader reader = new FileReader(filePath.toFile())) {
				this.jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (IOException e) {
				this.jsonObject = new JsonObject();
			}
		}

		public void save() {
			try (FileWriter writer = new FileWriter(filePath.toFile())) {
				gson.toJson(jsonObject, writer);
			} catch (IOException e) {
				LOGGER.error(e.toString());
			}
		}

		public void set(String key, Object value) {
			jsonObject.add(key, gson.toJsonTree(value));
		}

		public <T> T get(String key, Class<T> clazz) {
			return gson.fromJson(jsonObject.get(key), clazz);
		}

		public <T> T getOrDefault(String key, T defaultValue) {
			if (jsonObject.has(key)) {
				return gson.fromJson(jsonObject.get(key), (Class<T>) defaultValue.getClass());
			}
			else {
				set(key, defaultValue);
				save();
				return defaultValue;
			}
		}

		public <T> T getAll(Class<T> clazz) {
			return gson.fromJson(jsonObject, clazz);
		}
	}
}