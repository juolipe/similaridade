/**
 * Tabela hash manual: palavra -> frequência.
 * Colisões: endereçamento aberto com sondagem linear.
 * Rehash para o próximo primo >= 2x quando a carga passa de 0.75.
 */
public class HashTable {

    private static final int CAPACIDADE_INICIAL = 11;
    private static final double CARGA_MAXIMA = 0.75;

    private static final int OCUPADO = 1;
    private static final int REMOVIDO = -1; // lápide

    private Node[] tabela;
    private int tamanho;
    private int algoritmoHash;
    private int totalColisoes; // posições ocupadas puladas na sondagem
    private int totalRehashes;

    private static class Node {
        String chave;
        int frequencia;
        int status;

        Node(String chave, int frequencia) {
            this.chave = chave;
            this.frequencia = frequencia;
            this.status = OCUPADO;
        }
    }

    public HashTable() {
        this(1);
    }

    /** algoritmoHash: 1 = polinomial, 2 = SDBM. */
    public HashTable(int algoritmoHash) {
        if (algoritmoHash != 1 && algoritmoHash != 2) {
            throw new IllegalArgumentException("Algoritmo deve ser 1 ou 2");
        }

        this.algoritmoHash = algoritmoHash;
        this.tabela = new Node[CAPACIDADE_INICIAL];
        this.tamanho = 0;
        this.totalColisoes = 0;
        this.totalRehashes = 0;
    }

    // ---------------- Funções de dispersão ----------------

    /** Polinomial base 31 (Horner): a ordem dos caracteres importa. */
    private int hashPolinomial(String chave) {
        int h = 0;

        for (int i = 0; i < chave.length(); i++) {
            h = 31 * h + chave.charAt(i);
        }

        return Math.floorMod(h, tabela.length); // evita índice negativo
    }

    /** SDBM: h = h * 65599 + c, escrito com deslocamentos. */
    private int hashSDBM(String chave) {
        int h = 0;

        for (int i = 0; i < chave.length(); i++) {
            int c = chave.charAt(i);
            h = c + (h << 6) + (h << 16) - h;
        }

        return Math.floorMod(h, tabela.length);
    }

    private int hash(String chave) {
        if (algoritmoHash == 1) {
            return hashPolinomial(chave);
        }

        return hashSDBM(chave);
    }

    // ---------------- Sondagem linear ----------------

    /**
     * Retorna a posição da chave; se não existir, a primeira lápide ou vazio
     * encontrado; -1 se a tabela estiver cheia. Só para em null.
     */
    private int procurarPosicao(String chave) {
        int h = hash(chave);
        int primeiraRemovida = -1;

        for (int i = 0; i < tabela.length; i++) {
            int posicao = (h + i) % tabela.length;

            if (tabela[posicao] == null) {
                return (primeiraRemovida != -1) ? primeiraRemovida : posicao;
            }

            if (tabela[posicao].status == REMOVIDO) {
                if (primeiraRemovida == -1) {
                    primeiraRemovida = posicao;
                }
            } else if (tabela[posicao].chave.equals(chave)) {
                return posicao;
            }
        }

        return primeiraRemovida;
    }

    private boolean contemChaveEm(int i, String chave) {
        return i != -1
            && tabela[i] != null
            && tabela[i].status == OCUPADO
            && tabela[i].chave.equals(chave);
    }

    /** Conta as posições ocupadas entre a origem da chave e a posição final. */
    private int contarColisoes(String chave, int posicaoFinal) {
        int h = hash(chave);
        int colisoes = 0;

        for (int j = 0; j < tabela.length; j++) {
            int posicao = (h + j) % tabela.length;

            if (posicao == posicaoFinal) {
                break;
            }

            if (tabela[posicao] != null && tabela[posicao].status == OCUPADO) {
                colisoes++;
            }
        }

        return colisoes;
    }

    // ---------------- Operações ----------------

    /** Insere com frequência 1 ou incrementa se já existir. */
    public void inserir(String chave) {
        validarChave(chave);

        int i = procurarPosicao(chave);

        if (contemChaveEm(i, chave)) {
            tabela[i].frequencia++;
            return;
        }

        put(chave, 1);
    }

    /** Define o valor da chave (substitui se existir). */
    public void put(String chave, int valor) {
        validarChave(chave);

        int i = procurarPosicao(chave);

        if (contemChaveEm(i, chave)) {
            tabela[i].frequencia = valor;
            return;
        }

        if ((double) (tamanho + 1) / tabela.length > CARGA_MAXIMA || i == -1) {
            rehash();
            i = procurarPosicao(chave);
        }

        totalColisoes += contarColisoes(chave, i);

        tabela[i] = new Node(chave, valor);
        tamanho++;
    }

