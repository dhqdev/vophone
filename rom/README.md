# vophone como sistema operacional (ROM)

Aqui ficam os arquivos para gerar um **sistema completo** para o celular: o sistema original
do fabricante (Samsung, Motorola, Xiaomi...) é apagado e no lugar entra um sistema limpo,
com o vophone embutido como a única interface do celular.

## O que é, exatamente

Todo sistema alternativo que faz ligações em celulares hoje é construído sobre o código aberto do
Android (AOSP). O vophone usa o **LineageOS**, o mais usado deles, como base e muda o que importa:

- **Sem** a interface do fabricante, **sem** a tela inicial padrão e **sem** o app de telefone padrão.
- O vophone vem dentro do sistema como tela inicial e como telefone (tela de ligação com botões grandes).
- As permissões do vophone já vêm concedidas: não aparece nenhuma tela de "Permitir?". Isso inclui
  notificações, acesso às notificações (ícone de mensagens novas), "Não perturbe" (botão de som da tela
  inicial) e abrir a tela do alarme e do remédio sozinho, sem a permissão "sobrepor a outros apps".
- Configurações do sistema já ajustadas para idosos:
  - sem tela de bloqueio de arrastar: apertou o botão de ligar, já aparece o vophone;
  - botões Voltar / Início embaixo da tela, em vez de gestos de arrastar;
  - a tela não gira sozinha, brilho automático começando alto, tela apaga só depois de 2 minutos;
  - animações mais rápidas;
  - "tocar e segurar" leva 1 segundo, para dedos trêmulos não abrirem menus sem querer;
  - vibração ao tocar, som do teclado e som ao carregar ligados;
  - data, hora e fuso horário automáticos pela operadora.
- Na primeira vez que o vophone abre: campainha e volume da ligação no máximo, letras maiores
  (130%) e o vophone sempre igual (tema claro, um toque simples abre as coisas).
- Saem do sistema: a tela inicial e o telefone padrão, o atualizador do LineageOS (uma atualização
  oficial apagaria o vophone) e extras como gravador, agenda, música e equalizador.
- Loja de aplicativos (Play Store, com o pacote do Google do passo 4) no botão Mais > Loja de apps,
  funcionando também com o modo quiosque ligado.
- O modo quiosque continua disponível (veja o passo 6).

Os arquivos estão em `vendor/vophone/`:

| Arquivo | O que faz |
|---|---|
| `Android.bp` | Coloca o vophone no sistema e remove a tela inicial e o telefone padrão |
| `vophone.mk` | Liga tudo isso ao produto do celular |
| `default-permissions-vophone.xml` | Permissões já concedidas |
| `privapp-permissions-vophone.xml` | Permissões de app do sistema: atualizar sozinho e abrir alarmes sozinho |
| `overlay/.../config.xml` | vophone como app de telefone do sistema, com acesso às notificações e ao "Não perturbe" |
| `overlay/.../config.xml` (navegação) | Botões Voltar / Início em vez de gestos |
| `overlay/.../defaults.xml` | Padrões do sistema para idosos (lista acima) |

## 0. O celular tem suporte?

Isso depende **do modelo exato** do celular. Procure o modelo em https://wiki.lineageos.org/devices
e anote o **codinome** (exemplo: Galaxy A52 4G = `a52q`).

- **Está na lista:** dá para seguir este guia.
- **Não está na lista:** não existe um sistema pronto para esse modelo, e criar um do zero leva meses.
  Nesse caso o caminho é usar o vophone como app com o modo quiosque (veja `docs/INSTALACAO.md`),
  ou escolher um celular barato que esteja na lista.
- Celulares Samsung vendidos nos EUA, Huawei e alguns Motorola de operadora **não deixam** trocar o sistema.

## 1. O que você precisa

- Um computador com **Linux** (Ubuntu 22.04 ou mais novo), **16 GB de RAM** (32 GB é melhor) e
  **300 GB livres** em SSD.
- Internet boa: o código do LineageOS tem uns 100 GB.
- Tempo: a primeira compilação leva de 3 a 10 horas, dependendo do computador.
- Cabo USB e o `adb`/`fastboot` instalados (vêm no Android SDK Platform-Tools).

## 2. Baixar o código do LineageOS

