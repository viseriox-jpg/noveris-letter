package dev.noveris.letter.client;

import dev.noveris.letter.network.MailSnapshotPayload;
import dev.noveris.letter.network.RequestMailSnapshotPayload;
import dev.noveris.letter.network.SendLetterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Noveris postal panel. Presentation only; server owns all postal state. */
public final class MailScreen extends Screen {
    private static final int BG = 0xEA0D0C09;
    private static final int PANEL = 0xFF17140E;
    private static final int FIELD = 0xFF080807;
    private static final int GOLD = 0xFFFFD84D;
    private static final int ACTIVE = 0xFFD6A800;
    private static final int TEXT = 0xFFFFFBEB;
    private static final int MUTED = 0xFFC9BE9B;
    private static final int DIM = 0xFF77705D;
    private static final String[] TABS = {"RECEBIDAS", "ENVIADAS", "ESCREVER", "CARTEIRO"};

    private final int initialTab;
    private int tab, page, totalPages = 1;
    private boolean anonymous;
    private EditBox recipient, subject, message;
    private List<MailSnapshotPayload.Entry> entries = List.of();
    private MailSnapshotPayload.Entry preview;
    private List<String> onlinePlayers = List.of();

    public MailScreen(int tab) {
        super(Component.literal("Serviço Postal de Noveris"));
        this.initialTab = Math.max(0, Math.min(3, tab));
        this.tab = initialTab;
    }

    @Override
    protected void init() {
        int left = panelLeft(), width = panelWidth();
        int tabW = (width - 42) / 4;
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            addRenderableWidget(Button.builder(Component.literal(TABS[i]), button -> selectTab(index))
                    .bounds(left + 12 + i * (tabW + 6), top() + 43, tabW, 28).build());
        }
        if (tab == 2) createComposeFields(left, width);
        refreshOnlinePlayers();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    private void createComposeFields(int left, int width) {
        int mainRight = composeMainRight(left, width);
        int mainWidth = mainRight - left - 24;
        int y = top() + 91;

        recipient = addRenderableWidget(new EditBox(font, left + 24, y + 22, mainWidth - 92, 28,
                Component.literal("Destinatário")));
        recipient.setMaxLength(80);
        recipient.setHint(Component.literal("Digite o nick do jogador..."));

        subject = addRenderableWidget(new EditBox(font, left + 24, y + 62, mainWidth, 28,
                Component.literal("Assunto")));
        subject.setMaxLength(120);
        subject.setHint(Component.literal("Assunto (opcional)"));

        message = addRenderableWidget(new EditBox(font, left + 24, y + 102, mainWidth, 72,
                Component.literal("Mensagem")));
        message.setMaxLength(1500);
        message.setHint(Component.literal("Digite sua correspondência aqui..."));

        addRenderableWidget(Button.builder(Component.literal("CANCELAR"), button -> onClose())
                .bounds(left + 24, bottom() - 39, 118, 27).build());
        addRenderableWidget(Button.builder(Component.literal("ANÔNIMA"), button -> anonymous = !anonymous)
                .bounds(left + 150, bottom() - 39, 118, 27).build());
        addRenderableWidget(Button.builder(Component.literal("ENVIAR"), button -> send())
                .bounds(mainRight - 136, bottom() - 39, 112, 27).build());
    }

    private int composeMainRight(int left, int width) {
        return left + width - 278;
    }

    private void refreshOnlinePlayers() {
        if (Minecraft.getInstance().getConnection() == null) {
            onlinePlayers = List.of();
            return;
        }
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
            String name = info.getProfile().getName();
            if (!name.equals(Minecraft.getInstance().player.getGameProfile().getName())) names.add(name);
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

    private void requestPage() {
        PacketDistributor.sendToServer(new RequestMailSnapshotPayload(tab, page));
    }

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

        if (tab == 2) {
            int left = panelLeft();
            int right = composeMainRight(left, panelWidth());
            int playersTop = top() + 116;
            int rowH = 24;
            for (int i = 0; i < onlinePlayers.size(); i++) {
                int y = playersTop + i * rowH;
                if (y + rowH <= bottom() - 74 && mouseX >= right + 14 && mouseX <= panelLeft() + panelWidth() - 14
                        && mouseY >= y && mouseY < y + rowH) {
                    if (recipient != null) {
                        recipient.setValue(onlinePlayers.get(i));
                        recipient.setFocused(true);
                    }
                    return true;
                }
            }
            if (anonymous && mouseX >= left + 150 && mouseX <= left + 268
                    && mouseY >= bottom() - 39 && mouseY <= bottom() - 12) {
                anonymous = false;
                return true;
            }
            return false;
        }

        if (tab != 0 && tab != 1) return false;
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry entry : entries) {
            if (mouseX >= left && mouseX <= right && mouseY >= y && mouseY <= y + 38) {
                preview = preview == entry ? null : entry;
                return true;
            }
            y += 44;
        }
        if (mouseY >= bottom() - 39 && mouseY <= bottom() - 12) {
            if (mouseX >= left && mouseX < left + 130 && page > 0) { page--; requestPage(); return true; }
            if (mouseX >= right - 130 && mouseX <= right && page + 1 < totalPages) { page++; requestPage(); return true; }
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

        graphics.drawString(font, "✉  SERVIÇO POSTAL DE NOVERIS", left + 18, panelTop + 14, GOLD, false);
        graphics.drawString(font, "Correspondência segura, entregue pelo serviço postal.", left + 20, panelTop + 29, MUTED, false);

        if (tab == 2) drawCompose(graphics, left, right, panelTop, panelBottom);
        else if (tab == 0 || tab == 1) drawLetters(graphics);
        else drawEmpty(graphics, "MEUS CARTEIROS", "Seu portador selecionado aparecerá aqui.");

        super.render(graphics, mouseX, mouseY, partialTick);
        drawTabButtons(graphics, left, panelTop);
        if (tab == 2) drawComposeOverlay(graphics, left, right, panelTop, panelBottom, mouseX, mouseY);
        if (preview != null) drawPreview(graphics, preview);
    }

