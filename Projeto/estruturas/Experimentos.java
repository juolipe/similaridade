import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Dados para a Análise Experimental: hash, rotações, AVL x lista e tempo
 * por volume. Uso: java Experimentos [diretorio]. CSVs em ./experimentos/.
 */
public class Experimentos {

    private static final Path PASTA_SAIDA = Path.of("experimentos");
    private static final int REPETICOES = 5;

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "./documentos");
        Files.createDirectories(PASTA_SAIDA);

        List<String> vocabulario = vocabularioDosDocumentos(dir);
        System.out.println("Vocabulário distinto dos documentos: " + vocabulario.size());

        experimentoHash(vocabulario);
        experimentoRotacoes();
        experimentoBusca();
        experimentoVolume(vocabulario);

        System.out.println("\nCSVs gravados em " + PASTA_SAIDA.toAbsolutePath());
    }

    // ------------------------------------------------------------------
    // 1. Funções hash
    // ------------------------------------------------------------------

    private static void experimentoHash(List<String> vocabulario) throws IOException {
        titulo("1. Comparação das funções hash");

        // Vocabulário real + sintético
        List<String> chaves = new ArrayList<>(vocabulario);
        Random rnd = new Random(42);
        while (chaves.size() < 20000) {
            chaves.add(palavraAleatoria(rnd));
        }

        int[][] distribuicoes = new int[2][];

        System.out.printf("%-16s %8s %10s %12s %12s %10s %10s %12s%n",
            "Função", "Chaves", "Capacidade", "Col.sondagem", "Col.bucket",
            "Agrup.max", "Vazios", "Tempo(ms)");

        StringBuilder resumo = new StringBuilder(
            "funcao,chaves,capacidade,colisoes_sondagem,colisoes_bucket,"
            + "maior_agrupamento,buckets_vazios,max_chaves_bucket,tempo_ms\n");

        for (int alg = 1; alg <= 2; alg++) {
            HashTable t = null;
            long tempoTotal = 0;

            for (int rep = 0; rep < REPETICOES; rep++) {
                t = new HashTable(alg);
                long ini = System.nanoTime();
                for (String c : chaves) {
                    t.inserir(c);
                }
                tempoTotal += System.nanoTime() - ini;
            }

            int[] dist = t.getDistribuicaoBuckets();
            distribuicoes[alg - 1] = dist;

            int vazios = 0;
            int maxBucket = 0;
            for (int c : dist) {
                if (c == 0) {
                    vazios++;
                }
                maxBucket = Math.max(maxBucket, c);
            }

            double ms = tempoTotal / (REPETICOES * 1e6);

            System.out.printf(Locale.US, "%-16s %8d %10d %12d %12d %10d %10d %12.3f%n",
                t.getNomeAlgoritmo(), t.size(), t.capacity(), t.getTotalColisoes(),
                t.getColisoesDeBucket(), t.getMaiorAgrupamento(), vazios, ms);

            resumo.append(String.format(Locale.US, "%s,%d,%d,%d,%d,%d,%d,%d,%.3f%n",
                t.getNomeAlgoritmo(), t.size(), t.capacity(), t.getTotalColisoes(),
                t.getColisoesDeBucket(), t.getMaiorAgrupamento(), vazios, maxBucket, ms));
        }

        // Histograma: buckets com 0, 1, 2... chaves
        int maxC = 0;
        for (int[] d : distribuicoes) {
            for (int c : d) {
                maxC = Math.max(maxC, c);
            }
        }

        System.out.println("\nHistograma (chaves por bucket -> quantidade de buckets):");
        System.out.printf("%-8s %16s %16s%n", "chaves", "hashPolinomial", "hashSDBM");

        StringBuilder hist = new StringBuilder("chaves_no_bucket,hashPolinomial,hashSDBM\n");

        for (int k = 0; k <= maxC; k++) {
            int[] qtd = new int[2];
            for (int alg = 0; alg < 2; alg++) {
                for (int c : distribuicoes[alg]) {
                    if (c == k) {
                        qtd[alg]++;
                    }
                }
            }
            System.out.printf("%-8d %16d %16d%n", k, qtd[0], qtd[1]);
            hist.append(k).append(',').append(qtd[0]).append(',').append(qtd[1]).append('\n');
        }

        gravar("hash_resumo.csv", resumo.toString());
        gravar("hash_histograma.csv", hist.toString());

        // Chaves por bucket
        StringBuilder porBucket = new StringBuilder("bucket,hashPolinomial,hashSDBM\n");
        for (int i = 0; i < distribuicoes[0].length; i++) {
            porBucket.append(i).append(',')
                .append(distribuicoes[0][i]).append(',')
                .append(distribuicoes[1][i]).append('\n');
        }
        gravar("hash_por_bucket.csv", porBucket.toString());
    }

    // ------------------------------------------------------------------
    // 2. Rotações x ordem de inserção
    // ------------------------------------------------------------------

    private static void experimentoRotacoes() throws IOException {
        titulo("2. Rotações da AVL x ordem de inserção (chaves distintas)");

        int[] tamanhos = {100, 1000, 10000};

        System.out.printf("%-12s %7s %8s %8s %8s %8s %8s %7s%n",
            "Ordem", "N", "SimpEsq", "SimpDir", "DupED", "DupDE", "Total", "Altura");

        StringBuilder csv = new StringBuilder(
            "ordem,n,simples_esquerda,simples_direita,dupla_esq_dir,dupla_dir_esq,total,altura\n");

        for (int n : tamanhos) {
            for (String ordem : new String[] {"crescente", "decrescente", "aleatoria"}) {
                double[] chaves = new double[n];
                for (int i = 0; i < n; i++) {
                    chaves[i] = (double) i / n;
                }

                if (ordem.equals("decrescente")) {
                    for (int i = 0; i < n / 2; i++) {
                        double tmp = chaves[i];
                        chaves[i] = chaves[n - 1 - i];
                        chaves[n - 1 - i] = tmp;
                    }
                } else if (ordem.equals("aleatoria")) {
                    embaralhar(chaves, new Random(7));
                }

                AVLTree arv = new AVLTree();
                for (double c : chaves) {
                    arv.inserir(new Resultado("a", "b", c));
                }

                ContagemRotacoes r = arv.getRotacoesTotais();
                System.out.printf("%-12s %7d %8d %8d %8d %8d %8d %7d%n",
                    ordem, n, r.getSimplesEsquerda(), r.getSimplesDireita(),
                    r.getDuplaEsquerdaDireita(), r.getDuplaDireitaEsquerda(),
                    r.getTotal(), arv.altura());

                csv.append(String.format("%s,%d,%d,%d,%d,%d,%d,%d%n",
                    ordem, n, r.getSimplesEsquerda(), r.getSimplesDireita(),
                    r.getDuplaEsquerdaDireita(), r.getDuplaDireitaEsquerda(),
                    r.getTotal(), arv.altura()));
            }
        }

        gravar("avl_rotacoes.csv", csv.toString());
    }

    // ------------------------------------------------------------------
    // 3. AVL x lista ordenada
    // ------------------------------------------------------------------

    private static void experimentoBusca() throws IOException {
        titulo("3. Busca: AVL x lista ordenada (busca sequencial)");

        // Lista é O(n²) no total: para em 50 mil
        int[] tamanhos = {1000, 5000, 10000, 25000, 50000};
        int consultas = 1000;

        System.out.printf("%8s %14s %16s %14s %16s %10s%n",
            "N", "AVL ins(ms)", "Lista ins(ms)", "AVL busca(ms)", "Lista busca(ms)", "Altura");

        StringBuilder csv = new StringBuilder(
            "n,avl_insercao_ms,lista_insercao_ms,avl_busca_ms,lista_busca_ms,altura\n");

        for (int n : tamanhos) {
            Random rnd = new Random(n);
            double[] chaves = new double[n];
            for (int i = 0; i < n; i++) {
                chaves[i] = rnd.nextDouble();
            }

            double[] alvos = new double[consultas];
            for (int i = 0; i < consultas; i++) {
                alvos[i] = chaves[rnd.nextInt(n)];
            }

            long insAvl = 0;
            long insLista = 0;
            long buscaAvl = 0;
            long buscaLista = 0;
            int altura = 0;
            int encontrados = 0; // impede o JIT de descartar as buscas

            for (int rep = 0; rep < REPETICOES; rep++) {
                // --- AVL ---
                long ini = System.nanoTime();
                AVLTree arv = new AVLTree();
                for (double c : chaves) {
                    arv.inserir(new Resultado("a", "b", c));
                }
                insAvl += System.nanoTime() - ini;

                ini = System.nanoTime();
                for (double alvo : alvos) {
                    encontrados += arv.buscar(alvo).size();
                }
                buscaAvl += System.nanoTime() - ini;
                altura = arv.altura();

                // --- Lista ordenada ---
                ini = System.nanoTime();
                ListaOrdenada lista = new ListaOrdenada();
                for (double c : chaves) {
                    lista.inserir(new Resultado("a", "b", c));
                }
                insLista += System.nanoTime() - ini;

                ini = System.nanoTime();
                for (double alvo : alvos) {
                    encontrados += lista.buscar(alvo).size();
                }
                buscaLista += System.nanoTime() - ini;
            }

            double div = REPETICOES * 1e6;
            System.out.printf(Locale.US, "%8d %14.3f %16.3f %14.3f %16.3f %10d%n",
                n, insAvl / div, insLista / div, buscaAvl / div, buscaLista / div, altura);

            csv.append(String.format(Locale.US, "%d,%.3f,%.3f,%.3f,%.3f,%d%n",
                n, insAvl / div, insLista / div, buscaAvl / div, buscaLista / div, altura));

            if (encontrados == 0) {
                System.out.println("(nenhuma chave encontrada?)");
            }
        }

        gravar("avl_vs_lista.csv", csv.toString());
    }

    /** Lista em ordem crescente; inserção e busca sequenciais O(n). */
    private static class ListaOrdenada {
        private final List<Resultado> itens = new ArrayList<>();

        void inserir(Resultado r) {
            int i = 0;
            while (i < itens.size() && itens.get(i).getSimilaridade() < r.getSimilaridade()) {
                i++;
            }
            itens.add(i, r);
        }

        List<Resultado> buscar(double chave) {
            List<Resultado> achados = new ArrayList<>();
            for (Resultado r : itens) {
                if (r.getSimilaridade() == chave) {
                    achados.add(r);
                } else if (r.getSimilaridade() > chave) {
                    break;
                }
            }
            return achados;
        }
    }

    // ------------------------------------------------------------------
    // 4. Tempo total x volume de documentos
    // ------------------------------------------------------------------

    private static void experimentoVolume(List<String> vocabulario) throws IOException {
        titulo("4. Tempo total (normalização + comparações + AVL) x nº de documentos");

        if (vocabulario.isEmpty()) {
            vocabulario = new ArrayList<>();
            Random rnd = new Random(1);
            for (int i = 0; i < 2000; i++) {
                vocabulario.add(palavraAleatoria(rnd));
            }
        }

        int[] quantidades = {10, 50, 100, 200, 400};
        int palavrasPorDoc = 300;

        System.out.printf("%8s %10s %14s %14s %14s %14s%n",
            "Docs", "Pares", "Leitura(ms)", "Compar.(ms)", "Total(ms)", "Rotações");

        StringBuilder csv = new StringBuilder(
            "documentos,pares,leitura_ms,comparacao_ms,total_ms,rotacoes\n");

        for (int qtd : quantidades) {
            // Gera os textos fora da medição
            Random rnd = new Random(qtd);
            String[] textos = new String[qtd];
            for (int d = 0; d < qtd; d++) {
                StringBuilder sb = new StringBuilder();
                for (int p = 0; p < palavrasPorDoc; p++) {
                    sb.append(vocabulario.get(rnd.nextInt(vocabulario.size()))).append(' ');
                }
                textos[d] = sb.toString();
            }

            long leitura = 0;
            long comparacao = 0;
            int rotacoes = 0;
            int pares = 0;

            for (int rep = 0; rep < REPETICOES; rep++) {
                long ini = System.nanoTime();
                List<Documento> docs = new ArrayList<>();
                for (int d = 0; d < qtd; d++) {
                    docs.add(Documento.deTexto("doc" + d + ".txt", textos[d], 1));
                }
                leitura += System.nanoTime() - ini;

                ini = System.nanoTime();
                AVLTree arv = Main.compararTodos(docs);
                comparacao += System.nanoTime() - ini;

                rotacoes = arv.getRotacoesTotais().getTotal();
                pares = arv.getQuantidadeResultados();
            }

            double div = REPETICOES * 1e6;
            System.out.printf(Locale.US, "%8d %10d %14.3f %14.3f %14.3f %14d%n",
                qtd, pares, leitura / div, comparacao / div,
                (leitura + comparacao) / div, rotacoes);

            csv.append(String.format(Locale.US, "%d,%d,%.3f,%.3f,%.3f,%d%n",
                qtd, pares, leitura / div, comparacao / div,
                (leitura + comparacao) / div, rotacoes));
        }

        gravar("tempo_por_volume.csv", csv.toString());
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    private static List<String> vocabularioDosDocumentos(Path dir) throws IOException {
        List<String> vocab = new ArrayList<>();

        if (!Files.isDirectory(dir)) {
            System.out.println("Aviso: diretório " + dir + " não encontrado; usando só palavras sintéticas.");
            return vocab;
        }

        // HashTable como conjunto
        HashTable vistas = new HashTable(1);

        for (Documento d : Main.carregarDocumentos(dir, 1)) {
            for (String p : d.getPalavrasUnicas()) {
                if (!vistas.contains(p)) {
                    vistas.inserir(p);
                    vocab.add(p);
                }
            }
        }

        return vocab;
    }

    private static String palavraAleatoria(Random rnd) {
        int tam = 3 + rnd.nextInt(8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tam; i++) {
            sb.append((char) ('a' + rnd.nextInt(26)));
        }
        return sb.toString();
    }

    private static void embaralhar(double[] v, Random rnd) {
        for (int i = v.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            double tmp = v[i];
            v[i] = v[j];
            v[j] = tmp;
        }
    }

    private static void gravar(String nome, String conteudo) throws IOException {
        Files.writeString(PASTA_SAIDA.resolve(nome), conteudo, StandardCharsets.UTF_8);
    }

    private static void titulo(String t) {
        System.out.println();
        System.out.println("=== " + t + " ===");
    }
}
