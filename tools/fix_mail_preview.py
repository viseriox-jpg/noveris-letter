from pathlib import Path

p = Path("src/main/java/dev/noveris/letter/client/MailScreen.java")
s = p.read_text()

old_call = "drawCourierPreview(g, x, y + 27, cardW, buttonY - 4, i, mx, my);"
new_call = "drawCourierPreview(g, x, y + 34, cardW, buttonY - 4, i, mx, my);"
if old_call not in s:
    raise SystemExit("courier preview call not found")
s = s.replace(old_call, new_call, 1)

old_scale = "int scale = Math.max(26, Math.min(54, height + 10));"
new_scale = "int scale = Math.max(22, Math.min(30, height / 2));"
if old_scale not in s:
    raise SystemExit("courier preview scale line not found")
s = s.replace(old_scale, new_scale, 1)

p.write_text(s)
