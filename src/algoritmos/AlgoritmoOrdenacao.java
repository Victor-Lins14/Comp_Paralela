package algoritmos;

/**
 * Interface comum para todos os algoritmos analisados no estudo comparativo
 * em ambientes seriais e paralelos.
 */
public interface AlgoritmoOrdenacao {

    /**
     * Retorna o nome de exibição do algoritmo.
     */
    String getNome();

    /**
     * Executa o algoritmo de forma estritamente sequencial (1 thread).
     *
     * @param dados vetor de inteiros a ser processado in-place
     */
    void ordenarSerial(int[] dados);

    /**
     * Executa o algoritmo de forma paralela utilizando o número especificado de threads.
     *
     * @param dados      vetor de inteiros a ser processado in-place
     * @param numThreads quantidade de threads/núcleos no pool paralelo
     */
    void ordenarParalelo(int[] dados, int numThreads);

    /**
     * Indica se o algoritmo possui complexidade assintótica quadrática O(n²) no caso médio/pior.
     */
    default boolean isQuadratico() {
        return false;
    }
}
