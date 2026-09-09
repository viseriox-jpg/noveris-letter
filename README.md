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

## Build

Requer Java 21 e acesso aos repositórios Maven do NeoForge. Execute `./gradlew build`.

## Comandos planejados

`/correio` abrirá a interface postal e `/correio escrever` iniciará uma carta. Os comandos vanilla `/tell`, `/msg` e `/w` não são substituídos por padrão.
