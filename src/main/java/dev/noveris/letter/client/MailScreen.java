package dev.noveris.letter.client;

import dev.noveris.letter.courier.CourierAppearanceRegistry;
import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import dev.noveris.letter.network.SelectCourierPayload;
import dev.noveris.letter.network.SendLetterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

public final class MailScreen extends Screen {
    private static final int BG = 0xEA0D0C09, PANEL = 0xFF17140E, FIELD = 0xFF080807;
    private static final int GOLD = 0xFFFFD84D, ACTIVE = 0xFFD6A800, TEXT = 0xFFFFFBEB, MUTED = 0xFFC9BE9B, DIM = 0xFF77705D;
    private static final String[] TABS = {"RECEBIDAS", "ENVIADAS", "ESCREVER", "CARTEIRO"};
    private static final ResourceLocation[] COURIERS = {
            CourierAppearanceRegistry.MOSSBLOOM_ID,
            CourierAppearanceRegistry.COATI_ID,
            CourierAppearanceRegistry.RED_PANDA_ID,
            CourierAppearanceRegistry.BOOPLET_ID,
            CourierAppearanceRegistry.CAPYBARA_ID,
            CourierAppearanceRegistry.SPARROW_ID,
            CourierAppearanceRegistry.BARN_OWL_ID
    };
    private static final String[] NAMES = {"MOSSBLOOM", "COATI", "PANDA-VERMELHO", "BOOPLET", "CAPIVARA", "PARDAL", "CORUJA"};
    private static final String[] DESC = {"Natural e resistente", "Curioso e veloz", "Calmo e companheiro", "Pequeno e ágil", "Tranquilo e confiável", "Rápido e leve", "Silenciosa e observadora"};
    private static final int COURIERS_PER_PAGE = 6;

    private int tab;
    private int page;
    private int totalPages = 1;
    private int courierPage;
    private int selectedCourierIndex;
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
        if (tab < 2) requestPage();
        if (courierPage >= courierTotalPages()) courierPage = courierTotalPages() - 1;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    private void createComposeFields() {
        int left = panelLeft(), mainRight = composeMainRight(), fieldLeft = left + 24;
        int mainWidth = mainRight - fieldLeft, y = top() + 91;
        recipient = addRenderableWidget(new EditBox(font, fieldLeft, y + 22, Math.max(80, mainWidth - 92), 28, Component.literal("Destinatário")));
        recipient.setMaxLength(80);
        recipient.setHint(Component.literal("Digite o nick do jogador..."));
        subject = addRenderableWidget(new EditBox(font, fieldLeft, y + 62, Math.max(80, mainWidth), 28, Component.literal("Assunto")));
        subject.setMaxLength(120);
        subject.setHint(Component.literal("Assunto (opcional)"));
        message = addRenderableWidget(new EditBox(font, fieldLeft, y + 102, Math.max(80, mainWidth), 72, Component.literal("Mensagem")));
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
        if (tab == 3) courierPage = 0;
        preview = null;
        clearWidgets();
        init();
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
        PacketDistributor.sendToServer(new SendLetterPayload(recipient.getValue().trim(), subject.getValue().trim(), message.getValue(), anonymous));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int left = panelLeft(), right = left + panelWidth(), tabY = top() + 43, tabW = (panelWidth() - 42) / 4;
        for (int i = 0; i < 4; i++) {
            int x = left + 12 + i * (tabW + 6);
            if (inside(x, tabY, tabW, 28, mouseX, mouseY)) {
                selectTab(i);
                return true;
            }
        }

        if (tab == 2) {
            int mainRight = composeMainRight(), playersTop = top() + 118, playersBottom = bottom() - 52;
            for (int i = 0; i < onlinePlayers.size(); i++) {
                int y = playersTop + i * 24;
                if (y + 22 <= playersBottom && inside(mainRight + 14, y, right - mainRight - 28, 22, mouseX, mouseY)) {
                    recipient.setValue(onlinePlayers.get(i));
                    recipient.setFocused(true);
                    return true;
                }
            }
            int by = bottom() - 39;
            if (inside(left + 24, by, 118, 27, mouseX, mouseY)) { onClose(); return true; }
            if (inside(left + 150, by, 118, 27, mouseX, mouseY)) { anonymous = !anonymous; return true; }
            if (inside(right - 136, by, 112, 27, mouseX, mouseY)) { send(); return true; }
            return false;
        }

        if (tab == 3) {
            int navY = courierNavY();
            int pages = courierTotalPages();

            // Handle pagination FIRST. Card hitboxes never overlap this zone.
            if (inside(left + 24, navY, 130, 27, mouseX, mouseY)) {
                if (courierPage > 0) courierPage--;
                return true;
            }
            if (inside(right - 154, navY, 130, 27, mouseX, mouseY)) {
                if (courierPage + 1 < pages) courierPage++;
                return true;
            }

            int cardTop = courierCardTop(), gap = 10;
            int cardW = (panelWidth() - 48 - gap * 2) / 3;
            int cardH = courierCardHeight();
            int start = courierPage * COURIERS_PER_PAGE;
            int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);

            // Only the SELECT button is clickable, so cards can never steal the page controls.
            for (int i = start; i < end; i++) {
                int local = i - start, row = local / 3, col = local % 3;
                int x = left + 24 + col * (cardW + gap), y = cardTop + row * (cardH + gap);
                int buttonY = y + cardH - 29;
                if (inside(x + 14, buttonY, cardW - 28, 25, mouseX, mouseY)) {
                    selectedCourierIndex = i;
                    PacketDistributor.sendToServer(new SelectCourierPayload(COURIERS[i]));
                    return true;
                }
            }
            return false;
        }

