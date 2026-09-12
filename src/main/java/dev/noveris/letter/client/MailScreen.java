package dev.noveris.letter.client;

import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import dev.noveris.letter.network.SendLetterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Noveris postal panel. Presentation only; server owns all postal state. */
public final class MailScreen extends Screen {
    private static final int BG = 0xEA0D0C09, PANEL = 0xFF17140E, FIELD = 0xFF080807;
    private static final int GOLD = 0xFFFFD84D, ACTIVE = 0xFFD6A800, TEXT = 0xFFFFFBEB;
    private static final int MUTED = 0xFFC9BE9B, DIM = 0xFF77705D;
    private static final String[] TABS = {"RECEBIDAS", "ENVIADAS", "ESCREVER", "CARTEIRO"};

    private int tab, page, totalPages = 1;
    private boolean anonymous;
    private EditBox recipient, subject, message;
    private List<MailSnapshotPayload.Entry> entries = List.of();
    private MailSnapshotPayload.Entry preview;
    private List<String> onlinePlayers = List.of();

    public MailScreen(int tab) {
        super(Component.literal("Serviço Postal de Noveris"));
        this.tab = Math.max(0, Math.min(3, tab));
    }

    @Override
    protected void init() {
        if (tab == 2) createComposeFields();
        refreshOnlinePlayers();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    private void createComposeFields() {
        int left = panelLeft();
        int mainRight = composeMainRight();
        int fieldLeft = left + 24;
        int mainWidth = mainRight - fieldLeft;
        int y = top() + 91;

        recipient = addRenderableWidget(new EditBox(font, fieldLeft, y + 22, mainWidth - 92, 28,
                Component.literal("Destinatário")));
        recipient.setMaxLength(80);
        recipient.setHint(Component.literal("Digite o nick do jogador..."));

        subject = addRenderableWidget(new EditBox(font, fieldLeft, y + 62, mainWidth, 28,
                Component.literal("Assunto")));
        subject.setMaxLength(120);
        subject.setHint(Component.literal("Assunto (opcional)"));

        message = addRenderableWidget(new EditBox(font, fieldLeft, y + 102, mainWidth, 72,
                Component.literal("Mensagem")));
        message.setMaxLength(1500);
        message.setHint(Component.literal("Digite sua correspondência aqui..."));
    }

    private int composeMainRight() { return panelLeft() + panelWidth() - 278; }

    private void refreshOnlinePlayers() {
        if (Minecraft.getInstance().getConnection() == null || Minecraft.getInstance().player == null) {
            onlinePlayers = List.of();
            return;
        }
        List<String> names = new ArrayList<>();
        String self = Minecraft.getInstance().player.getGameProfile().getName();
        for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (!name.equals(self)) names.add(name);
        }
        onlinePlayers = names;
    }

    private void selectTab(int index) {
        tab = index;
        page = 0;
        preview = null;
        clearWidgets();
        init();
        if (index == 0 || index == 1) requestPage();
    }

    private void requestPage() { PacketDistributor.sendToServer(new RequestMailSnapshotPayload(tab, page)); }

    public void setSnapshot(MailSnapshotPayload payload) {
        if (payload.tab() == tab) {
            page = payload.page();
            totalPages = Math.max(1, payload.totalPages());
            entries = payload.entries();
            preview = null;
        }
    }

