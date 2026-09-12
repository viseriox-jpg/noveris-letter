from pathlib import Path

p = Path("src/main/java/dev/noveris/letter/client/MailScreen.java")
s = p.read_text()
s = s.replace("private static final int COURIERS_PER_PAGE = 6;", "private static final int COURIERS_PER_PAGE = 3;")
old = '''            int cardTop = courierCardTop(), gap = 10;
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
new = '''            int cardTop = courierCardTop(), gap = 10;
            int cardW = (panelWidth() - 48 - gap * 2) / 3;
            int cardH = courierCardHeight();
            int start = courierPage * COURIERS_PER_PAGE;
            int end = Math.min(COURIERS.length, start + COURIERS_PER_PAGE);

            // One row per page: the pagination bar has its own dedicated space below the cards.
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
if old not in s:
    raise SystemExit("courier click block not found")
s = s.replace(old, new)
old2 = '''        for (int i = start; i < end; i++) {
            int local = i - start, row = local / 3, col = local % 3;
            int x = left + 24 + col * (cardW + gap), y = cardTop + row * (cardH + gap);
            boolean selected = selectedCourierIndex == i;'''
new2 = '''        for (int i = start; i < end; i++) {
            int col = i - start;
            int x = left + 24 + col * (cardW + gap), y = cardTop;
            boolean selected = selectedCourierIndex == i;'''
if old2 not in s:
    raise SystemExit("courier draw block not found")
s = s.replace(old2, new2)
s = s.replace("private int courierNavY() { return bottom() - 39; }", "private int courierNavY() { return bottom() - 34; }")
old3 = '''    private int courierCardHeight() {
        int gap = 10;
        int available = courierNavY() - courierCardTop() - gap;
        // Keep a hard ceiling so the second row can never approach the pagination bar.
        return Math.max(96, Math.min(180, available / 2));
    }'''
new3 = '''    private int courierCardHeight() {
        // A single row is intentional: pagination must never share vertical space with cards.
        int available = courierNavY() - courierCardTop() - 16;
        return Math.max(88, Math.min(128, available));
    }'''
if old3 not in s:
    raise SystemExit("courier height block not found")
s = s.replace(old3, new3)
p.write_text(s)
