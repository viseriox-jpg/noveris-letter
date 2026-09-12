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

    public static final DeferredHolder<EntityType<?>, EntityType<CourierBirdEntity>> COURIER_BIRD =
            ENTITY_TYPES.register("courier_bird", () -> EntityType.Builder.of(CourierBirdEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.7F)
                    .clientTrackingRange(64)
                    .updateInterval(2)
                    .build(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, "courier_bird").toString()));

    private CourierEntities() { }
}
