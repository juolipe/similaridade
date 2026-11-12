#!/bin/sh
# Compila e executa o programa com vários parâmetros, simulando a correção.
# Uso: ./testes.sh   (a partir da pasta Projeto)

set -e
cd "$(dirname "$0")"

echo ">>> Compilando"
javac *.java

falhas=0

rodar() {
    echo
    echo ">>> java Main $*"
    if java Main "$@"; then
        test -s resultado.txt || { echo "FALHA: resultado.txt vazio"; falhas=$((falhas + 1)); }
    else
        echo "FALHA: código de saída diferente de 0"
        falhas=$((falhas + 1))
    fi
}

rodar_erro() {
    echo
    echo ">>> (deve falhar) java Main $*"
    if java Main "$@" 2>/dev/null; then
        echo "FALHA: deveria ter retornado erro"
        falhas=$((falhas + 1))
    else
        echo "ok: erro tratado"
    fi
}

rodar ./documentos 0.7 lista
rodar ./documentos 0.75 lista --hash=2
rodar ./documentos 0.0 lista
rodar ./documentos 1.0 lista
rodar ./documentos 0.8 topK 5
rodar ./documentos 0.0 topK 3 --hash=2
rodar ./documentos 0.0 busca doc1.txt doc4.txt
rodar ./documentos 0.0 busca doc3.txt doc7.txt

rodar_erro
rodar_erro ./documentos 2 lista
rodar_erro ./documentos abc lista
rodar_erro ./nao_existe 0.5 lista
rodar_erro ./documentos 0.5 topK
rodar_erro ./documentos 0.5 topK zero
rodar_erro ./documentos 0.5 busca doc1.txt
rodar_erro ./documentos 0.5 busca doc1.txt inexistente.txt
rodar_erro ./documentos 0.5 modoInvalido
rodar_erro ./documentos 0.5 lista --hash=3

echo
if [ "$falhas" -eq 0 ]; then
    echo "=== TODOS OS TESTES PASSARAM ==="
else
    echo "=== $falhas FALHA(S) ==="
    exit 1
fi
