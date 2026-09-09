# Desenvolvimento

Use Java 21, Minecraft 1.21.1 e NeoForge 21.1.248. Rode `./gradlew build` após cada conjunto pequeno de alterações. O servidor dedicado não pode carregar classes de `client`, renderer ou screen.

Antes de adicionar loja ou cosméticos, preserve o fluxo postal e seus testes de segurança: UUID/ownership server-side, limite e cooldown server-side, nenhuma carta em dados sincronizados de entidade e nenhuma perda ao jogador offline.
