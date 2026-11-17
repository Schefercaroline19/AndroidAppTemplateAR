# Sanrio AR — integração dos personagens

A interface do aplicativo foi personalizada com uma identidade visual rosa e com uma lista de personagens reconhecidos por nome de arquivo.

## Modelos esperados

Copie os arquivos GLB para `app/src/main/assets/` no Android Studio:

| Arquivo | Nome exibido |
|---|---|
| `hello_kitty.glb` | Hello Kitty |
| `my_melody.glb` | My Melody |
| `kuromi.glb` | Kuromi |
| `cinamon.glb` | Cinnamoroll |
| `pompompurin.glb` | Pompompurin |

A lista é montada automaticamente lendo os arquivos `.glb` nessa pasta. O nome de Cinnamoroll aceita `cinamon.glb`, `cinnamon.glb` ou `cinnamoroll.glb`.

## Adicionar os quatro modelos recebidos

O pacote `personagens_sanrio_assets.zip` contém:
- `my_melody.glb`
- `kuromi.glb`
- `cinamon.glb`
- `pompompurin.glb`

Extraia o ZIP na raiz do projeto, preservando o caminho `app/src/main/assets/`. O arquivo `hello_kitty.glb` que já existe no projeto local deve permanecer nessa mesma pasta.

## Fluxo do jogo

1. Abra a aba de coleção.
2. Escolha um personagem em **Ver na câmera**.
3. Posicione o modelo no ambiente.
4. Pressione **Coletar personagem (+1)** para somar um ponto.

## Observação de integração

A interface e o reconhecimento dos nomes já estão na branch `feature-sanrio`. Os quatro arquivos GLB recebidos foram reunidos em um ZIP para serem copiados para a pasta de assets; eles ainda precisam ser adicionados e enviados ao GitHub pelo Android Studio/GitHub Desktop. Faça isso antes de compilar para que os personagens apareçam na lista.
