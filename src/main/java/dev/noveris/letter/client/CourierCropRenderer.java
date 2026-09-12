package dev.noveris.letter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.noveris.letter.courier.CourierCropEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class CourierCropRenderer extends GeoEntityRenderer<CourierCropEntity> {
    public CourierCropRenderer(EntityRendererProvider.Context context) {
        super(context, new CourierCropModel());
        this.shadowRadius = 0.35F;
    }

    @Override
    protected boolean shouldShowName(CourierCropEntity entity) {
        return CourierSpeechManager.has(entity.getId());
    }

    @Override
    protected void renderNameTag(CourierCropEntity entity, Component displayName, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, float partialTick) {
        CourierSpeechManager.render(entity, this, this.entityRenderDispatcher, poseStack, bufferSource, packedLight);
    }
}
