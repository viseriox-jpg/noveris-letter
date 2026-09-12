package dev.noveris.letter.courier;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CourierEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NoverisLetter.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CourierBoopletEntity>> COURIER_BOOPLET = ENTITY_TYPES.register("courier_booplet", () -> EntityType.Builder.of(CourierBoopletEntity::new, MobCategory.CREATURE).sized(0.8F, 0.8F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_booplet").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CourierCapybaraEntity>> COURIER_CAPYBARA = ENTITY_TYPES.register("courier_capybara", () -> EntityType.Builder.of(CourierCapybaraEntity::new, MobCategory.CREATURE).sized(1.5F, 1.0F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_capybara").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CourierCoatiEntity>> COURIER_COATI = ENTITY_TYPES.register("courier_coati", () -> EntityType.Builder.of(CourierCoatiEntity::new, MobCategory.CREATURE).sized(0.9F, 1.1F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_coati").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CourierMossbloomEntity>> COURIER_MOSSBLOOM = ENTITY_TYPES.register("courier_mossbloom", () -> EntityType.Builder.of(CourierMossbloomEntity::new, MobCategory.CREATURE).sized(1.2F, 1.5F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_mossbloom").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CourierRedPandaEntity>> COURIER_RED_PANDA = ENTITY_TYPES.register("courier_red_panda", () -> EntityType.Builder.of(CourierRedPandaEntity::new, MobCategory.CREATURE).sized(0.9F, 0.9F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_red_panda").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CourierLegacyBirdEntity>> COURIER_LEGACY_BIRD = ENTITY_TYPES.register("courier_legacy_bird", () -> EntityType.Builder.of(CourierLegacyBirdEntity::new, MobCategory.CREATURE).sized(0.7F, 0.7F).clientTrackingRange(64).updateInterval(2).build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_legacy_bird").toString()));
    private CourierEntities() { }
}
