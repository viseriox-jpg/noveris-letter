package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierBirdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

public final class CourierBirdRenderer extends MobRenderer<Mob, CourierBirdModel> {
    private static final ResourceLocation BOOPLET_TEXTURE = id("textures/entity/courier/booplet.png");
    private static final ResourceLocation CAPYBARA_TEXTURE = id("textures/entity/courier/capybara.png");
    private static final ResourceLocation COATI_TEXTURE = id("textures/entity/courier/coati.png");
    private static final ResourceLocation MOSSBLOOM_TEXTURE = id("textures/entity/courier/mossbloom.png");
    private static final ResourceLocation RED_PANDA_TEXTURE = id("textures/entity/courier/red_panda.png");
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("noveris_letter", path); }

    public CourierBirdRenderer(EntityRendererProvider.Context context) { super(context, new CourierBirdModel(context), 0.35F); }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        if (!(entity instanceof CourierBirdEntity courier)) return MOSSBLOOM_TEXTURE;
        var id = courier.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.BOOPLET_ID)) return BOOPLET_TEXTURE;
        if (id.equals(CourierAppearanceRegistry.CAPYBARA_ID)) return CAPYBARA_TEXTURE;
        if (id.equals(CourierAppearanceRegistry.COATI_ID)) return COATI_TEXTURE;
        if (id.equals(CourierAppearanceRegistry.RED_PANDA_ID)) return RED_PANDA_TEXTURE;
        return MOSSBLOOM_TEXTURE;
    }
}
