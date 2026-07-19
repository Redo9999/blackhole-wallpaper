#!/data/data/com.termux/files/usr/bin/bash
# Comandos pra rodar no Termux, dentro da pasta BlackHoleWallpaper
# (depois de extrair o zip no celular, ex: em ~/storage/shared/... ou ~/)

# 1) instalar git, se ainda não tiver
pkg install git -y

# 2) entrar na pasta do projeto (ajuste o caminho conforme onde extraiu)
cd ~/BlackHoleWallpaper

# 3) inicializar o repositório
git init
git add .
git commit -m "primeira versão: live wallpaper do buraco negro pessoal"

# 4) configurar identidade do git (só precisa fazer uma vez no Termux)
git config --global user.name "SEU_USUARIO_GITHUB"
git config --global user.email "seu_email@exemplo.com"

# 5) criar o repositório vazio no GitHub antes (pelo app ou site do GitHub,
#    em github.com/new) com o nome, por exemplo, "blackhole-wallpaper".
#    NÃO marque "adicionar README" lá, pra evitar conflito.

# 6) conectar o repositório local ao remoto
git remote add origin https://github.com/SEU_USUARIO_GITHUB/blackhole-wallpaper.git
git branch -M main

# 7) enviar (vai pedir usuário + token de acesso pessoal, não a senha normal
#    -- crie um em github.com/settings/tokens, com permissão "repo")
git push -u origin main
