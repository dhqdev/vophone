# Atualizações automáticas do vophone

O vophone se atualiza sozinho a partir das versões publicadas no GitHub
(aba **Releases** do repositório `dhqdev/vophone`).

## Como funciona

1. Alguém publica uma versão nova criando uma **tag** como `v1.0.1`.
2. O GitHub monta o APK sozinho (`.github/workflows/release.yml`, uns 5 a 10 minutos) e publica a versão
   na aba Releases, com o arquivo `vophone-v1.0.1.apk`.
3. O vophone procura versão nova **a cada 6 horas**, quando a tela inicial aparece, **só no Wi-Fi**
   (para não gastar os dados móveis) e **nunca durante uma ligação**.
   Também dá para procurar na hora: vophone > Configurações > **Procurar atualizações**.
4. Achou versão nova: baixa e instala.
   - Celular com o **modo quiosque** (vophone administrador do celular) ou com o **sistema vophone** (ROM):
     instala sozinho, sem perguntar nada.
   - Nos outros casos: o Android mostra a tela "Deseja atualizar este app?", é só tocar em **Atualizar**.
     Na primeira vez ele pode pedir para permitir que o vophone instale apps: permita.

Um celular com um vophone instalado **antes** desta função (APKs de antes de 21/11/2026) precisa receber
o APK novo uma vez, do jeito normal. A partir daí ele se atualiza sozinho.

## Publicar uma versão nova

Use sempre um número **maior** que o da última versão, no formato `vX.Y.Z`
(Y e Z vão de 0 a 99). Exemplos: `v1.0.0`, `v1.0.1`, `v1.1.0`, `v2.0.0`.

**Pelo site do GitHub** (mais fácil):

1. Abra o repositório, aba **Actions** > **Release** (na lista da esquerda) > botão **Run workflow**.
2. Digite a versão nova sem o "v" (por exemplo `1.0.1`) e clique em **Run workflow**.
3. Em uns 5 a 10 minutos aparece a versão `v1.0.1` na aba **Releases**, com o arquivo `vophone-v1.0.1.apk`.

**Pelo terminal:**

```
git tag v1.0.1
git push origin v1.0.1
```

Ou peça para o Claude: "publica a versão 1.0.1".

Se a montagem falhar (aba Actions em vermelho), a versão fica sem APK e nenhum celular é afetado.
Corrija, apague a tag e a versão no GitHub, e publique de novo.

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
