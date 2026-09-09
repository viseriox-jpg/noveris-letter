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
