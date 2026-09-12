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

old_roster = '''    private static final ResourceLocation[] COURIERS = {\n            CourierAppearanceRegistry.MOSSBLOOM_ID,\n            CourierAppearanceRegistry.COATI_ID,\n            CourierAppearanceRegistry.RED_PANDA_ID,\n            CourierAppearanceRegistry.BOOPLET_ID,\n            CourierAppearanceRegistry.CAPYBARA_ID,\n            CourierAppearanceRegistry.SPARROW_ID,\n            CourierAppearanceRegistry.BARN_OWL_ID\n    };\n    private static final String[] NAMES = {"MOSSBLOOM", "COATI", "PANDA-VERMELHO", "BOOPLET", "CAPIVARA", "PARDAL", "CORUJA"};\n    private static final String[] DESC = {"Natural e resistente", "Curioso e veloz", "Calmo e companheiro", "Pequeno e ágil", "Tranquilo e confiável", "Rápido e leve", "Silenciosa e observadora"};'''
new_roster = '''    private static final ResourceLocation[] COURIERS = {\n            CourierAppearanceRegistry.MELON_ID,\n            CourierAppearanceRegistry.CARROT_ID,\n            CourierAppearanceRegistry.WHEAT_ID,\n            CourierAppearanceRegistry.PUMPKIN_ID,\n            CourierAppearanceRegistry.POTATO_ID,\n            CourierAppearanceRegistry.SPARROW_ID\n    };\n    private static final String[] NAMES = {"MELITO", "CENOURITO", "TIQUINHO", "ABOBITO", "BATATIN", "PARDAL"};'''
if old_roster not in s:
    raise SystemExit("courier roster block not found")
s = s.replace(old_roster, new_roster)
s = s.replace("private static final int COURIERS_PER_PAGE = 6;", "private static final int COURIERS_PER_PAGE = 3;")

field_anchor = "    private List<String> onlinePlayers = List.of();"
field_replacement = field_anchor + "\n    private LivingEntity courierPreviewEntity;\n    private ResourceLocation courierPreviewAppearance;"
if field_anchor not in s:
    raise SystemExit("preview field anchor not found")
s = s.replace(field_anchor, field_replacement, 1)

old_click = '''            int cardTop = courierCardTop(), gap = 10;\n            int cardW = (panelWidth() - 48 - gap * 2) / 3;\n            int cardH = courierCardHeight();\n            int start = courierPage * COURIERS_PER_PAGE;\n            int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);\n\n            // Only the SELECT button is clickable, so cards can never steal the page controls.\n            for (int i = start; i < end; i++) {\n                int local = i - start, row = local / 3, col = local % 3;\n                int x = left + 24 + col * (cardW + gap), y = cardTop + row * (cardH + gap);\n                int buttonY = y + cardH - 29;\n                if (inside(x + 14, buttonY, cardW - 28, 25, mouseX, mouseY)) {\n                    selectedCourierIndex = i;\n                    PacketDistributor.sendToServer(new SelectCourierPayload(COURIERS[i]));\n                    return true;\n                }\n            }'''
new_click = '''            int cardTop = courierCardTop(), gap = 10;\n            int cardW = (panelWidth() - 48 - gap * 2) / 3;\n            int cardH = courierCardHeight();\n            int start = courierPage * COURIERS_PER_PAGE;\n            int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);\n\n            // One row per page: pagination has a dedicated strip below the cards.\n            for (int i = start; i < end; i++) {\n                int col = i - start;\n                int x = left + 24 + col * (cardW + gap), y = cardTop;\n                int buttonY = y + cardH - 29;\n                if (inside(x + 14, buttonY, cardW - 28, 25, mouseX, mouseY)) {\n                    selectedCourierIndex = i;\n                    courierPreviewEntity = null;\n                    courierPreviewAppearance = null;\n                    PacketDistributor.sendToServer(new SelectCourierPayload(COURIERS[i]));\n                    return true;\n                }\n            }'''
if old_click not in s:
    raise SystemExit("courier click block not found")
s = s.replace(old_click, new_click)

