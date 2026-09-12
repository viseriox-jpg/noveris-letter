from pathlib import Path

p = Path("src/main/java/dev/noveris/letter/client/MailScreen.java")
s = p.read_text()

# Client-side 3D courier preview imports.
s = s.replace(
    "import dev.noveris.letter.courier.CourierAppearanceRegistry;",
    "import dev.noveris.letter.courier.CourierAppearanceRegistry;\nimport dev.noveris.letter.courier.CourierCropEntity;\nimport dev.noveris.letter.courier.CourierEntities;\nimport dev.noveris.letter.courier.CourierLegacyBirdEntity;"
)
s = s.replace(
    "import net.minecraft.client.gui.screens.Screen;",
    "import net.minecraft.client.gui.screens.Screen;\nimport net.minecraft.client.gui.screens.inventory.InventoryScreen;"
)
s = s.replace(
    "import net.minecraft.client.multiplayer.PlayerInfo;",
    "import net.minecraft.client.multiplayer.PlayerInfo;\nimport net.minecraft.world.entity.LivingEntity;"
)

old_roster = '''    private static final ResourceLocation[] COURIERS = {
            CourierAppearanceRegistry.MOSSBLOOM_ID,
            CourierAppearanceRegistry.COATI_ID,
            CourierAppearanceRegistry.RED_PANDA_ID,
            CourierAppearanceRegistry.BOOPLET_ID,
            CourierAppearanceRegistry.CAPYBARA_ID,
            CourierAppearanceRegistry.SPARROW_ID,
            CourierAppearanceRegistry.BARN_OWL_ID
    };
    private static final String[] NAMES = {"MOSSBLOOM", "COATI", "PANDA-VERMELHO", "BOOPLET", "CAPIVARA", "PARDAL", "CORUJA"};
    private static final String[] DESC = {"Natural e resistente", "Curioso e veloz", "Calmo e companheiro", "Pequeno e ágil", "Tranquilo e confiável", "Rápido e leve", "Silenciosa e observadora"};'''
new_roster = '''    private static final ResourceLocation[] COURIERS = {
            CourierAppearanceRegistry.MELON_ID,
            CourierAppearanceRegistry.CARROT_ID,
            CourierAppearanceRegistry.WHEAT_ID,
            CourierAppearanceRegistry.PUMPKIN_ID,
            CourierAppearanceRegistry.POTATO_ID,
            CourierAppearanceRegistry.SPARROW_ID,
            CourierAppearanceRegistry.BARN_OWL_ID
    };
    private static final String[] NAMES = {"MELITO", "CENOURITO", "TIQUINHO", "ABOBITO", "BATATIN", "PARDAL", "CORUJA"};
    private static final String[] DESC = {"Doce e animado", "Ágil e simpático", "Leve e trabalhador", "Forte e festivo", "Pequeno e resistente", "Rápido e leve", "Silenciosa e observadora"};'''
if old_roster not in s:
    raise SystemExit("courier roster block not found")
s = s.replace(old_roster, new_roster)
s = s.replace("private static final int COURIERS_PER_PAGE = 6;", "private static final int COURIERS_PER_PAGE = 3;")

# Keep the preview entity cached for the current card instead of constructing one every frame.
field_anchor = "    private List<String> onlinePlayers = List.of();"
field_replacement = field_anchor + "\n    private LivingEntity courierPreviewEntity;\n    private ResourceLocation courierPreviewAppearance;"
if field_anchor not in s:
    raise SystemExit("preview field anchor not found")
s = s.replace(field_anchor, field_replacement, 1)

old_click = '''            int cardTop = courierCardTop(), gap = 10;
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
            }'''
new_click = '''            int cardTop = courierCardTop(), gap = 10;
            int cardW = (panelWidth() - 48 - gap * 2) / 3;
            int cardH = courierCardHeight();
            int start = courierPage * COURIERS_PER_PAGE;
            int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);

            // One row per page: pagination has a dedicated strip below the cards.
            for (int i = start; i < end; i++) {
                int col = i - start;
                int x = left + 24 + col * (cardW + gap), y = cardTop;
                int buttonY = y + cardH - 29;
                if (inside(x + 14, buttonY, cardW - 28, 25, mouseX, mouseY)) {
                    selectedCourierIndex = i;
                    courierPreviewEntity = null;
                    courierPreviewAppearance = null;
                    PacketDistributor.sendToServer(new SelectCourierPayload(COURIERS[i]));
                    return true;
                }
            }'''
if old_click not in s:
    raise SystemExit("courier click block not found")
