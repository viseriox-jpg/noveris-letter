package dev.noveris.letter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.noveris.letter.courier.CourierLegacyBirdEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class LegacyBirdRenderer extends GeoEntityRenderer<CourierLegacyBirdEntity> {
    public LegacyBirdRenderer(EntityRendererProvider.Context context) {
        super(context, new LegacyBirdModel());
        this.shadowRadius = 0.35F;
    }

    @Override
    public boolean shouldShowName(CourierLegacyBirdEntity entity) {
        return CourierSpeechManager.has(entity.getId());
    }

    @Override
    protected void renderNameTag(CourierLegacyBirdEntity entity, Component displayName, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, float partialTick) {
        CourierSpeechManager.render(entity, this, this.entityRenderDispatcher, poseStack, bufferSource, packedLight);
    }
}
