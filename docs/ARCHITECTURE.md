# Arquitetura

O fluxo é separado em quatro responsabilidades:

`MailLetter` representa conteúdo e ciclo de vida. `MailService` valida ações server-side. `MailSavedData` persiste cartas uma vez e mantém apenas UUIDs nos índices. `DeliveryEntry` representa o trabalho de entrega, separado da inbox.

O `DeliveryManager` futuro consumirá a `DeliveryQueue` e delegará a apresentação a um controlador. `CourierEntity` será visual e não conterá texto de carta nem conteúdo privado.

Todas as mutações persistentes devem terminar em `MailSavedData.markChanged()`. O jogador conectado é sempre a autoridade para o remetente e para ownership; payloads nunca fornecem esses dados como verdade.
