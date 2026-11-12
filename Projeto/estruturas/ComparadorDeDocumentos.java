/**
 * Similaridade do Cosseno: soma(fA * fB) / (|A| * |B|).
 * Usa as frequências e não depende do tamanho do texto. Resultado em [0, 1].
 */
public class ComparadorDeDocumentos {

    public static final String NOME_METRICA = "Cosseno";

    private ComparadorDeDocumentos() {
    }

    public static double similaridadeCosseno(Documento a, Documento b) {
        double normaA = a.getNorma();
        double normaB = b.getNorma();

        if (normaA == 0 || normaB == 0) {
            return 0.0;
        }

        // Percorre o vocabulário menor e consulta o maior
        Documento menor = a;
        Documento maior = b;

        if (a.getQuantidadePalavrasUnicas() > b.getQuantidadePalavrasUnicas()) {
            menor = b;
            maior = a;
        }

        HashTable tabelaMenor = menor.getTabelaPalavras();
        HashTable tabelaMaior = maior.getTabelaPalavras();

        double produtoEscalar = 0;

        for (String palavra : menor.getPalavrasUnicas()) {
            int fMaior = tabelaMaior.get(palavra);

            if (fMaior > 0) {
                produtoEscalar += (double) tabelaMenor.get(palavra) * fMaior;
            }
        }

        double cosseno = produtoEscalar / (normaA * normaB);

        return Math.min(1.0, cosseno); // corrige arredondamento (1.0000000002)
    }

    public static Resultado comparar(Documento a, Documento b) {
        return new Resultado(a.getNome(), b.getNome(), similaridadeCosseno(a, b));
    }
}
