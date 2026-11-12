/*
 * ==========================================================================
 * Projeto Prático 2 - Estrutura de Dados II
 * Verificador de Similaridade de Textos com Hash e AVL
 * Prof. Dr. Jean M. Laine
 *
 * Integrantes:
 *   Nome completo: Jully Lima  RA: 10420556
 *   Nome completo: Júlia Pestana  RA: 10431863
 *   Nome completo: Calvin Santos  RA: 10723926
 * ==========================================================================
 *
 * Uso: java Main <diretorio> <limiar> <lista | topK K | busca arq1 arq2> [--hash=1|2]
 * Saída no terminal e em resultado.txt.
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class Main {

    private static final String ARQUIVO_SAIDA = "resultado.txt";
    private static final String SEPARADOR = "---------------------------------";

    // Mesmo conteúdo do terminal, gravado em resultado.txt no final
    private static final StringBuilder log = new StringBuilder();

    private static void println(String linha) {
        System.out.println(linha);
        log.append(linha).append(System.lineSeparator());
    }

    private static void println() {
        println("");
    }

    public static void main(String[] args) {
        try {
            executar(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            imprimirUso();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erro de E/S: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void executar(String[] argsOriginais) throws IOException {
        // Separa --hash=N dos demais argumentos
        List<String> args = new ArrayList<>();
        int algoritmoHash = 1;

        for (String a : argsOriginais) {
            if (a.startsWith("--hash=")) {
                algoritmoHash = lerAlgoritmoHash(a.substring("--hash=".length()));
            } else {
                args.add(a);
            }
        }

        if (args.size() < 3) {
            throw new IllegalArgumentException("argumentos insuficientes.");
        }

        Path diretorio = Path.of(args.get(0));
        double limiar = lerLimiar(args.get(1));
        String modo = args.get(2).toLowerCase(Locale.ROOT);

        if (!Files.isDirectory(diretorio)) {
            throw new IllegalArgumentException(
                "diretório não encontrado: " + diretorio
            );
        }

        switch (modo) {
            case "lista":
                modoLista(diretorio, limiar, algoritmoHash);
                break;

            case "topk":
                if (args.size() < 4) {
                    throw new IllegalArgumentException("informe K após o modo topK.");
                }
                modoTopK(diretorio, limiar, lerK(args.get(3)), algoritmoHash);
                break;

            case "busca":
                if (args.size() < 5) {
                    throw new IllegalArgumentException(
                        "informe os dois arquivos após o modo busca."
                    );
                }
                modoBusca(diretorio, args.get(3), args.get(4), algoritmoHash);
                break;

            default:
                throw new IllegalArgumentException("modo inválido: " + args.get(2));
        }

        gravarLog();
    }

    // ------------------------------------------------------------------
    // Modos de execução
    // ------------------------------------------------------------------

    private static void modoLista(Path dir, double limiar, int algoritmoHash)
            throws IOException {

        List<Documento> docs = carregarDocumentos(dir, algoritmoHash);
        AVLTree arvore = compararTodos(docs);

        imprimirCabecalho(docs.size(), arvore, algoritmoHash);

        println("Pares com similaridade >= " + formatar(limiar) + ":");
        println(SEPARADOR);
        imprimirResultados(arvore.listarAcimaDe(limiar));
        println();

        imprimirMenores(arvore);
        imprimirEstatisticasAVL(arvore);
    }

    private static void modoTopK(Path dir, double limiar, int k, int algoritmoHash)
            throws IOException {

        List<Documento> docs = carregarDocumentos(dir, algoritmoHash);
        AVLTree arvore = compararTodos(docs);

        imprimirCabecalho(docs.size(), arvore, algoritmoHash);

        println("Top " + k + " pares mais semelhantes (similaridade >= "
            + formatar(limiar) + "):");
        println(SEPARADOR);
        imprimirResultados(arvore.topK(k, limiar));
        println();

        imprimirMenores(arvore);
        imprimirEstatisticasAVL(arvore);
    }

    private static void modoBusca(Path dir, String nome1, String nome2,
            int algoritmoHash) throws IOException {

        Documento a = new Documento(resolverArquivo(dir, nome1).toString(), algoritmoHash);
        Documento b = new Documento(resolverArquivo(dir, nome2).toString(), algoritmoHash);

        double sim = ComparadorDeDocumentos.similaridadeCosseno(a, b);

        println("=== VERIFICADOR DE SIMILARIDADE DE TEXTOS ===");
        println("Comparando: " + a.getNome() + " <-> " + b.getNome());
        println("Similaridade calculada: " + formatar(sim));
        println("Métrica utilizada: " + ComparadorDeDocumentos.NOME_METRICA);
    }

    // ------------------------------------------------------------------
    // Processamento
    // ------------------------------------------------------------------

    /** Arquivos não ocultos do diretório, em ordem alfabética. */
    static List<Documento> carregarDocumentos(Path dir, int algoritmoHash)
            throws IOException {

        List<Path> arquivos = new ArrayList<>();

        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isRegularFile)
             .filter(p -> !p.getFileName().toString().startsWith("."))
             .sorted()
             .forEach(arquivos::add);
        }

        List<Documento> docs = new ArrayList<>();

        for (Path p : arquivos) {
            try {
                docs.add(new Documento(p.toString(), algoritmoHash));
            } catch (IOException e) {
                // Ex.: arquivo binário ou fora de UTF-8
                System.err.println("Aviso: ignorando " + p.getFileName()
                    + " (" + e.getClass().getSimpleName() + ")");
            }
        }

        return docs;
    }

    /** Compara os n*(n-1)/2 pares e insere cada resultado na AVL. */
    static AVLTree compararTodos(List<Documento> docs) {
        AVLTree arvore = new AVLTree();

        for (int i = 0; i < docs.size(); i++) {
            for (int j = i + 1; j < docs.size(); j++) {
                arvore.inserir(ComparadorDeDocumentos.comparar(docs.get(i), docs.get(j)));
            }
        }

        return arvore;
    }

    /** Nome relativo ao diretório ou caminho completo. */
    private static Path resolverArquivo(Path dir, String nome) {
        Path p = dir.resolve(nome);

        if (Files.isRegularFile(p)) {
            return p;
        }

        p = Path.of(nome);

        if (Files.isRegularFile(p)) {
            return p;
        }

        throw new IllegalArgumentException("arquivo não encontrado: " + nome);
    }

    // ------------------------------------------------------------------
    // Saída
    // ------------------------------------------------------------------

    private static void imprimirCabecalho(int totalDocs, AVLTree arvore,
            int algoritmoHash) {
        println("=== VERIFICADOR DE SIMILARIDADE DE TEXTOS ===");
        println("Total de documentos processados: " + totalDocs);
        println("Total de pares comparados: " + arvore.getQuantidadeResultados());
        println("Função hash utilizada: " + HashTable.nomeAlgoritmo(algoritmoHash));
        println("Métrica de similaridade: " + ComparadorDeDocumentos.NOME_METRICA);
        println();
    }

    private static void imprimirResultados(List<Resultado> resultados) {
        if (resultados.isEmpty()) {
            println("(nenhum par encontrado)");
            return;
        }

        for (Resultado r : resultados) {
            println(r.toString());
        }
    }

    private static void imprimirMenores(AVLTree arvore) {
        println("Pares com menor similaridade:");
        println(SEPARADOR);
        imprimirResultados(arvore.menorSimilaridade());
        println();
    }

    private static void imprimirEstatisticasAVL(AVLTree arvore) {
        ContagemRotacoes r = arvore.getRotacoesTotais();

        println("Estatísticas da Árvore AVL:");
        println(SEPARADOR);
        println("Nós: " + arvore.getQuantidadeNos()
            + " | Altura: " + arvore.altura());
        println("Rotações simples: " + r.getTotalSimples()
            + " (esquerda: " + r.getSimplesEsquerda()
            + ", direita: " + r.getSimplesDireita() + ")");
        println("Rotações duplas: " + r.getTotalDuplas()
            + " (esquerda-direita: " + r.getDuplaEsquerdaDireita()
            + ", direita-esquerda: " + r.getDuplaDireitaEsquerda() + ")");
    }

    private static void gravarLog() throws IOException {
        Files.writeString(Path.of(ARQUIVO_SAIDA), log.toString(), StandardCharsets.UTF_8);
    }

    private static String formatar(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    // ------------------------------------------------------------------
    // Validação de argumentos
    // ------------------------------------------------------------------

    private static double lerLimiar(String texto) {
        double limiar;

        try {
            // Aceita "0,75" e "0.75"
            limiar = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("limiar inválido: " + texto);
        }

        if (limiar < 0 || limiar > 1) {
            throw new IllegalArgumentException("o limiar deve estar entre 0 e 1.");
        }

        return limiar;
    }

    private static int lerK(String texto) {
        try {
            int k = Integer.parseInt(texto);

            if (k <= 0) {
                throw new IllegalArgumentException("K deve ser maior que zero.");
            }

            return k;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("K inválido: " + texto);
        }
    }

    private static int lerAlgoritmoHash(String texto) {
        if (texto.equals("1") || texto.equals("2")) {
            return Integer.parseInt(texto);
        }

        throw new IllegalArgumentException("--hash deve ser 1 ou 2.");
    }

    private static void imprimirUso() {
        System.err.println();
        System.err.println("Uso: java Main <diretorio_documentos> <limiar> <modo> [args]");
        System.err.println("  lista");
        System.err.println("  topK <K>");
        System.err.println("  busca <arquivo1> <arquivo2>");
        System.err.println("Opcional: --hash=1 (polinomial, padrão) | --hash=2 (SDBM)");
    }
}