Siga a página **"Build for <codinome>"** do wiki do seu modelo (link na página do aparelho em
https://wiki.lineageos.org/devices). Ela tem a versão certa do LineageOS para o modelo e mostra como:

1. Instalar os pacotes de compilação e o `repo`.
2. `repo init -u https://github.com/LineageOS/android.git -b lineage-<versão> --git-lfs`
3. `repo sync`
4. Extrair os arquivos proprietários do aparelho (`extract-files.sh`).

**Pare antes do passo "Start the build"**: a compilação é feita pelo script do próximo passo.

## 3. Compilar o sistema vophone

Na pasta deste repositório:

```
rom/build-rom.sh ~/android/lineage <codinome>
```

O script compila o vophone, coloca o pacote `vendor/vophone` dentro do código do LineageOS,
liga o vophone ao produto do aparelho e compila o sistema. No fim ele mostra onde está o
arquivo `lineage-*.zip`.

Se você já tem o APK do vophone: `VOPHONE_APK=/caminho/vophone.apk rom/build-rom.sh ...`

## 4. Gravar no celular

Siga a página **"Install LineageOS on <codinome>"** do wiki do seu modelo, usando os arquivos que você compilou
(pasta `out/target/product/<codinome>/`) em vez dos arquivos oficiais. Em resumo:

1. **Faça backup** de fotos e contatos. **O desbloqueio apaga tudo.**
2. Ative Opções do desenvolvedor > **Desbloqueio de OEM** e **Depuração USB**.
3. Desbloqueie o bootloader (o comando varia: `fastboot flashing unlock`, ou o app da Xiaomi, ou o
   modo Download da Samsung). Na Xiaomi o desbloqueio pode exigir esperar alguns dias.
4. Grave o recovery que você compilou (`recovery.img` ou `boot.img`, como o wiki indicar).
5. No recovery: *Factory reset* > *Format data*, depois *Apply update* > *Apply from ADB* e no computador:

   ```
   adb sideload lineage-*.zip
   ```

6. **Recomendado:** ainda no recovery, grave também o pacote do Google (MindTheGapps, link no wiki).
   É ele que traz a **Play Store**, que aparece no vophone em Mais > **Loja de apps** para baixar outros
   aplicativos, e faz as mensagens do WhatsApp chegarem na hora.
   Sem conta Google, dá para instalar no lugar a Aurora Store ou o F-Droid (lojas abertas): o botão
   Loja de apps do vophone abre a que estiver instalada.
7. *Reboot system now*.

## 5. Primeira vez que liga

1. No assistente, escolha **Português (Brasil)**, conecte no Wi-Fi e **não** adicione conta Google ainda.
2. O vophone já abre como tela inicial. Cadastre os contatos e os contatos de emergência (tela SOS).

## 6. Modo quiosque (opcional)

Com o celular recém-instalado e sem nenhuma conta, rode no computador:

```
adb shell dpm set-device-owner com.bald.uriah.baldphone/.broadcast_receivers.BaldDeviceAdminReceiver
```

Depois adicione a conta Google, instale e configure o WhatsApp e, por último, ligue o modo quiosque em
vophone > Configurações > Modo quiosque. Detalhes em `docs/INSTALACAO.md`.

## Atualizar o vophone depois

Não precisa gravar o sistema de novo: o vophone do sistema se atualiza sozinho, sem perguntar, quando sai
uma versão nova no GitHub (veja `docs/ATUALIZACOES.md`). As versões são assinadas com a mesma chave
(`app/vophone-debug.keystore`) do vophone embutido no sistema.
Para instalar um APK feito no computador: `adb install -r -d vophone-debug.apk`.

## Voltar ao sistema original

Baixe o sistema original do fabricante para o modelo e grave com a ferramenta oficial do fabricante
(Odin na Samsung, Mi Flash na Xiaomi, Rescue and Smart Assistant na Motorola). Isso também apaga tudo.

## Situação

Estes arquivos foram escritos para LineageOS 20 a 22 (Android 13 a 15). **Ainda não foram compilados
nem testados em um aparelho**: o primeiro build para o modelo do celular pode precisar de ajustes.
Se a compilação reclamar de um nome na lista `overrides` do `Android.bp`, apague aquela linha.
Se o celular não passar da animação de início depois de gravar, rode `adb logcat | grep -i privapp`: toda
permissão de app do sistema pedida no `AndroidManifest.xml` precisa estar em `privapp-permissions-vophone.xml`.
Por isso compile o sistema sempre com o APK do mesmo código (o `build-rom.sh` já faz assim).
Sem a tela inicial padrão, o botão de apps recentes deixa de fazer alguma coisa (de propósito).
