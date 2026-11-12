import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Arquivo de texto normalizado (minúsculas, sem pontuação, tokenizado,
 * sem stop words), com as frequências numa HashTable própria.
 */
public class Documento {

    private final String nome;
    private final String caminho;
    private final HashTable tabelaPalavras;
    private final List<String> palavrasUnicas; // para percorrer o vocabulário
    private double norma = -1;                 // cache para o Cosseno

    private static final Set<String> STOP_WORDS = new HashSet<>(
        Arrays.asList(
            "a", "o", "as", "os", "um", "uma", "uns", "umas",
            "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas",
            "para", "por", "com", "sem", "pelo", "pela", "pelos", "pelas",
            "e", "ou", "mas",
            "é", "ser", "estar", "foi", "são", "era", "há",
            "ao", "aos", "à", "às",
            "que", "se", "como", "mais", "menos", "muito",
            "sua", "seu", "suas", "seus",
            "meu", "minha", "este", "esta", "estes", "estas",
            "esse", "essa", "esses", "essas", "isso", "isto",
            "aquele", "aquela", "aquilo",
            "ele", "ela", "eles", "elas",
            "eu", "tu", "nós", "vós", "lhe", "lhes",
            "não", "sim", "já", "também", "quando", "onde", "entre"
        )
    );

    public Documento(String caminho, int algoritmoHash) throws IOException {
        this(
            Path.of(caminho).getFileName().toString(),
            caminho,
            Files.readString(Path.of(caminho), StandardCharsets.UTF_8),
            algoritmoHash
        );
    }

    private Documento(String nome, String caminho, String texto, int algoritmoHash) {
        this.nome = nome;
        this.caminho = caminho;
        this.tabelaPalavras = new HashTable(algoritmoHash);
        this.palavrasUnicas = new ArrayList<>();

        processarTexto(texto);
    }

    /** Documento a partir de texto em memória (experimentos). */
    public static Documento deTexto(String nome, String texto, int algoritmoHash) {
        return new Documento(nome, null, texto, algoritmoHash);
    }

    private void processarTexto(String texto) {
        texto = texto.toLowerCase(Locale.ROOT);

        // Mantém só letras (com acento), dígitos e espaços
        texto = texto.replaceAll("[^\\p{L}\\p{N}\\s]", " ");

        for (String palavra : texto.split("\\s+")) {
            if (palavra.isEmpty() || STOP_WORDS.contains(palavra)) {
                continue;
            }

            if (!tabelaPalavras.contains(palavra)) {
                palavrasUnicas.add(palavra);
            }

            tabelaPalavras.inserir(palavra);
        }
    }

    public String getNome() {
        return nome;
    }

    public String getCaminho() {
        return caminho;
    }

    public HashTable getTabelaPalavras() {
        return tabelaPalavras;
    }

    public List<String> getPalavrasUnicas() {
        return new ArrayList<>(palavrasUnicas);
    }

    /** 0 se a palavra não aparece. */
    public int getFrequencia(String palavra) {
        int f = tabelaPalavras.get(palavra);
        return (f == -1) ? 0 : f;
    }

    public int getQuantidadePalavrasUnicas() {
        return palavrasUnicas.size();
    }

    public int getQuantidadePalavras() {
        int total = 0;

        for (String palavra : palavrasUnicas) {
            total += tabelaPalavras.get(palavra);
        }

        return total;
    }

    /** sqrt(soma das frequências²). */
    public double getNorma() {
        if (norma < 0) {
            double soma = 0;

            for (String palavra : palavrasUnicas) {
                int f = tabelaPalavras.get(palavra);
                soma += (double) f * f;
            }

            norma = Math.sqrt(soma);
        }

        return norma;
    }
}