    private void send() {
        if (recipient == null || recipient.getValue().isBlank() || message == null || message.getValue().isBlank()) return;
        PacketDistributor.sendToServer(new SendLetterPayload(
                recipient.getValue().trim(), subject.getValue().trim(), message.getValue(), anonymous));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int left = panelLeft();
        int right = left + panelWidth();
        int tabY = top() + 43;
        int tabW = (panelWidth() - 42) / 4;
        for (int i = 0; i < TABS.length; i++) {
            int x = left + 12 + i * (tabW + 6);
            if (mouseInside(x, tabY, tabW, 28, mouseX, mouseY)) {
                selectTab(i);
                return true;
            }
        }

        if (tab == 2) {
            int mainRight = composeMainRight();
            int playersTop = top() + 118;
            int playersBottom = bottom() - 52;
            for (int i = 0; i < onlinePlayers.size(); i++) {
                int y = playersTop + i * 24;
                if (y + 22 <= playersBottom && mouseX >= mainRight + 14
                        && mouseX <= right - 14 && mouseY >= y && mouseY < y + 22) {
                    recipient.setValue(onlinePlayers.get(i));
                    recipient.setFocused(true);
                    return true;
                }
            }

            int buttonY = bottom() - 39;
            if (mouseInside(left + 24, buttonY, 118, 27, mouseX, mouseY)) {
                onClose();
                return true;
            }
            if (mouseInside(left + 150, buttonY, 118, 27, mouseX, mouseY)) {
                anonymous = !anonymous;
                return true;
            }
            if (mouseInside(mainRight - 136, buttonY, 112, 27, mouseX, mouseY)) {
                send();
                return true;
            }
            return false;
        }

        if (tab != 0 && tab != 1) return false;
        int listLeft = left + 24, listRight = right - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry entry : entries) {
            if (mouseInside(listLeft, y, listRight - listLeft, 38, mouseX, mouseY)) {
                preview = preview == entry ? null : entry;
                return true;
            }
            y += 44;
        }
        int pageY = bottom() - 39;
        if (mouseInside(listLeft, pageY, 130, 27, mouseX, mouseY) && page > 0) {
            page--;
            requestPage();
            return true;
        }
        if (mouseInside(listRight - 130, pageY, 130, 27, mouseX, mouseY) && page + 1 < totalPages) {
            page++;
            requestPage();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft(), panelTop = top(), right = left + panelWidth(), panelBottom = bottom();
        graphics.fill(0, 0, width, height, 0x55000000);
        graphics.fill(left, panelTop, right, panelBottom, BG);
        graphics.fill(left + 3, panelTop + 3, right - 3, panelBottom - 3, PANEL);
        drawFrame(graphics, left, panelTop, right, panelBottom);

        graphics.drawString(font, "SERVIÇO POSTAL DE NOVERIS", left + 18, panelTop + 13, GOLD, false);
        graphics.drawString(font, "Correspondência segura, entregue pelo serviço postal.", left + 20, panelTop + 28, MUTED, false);

        drawTabButtons(graphics, left, panelTop, mouseX, mouseY);
        if (tab == 2) drawCompose(graphics, left, right, panelTop, panelBottom, mouseX, mouseY);
        else if (tab == 0 || tab == 1) drawLetters(graphics, mouseX, mouseY);
        else drawEmpty(graphics, "MEUS CARTEIROS", "Seu portador selecionado aparecerá aqui.");

        super.render(graphics, mouseX, mouseY, partialTick);

        if (tab == 2) drawComposeButtons(graphics, left, panelTop, panelBottom, mouseX, mouseY);
        if (preview != null) drawPreview(graphics, preview);
    }

