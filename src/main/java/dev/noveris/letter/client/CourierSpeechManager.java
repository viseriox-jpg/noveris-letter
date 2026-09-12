package dev.noveris.letter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class CourierSpeechManager {
    private static final Map<Integer, Speech> ACTIVE = new HashMap<>();

    private CourierSpeechManager() { }

    public static void show(int entityId, String message, int durationTicks) {
        ACTIVE.put(entityId, new Speech(message, Math.max(1, durationTicks)));
    }

    public static boolean has(int entityId) {
        return ACTIVE.containsKey(entityId);
    }

    public static float alpha(int entityId) {
        Speech speech = ACTIVE.get(entityId);
        if (speech == null) return 0.0F;
        if (speech.remaining() <= 8) return speech.remaining() / 8.0F;
        if (speech.elapsed() < 5) return speech.elapsed() / 5.0F;
        return 1.0F;
    }

    public static void render(Entity entity, EntityRenderer<?> renderer, EntityRenderDispatcher dispatcher,
                               PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Speech speech = ACTIVE.get(entity.getId());
        if (speech == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        String message = speech.message();
        float scale = 0.025F;
        float y = entity.getBbHeight() + 0.62F;
        int opacity = (int) (alpha(entity.getId()) * 255.0F) & 0xFF;
        int textColor = (opacity << 24) | 0xF2D27A;
        int backgroundOpacity = (int) (opacity * 0.88F) & 0xFF;
        int background = (backgroundOpacity << 24) | 0x17140F;

        poseStack.pushPose();
        poseStack.translate(0.0D, y, 0.0D);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        Component text = Component.literal("  " + message + "  ");
        float textX = -font.width(text) / 2.0F;
        float textY = -font.lineHeight / 2.0F;
        font.drawInBatch(text, textX, textY, textColor, false, matrix, buffer,
                Font.DisplayMode.NORMAL, background, packedLight);

        Component tail = Component.literal("▾");
        font.drawInBatch(tail, -font.width(tail) / 2.0F, 5.0F,
                textColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Iterator<Map.Entry<Integer, Speech>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Speech> entry = iterator.next();
            Speech speech = entry.getValue().tick();
            if (speech.remaining() <= 0) iterator.remove();
            else entry.setValue(speech);
        }
    }

    private record Speech(String message, int remaining, int elapsed) {
        private Speech(String message, int duration) {
            this(message, duration, 0);
        }

        private Speech tick() {
            return new Speech(message, remaining - 1, elapsed + 1);
        }
    }
}
