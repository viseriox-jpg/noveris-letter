from pathlib import Path

p = Path("src/main/java/dev/noveris/letter/client/MailScreen.java")
s = p.read_text()

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
            boolean selected = selectedCourierIndex == i;'''
new_draw = '''        for (int i = start; i < end; i++) {
            int col = i - start;
            int x = left + 24 + col * (cardW + gap), y = cardTop;
            boolean selected = selectedCourierIndex == i;'''
if old_draw not in s:
    raise SystemExit("courier draw block not found")
s = s.replace(old_draw, new_draw)
s = s.replace("private int courierNavY() { return bottom() - 39; }", "private int courierNavY() { return bottom() - 34; }")

old_height = '''    private int courierCardHeight() {
        int gap = 10;
        int available = courierNavY() - courierCardTop() - gap;
        // Keep a hard ceiling so the second row can never approach the pagination bar.
        return Math.max(96, Math.min(180, available / 2));
    }'''
new_height = '''    private int courierCardHeight() {
        // A single row is intentional: pagination never shares vertical space with cards.
        int available = courierNavY() - courierCardTop() - 16;
        return Math.max(88, Math.min(128, available));
    }'''
if old_height not in s:
    raise SystemExit("courier height block not found")
s = s.replace(old_height, new_height)

p.write_text(s)
