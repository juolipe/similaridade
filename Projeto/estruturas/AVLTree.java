import java.util.ArrayList;
import java.util.List;

/**
 * AVL manual. Chave = similaridade; cada nó guarda a lista de pares com
 * aquela similaridade. Fator de balanceamento = altura(esq) - altura(dir).
 */
public class AVLTree {

    // Similaridades mais próximas que isso são a mesma chave (erro de double)
    private static final double EPSILON = 1e-9;

    private static class No {
        double chave;
        List<Resultado> resultados;
        int altura;
        No esquerda;
        No direita;

        No(Resultado r) {
            this.chave = r.getSimilaridade();
            this.resultados = new ArrayList<>();
            this.resultados.add(r);
            this.altura = 1;
        }
    }

    private No raiz;
    private int quantidadeNos;
    private int quantidadeResultados;
    private final ContagemRotacoes rotacoesTotais = new ContagemRotacoes();

    private int altura(No n) {
        return (n == null) ? 0 : n.altura;
    }

    private void atualizarAltura(No n) {
        n.altura = 1 + Math.max(altura(n.esquerda), altura(n.direita));
    }

    private int fatorBalanceamento(No n) {
        return (n == null) ? 0 : altura(n.esquerda) - altura(n.direita);
    }

    private int comparar(double a, double b) {
        if (Math.abs(a - b) < EPSILON) {
            return 0;
        }
        return (a < b) ? -1 : 1;
    }

    // ---------------- Rotações ----------------

    /**
     *       y            x
     *      / \          / \
     *     x   C  ==>   A   y
     *    / \              / \
     *   A   B            B   C
     */
    private No rotacaoDireita(No y) {
        No x = y.esquerda;
        No b = x.direita;

        x.direita = y;
        y.esquerda = b;

        atualizarAltura(y); // y ficou embaixo: atualiza primeiro
        atualizarAltura(x);

        return x;
    }

    /** Espelho da rotação à direita. */
    private No rotacaoEsquerda(No x) {
        No y = x.direita;
        No b = y.esquerda;

        y.esquerda = x;
        x.direita = b;

        atualizarAltura(x);
        atualizarAltura(y);

        return y;
    }

    /** Rebalanceia o nó se |fb| > 1 e registra a rotação usada. */
    private No balancear(No n, ContagemRotacoes cont) {
        atualizarAltura(n);
        int fb = fatorBalanceamento(n);

        if (fb > 1) {
            if (fatorBalanceamento(n.esquerda) >= 0) {
                cont.simplesDireita++;          // LL
                return rotacaoDireita(n);
            }

            cont.duplaEsquerdaDireita++;        // LR
            n.esquerda = rotacaoEsquerda(n.esquerda);
            return rotacaoDireita(n);
        }

        if (fb < -1) {
            if (fatorBalanceamento(n.direita) <= 0) {
                cont.simplesEsquerda++;         // RR
                return rotacaoEsquerda(n);
            }

            cont.duplaDireitaEsquerda++;        // RL
            n.direita = rotacaoDireita(n.direita);
            return rotacaoEsquerda(n);
        }

        return n;
    }

    // ---------------- Inserção ----------------

    /** Insere e retorna as rotações feitas nesta inserção. */
    public ContagemRotacoes inserir(Resultado r) {
        ContagemRotacoes cont = new ContagemRotacoes();
        raiz = inserir(raiz, r, cont);
        quantidadeResultados++;
        rotacoesTotais.somar(cont);
        return cont;
    }

    /** Desce como BST e rebalanceia os ancestrais na volta da recursão. */
    private No inserir(No n, Resultado r, ContagemRotacoes cont) {
        if (n == null) {
            quantidadeNos++;
            return new No(r);
        }

        int c = comparar(r.getSimilaridade(), n.chave);

        if (c == 0) {
            n.resultados.add(r); // chave repetida: árvore não muda
            return n;
        }

        if (c < 0) {
            n.esquerda = inserir(n.esquerda, r, cont);
        } else {
            n.direita = inserir(n.direita, r, cont);
        }

        return balancear(n, cont);
    }

