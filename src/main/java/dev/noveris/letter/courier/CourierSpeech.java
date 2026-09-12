package dev.noveris.letter.courier;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CourierSpeech {
    private CourierSpeech() { }

    public static String randomFor(ResourceLocation appearanceId) {
        List<String> lines = switch (appearanceId.toString()) {
            case "noveris_letter:melon_critter" -> List.of("Chegou! ♡", "Tudo certo!", "Entrega feita!");
            case "noveris_letter:carrot_critter" -> List.of("Prontinho!", "Cheguei!", "Entrega rápida!");
            case "noveris_letter:wheat_critter" -> List.of("Mais uma entregue.", "Tudo certo.", "Com calma.");
            case "noveris_letter:pumpkin_critter" -> List.of("Chegou do além!", "Pegadinha!", "Uma entrega assustadora!");
            case "noveris_letter:potato_critter" -> List.of("Ufa...", "Cheguei.", "Entrega feita.");
            case "noveris_letter:sparrow" -> List.of("Piu! Entregue.", "Entrega expressa!", "Cheguei voando!");
            default -> List.of("Entrega feita!");
        };
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }
}
