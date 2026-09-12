# Aparências de carteiro

Cada aparência é identificada por `ResourceLocation`, nunca por índice. A aparência selecionada é copiada para o `DeliveryEntry` no momento do envio; trocar a aparência depois não altera entregas já criadas.

O roster atual contém somente sete aparências: Melon Critter, Carrot Critter, Wheat Critter, Pumpkin Critter, Potato Critter, Pardal e Coruja-das-torres. O fallback é `noveris_letter:melon_critter`.

A GUI usa uma grade de três carteiros por página, com a paginação em uma faixa separada. Assim, adicionar novas aparências ao registro cria novas páginas sem sobrepor os cards aos controles.

Adicionar um cosmético não deve exigir mudanças em `MailService`, `MailLetter` ou `DeliveryQueue`.
