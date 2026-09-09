package dev.noveris.letter.client;

import dev.noveris.letter.network.SendLetterPayload;
import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Noveris postal panel. It contains presentation only; mutations go through payloads. */
public final class MailScreen extends Screen {
    private static final int BG = 0xEA0D0C09;
    private static final int PANEL = 0xFF17140E;
    private static final int GOLD = 0xFFFFD84D;
    private static final int ACTIVE = 0xFFD6A800;
    private static final int TEXT = 0xFFFFFBEB;
    private static final int MUTED = 0xFFC9BE9B;
    private static final String[] TABS = {"RECEBIDAS", "ENVIADAS", "ESCREVER", "CARTEIRO"};
    private final int initialTab;
    private int tab;
    private EditBox recipient;
    private EditBox subject;
    private EditBox message;
    private List<MailSnapshotPayload.Entry> entries = List.of();

    public MailScreen(int tab) {
        super(Component.literal("Serviço Postal de Noveris"));
        this.initialTab = Math.max(0, Math.min(3, tab));
        this.tab = this.initialTab;
    }

    @Override protected void init() {
        int left = panelLeft();
        int width = panelWidth();
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> selectTab(index))
                    .bounds(left + 12 + i * ((width - 24) / 4), top() + 42, (width - 24) / 4 - 4, 24).build());
        }
        if (tab == 2) createComposeFields(left, width);
    }

    /** Keep the world crisp; Screen#render calls this hook before drawing widgets. */
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: the world rendered by Minecraft remains visible without blur.
    }

    private void createComposeFields(int left, int width) {
        recipient = addRenderableWidget(new EditBox(font, left + 24, top() + 92, width - 48, 22, Component.literal("Destinatário")));
        recipient.setMaxLength(80);
        recipient.setHint(Component.literal("NOME DO DESTINATÁRIO"));
        subject = addRenderableWidget(new EditBox(font, left + 24, top() + 132, width - 48, 22, Component.literal("Assunto")));
        subject.setMaxLength(120);
        subject.setHint(Component.literal("ASSUNTO (OPCIONAL)"));
        message = addRenderableWidget(new EditBox(font, left + 24, top() + 172, width - 48, 70, Component.literal("Mensagem")));
        message.setMaxLength(1500);
        message.setHint(Component.literal("ESCREVA SUA CORRESPONDÊNCIA"));
        addRenderableWidget(Button.builder(Component.literal("CANCELAR"), button -> onClose())
                .bounds(left + 24, bottom() - 42, 110, 28).build());
        addRenderableWidget(Button.builder(Component.literal("SELAR E ENVIAR"), button -> send())
                .bounds(left + width - 154, bottom() - 42, 130, 28).build());
    }

    private void selectTab(int index) {
        if (index == 2) {
            tab = 2;
            clearWidgets();
            init();
            return;
        }
        tab = index;
        clearWidgets();
        init();
        PacketDistributor.sendToServer(new RequestMailSnapshotPayload(index));
    }

    public void setSnapshot(MailSnapshotPayload payload) {
        if (payload.tab() == tab) entries = payload.entries();
    }

    private void send() {
        if (recipient == null || recipient.getValue().isBlank() || message == null || message.getValue().isBlank()) return;
        PacketDistributor.sendToServer(new SendLetterPayload(recipient.getValue(), subject.getValue(), message.getValue()));
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = top();
        int right = left + panelWidth();
        graphics.fill(0, 0, width, height, 0x66000000);
        graphics.fill(left, top, right, bottom(), BG);
        graphics.fill(left + 3, top + 3, right - 3, bottom() - 3, PANEL);
        graphics.fill(left + 3, top + 3, right - 3, top + 6, GOLD);
        graphics.fill(left + 3, top + 38, right - 3, top + 40, GOLD);
        drawCorners(graphics, left + 8, top + 8, right - 8, bottom() - 8);
        graphics.drawString(font, "SERVIÇO POSTAL DE NOVERIS", left + 18, top + 15, GOLD, false);
        graphics.drawString(font, tab == 2 ? "ESCREVER CORRESPONDÊNCIA" : TABS[tab], left + 24, top + 72, 0xFFFFFBEB, false);
        if (tab == 0 || tab == 1) drawLetters(graphics);
        if (tab == 3) drawEmpty(graphics, "MEUS CARTEIROS", "Seu portador selecionado aparecerá aqui.");
        if (tab == 2 && message != null) graphics.drawString(font, message.getValue().length() + " / 1500", right - 88, top + 250, MUTED, false);
        graphics.drawString(font, "ESC  FECHAR", left + 18, bottom() - 16, MUTED, false);
        graphics.drawString(font, "◆  SERVIÇO POSTAL ATIVO", left + panelWidth() / 2 - 62, bottom() - 16, MUTED, false);
        graphics.drawString(font, "NOVERIS", right - 52, bottom() - 16, GOLD, false);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawCustomButtons(graphics, mouseX, mouseY);
    }

    private void drawCustomButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft();
        int width = panelWidth();
        int tabWidth = (width - 24) / 4 - 4;
        for (int i = 0; i < TABS.length; i++) {
            int x = left + 12 + i * ((width - 24) / 4);
            drawNoverisButton(graphics, x, top() + 42, tabWidth, 24, TABS[i], i == tab, mouseX, mouseY);
        }
        if (tab == 2) {
            drawNoverisButton(graphics, left + 24, bottom() - 42, 110, 28, "CANCELAR", false, mouseX, mouseY);
            drawNoverisButton(graphics, left + width - 154, bottom() - 42, 130, 28, "SELAR E ENVIAR", true, mouseX, mouseY);
        }
    }

    private void drawNoverisButton(GuiGraphics graphics, int x, int y, int buttonWidth, int buttonHeight,
                                   String label, boolean selected, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + buttonWidth && mouseY >= y && mouseY < y + buttonHeight;
        int fill = selected ? ACTIVE : hovered ? 0xFFF2C94C : PANEL;
        int textColor = hovered || selected ? TEXT : MUTED;
        graphics.fill(x, y, x + buttonWidth, y + buttonHeight, GOLD);
        graphics.fill(x + 2, y + 2, x + buttonWidth - 2, y + buttonHeight - 2, fill);
        graphics.drawCenteredString(font, label, x + buttonWidth / 2, y + (buttonHeight - 8) / 2, textColor);
    }

    private void drawEmpty(GuiGraphics graphics, String title, String description) {
        int left = panelLeft();
        int center = left + panelWidth() / 2;
        graphics.drawCenteredString(font, title, center, top() + 150, GOLD);
        graphics.drawCenteredString(font, description, center, top() + 174, MUTED);
    }

    private void drawLetters(GuiGraphics graphics) {
        if (entries.isEmpty()) {
            drawEmpty(graphics, tab == 0 ? "NENHUMA CORRESPONDÊNCIA ABERTA" : "NENHUMA CARTA SELADA", "As cartas aparecerão aqui quando chegarem.");
            return;
        }
        int left = panelLeft() + 24;
        int right = panelLeft() + panelWidth() - 24;
        int y = top() + 92;
        for (MailSnapshotPayload.Entry entry : entries.stream().limit(8).toList()) {
            graphics.fill(left, y, right, y + 38, 0xFF0D0C09);
            graphics.fill(left, y, left + 3, y + 38, entry.status().name().equals("READ") ? MUTED : GOLD);
            String person = tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient();
            graphics.drawString(font, person, left + 12, y + 6, TEXT, false);
            graphics.drawString(font, entry.subject().isBlank() ? "Sem assunto" : entry.subject(), left + 12, y + 20, MUTED, false);
            graphics.drawString(font, displayStatus(entry.status().name()), right - 76, y + 13, GOLD, false);
            y += 44;
        }
    }

    private String displayStatus(String status) {
        return switch (status) {
            case "READ" -> "LIDA";
            case "DELIVERED" -> "NOVA";
            case "IN_TRANSIT" -> "A CAMINHO";
            default -> status;
        };
    }

    private void drawCorners(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.drawString(font, "+", left, top, GOLD, false);
        graphics.drawString(font, "+", right - 7, top, GOLD, false);
        graphics.drawString(font, "+", left, bottom - 9, GOLD, false);
        graphics.drawString(font, "+", right - 7, bottom - 9, GOLD, false);
    }

    private int panelWidth() { return Math.min(720, width - 40); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelHeight() { return Math.min(420, height - 30); }
    private int top() { return (height - panelHeight()) / 2; }
    private int bottom() { return top() + panelHeight(); }
}