    private void drawFrame(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left + 3, top + 3, right - 3, top + 6, GOLD);
        g.fill(left + 3, top + 40, right - 3, top + 42, GOLD);
        g.fill(left + 3, bottom - 44, right - 3, bottom - 42, 0xFF5C4C22);
        g.drawString(font, "+", left + 7, top + 7, GOLD, false);
        g.drawString(font, "+", right - 13, top + 7, GOLD, false);
        g.drawString(font, "+", left + 7, bottom - 15, GOLD, false);
        g.drawString(font, "+", right - 13, bottom - 15, GOLD, false);
    }

    private void drawTabButtons(GuiGraphics g, int left, int top, int mouseX, int mouseY) {
        int tabW = (panelWidth() - 42) / 4;
        for (int i = 0; i < TABS.length; i++) {
            int x = left + 12 + i * (tabW + 6), y = top + 43;
            drawButton(g, x, y, tabW, 28, TABS[i], i == tab, mouseX, mouseY);
        }
    }

    private void drawCompose(GuiGraphics g, int left, int right, int top, int bottom, int mouseX, int mouseY) {
        int mainRight = composeMainRight();
        int fieldLeft = left + 24, y = top + 91;
        g.drawString(font, "DESTINATÁRIO", fieldLeft, y, GOLD, false);
        g.drawString(font, "JOGADORES ONLINE", mainRight + 14, y, GOLD, false);
        g.drawString(font, "MENSAGEM", fieldLeft, y + 92, GOLD, false);
        drawField(g, fieldLeft, y + 22, mainRight - fieldLeft - 92, 28);
        drawField(g, fieldLeft, y + 62, mainRight - fieldLeft, 28);
        drawField(g, fieldLeft, y + 102, mainRight - fieldLeft, 72);
        g.drawString(font, (message == null ? 0 : message.getValue().length()) + " / 1500", mainRight - 66, y + 181, MUTED, false);
        drawOnlinePanel(g, mainRight + 14, y, right - 14, bottom - 52, mouseX, mouseY);
    }

    private void drawField(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B877E);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, FIELD);
    }

    private void drawOnlinePanel(GuiGraphics g, int left, int top, int right, int bottom, int mouseX, int mouseY) {
        g.fill(left, top, right, bottom, 0xFF5D512E);
        g.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF0D0C0A);
        int rowY = top + 27;
        int maxRows = Math.max(1, (bottom - rowY - 4) / 24);
        if (onlinePlayers.isEmpty()) {
            g.drawCenteredString(font, "Nenhum outro jogador", (left + right) / 2, rowY, MUTED);
            g.drawCenteredString(font, "está online.", (left + right) / 2, rowY + 14, MUTED);
            return;
        }
        for (int i = 0; i < Math.min(maxRows, onlinePlayers.size()); i++) {
            int y = rowY + i * 24;
            boolean hover = mouseInside(left + 3, y - 2, right - left - 6, 22, mouseX, mouseY);
            if (hover) g.fill(left + 3, y - 2, right - 3, y + 20, 0xFF2A2417);
            g.fill(left + 9, y + 4, left + 17, y + 12, GOLD);
            g.drawString(font, onlinePlayers.get(i), left + 23, y + 2, hover ? TEXT : MUTED, false);
        }
        if (onlinePlayers.size() > maxRows) g.drawString(font, "+" + (onlinePlayers.size() - maxRows) + " jogador(es)", left + 9, bottom - 14, DIM, false);
    }

    private void drawComposeButtons(GuiGraphics g, int left, int top, int bottom, int mouseX, int mouseY) {
        int mainRight = composeMainRight(), y = bottom - 39;
        drawButton(g, left + 24, y, 118, 27, "CANCELAR", false, mouseX, mouseY);
        drawButton(g, left + 150, y, 118, 27, anonymous ? "ANONIMA" : "ANONIMA", anonymous, mouseX, mouseY);
        drawButton(g, mainRight - 136, y, 112, 27, "ENVIAR", true, mouseX, mouseY);
        g.drawString(font, anonymous ? "Sua identidade ficará oculta." : "O destinatário verá quem enviou.", left + 286, y + 9, MUTED, false);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean selected, int mouseX, int mouseY) {
        boolean hover = mouseInside(x, y, w, h, mouseX, mouseY);
        g.fill(x, y, x + w, y + h, GOLD);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, selected ? ACTIVE : hover ? 0xFF2A2417 : PANEL);
        g.drawCenteredString(font, label, x + w / 2, y + 9, selected || hover ? TEXT : MUTED);
    }

    private void drawLetters(GuiGraphics g, int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            drawEmpty(g, tab == 0 ? "NENHUMA CORRESPONDÊNCIA" : "NENHUMA CARTA SELADA", "As cartas aparecerão aqui quando chegarem.");
            return;
        }
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry entry : entries) {
            boolean hover = mouseInside(left, y, right - left, 38, mouseX, mouseY);
            g.fill(left, y, right, y + 38, hover ? 0xFF211C12 : FIELD);
            g.fill(left, y, left + 3, y + 38, entry.status().name().equals("READ") ? MUTED : GOLD);
            String person = tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient();
            g.drawString(font, person, left + 12, y + 5, TEXT, false);
            g.drawString(font, entry.subject().isBlank() ? "Sem assunto" : truncate(entry.subject(), 36), left + 12, y + 17, MUTED, false);
            g.drawString(font, truncate(entry.preview(), 58), left + 12, y + 29, MUTED, false);
            g.drawString(font, displayStatus(entry.status().name()), right - 76, y + 13, GOLD, false);
            y += 44;
        }
        g.drawString(font, "Clique em uma carta para abrir a prévia.", left, bottom() - 30, MUTED, false);
        g.drawCenteredString(font, "PÁGINA " + (page + 1) + " / " + totalPages, (left + right) / 2, bottom() - 30, MUTED);
        drawButton(g, left, bottom() - 39, 130, 27, "ANTERIOR", page > 0, mouseX, mouseY);
        drawButton(g, right - 130, bottom() - 39, 130, 27, "PRÓXIMA", page + 1 < totalPages, mouseX, mouseY);
    }

    private void drawPreview(GuiGraphics g, MailSnapshotPayload.Entry entry) {
        int left = panelLeft() + 70, right = panelLeft() + panelWidth() - 70;
        int previewTop = top() + 86, previewBottom = bottom() - 52;
        g.fill(left, previewTop, right, previewBottom, 0xF50D0C09);
        g.fill(left, previewTop, right, previewTop + 3, GOLD);
        g.drawString(font, entry.subject().isBlank() ? "SEM ASSUNTO" : truncate(entry.subject(), 50), left + 14, previewTop + 14, GOLD, false);
        g.drawString(font, tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient(), left + 14, previewTop + 30, TEXT, false);
        g.drawString(font, "PRÉVIA DA CORRESPONDÊNCIA", left + 14, previewTop + 48, MUTED, false);
        g.drawString(font, truncate(entry.preview(), 90), left + 14, previewTop + 70, TEXT, false);
        g.drawString(font, "Clique novamente para fechar.", left + 14, previewBottom - 18, MUTED, false);
    }

    private void drawEmpty(GuiGraphics g, String title, String description) {
        int center = panelLeft() + panelWidth() / 2;
        g.drawCenteredString(font, title, center, top() + 160, GOLD);
        g.drawCenteredString(font, description, center, top() + 184, MUTED);
    }

    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 3) + "..."; }

    private String displayStatus(String status) {
        return switch (status) {
            case "READ" -> "LIDA";
            case "DELIVERED" -> "NOVA";
            case "IN_TRANSIT" -> "A CAMINHO";
            default -> status;
        };
    }

    private boolean mouseInside(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int panelWidth() { return Math.min(980, width - 24); }
    private int panelHeight() { return Math.min(350, height - 12); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int bottom() { return top() + panelHeight(); }
}
