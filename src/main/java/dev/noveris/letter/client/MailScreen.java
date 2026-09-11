package dev.noveris.letter.client;

import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import dev.noveris.letter.network.SendLetterPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Noveris postal panel. Presentation only; server owns all postal state. */
public final class MailScreen extends Screen {
    private static final int BG = 0xEA0D0C09, PANEL = 0xFF17140E, GOLD = 0xFFFFD84D, ACTIVE = 0xFFD6A800, TEXT = 0xFFFFFBEB, MUTED = 0xFFC9BE9B;
    private static final String[] TABS = {"RECEBIDAS", "ENVIADAS", "ESCREVER", "CARTEIRO"};
    private final int initialTab;
    private int tab, page, totalPages = 1;
    private boolean anonymous;
    private EditBox recipient, subject, message;
    private List<MailSnapshotPayload.Entry> entries = List.of();
    private MailSnapshotPayload.Entry preview;

    public MailScreen(int tab) { super(Component.literal("Serviço Postal de Noveris")); this.initialTab = Math.max(0, Math.min(3, tab)); this.tab = initialTab; }

    @Override protected void init() {
        int left = panelLeft(), width = panelWidth();
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> selectTab(index)).bounds(left + 12 + i * ((width - 24) / 4), top() + 42, (width - 24) / 4 - 4, 24).build());
        }
        if (tab == 2) createComposeFields(left, width);
    }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    private void createComposeFields(int left, int width) {
        recipient = addRenderableWidget(new EditBox(font, left + 24, top() + 92, width - 48, 26, Component.literal("Destinatário")));
        recipient.setMaxLength(80); recipient.setHint(Component.literal("NICK DO DESTINATÁRIO"));
        subject = addRenderableWidget(new EditBox(font, left + 24, top() + 132, width - 48, 26, Component.literal("Assunto")));
        subject.setMaxLength(120); subject.setHint(Component.literal("ASSUNTO (OPCIONAL)"));
        message = addRenderableWidget(new EditBox(font, left + 24, top() + 172, width - 48, 70, Component.literal("Mensagem")));
        message.setMaxLength(1500); message.setHint(Component.literal("ESCREVA SUA CORRESPONDÊNCIA"));
        addRenderableWidget(Button.builder(Component.literal("CANCELAR"), button -> onClose()).bounds(left + 24, bottom() - 42, 110, 28).build());
        addRenderableWidget(Button.builder(Component.literal("SELAR E ENVIAR"), button -> send()).bounds(left + width - 154, bottom() - 42, 130, 28).build());
    }
    private void selectTab(int index) { tab = index; page = 0; preview = null; clearWidgets(); init(); if (index == 0 || index == 1) requestPage(); }
    private void requestPage() { PacketDistributor.sendToServer(new RequestMailSnapshotPayload(tab, page)); }
    public void setSnapshot(MailSnapshotPayload payload) { if (payload.tab() == tab) { page = payload.page(); totalPages = Math.max(1, payload.totalPages()); entries = payload.entries(); preview = null; } }

    private void send() {
        if (recipient == null || recipient.getValue().isBlank() || message == null || message.getValue().isBlank()) return;
        PacketDistributor.sendToServer(new SendLetterPayload(recipient.getValue().trim(), subject.getValue().trim(), message.getValue(), anonymous));
        onClose();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (tab == 2 && mouseY >= bottom() - 42 && mouseY <= bottom() - 14 && mouseX >= panelLeft() + 150 && mouseX < panelLeft() + 270) { anonymous = !anonymous; return true; }
        if (tab != 0 && tab != 1) return false;
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 92;
        for (MailSnapshotPayload.Entry entry : entries) {
            if (mouseX >= left && mouseX <= right && mouseY >= y && mouseY <= y + 38) { preview = preview == entry ? null : entry; return true; }
            y += 44;
        }
        if (mouseY >= bottom() - 42 && mouseY <= bottom() - 14) {
            if (mouseX >= left && mouseX < left + 130 && page > 0) { page--; requestPage(); return true; }
            if (mouseX >= right - 130 && mouseX <= right && page + 1 < totalPages) { page++; requestPage(); return true; }
        }
        return false;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft(), top = top(), right = left + panelWidth();
        graphics.fill(0, 0, width, height, 0x66000000); graphics.fill(left, top, right, bottom(), BG); graphics.fill(left + 3, top + 3, right - 3, bottom() - 3, PANEL);
        graphics.fill(left + 3, top + 3, right - 3, top + 6, GOLD); graphics.fill(left + 3, top + 38, right - 3, top + 40, GOLD);
        drawCorners(graphics, left + 8, top + 8, right - 8, bottom() - 8);
        graphics.drawString(font, "SERVIÇO POSTAL DE NOVERIS", left + 18, top + 15, GOLD, false);
        graphics.drawString(font, tab == 2 ? "ESCREVER CORRESPONDÊNCIA" : TABS[tab], left + 24, top + 72, TEXT, false);
        if (tab == 0 || tab == 1) drawLetters(graphics); else if (tab == 3) drawEmpty(graphics, "MEUS CARTEIROS", "Seu portador selecionado aparecerá aqui.");
        if (tab == 2 && message != null) graphics.drawString(font, message.getValue().length() + " / 1500", right - 88, top + 250, MUTED, false);
        graphics.drawString(font, "ESC  FECHAR", left + 18, bottom() - 16, MUTED, false); graphics.drawString(font, "◆  SERVIÇO POSTAL ATIVO", left + panelWidth() / 2 - 62, bottom() - 16, MUTED, false); graphics.drawString(font, "NOVERIS", right - 52, bottom() - 16, GOLD, false);
        super.render(graphics, mouseX, mouseY, partialTick); drawCustomButtons(graphics, mouseX, mouseY); if (preview != null) drawPreview(graphics, preview);
    }

    private void drawCustomButtons(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = panelLeft(), width = panelWidth(), tabWidth = (width - 24) / 4 - 4;
        for (int i = 0; i < TABS.length; i++) drawNoverisButton(graphics, left + 12 + i * ((width - 24) / 4), top() + 42, tabWidth, 24, TABS[i], i == tab, mouseX, mouseY);
        if (tab == 0 || tab == 1) {
            drawNoverisButton(graphics, left + 24, bottom() - 42, 130, 28, "◀ ANTERIOR", page > 0, mouseX, mouseY); drawNoverisButton(graphics, left + width - 154, bottom() - 42, 130, 28, "PRÓXIMA ▶", page + 1 < totalPages, mouseX, mouseY);
            graphics.drawCenteredString(font, "PÁGINA " + (page + 1) + " / " + totalPages, left + width / 2, bottom() - 33, MUTED);
        } else if (tab == 2) {
            drawNoverisButton(graphics, left + 24, bottom() - 42, 110, 28, "CANCELAR", false, mouseX, mouseY); drawNoverisButton(graphics, left + 150, bottom() - 42, 120, 28, anonymous ? "☑ ANÔNIMA" : "☐ ANÔNIMA", anonymous, mouseX, mouseY); drawNoverisButton(graphics, left + width - 154, bottom() - 42, 130, 28, "SELAR E ENVIAR", true, mouseX, mouseY);
        }
    }
    private void drawNoverisButton(GuiGraphics graphics, int x, int y, int w, int h, String label, boolean selected, int mouseX, int mouseY) { boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h; int fill = selected ? ACTIVE : hovered ? 0xFFF2C94C : PANEL; int color = hovered || selected ? TEXT : MUTED; graphics.fill(x, y, x + w, y + h, GOLD); graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, fill); graphics.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, color); }
    private void drawLetters(GuiGraphics graphics) {
        if (entries.isEmpty()) { drawEmpty(graphics, tab == 0 ? "NENHUMA CORRESPONDÊNCIA" : "NENHUMA CARTA SELADA", "As cartas aparecerão aqui quando chegarem."); return; }
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 92;
        for (MailSnapshotPayload.Entry entry : entries) { graphics.fill(left, y, right, y + 38, 0xFF0D0C09); graphics.fill(left, y, left + 3, y + 38, entry.status().name().equals("READ") ? MUTED : GOLD); String person = tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient(); graphics.drawString(font, person, left + 12, y + 5, TEXT, false); graphics.drawString(font, entry.subject().isBlank() ? "Sem assunto" : entry.subject(), left + 12, y + 17, MUTED, false); graphics.drawString(font, truncate(entry.preview(), 58), left + 12, y + 29, MUTED, false); graphics.drawString(font, displayStatus(entry.status().name()), right - 76, y + 13, GOLD, false); y += 44; }
    }
    private void drawPreview(GuiGraphics graphics, MailSnapshotPayload.Entry entry) { int left = panelLeft() + 70, right = panelLeft() + panelWidth() - 70, top = this.top() + 105, bottom = this.bottom() - 55; graphics.fill(left, top, right, bottom, 0xF50D0C09); graphics.fill(left, top, right, top + 3, GOLD); graphics.drawString(font, entry.subject().isBlank() ? "SEM ASSUNTO" : entry.subject(), left + 14, top + 14, GOLD, false); graphics.drawString(font, tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient(), left + 14, top + 30, TEXT, false); graphics.drawString(font, "PRÉVIA DA CORRESPONDÊNCIA", left + 14, top + 48, MUTED, false); graphics.drawString(font, entry.preview(), left + 14, top + 70, TEXT, false); graphics.drawString(font, "Clique na carta novamente para fechar a prévia.", left + 14, bottom - 18, MUTED, false); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 3) + "..."; }
    private String displayStatus(String status) { return switch (status) { case "READ" -> "LIDA"; case "DELIVERED" -> "NOVA"; case "IN_TRANSIT" -> "A CAMINHO"; default -> status; }; }
    private void drawEmpty(GuiGraphics graphics, String title, String description) { int center = panelLeft() + panelWidth() / 2; graphics.drawCenteredString(font, title, center, top() + 150, GOLD); graphics.drawCenteredString(font, description, center, top() + 174, MUTED); }
    private void drawCorners(GuiGraphics graphics, int left, int top, int right, int bottom) { graphics.drawString(font, "+", left, top, GOLD, false); graphics.drawString(font, "+", right - 7, top, GOLD, false); graphics.drawString(font, "+", left, bottom - 9, GOLD, false); graphics.drawString(font, "+", right - 7, bottom - 9, GOLD, false); }
    private int panelWidth() { return Math.min(720, width - 40); } private int panelLeft() { return (width - panelWidth()) / 2; } private int panelHeight() { return Math.min(420, height - 30); } private int top() { return (height - panelHeight()) / 2; } private int bottom() { return top() + panelHeight(); }
}
