# Aparências de carteiro

Cada aparência é identificada por `ResourceLocation`, nunca por índice. A aparência selecionada é copiada para o `DeliveryEntry` no momento do envio; trocar a aparência depois não altera entregas já criadas.

O fallback é `noveris_letter:classic_mailman`. Definições futuras poderão ser carregadas de `data/noveris_letter/courier_appearances/<id>.json`, contendo movimento, raridade, modelo, textura, ícone e preço. JSON inválido ou recursos ausentes devem gerar aviso e usar o fallback sem cancelar a carta.

Adicionar um cosmético não deve exigir mudanças em `MailService`, `MailLetter` ou `DeliveryQueue`.
