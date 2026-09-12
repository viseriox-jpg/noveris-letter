package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.courier.CourierBirdEntity;
import dev.noveris.letter.courier.CourierBoopletEntity;
import dev.noveris.letter.courier.CourierCapybaraEntity;
import dev.noveris.letter.courier.CourierCoatiEntity;
import dev.noveris.letter.courier.CourierMossbloomEntity;
import dev.noveris.letter.courier.CourierRedPandaEntity;
import dev.noveris.letter.courier.precompiled.BoopletModel;
import dev.noveris.letter.courier.precompiled.CapybaraModel;
import dev.noveris.letter.courier.precompiled.CoatiModel;
import dev.noveris.letter.courier.precompiled.MossbloomModel;
import dev.noveris.letter.courier.precompiled.RedPandaModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Mob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class CourierBirdModel extends EntityModel<Mob> {
    private final BoopletModel<CourierBoopletEntity> booplet;
    private final CapybaraModel<CourierCapybaraEntity> capybara;
    private final CoatiModel<CourierCoatiEntity> coati;
    private final MossbloomModel<CourierMossbloomEntity> mossbloom;
    private final RedPandaModel<CourierRedPandaEntity> redPanda;
    private EntityModel active;

    public CourierBirdModel(EntityRendererProvider.Context context) {
        this.booplet = new BoopletModel<>(context.bakeLayer(CourierModelLayers.BOOPLET));
        this.capybara = new CapybaraModel<>(context.bakeLayer(CourierModelLayers.CAPYBARA));
        this.coati = new CoatiModel<>(context.bakeLayer(CourierModelLayers.COATI));
        this.mossbloom = new MossbloomModel<>(context.bakeLayer(CourierModelLayers.MOSSBLOOM));
        this.redPanda = new RedPandaModel<>(context.bakeLayer(CourierModelLayers.RED_PANDA));
        this.active = mossbloom;
    }

    private EntityModel select(CourierBirdEntity entity) {
        var id = entity.getAppearanceId();
        if (id.equals(CourierAppearanceRegistry.BOOPLET_ID)) return booplet;
        if (id.equals(CourierAppearanceRegistry.CAPYBARA_ID)) return capybara;
        if (id.equals(CourierAppearanceRegistry.COATI_ID)) return coati;
        if (id.equals(CourierAppearanceRegistry.RED_PANDA_ID)) return redPanda;
        return mossbloom;
    }

    @Override
    public void setupAnim(Mob entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof CourierBirdEntity courier)) return;
        active = select(courier);
        active.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (active instanceof HierarchicalModel<?> hierarchical) {
            ModelPart root = hierarchical.root();
            root.y += (float) Math.sin(ageInTicks * 0.14F) * 0.08F;
            root.xRot += (float) Math.sin(ageInTicks * 0.10F) * 0.025F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        active.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
