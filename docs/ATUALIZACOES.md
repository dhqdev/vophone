# Atualizações automáticas do vophone

O vophone se atualiza sozinho a partir das versões publicadas no GitHub
(aba **Releases** do repositório `dhqdev/vophone`).

## Como funciona

1. Toda mudança que entra no `master` vira uma versão nova **sozinha**: o GitHub cria a próxima tag
   (`v1.0.0` > `v1.0.1` > `v1.0.2`...), monta o APK (`.github/workflows/release.yml`, uns 5 a 10 minutos)
   e publica a versão na aba Releases, com o arquivo `vophone-v1.0.1.apk` e a lista do que mudou.
2. O vophone procura versão nova **a cada 6 horas**, quando a tela inicial aparece, **só no Wi-Fi**
   (para não gastar os dados móveis) e **nunca durante uma ligação**.
   Também dá para procurar na hora: vophone > Configurações > **Procurar atualizações**.
3. Achou versão nova: baixa e instala.
   - Celular com o **modo quiosque** (vophone administrador do celular) ou com o **sistema vophone** (ROM):
     instala sozinho, sem perguntar nada.
   - Nos outros casos: o Android mostra a tela "Deseja atualizar este app?", é só tocar em **Atualizar**.
     Na primeira vez ele pode pedir para permitir que o vophone instale apps: permita.

Um celular com um vophone instalado **antes** desta função (APKs de antes de 21/11/2026) precisa receber
o APK novo uma vez, do jeito normal. A partir daí ele se atualiza sozinho.

## Publicar uma versão nova

**Não precisa fazer nada**: é só mandar a mudança para o `master`. O GitHub soma 1 no último número
(`v1.0.1` > `v1.0.2`), e depois de `v1.0.99` vem `v1.1.0`. Acompanhe na aba **Actions** > **Release**.

Não viram versão nova:

- Envios que só mudam textos de documentação (arquivos `.md` e a pasta `docs/`), porque o app não muda.
- Commits com `[skip ci]` na mensagem, para quando quiser mandar algo sem publicar.

**Pular para um número maior** (por exemplo `v1.1.0` ou `v2.0.0`, para marcar uma mudança grande):

- Pelo site: aba **Actions** > **Release** > **Run workflow**, digite a versão sem o "v" (por exemplo
  `1.1.0`) e clique em **Run workflow**. Deixando vazio, publica a próxima automática.
- Pelo terminal: `git tag v1.1.0` e `git push origin v1.1.0`.

O número precisa ser **maior** que o da última versão, no formato `vX.Y.Z` (Y e Z vão de 0 a 99).
Se for menor, o GitHub recusa, porque os celulares só instalam versões maiores.

Se a montagem falhar (aba Actions em vermelho), nenhuma versão é criada e nenhum celular é afetado.
Corrija e mande de novo: a próxima mudança no `master` publica a versão.

## Instalar um APK feito no computador

Os APKs montados no computador (sem tag) têm um número de versão menor que qualquer versão publicada,
então não instalam por cima de uma versão do GitHub com `adb install -r`. Para testar um APK local
num celular que já recebeu uma versão publicada, use:

```
adb install -r -d vophone-debug.apk
```

Na próxima verificação o vophone volta para a última versão publicada.

## Segurança

O vophone só baixa atualizações das Releases deste repositório. Quem conseguir publicar uma versão nele
consegue atualizar os celulares, então mantenha a conta do GitHub protegida (verificação em duas etapas).

Como tudo que entra no `master` chega aos celulares em até 6 horas, teste as mudanças grandes antes de mandar
(por exemplo no emulador ou num celular de teste com o APK feito no computador).