s = s.replace(old_click, new_click)

old_draw = '''        for (int i = start; i < end; i++) {
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
        }'''
new_draw = '''        for (int i = start; i < end; i++) {
            int col = i - start;
            int x = left + 24 + col * (cardW + gap), y = cardTop;
            boolean selected = selectedCourierIndex == i;
            boolean hover = inside(x, y, cardW, cardH, mx, my);
            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2A2417 : FIELD);
            g.fill(x, y, x + cardW, y + 3, selected ? GOLD : 0xFF5C4C22);
            g.drawCenteredString(font, truncate(NAMES[i], Math.max(12, cardW / 7)), x + cardW / 2, y + 14, selected ? GOLD : TEXT);
            g.drawCenteredString(font, truncate(DESC[i], Math.max(15, cardW / 6)), x + cardW / 2, y + 35, MUTED);

            // The old movement sentence is intentionally gone. The empty middle area is now a
            // compact live demonstration of the actual courier model, leaving the button below untouched.
            int buttonY = y + cardH - 29;
            drawCourierPreview(g, x, y + 48, cardW, buttonY - 4, i, mx, my);
            drawButton(g, x + 14, buttonY, cardW - 28, 25, selected ? "SELECIONADO" : "SELECIONAR", selected, mx, my);
        }'''
if old_draw not in s:
    raise SystemExit("courier draw block not found")
s = s.replace(old_draw, new_draw)

# Insert preview helpers immediately before pagination.
helper_anchor = '''    private void drawCourierPagination(GuiGraphics g, int left, int bottom, int right, int mx, int my) {'''
helpers = '''    private void drawCourierPreview(GuiGraphics g, int x, int top, int cardW, int bottom, int index, int mx, int my) {
        if (bottom <= top) return;
        ResourceLocation appearance = COURIERS[index];
        LivingEntity entity = getCourierPreviewEntity(appearance);
        if (entity == null) return;

        int left = x + 12;
        int right = x + cardW - 12;
        int height = bottom - top;
        int scale = Math.max(26, Math.min(54, height + 10));
        int centerX = (left + right) / 2;
        int centerY = (top + bottom) / 2;

        // InventoryScreen's renderer is designed for LivingEntity previews and works with the
        // same GeckoLib-backed renderers registered for the real courier entities.
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                centerX - scale,
                centerY - scale,
                centerX + scale,
                centerY + scale,
                scale,
                0.0F,
                mx,
                my,
                entity
        );
    }

    private LivingEntity getCourierPreviewEntity(ResourceLocation appearance) {
        if (Minecraft.getInstance().level == null) return null;
        if (courierPreviewEntity != null && appearance.equals(courierPreviewAppearance)) return courierPreviewEntity;

        LivingEntity entity;
        if (appearance.equals(CourierAppearanceRegistry.SPARROW_ID) || appearance.equals(CourierAppearanceRegistry.BARN_OWL_ID)) {
            CourierLegacyBirdEntity bird = CourierEntities.COURIER_LEGACY_BIRD.get().create(Minecraft.getInstance().level);
            if (bird == null) return null;
            bird.setAppearance(appearance);
            entity = bird;
        } else {
            CourierCropEntity crop = CourierEntities.COURIER_CROP.get().create(Minecraft.getInstance().level);
            if (crop == null) return null;
            crop.setAppearance(appearance);
            entity = crop;
        }

        entity.setNoGravity(true);
        entity.setYRot(0.0F);
        entity.setYBodyRot(0.0F);
        entity.setYHeadRot(0.0F);
        entity.yRotO = 0.0F;
        entity.yBodyRotO = 0.0F;
        entity.yHeadRotO = 0.0F;
        courierPreviewAppearance = appearance;
        courierPreviewEntity = entity;
        return entity;
    }

'''
if helper_anchor not in s:
    raise SystemExit("pagination anchor not found")
s = s.replace(helper_anchor, helpers + helper_anchor, 1)

s = s.replace("private int courierNavY() { return bottom() - 39; }", "private int courierNavY() { return bottom() - 34; }")
s = s.replace("private int courierCardHeight() {\n        int gap = 10;\n        int available = courierNavY() - courierCardTop() - gap;\n        // Keep a hard ceiling so the second row can never approach the pagination bar.\n        return Math.max(96, Math.min(180, available / 2));\n    }", "private int courierCardHeight() {\n        // A single row is intentional: the preview and select button stay above the pagination strip.\n        int available = courierNavY() - courierCardTop() - 16;\n        return Math.max(88, Math.min(128, available));\n    }")

p.write_text(s)
