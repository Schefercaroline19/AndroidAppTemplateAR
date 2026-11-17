# Fase 2 — Realidade Aumentada no Android

## Modelos 3D
A pasta `app/src/main/assets` contém os modelos GLB:
- `neutrofilo.glb`
- `plane.glb`
- `sofa.glb`

A tela de seleção (`ChangeObjectFragment`) lê automaticamente os arquivos `.glb` dessa pasta e cria uma opção para exibir cada modelo na câmera de realidade aumentada.

## Nova funcionalidade: coleta e pontuação
Na tela de realidade aumentada:
1. O usuário escolhe um modelo na lista e o posiciona no ambiente.
2. O botão **Coletar objeto (+1)** remove o modelo atual da cena.
3. Cada coleta soma um ponto no contador.
4. A coleta reproduz um efeito sonoro de confirmação.

O botão de coleta permanece desativado enquanto o modelo está carregando ou depois de ser coletado. Para coletar outro objeto, basta voltar à lista e selecionar outro modelo.

## Arquivos principais
- `CameraFragment.kt`: carregamento do modelo, coleta, pontuação e efeito sonoro.
- `fragment_camera.xml`: contador de pontos e botão de coleta.
- `ChangeObjectFragment.kt`: lista dinâmica dos modelos GLB já existente no projeto.

## Testes necessários antes da entrega
- Executar em um celular compatível com Google ARCore e conceder permissão de câmera.
- Selecionar e posicionar cada modelo GLB.
- Verificar que a coleta remove o modelo, soma exatamente um ponto e toca o som.
- Repetir com os três modelos e registrar prints/fotos da execução real.

> A compilação e os testes em dispositivo precisam ser realizados no Android Studio; não foram executados pelo GitHub.