    // ---------------- Consultas ----------------

    /** Pares com a similaridade exata, ou lista vazia. */
    public List<Resultado> buscar(double similaridade) {
        No n = raiz;

        while (n != null) {
            int c = comparar(similaridade, n.chave);

            if (c == 0) {
                return new ArrayList<>(n.resultados);
            }

            n = (c < 0) ? n.esquerda : n.direita;
        }

        return new ArrayList<>();
    }

    public List<Resultado> listarDecrescente() {
        List<Resultado> saida = new ArrayList<>();
        acimaDe(raiz, Double.NEGATIVE_INFINITY, Integer.MAX_VALUE, saida);
        return saida;
    }

    public List<Resultado> listarAcimaDe(double limiar) {
        List<Resultado> saida = new ArrayList<>();
        acimaDe(raiz, limiar, Integer.MAX_VALUE, saida);
        return saida;
    }

    public List<Resultado> topK(int k, double limiar) {
        List<Resultado> saida = new ArrayList<>();
        acimaDe(raiz, limiar, k, saida);
        return saida;
    }

    /**
     * Em-ordem reversa (dir, nó, esq) = decrescente. Se o nó está abaixo do
     * limiar, a subárvore esquerda é ignorada. Para ao atingir o limite.
     */
    private void acimaDe(No n, double limiar, int limite, List<Resultado> saida) {
        if (n == null || saida.size() >= limite) {
            return;
        }

        if (n.chave < limiar) {
            acimaDe(n.direita, limiar, limite, saida);
            return;
        }

        acimaDe(n.direita, limiar, limite, saida);

        for (Resultado r : n.resultados) {
            if (saida.size() >= limite) {
                return;
            }
            saida.add(r);
        }

        acimaDe(n.esquerda, limiar, limite, saida);
    }

    /** Nó mais à esquerda. */
    public List<Resultado> menorSimilaridade() {
        if (raiz == null) {
            return new ArrayList<>();
        }

        No n = raiz;

        while (n.esquerda != null) {
            n = n.esquerda;
        }

        return new ArrayList<>(n.resultados);
    }

    /** Nó mais à direita. */
    public List<Resultado> maiorSimilaridade() {
        if (raiz == null) {
            return new ArrayList<>();
        }

        No n = raiz;

        while (n.direita != null) {
            n = n.direita;
        }

        return new ArrayList<>(n.resultados);
    }

    // ---------------- Estatísticas ----------------

    public int altura() {
        return altura(raiz);
    }

    public int getQuantidadeNos() {
        return quantidadeNos;
    }

    public int getQuantidadeResultados() {
        return quantidadeResultados;
    }

    public ContagemRotacoes getRotacoesTotais() {
        return rotacoesTotais;
    }

    public boolean estaVazia() {
        return raiz == null;
    }

    /** true se é BST válida, balanceada e com alturas corretas. */
    public boolean validar() {
        return validar(raiz, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) >= 0;
    }

    /** Altura do nó, ou -1 se algo estiver errado. */
    private int validar(No n, double min, double max) {
        if (n == null) {
            return 0;
        }

        if (n.chave <= min || n.chave >= max) {
            return -1;
        }

        int he = validar(n.esquerda, min, n.chave);
        int hd = validar(n.direita, n.chave, max);

        if (he < 0 || hd < 0 || Math.abs(he - hd) > 1) {
            return -1;
        }

        int h = 1 + Math.max(he, hd);
        return (h == n.altura) ? h : -1;
    }

    /** Árvore deitada em texto (raiz à esquerda, maiores em cima). */
    public String desenhar() {
        StringBuilder sb = new StringBuilder();
        desenhar(raiz, "", sb);
        return sb.toString();
    }

    private void desenhar(No n, String prefixo, StringBuilder sb) {
        if (n == null) {
            return;
        }

        desenhar(n.direita, prefixo + "        ", sb);
        sb.append(prefixo)
          .append(String.format(java.util.Locale.US, "%.4f", n.chave))
          .append(" (").append(n.resultados.size()).append(")\n");
        desenhar(n.esquerda, prefixo + "        ", sb);
    }
}
