# Noveris Letter

Sistema de correspondência imersivo para Minecraft 1.21.1 com NeoForge 21.1.248.

## Arquitetura

O núcleo separa **cartas**, **fila de entrega**, **apresentação da entrega** e **aparência do carteiro**.

- `MailLetter`: conteúdo persistente da correspondência.
- `DeliveryEntry` / `DeliveryQueue`: agenda e prioridade de entrega.
- `CourierAppearance`: definição cosmética independente da lógica postal.
- A aparência do carteiro é capturada no momento do envio e armazenada no `DeliveryEntry`, preservando o visual histórico da entrega.

## Ambiente

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21

> O projeto está sendo construído incrementalmente a partir do MDK oficial do NeoForge para 1.21.1.

## Estado atual

O núcleo server-side já possui `MailSavedData` como fonte única persistente para cartas, índices por UUID, fila de entrega, bloqueios e perfis de carteiro. `MailService` concentra as operações autorizadas; a aparência do remetente é capturada no `DeliveryEntry` no envio.

O projeto usa o networking moderno de NeoForge (CustomPacketPayload) como próximo passo. GeckoLib não foi adicionado: a primeira apresentação será baseada em entidades/renderers vanilla, mantendo o domínio postal independente de renderização.

## Carteiros

Os carteiros disponíveis atualmente são:

- Melito
- Cenourito
- Tiquinho
- Abobito
- Batatin
- Pardal
- Coruja-das-torres

A interface postal organiza os carteiros em páginas, permitindo adicionar novos modelos sem sobrepor os controles de navegação.

## Comandos

- `/correio` — abre a interface postal.
- `/correio carteiro` — abre diretamente a aba de seleção de carteiros.
- `/correio historico apagar <jogador>` — remove definitivamente o histórico de correspondências do jogador; requer OP nível 2.

O envio, a caixa de entrada, as enviadas e a composição de cartas são acessados pela interface `/correio`.

## Build

Requer Java 21 e acesso aos repositórios Maven do NeoForge. Execute `./gradlew build`.

<!-- Build trigger: 2026-09-12 -->