    /** Frequência da chave ou -1 se não existir. */
    public int get(String chave) {
        if (chave == null) {
            return -1;
        }

        int i = procurarPosicao(chave);

        return contemChaveEm(i, chave) ? tabela[i].frequencia : -1;
    }

    public boolean contains(String chave) {
        if (chave == null) {
            return false;
        }

        return contemChaveEm(procurarPosicao(chave), chave);
    }

    /** Remove marcando lápide, para não quebrar a sondagem de outras chaves. */
    public boolean remover(String chave) {
        if (chave == null) {
            return false;
        }

        int i = procurarPosicao(chave);

        if (contemChaveEm(i, chave)) {
            tabela[i].status = REMOVIDO;
            tamanho--;
            return true;
        }

        return false;
    }

    public int size() {
        return tamanho;
    }

    public int capacity() {
        return tabela.length;
    }

    public int getTotalColisoes() {
        return totalColisoes;
    }

    public int getTotalRehashes() {
        return totalRehashes;
    }

    public double getFatorCarga() {
        return (double) tamanho / tabela.length;
    }

    public String getNomeAlgoritmo() {
        return nomeAlgoritmo(algoritmoHash);
    }

    public static String nomeAlgoritmo(int algoritmo) {
        return (algoritmo == 1) ? "hashPolinomial" : "hashSDBM";
    }

    // ---------------- Estatísticas (relatório) ----------------

    /** Quantas chaves têm cada posição como origem (hash). */
    public int[] getDistribuicaoBuckets() {
        int[] contagem = new int[tabela.length];

        for (Node no : tabela) {
            if (no != null && no.status == OCUPADO) {
                contagem[hash(no.chave)]++;
            }
        }

        return contagem;
    }

    /** Soma de (chaves - 1) nos buckets com mais de uma chave. */
    public int getColisoesDeBucket() {
        int colisoes = 0;

        for (int c : getDistribuicaoBuckets()) {
            if (c > 1) {
                colisoes += c - 1;
            }
        }

        return colisoes;
    }

    /** Maior sequência de posições ocupadas (agrupamento primário). */
    public int getMaiorAgrupamento() {
        int maior = 0;
        int atual = 0;

        // Duas voltas para pegar o agrupamento que passa do fim ao início
        for (int k = 0; k < 2 * tabela.length; k++) {
            Node no = tabela[k % tabela.length];

            if (no != null && no.status == OCUPADO) {
                atual++;
                maior = Math.max(maior, Math.min(atual, tabela.length));
            } else {
                atual = 0;
            }
        }

        return maior;
    }

    // ---------------- Rehash ----------------

    /** Reinsere as chaves ativas num vetor maior; descarta lápides. */
    private void rehash() {
        Node[] velhaTabela = tabela;

        tabela = new Node[proximoPrimo(velhaTabela.length * 2)];
        tamanho = 0;
        totalColisoes = 0; // posições mudam, recontamos
        totalRehashes++;

        for (Node no : velhaTabela) {
            if (no != null && no.status == OCUPADO) {
                int posicao = hash(no.chave);

                while (tabela[posicao] != null) {
                    totalColisoes++;
                    posicao = (posicao + 1) % tabela.length;
                }

                tabela[posicao] = new Node(no.chave, no.frequencia);
                tamanho++;
            }
        }
    }

    private int proximoPrimo(int n) {
        if (n <= 2) {
            return 2;
        }

        if (n % 2 == 0) {
            n++;
        }

        while (!ehPrimo(n)) {
            n += 2;
        }

        return n;
    }

    private boolean ehPrimo(int n) {
        if (n < 2) {
            return false;
        }

        if (n == 2) {
            return true;
        }

        if (n % 2 == 0) {
            return false;
        }

        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) {
                return false;
            }
        }

        return true;
    }

    private void validarChave(String chave) {
        if (chave == null || chave.isEmpty()) {
            throw new IllegalArgumentException(
                "A chave não pode ser null ou vazia"
            );
        }
    }

    /** Imprime o vetor interno (depuração). */
    public void imprimirTabela() {
        System.out.println(
            "\nHashTable (" + getNomeAlgoritmo() + ") - Capacidade: "
            + tabela.length
            + " | Elementos: " + tamanho
            + " | Colisões: " + totalColisoes
        );

        for (int i = 0; i < tabela.length; i++) {
            System.out.print("[" + i + "] ");

            if (tabela[i] == null) {
                System.out.println("VAZIO");
            } else if (tabela[i].status == REMOVIDO) {
                System.out.println("REMOVIDO");
            } else {
                System.out.println(tabela[i].chave + " -> " + tabela[i].frequencia);
            }
        }
    }
}