        int listLeft = left + 24, listRight = right - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry entry : entries) {
            if (inside(listLeft, y, listRight - listLeft, 38, mouseX, mouseY)) {
                preview = preview == entry ? null : entry;
                return true;
            }
            y += 44;
        }
        int py = bottom() - 39;
        if (inside(listLeft, py, 130, 27, mouseX, mouseY) && page > 0) { page--; requestPage(); return true; }
        if (inside(listRight - 130, py, 130, 27, mouseX, mouseY) && page + 1 < totalPages) { page++; requestPage(); return true; }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft(), panelTop = top(), right = left + panelWidth(), panelBottom = bottom();
        g.fill(0, 0, width, height, 0x55000000);
        g.fill(left, panelTop, right, panelBottom, BG);
        g.fill(left + 3, panelTop + 3, right - 3, panelBottom - 3, PANEL);
        drawFrame(g, left, panelTop, right, panelBottom);
        g.drawString(font, "SERVIÇO POSTAL DE NOVERIS", left + 18, panelTop + 13, GOLD, false);
        g.drawString(font, "Correspondência segura, entregue pelo serviço postal.", left + 20, panelTop + 28, MUTED, false);
        drawTabs(g, left, panelTop, mouseX, mouseY);

        if (tab == 2) drawCompose(g, left, right, panelTop, panelBottom, mouseX, mouseY);
        else if (tab < 2) drawLetters(g, mouseX, mouseY);
        else drawCouriers(g, left, panelTop, panelBottom, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
        if (tab == 2) drawComposeButtons(g, left, panelBottom, mouseX, mouseY);
        if (preview != null) drawPreview(g, preview);
    }

    private void drawFrame(GuiGraphics g, int left, int top, int right, int bottom) {
        g.fill(left + 3, top + 3, right - 3, top + 6, GOLD);
        g.fill(left + 3, top + 40, right - 3, top + 42, GOLD);
        g.fill(left + 3, bottom - 44, right - 3, bottom - 42, 0xFF5C4C22);
        g.drawString(font, "+", left + 7, top + 7, GOLD, false);
        g.drawString(font, "+", right - 13, top + 7, GOLD, false);
    }

    private void drawTabs(GuiGraphics g, int left, int top, int mx, int my) {
        int tw = (panelWidth() - 42) / 4;
        for (int i = 0; i < 4; i++) drawButton(g, left + 12 + i * (tw + 6), top + 43, tw, 28, TABS[i], i == tab, mx, my);
    }

    private void drawCompose(GuiGraphics g, int left, int right, int top, int bottom, int mx, int my) {
        int mainRight = composeMainRight(), fieldLeft = left + 24, y = top + 91;
        g.drawString(font, "DESTINATÁRIO", fieldLeft, y, GOLD, false);
        g.drawString(font, "JOGADORES ONLINE", mainRight + 14, y, GOLD, false);
        g.drawString(font, "ASSUNTO", fieldLeft, y + 52, GOLD, false);
        g.drawString(font, "MENSAGEM", fieldLeft, y + 92, GOLD, false);
        drawField(g, fieldLeft, y + 22, mainRight - fieldLeft - 92, 28);
        drawField(g, fieldLeft, y + 62, mainRight - fieldLeft, 28);
        drawField(g, fieldLeft, y + 102, mainRight - fieldLeft, 72);
        g.drawString(font, (message == null ? 0 : message.getValue().length()) + " / 1500", mainRight - 66, y + 181, MUTED, false);
        drawOnlinePanel(g, mainRight + 14, y, right - 14, bottom - 52, mx, my);
    }

    private void drawField(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF8B877E);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, FIELD);
    }

    private void drawOnlinePanel(GuiGraphics g, int left, int top, int right, int bottom, int mx, int my) {
        g.fill(left, top, right, bottom, 0xFF5D512E);
        g.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF0D0C0A);
        int rowY = top + 27, maxRows = Math.max(1, (bottom - rowY - 4) / 24);
        if (onlinePlayers.isEmpty()) {
            g.drawCenteredString(font, "Nenhum outro jogador", (left + right) / 2, rowY, MUTED);
            g.drawCenteredString(font, "está online.", (left + right) / 2, rowY + 14, MUTED);
            return;
        }
        for (int i = 0; i < Math.min(maxRows, onlinePlayers.size()); i++) {
            int y = rowY + i * 24;
            boolean hover = inside(left + 3, y - 2, right - left - 6, 22, mx, my);
            if (hover) g.fill(left + 3, y - 2, right - 3, y + 20, 0xFF2A2417);
            g.fill(left + 9, y + 4, left + 17, y + 12, GOLD);
            g.drawString(font, truncate(onlinePlayers.get(i), 20), left + 23, y + 2, hover ? TEXT : MUTED, false);
        }
        if (onlinePlayers.size() > maxRows) g.drawString(font, "+" + (onlinePlayers.size() - maxRows) + " jogador(es)", left + 9, bottom - 14, DIM, false);
    }

    private void drawComposeButtons(GuiGraphics g, int left, int bottom, int mx, int my) {
        int y = bottom - 39;
        drawButton(g, left + 24, y, 118, 27, "CANCELAR", false, mx, my);
        drawButton(g, left + 150, y, 118, 27, anonymous ? "ANÔNIMA ✓" : "ANÔNIMA", anonymous, mx, my);
        drawButton(g, left + panelWidth() - 136, y, 112, 27, "ENVIAR", true, mx, my);
    }

    private void drawCouriers(GuiGraphics g, int left, int top, int bottom, int mx, int my) {
        g.drawString(font, "CARTEIROS DISPONÍVEIS", left + 24, top + 73, GOLD, false);
        g.drawString(font, "Escolha o mensageiro das próximas entregas.", left + 24, top + 84, MUTED, false);

        int cardTop = courierCardTop(), gap = 10;
        int cardW = (panelWidth() - 48 - gap * 2) / 3;
        int cardH = courierCardHeight();
        int start = courierPage * COURIERS_PER_PAGE;
        int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);

        for (int i = start; i < end; i++) {
            int local = i - start, row = local / 3, col = local % 3;
            int x = left + 24 + col * (cardW + gap), y = cardTop + row * (cardH + gap);
            boolean selected = selectedCourierIndex == i;
            boolean hover = inside(x, y, cardW, cardH, mx, my);
            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2A2417 : FIELD);
            g.fill(x, y, x + cardW, y + 3, selected ? GOLD : 0xFF5C4C22);
            g.drawCenteredString(font, truncate(NAMES[i], Math.max(12, cardW / 7)), x + cardW / 2, y + 14, selected ? GOLD : TEXT);
            g.drawCenteredString(font, truncate(DESC[i], Math.max(15, cardW / 6)), x + cardW / 2, y + 35, MUTED);
            g.drawCenteredString(font, i < 5 ? "CAMINHA ATÉ VOCÊ" : "VOA ATÉ VOCÊ", x + cardW / 2, y + 56, DIM);
            int buttonY = y + cardH - 29;
            drawButton(g, x + 14, buttonY, cardW - 28, 25, selected ? "SELECIONADO" : "SELECIONAR", selected, mx, my);
        }

        drawCourierPagination(g, left, bottom, rightOf(left), mx, my);
    }

    private void drawCourierPagination(GuiGraphics g, int left, int bottom, int right, int mx, int my) {
        int navY = courierNavY();
        int pages = courierTotalPages();
        drawButton(g, left + 24, navY, 130, 27, "ANTERIOR", courierPage > 0, mx, my);
        drawButton(g, right - 154, navY, 130, 27, "PRÓXIMA", courierPage + 1 < pages, mx, my);
        g.drawCenteredString(font, "PÁGINA " + (courierPage + 1) + " / " + pages, (left + right) / 2, navY + 9, MUTED);
    }

    private int rightOf(int left) { return left + panelWidth(); }

    private void drawLetters(GuiGraphics g, int mx, int my) {
        if (entries.isEmpty()) {
            drawEmpty(g, tab == 0 ? "NENHUMA CORRESPONDÊNCIA" : "NENHUMA CARTA SELADA", "As cartas aparecerão aqui quando chegarem.");
            return;
        }
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry e : entries) {
            boolean hover = inside(left, y, right - left, 38, mx, my);
            g.fill(left, y, right, y + 38, hover ? 0xFF211C12 : FIELD);
            g.fill(left, y, left + 3, y + 38, e.status().name().equals("READ") ? MUTED : GOLD);
            String person = tab == 0 ? "De: " + e.sender() : "Para: " + e.recipient();
            g.drawString(font, truncate(person, 40), left + 12, y + 5, TEXT, false);
            g.drawString(font, e.subject().isBlank() ? "Sem assunto" : truncate(e.subject(), 36), left + 12, y + 17, MUTED, false);
            g.drawString(font, truncate(e.preview(), 58), left + 12, y + 29, MUTED, false);
            g.drawString(font, status(e.status().name()), right - 76, y + 13, GOLD, false);
            y += 44;
        }
        g.drawCenteredString(font, "PÁGINA " + (page + 1) + " / " + totalPages, (left + right) / 2, bottom() - 30, MUTED);
        drawButton(g, left, bottom() - 39, 130, 27, "ANTERIOR", page > 0, mx, my);
        drawButton(g, right - 130, bottom() - 39, 130, 27, "PRÓXIMA", page + 1 < totalPages, mx, my);
    }

    private void drawPreview(GuiGraphics g, MailSnapshotPayload.Entry e) {
        int w = 300, h = 118, x = (width - w) / 2, y = (height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xFF0D0C09);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, PANEL);
        g.fill(x + 2, y + 2, x + w - 2, y + 5, GOLD);
        g.drawString(font, "CORRESPONDÊNCIA", x + 12, y + 12, GOLD, false);
        g.drawString(font, truncate(e.sender() + " → " + e.recipient(), 42), x + 12, y + 29, TEXT, false);
        g.drawString(font, truncate(e.subject().isBlank() ? "Sem assunto" : e.subject(), 42), x + 12, y + 45, MUTED, false);
        g.drawString(font, truncate(e.preview(), 48), x + 12, y + 64, TEXT, false);
        g.drawString(font, "Clique novamente para fechar", x + 12, y + 96, DIM, false);
    }

    private void drawEmpty(GuiGraphics g, String title, String subtitle) {
        int cx = panelLeft() + panelWidth() / 2, cy = top() + 175;
        g.drawCenteredString(font, title, cx, cy, GOLD);
        g.drawCenteredString(font, subtitle, cx, cy + 20, MUTED);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, boolean selected, int mx, int my) {
        boolean hover = inside(x, y, w, h, mx, my);
        g.fill(x, y, x + w, y + h, GOLD);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, selected ? ACTIVE : hover ? 0xFF2A2417 : PANEL);
        g.drawCenteredString(font, label, x + w / 2, y + 9, selected || hover ? TEXT : MUTED);
    }

    private String status(String s) {
        return switch (s) {
            case "READ" -> "LIDA";
            case "DELIVERED" -> "ENTREGUE";
            case "IN_TRANSIT" -> "A CAMINHO";
            default -> s;
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private boolean inside(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int courierTotalPages() { return Math.max(1, (COURIERS.length + COURIERS_PER_PAGE - 1) / COURIERS_PER_PAGE); }

    private int panelWidth() { return Math.min(980, Math.max(360, width - 40)); }

    private int panelHeight() { return Math.min(720, Math.max(420, height - 20)); }

    private int panelLeft() { return (width - panelWidth()) / 2; }

    private int top() { return Math.max(10, (height - panelHeight()) / 2); }

    private int bottom() { return Math.min(height - 10, top() + panelHeight()); }

    private int courierNavY() { return bottom() - 39; }

    private int courierCardTop() { return top() + 101; }

    private int courierCardHeight() {
        int gap = 10;
        int available = courierNavY() - courierCardTop() - gap;
        // Keep a hard ceiling so the second row can never approach the pagination bar.
        return Math.max(96, Math.min(180, available / 2));
    }
}
