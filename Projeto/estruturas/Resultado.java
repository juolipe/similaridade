import java.util.Locale;

/** Par de documentos e sua similaridade. */
public class Resultado {

    private final String documento1;
    private final String documento2;
    private final double similaridade;

    public Resultado(String documento1, String documento2, double similaridade) {
        this.documento1 = documento1;
        this.documento2 = documento2;
        this.similaridade = similaridade;
    }

    public String getDocumento1() {
        return documento1;
    }

    public String getDocumento2() {
        return documento2;
    }

    public double getSimilaridade() {
        return similaridade;
    }

    /** "doc1.txt <-> doc2.txt = 0.82" */
    @Override
    public String toString() {
        return documento1 + " <-> " + documento2 + " = "
            + String.format(Locale.US, "%.2f", similaridade);
    }
}
