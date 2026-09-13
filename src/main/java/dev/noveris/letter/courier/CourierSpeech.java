package dev.noveris.letter.courier;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class CourierSpeech {
    private CourierSpeech() { }

    public static String deliveryFor(ResourceLocation appearanceId) {
        List<String> lines = switch (appearanceId.toString()) {
            case "noveris_letter:melon_critter" -> List.of("Chegou! ♡", "Tudo certo!", "Entrega feita!");
            case "noveris_letter:carrot_critter" -> List.of("Prontinho!", "Cheguei!", "Entrega rápida!");
            case "noveris_letter:wheat_critter" -> List.of("Mais uma entregue.", "Tudo certo.", "Com calma.");
            case "noveris_letter:pumpkin_critter" -> List.of("Chegou do além!", "Pegadinha!", "Uma entrega assustadora!");
            case "noveris_letter:potato_critter" -> List.of("Ufa...", "Cheguei.", "Entrega feita.");
            case "noveris_letter:sparrow" -> List.of();
            default -> List.of("Entrega feita!");
        };
        return lines.isEmpty() ? "" : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    public static String pickupArrivalFor(ResourceLocation appearanceId) {
        List<String> lines = switch (appearanceId.toString()) {
            case "noveris_letter:melon_critter" -> List.of("Vim buscar! ♡", "Vim buscar sua carta!");
            case "noveris_letter:carrot_critter" -> List.of("Vim buscar!", "Já levo!", "Pode deixar comigo!");
            case "noveris_letter:wheat_critter" -> List.of("Vou levar com cuidado.", "Vim buscar sua carta.", "Pode deixar comigo.");
            case "noveris_letter:pumpkin_critter" -> List.of("Hehehe... uma carta!", "Vim buscar!", "Essa vai viajar!");
            case "noveris_letter:potato_critter" -> List.of("Ufa... vim buscar.", "Pode deixar comigo.", "Já levo!");
            case "noveris_letter:sparrow" -> List.of();
            default -> List.of();
        };
        return lines.isEmpty() ? "" : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }

    public static String pickupAcceptedFor(ResourceLocation appearanceId) {
        List<String> lines = switch (appearanceId.toString()) {
            case "noveris_letter:melon_critter" -> List.of("Pode deixar comigo! ♡", "Vou levar!", "Até já!");
            case "noveris_letter:carrot_critter" -> List.of("Pode deixar comigo!", "Já estou indo!", "Até já!");
            case "noveris_letter:wheat_critter" -> List.of("Vou levar com cuidado.", "Tudo certo.", "Até a entrega.");
            case "noveris_letter:pumpkin_critter" -> List.of("Hehehe... até a entrega!", "Essa vai longe!", "Até já!");
            case "noveris_letter:potato_critter" -> List.of("Ufa... consegui!", "Vou levar.", "Até já.");
            case "noveris_letter:sparrow" -> List.of();
            default -> List.of();
        };
        return lines.isEmpty() ? "" : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }
}
