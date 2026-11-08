package estruturas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Documento {

    private String nome;
    private String caminho;
    private HashTable tabelaPalavras;
    private List<String> palavrasUnicas;

    // Palavras que serão desconsideradas
    private static final Set<String> STOP_WORDS = new HashSet<>(
        Arrays.asList(
            "a", "o", "as", "os", "um", "uma", "uns", "umas",
            "de", "do", "da", "dos", "das",
            "em", "no", "na", "nos", "nas",
            "para", "por", "com", "sem",
            "e", "ou", "mas",
            "é", "ser", "estar", "foi", "são",
            "ao", "aos", "à", "às",
            "que", "se", "como", "mais", "menos",
            "sua", "seu", "suas", "seus",
            "meu", "minha", "este", "esta",
            "esse", "essa", "isso", "isto",
            "ele", "ela", "eles", "elas",
            "eu", "tu", "nós", "vós",
            "não", "sim", "já", "também"
        )
    );

    // Construtor
    public Documento(String caminho, int algoritmoHash)
            throws IOException {

        this.caminho = caminho;
        this.nome = Path.of(caminho).getFileName().toString();

        this.tabelaPalavras = new HashTable(algoritmoHash);
        this.palavrasUnicas = new ArrayList<>();

        carregarDocumento();
    }

    // Lê o arquivo
    private void carregarDocumento() throws IOException {

        String texto = Files.readString(
            Path.of(caminho),
            StandardCharsets.UTF_8
        );

        processarTexto(texto);
    }

    // Normaliza e processa o conteúdo
    private void processarTexto(String texto) {

        texto = texto.toLowerCase(Locale.ROOT);

        // Remove pontuação e caracteres especiais,
        // preservando letras, números e espaços.
        texto = texto.replaceAll("[^\\p{L}\\p{N}\\s]", " ");

        // Separa o texto em palavras
        String[] palavras = texto.split("\\s+");

        for (String palavra : palavras) {

            if (palavra.isEmpty()) {
                continue;
            }

            if (STOP_WORDS.contains(palavra)) {
                continue;
            }

            // Registra a palavra se for sua primeira ocorrência
            if (tabelaPalavras.get(palavra) == -1) {
                palavrasUnicas.add(palavra);
            }

            // Insere ou incrementa a frequência
            tabelaPalavras.inserir(palavra);
        }
    }

    // Getters
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

    public int getFrequencia(String palavra) {
        return tabelaPalavras.get(palavra);
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
}