old_draw = '''        for (int i = start; i < end; i++) {\n            int local = i - start, row = local / 3, col = local % 3;\n            int x = left + 24 + col * (cardW + gap), y = cardTop + row * (cardH + gap);\n            boolean selected = selectedCourierIndex == i;\n            boolean hover = inside(x, y, cardW, cardH, mx, my);\n            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2A2417 : FIELD);\n            g.fill(x, y, x + cardW, y + 3, selected ? GOLD : 0xFF5C4C22);\n            g.drawCenteredString(font, truncate(NAMES[i], Math.max(12, cardW / 7)), x + cardW / 2, y + 14, selected ? GOLD : TEXT);\n            g.drawCenteredString(font, truncate(DESC[i], Math.max(15, cardW / 6)), x + cardW / 2, y + 35, MUTED);\n            g.drawCenteredString(font, i < 5 ? "CAMINHA ATÉ VOCÊ" : "VOA ATÉ VOCÊ", x + cardW / 2, y + 56, DIM);\n            int buttonY = y + cardH - 29;\n            drawButton(g, x + 14, buttonY, cardW - 28, 25, selected ? "SELECIONADO" : "SELECIONAR", selected, mx, my);\n        }'''
new_draw = '''        for (int i = start; i < end; i++) {\n            int col = i - start;\n            int x = left + 24 + col * (cardW + gap), y = cardTop;\n            boolean selected = selectedCourierIndex == i;\n            boolean hover = inside(x, y, cardW, cardH, mx, my);\n            g.fill(x, y, x + cardW, y + cardH, hover ? 0xFF2A2417 : FIELD);\n            g.fill(x, y, x + cardW, y + 3, selected ? GOLD : 0xFF5C4C22);\n            g.drawCenteredString(font, truncate(NAMES[i], Math.max(12, cardW / 7)), x + cardW / 2, y + 14, selected ? GOLD : TEXT);\n\n            // No descriptive sentence: the middle of the card is reserved for the live model preview.\n            int buttonY = y + cardH - 29;\n            drawCourierPreview(g, x, y + 27, cardW, buttonY - 4, i, mx, my);\n            drawButton(g, x + 14, buttonY, cardW - 28, 25, selected ? "SELECIONADO" : "SELECIONAR", selected, mx, my);\n        }'''
if old_draw not in s:
    raise SystemExit("courier draw block not found")
s = s.replace(old_draw, new_draw)

helper_anchor = '''    private void drawCourierPagination(GuiGraphics g, int left, int bottom, int right, int mx, int my) {'''
helpers = '''    private void drawCourierPreview(GuiGraphics g, int x, int top, int cardW, int bottom, int index, int mx, int my) {\n        if (bottom <= top) return;\n        ResourceLocation appearance = COURIERS[index];\n        LivingEntity entity = getCourierPreviewEntity(appearance);\n        if (entity == null) return;\n\n        int height = bottom - top;\n        int scale = Math.max(26, Math.min(54, height + 10));\n        int centerX = x + cardW / 2;\n        int centerY = (top + bottom) / 2;\n\n        InventoryScreen.renderEntityInInventoryFollowsMouse(\n                g, centerX - scale, centerY - scale, centerX + scale, centerY + scale,\n                scale, 0.0F, mx, my, entity);\n    }\n\n    private LivingEntity getCourierPreviewEntity(ResourceLocation appearance) {\n        if (Minecraft.getInstance().level == null) return null;\n        if (courierPreviewEntity != null && appearance.equals(courierPreviewAppearance)) return courierPreviewEntity;\n\n        LivingEntity entity;\n        if (appearance.equals(CourierAppearanceRegistry.SPARROW_ID)) {\n            CourierLegacyBirdEntity bird = CourierEntities.COURIER_LEGACY_BIRD.get().create(Minecraft.getInstance().level);\n            if (bird == null) return null;\n            bird.setAppearance(appearance);\n            entity = bird;\n        } else {\n            CourierCropEntity crop = CourierEntities.COURIER_CROP.get().create(Minecraft.getInstance().level);\n            if (crop == null) return null;\n            crop.setAppearance(appearance);\n            entity = crop;\n        }\n\n        entity.setNoGravity(true);\n        entity.setYRot(0.0F);\n        entity.setYBodyRot(0.0F);\n        entity.setYHeadRot(0.0F);\n        entity.yRotO = 0.0F;\n        entity.yBodyRotO = 0.0F;\n        entity.yHeadRotO = 0.0F;\n        courierPreviewAppearance = appearance;\n        courierPreviewEntity = entity;\n        return entity;\n    }\n\n'''
if helper_anchor not in s:
    raise SystemExit("pagination anchor not found")
s = s.replace(helper_anchor, helpers + helper_anchor, 1)

s = s.replace("private int courierNavY() { return bottom() - 39; }", "private int courierNavY() { return bottom() - 34; }")
s = s.replace("private int courierCardHeight() {\n        int gap = 10;\n        int available = courierNavY() - courierCardTop() - gap;\n        // Keep a hard ceiling so the second row can never approach the pagination bar.\n        return Math.max(96, Math.min(180, available / 2));\n    }", "private int courierCardHeight() {\n        // A single row is intentional: the preview and select button stay above the pagination strip.\n        int available = courierNavY() - courierCardTop() - 16;\n        return Math.max(88, Math.min(128, available));\n    }")

p.write_text(s)
