# uso: baixar ARQUIVO SHA256_OU_VAZIO URL1 URL2 ...
baixar() {
  destino="$1"; sha="$2"; shift 2
  for url in "$@"; do
    echo "Baixando $url"
    rm -f "$destino"
    if wget -nv --tries=3 --timeout=30 --retry-connrefused --waitretry=5 "$url" -O "$destino"; then
      if [ -z "$sha" ] || echo "$sha  $destino" | sha256sum -c -; then
        return 0
      fi
      echo "Checksum nao confere, tentando o proximo endereco"
    else
      echo "Falhou, tentando o proximo endereco"
    fi
  done
  echo "ERRO: nao consegui baixar $destino"
  return 1
}
