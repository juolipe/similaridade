/** Rotações feitas pela AVL. Dupla conta como 1 dupla, não 2 simples. */
public class ContagemRotacoes {

    int simplesEsquerda;       // RR
    int simplesDireita;        // LL
    int duplaEsquerdaDireita;  // LR
    int duplaDireitaEsquerda;  // RL

    public int getSimplesEsquerda() {
        return simplesEsquerda;
    }

    public int getSimplesDireita() {
        return simplesDireita;
    }

    public int getDuplaEsquerdaDireita() {
        return duplaEsquerdaDireita;
    }

    public int getDuplaDireitaEsquerda() {
        return duplaDireitaEsquerda;
    }

    public int getTotalSimples() {
        return simplesEsquerda + simplesDireita;
    }

    public int getTotalDuplas() {
        return duplaEsquerdaDireita + duplaDireitaEsquerda;
    }

    public int getTotal() {
        return getTotalSimples() + getTotalDuplas();
    }

    void somar(ContagemRotacoes outra) {
        simplesEsquerda += outra.simplesEsquerda;
        simplesDireita += outra.simplesDireita;
        duplaEsquerdaDireita += outra.duplaEsquerdaDireita;
        duplaDireitaEsquerda += outra.duplaDireitaEsquerda;
    }

    @Override
    public String toString() {
        return "Simples Esquerda: " + simplesEsquerda
            + " | Simples Direita: " + simplesDireita
            + " | Dupla Esquerda-Direita: " + duplaEsquerdaDireita
            + " | Dupla Direita-Esquerda: " + duplaDireitaEsquerda
            + " | Total: " + getTotal();
    }
}