    private void drawFrame(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left + 3, top + 3, right - 3, top + 6, GOLD);
        graphics.fill(left + 3, top + 40, right - 3, top + 42, GOLD);
        graphics.fill(left + 3, bottom - 44, right - 3, bottom - 42, 0xFF5C4C22);
        int c = GOLD;
        graphics.drawString(font, "╔", left + 6, top + 5, c, false);
        graphics.drawString(font, "╗", right - 13, top + 5, c, false);
        graphics.drawString(font, "╚", left + 6, bottom - 15, c, false);
        graphics.drawString(font, "╝", right - 13, bottom - 15, c, false);
    }

    private void drawTabButtons(GuiGraphics graphics, int left, int top) {
        int width = panelWidth(), tabW = (width - 42) / 4;
        for (int i = 0; i < TABS.length; i++) {
            int x = left + 12 + i * (tabW + 6), y = top + 43;
            boolean selected = i == tab;
            boolean hover = mouseInside(x, y, tabW, 28, lastMouseX, lastMouseY);
            graphics.fill(x, y, x + tabW, y + 28, GOLD);
            graphics.fill(x + 2, y + 2, x + tabW - 2, y + 26, selected ? ACTIVE : hover ? 0xFF2A2417 : PANEL);
            graphics.drawCenteredString(font, TABS[i], x + tabW / 2, y + 9, selected || hover ? TEXT : MUTED);
        }
    }

    private int lastMouseX, lastMouseY;

    private boolean mouseInside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void drawCompose(GuiGraphics graphics, int left, int right, int top, int bottom) {
        int mainRight = composeMainRight(left, panelWidth());
        int fieldLeft = left + 24;
        int y = top + 91;

        graphics.drawString(font, "DESTINATÁRIO", fieldLeft, y, GOLD, false);
        graphics.drawString(font, "JOGADORES ONLINE", mainRight + 14, top + 91, GOLD, false);

        drawFieldFrame(graphics, fieldLeft, y + 22, mainRight - fieldLeft - 92, 28);
        drawFieldFrame(graphics, fieldLeft, y + 62, mainRight - fieldLeft, 28);
        drawFieldFrame(graphics, fieldLeft, y + 102, mainRight - fieldLeft, 72);

        graphics.drawString(font, "MENSAGEM", fieldLeft, y + 92, GOLD, false);
        graphics.drawString(font, (message == null ? 0 : message.getValue().length()) + " / 1500",
                mainRight - 66, y + 181, MUTED, false);

        drawOnlinePanel(graphics, mainRight + 14, top + 91, right - 14, bottom - 58);
    }

    private void drawFieldFrame(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFF8B877E);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2, FIELD);
    }

    private void drawOnlinePanel(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, 0xFF5D512E);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF0D0C0A);
        int rowY = top + 27;
        if (onlinePlayers.isEmpty()) {
            graphics.drawCenteredString(font, "Nenhum outro jogador", (left + right) / 2, rowY, MUTED);
            graphics.drawCenteredString(font, "está online.", (left + right) / 2, rowY + 14, MUTED);
            return;
        }
        int maxRows = Math.max(1, (bottom - rowY - 4) / 24);
        for (int i = 0; i < Math.min(maxRows, onlinePlayers.size()); i++) {
            int y = rowY + i * 24;
            boolean hover = mouseInside(left + 3, y - 2, right - left - 6, 22, lastMouseX, lastMouseY);
            if (hover) graphics.fill(left + 3, y - 2, right - 3, y + 20, 0xFF2A2417);
            graphics.fill(left + 9, y + 4, left + 17, y + 12, GOLD);
            graphics.drawString(font, onlinePlayers.get(i), left + 23, y + 2, hover ? TEXT : MUTED, false);
        }
        if (onlinePlayers.size() > maxRows) {
            graphics.drawString(font, "+" + (onlinePlayers.size() - maxRows) + " jogador(es)", left + 9, bottom - 14, DIM, false);
        }
    }

    private void drawComposeOverlay(GuiGraphics graphics, int left, int right, int top, int bottom, int mouseX, int mouseY) {
        int mainRight = composeMainRight(left, panelWidth());
        int buttonY = bottom - 39;
        drawNoverisButton(graphics, left + 24, buttonY, 118, 27, "✕  CANCELAR", false, mouseX, mouseY);
        drawNoverisButton(graphics, left + 150, buttonY, 118, 27, anonymous ? "☑  ANÔNIMA" : "☐  ANÔNIMA", anonymous, mouseX, mouseY);
        drawNoverisButton(graphics, mainRight - 136, buttonY, 112, 27, "ENVIAR", true, mouseX, mouseY);
        graphics.drawString(font, anonymous ? "Sua identidade ficará oculta." : "O destinatário verá quem enviou.",
                left + 286, buttonY + 9, MUTED, false);
    }

    private void drawNoverisButton(GuiGraphics graphics, int x, int y, int w, int h, String label,
                                   boolean selected, int mouseX, int mouseY) {
        boolean hover = mouseInside(x, y, w, h, mouseX, mouseY);
        graphics.fill(x, y, x + w, y + h, GOLD);
        graphics.fill(x + 2, y + 2, x + w - 2, y + h - 2,
                selected ? ACTIVE : hover ? 0xFF2A2417 : PANEL);
        graphics.drawCenteredString(font, label, x + w / 2, y + 9, selected || hover ? TEXT : MUTED);
    }

    private void drawLetters(GuiGraphics graphics) {
        if (entries.isEmpty()) {
            drawEmpty(graphics, tab == 0 ? "NENHUMA CORRESPONDÊNCIA" : "NENHUMA CARTA SELADA",
                    "As cartas aparecerão aqui quando chegarem.");
            return;
        }
        int left = panelLeft() + 24, right = panelLeft() + panelWidth() - 24, y = top() + 91;
        for (MailSnapshotPayload.Entry entry : entries) {
            graphics.fill(left, y, right, y + 38, FIELD);
            graphics.fill(left, y, left + 3, y + 38,
                    entry.status().name().equals("READ") ? MUTED : GOLD);
            String person = tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient();
            graphics.drawString(font, person, left + 12, y + 5, TEXT, false);
            graphics.drawString(font, entry.subject().isBlank() ? "Sem assunto" : entry.subject(), left + 12, y + 17, MUTED, false);
            graphics.drawString(font, truncate(entry.preview(), 58), left + 12, y + 29, MUTED, false);
            graphics.drawString(font, displayStatus(entry.status().name()), right - 76, y + 13, GOLD, false);
            y += 44;
        }
        graphics.drawString(font, "Clique em uma carta para abrir a prévia.", left, bottom() - 30, MUTED, false);
        graphics.drawCenteredString(font, "PÁGINA " + (page + 1) + " / " + totalPages,
                (left + right) / 2, bottom() - 30, MUTED);
        drawNoverisButton(graphics, left, bottom() - 39, 130, 27, "◀ ANTERIOR", page > 0, lastMouseX, lastMouseY);
        drawNoverisButton(graphics, right - 130, bottom() - 39, 130, 27, "PRÓXIMA ▶", page + 1 < totalPages, lastMouseX, lastMouseY);
    }

    private void drawPreview(GuiGraphics graphics, MailSnapshotPayload.Entry entry) {
        int left = panelLeft() + 70, right = panelLeft() + panelWidth() - 70;
        int previewTop = top() + 86, previewBottom = bottom() - 52;
        graphics.fill(left, previewTop, right, previewBottom, 0xF50D0C09);
        graphics.fill(left, previewTop, right, previewTop + 3, GOLD);
        graphics.drawString(font, entry.subject().isBlank() ? "SEM ASSUNTO" : entry.subject(), left + 14, previewTop + 14, GOLD, false);
        graphics.drawString(font, tab == 0 ? "De: " + entry.sender() : "Para: " + entry.recipient(), left + 14, previewTop + 30, TEXT, false);
        graphics.drawString(font, "PRÉVIA DA CORRESPONDÊNCIA", left + 14, previewTop + 48, MUTED, false);
        graphics.drawString(font, truncate(entry.preview(), 90), left + 14, previewTop + 70, TEXT, false);
        graphics.drawString(font, "Clique novamente para fechar.", left + 14, previewBottom - 18, MUTED, false);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private String displayStatus(String status) {
        return switch (status) {
            case "READ" -> "LIDA";
            case "DELIVERED" -> "NOVA";
            case "IN_TRANSIT" -> "A CAMINHO";
            default -> status;
        };
    }

    private void drawEmpty(GuiGraphics graphics, String title, String description) {
        int center = panelLeft() + panelWidth() / 2;
        graphics.drawCenteredString(font, title, center, top() + 160, GOLD);
        graphics.drawCenteredString(font, description, center, top() + 184, MUTED);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int panelWidth() { return Math.min(980, width - 24); }
    private int panelHeight() { return Math.min(350, height - 12); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    private int bottom() { return top() + panelHeight(); }
}
