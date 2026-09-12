package dev.noveris.letter.client;

import dev.noveris.letter.NoverisLetter;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public final class CourierModelLayers {
    public static final ModelLayerLocation BOOPLET = layer("booplet");
    public static final ModelLayerLocation CAPYBARA = layer("capybara");
    public static final ModelLayerLocation COATI = layer("coati");
    public static final ModelLayerLocation MOSSBLOOM = layer("mossbloom");
    public static final ModelLayerLocation RED_PANDA = layer("red_panda");
    private static ModelLayerLocation layer(String name) { return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(NoverisLetter.MOD_ID, name), "main"); }
    private CourierModelLayers() { }
}
