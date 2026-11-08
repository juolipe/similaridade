package estruturas;

public class HashTable {

    private static final int CAPACIDADE_INICIAL = 11;
    private static final double CARGA_MAXIMA = 0.75;

    private Node[] tabela;
    private int tamanho;
    private int algoritmoHash;
    private int totalColisoes;

    private static class Node {
        String chave;
        int frequencia;
        int status;

        Node(String chave) {
            this.chave = chave;
            this.frequencia = 1;
            this.status = 1;
        }
    }

    // Construtores
    public HashTable() {
        this(1);
    }

    public HashTable(int algoritmoHash) {
        if (algoritmoHash != 1 && algoritmoHash != 2) {
            throw new IllegalArgumentException(
                "Algoritmo deve ser 1 ou 2"
            );
        }

        this.algoritmoHash = algoritmoHash;
        this.tabela = new Node[CAPACIDADE_INICIAL];
        this.tamanho = 0;
        this.totalColisoes = 0;
    }

    // Hash polinomial
    private int hashPolinomial(String chave) {
        int h = 0;

        for (int i = 0; i < chave.length(); i++) {
            h = 31 * h + chave.charAt(i);
        }

        return Math.floorMod(h, tabela.length);
    }

    // Hash SDBM
    private int hashSDBM(String chave) {
        int h = 0;

        for (int i = 0; i < chave.length(); i++) {
            int c = chave.charAt(i);
            h = c + (h << 6) + (h << 16) - h;
        }

        return Math.floorMod(h, tabela.length);
    }

    // Seleciona a função hash
    private int hash(String chave) {
        if (algoritmoHash == 1) {
            return hashPolinomial(chave);
        }

        return hashSDBM(chave);
    }

    // Procura uma chave ou uma posição disponível
    private int procurarPosicao(String chave) {
        int h = hash(chave);
        int primeiraRemovida = -1;

        for (int i = 0; i < tabela.length; i++) {
            int posicao = (h + i) % tabela.length;

            if (tabela[posicao] == null) {
                if (primeiraRemovida != -1) {
                    return primeiraRemovida;
                }

                return posicao;
            }

            if (tabela[posicao].status == -1) {
                if (primeiraRemovida == -1) {
                    primeiraRemovida = posicao;
                }
            } else if (tabela[posicao].chave.equals(chave)) {
                return posicao;
            }
        }

        return primeiraRemovida;
    }

    // Inserção
    public void inserir(String chave) {
        if (chave == null || chave.isEmpty()) {
            throw new IllegalArgumentException(
                "A chave não pode ser null ou vazia"
            );
        }

        int i = procurarPosicao(chave);

        // Se a chave já existe, incrementa a frequência
        if (i != -1 && tabela[i] != null
                && tabela[i].status == 1
                && tabela[i].chave.equals(chave)) {
            tabela[i].frequencia++;
            return;
        }

        // Verifica o fator de carga
        if ((double) (tamanho + 1) / tabela.length > CARGA_MAXIMA) {
            rehash();
            i = procurarPosicao(chave);
        }

        // Se não houver posição disponível
        if (i == -1) {
            rehash();
            i = procurarPosicao(chave);
        }

        // Conta colisões durante a sondagem
        int h = hash(chave);

        for (int j = 0; j < tabela.length; j++) {
            int posicao = (h + j) % tabela.length;

            if (posicao == i) {
                break;
            }

            if (tabela[posicao] != null
                    && tabela[posicao].status == 1) {
                totalColisoes++;
            }
        }

        tabela[i] = new Node(chave);
        tamanho++;
    }

    // Busca
    public int get(String chave) {
        if (chave == null) {
            return -1;
        }

        int i = procurarPosicao(chave);

        if (i != -1 && tabela[i] != null
                && tabela[i].status == 1
                && tabela[i].chave.equals(chave)) {
            return tabela[i].frequencia;
        }

        return -1;
    }

    // Verifica existência
    public boolean contains(String chave) {
        if (chave == null) {
            return false;
        }

        int i = procurarPosicao(chave);

        return i != -1
            && tabela[i] != null
            && tabela[i].status == 1
            && tabela[i].chave.equals(chave);
    }

    // Remoção
    public boolean remover(String chave) {
        if (chave == null) {
            return false;
        }

        int i = procurarPosicao(chave);

        if (i != -1 && tabela[i] != null
                && tabela[i].status == 1
                && tabela[i].chave.equals(chave)) {

            tabela[i].status = -1;
            tamanho--;

            return true;
        }

        return false;
    }

    // Quantidade de elementos
    public int size() {
        return tamanho;
    }

    // Capacidade da tabela
    public int capacity() {
        return tabela.length;
    }

    // Quantidade de colisões
    public int getTotalColisoes() {
        return totalColisoes;
    }

    // Rehash
    private void rehash() {
        Node[] velhaTabela = tabela;

        int novaCapacidade = proximoPrimo(velhaTabela.length * 2);

        tabela = new Node[novaCapacidade];
        tamanho = 0;

        for (Node no : velhaTabela) {
            if (no != null && no.status == 1) {

                int posicao = hash(no.chave);

                while (tabela[posicao] != null) {
                    posicao = (posicao + 1) % tabela.length;
                }

                tabela[posicao] = new Node(no.chave);
                tabela[posicao].frequencia = no.frequencia;

                tamanho++;
            }
        }
    }

    // Próximo número primo
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

    // Verifica se é primo
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

    // Impressão da tabela
    public void imprimirTabela() {
        System.out.println(
            "\nHashTable - Capacidade: " + tabela.length
            + " | Elementos: " + tamanho
            + " | Colisões: " + totalColisoes
        );

        for (int i = 0; i < tabela.length; i++) {
            System.out.print("[" + i + "] ");

            if (tabela[i] == null) {
                System.out.println("VAZIO");
            } else if (tabela[i].status == -1) {
                System.out.println("REMOVIDO");
            } else {
                System.out.println(
                    tabela[i].chave + " -> "
                    + tabela[i].frequencia
                );
            }
        }
    }
}
