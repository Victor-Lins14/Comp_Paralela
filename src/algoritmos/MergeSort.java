package algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Implementação Serial e Paralela do algoritmo Merge Sort.
 * Utiliza um único vetor auxiliar pré-alocado para evitar contenção de alocação
 * na memória Heap (Garbage Collector) durante a execução paralela em múltiplos núcleos.
 */
public class MergeSort implements AlgoritmoOrdenacao {

    private static final int LIMIAR_MINIMO_SEQUENCIAL = 2048;

    @Override
    public String getNome() {
        return "Merge Sort";
    }

    @Override
    public void ordenarSerial(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        int[] aux = new int[dados.length];
        mergeSortSequencial(dados, aux, 0, dados.length - 1);
    }

    @Override
    public void ordenarParalelo(int[] dados, int numThreads) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        if (numThreads <= 1) {
            ordenarSerial(dados);
            return;
        }
        int[] aux = new int[dados.length];
        int limiar = Math.max(LIMIAR_MINIMO_SEQUENCIAL, dados.length / (numThreads * 4));
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            pool.invoke(new TarefaMergeSort(dados, aux, 0, dados.length - 1, limiar));
        } finally {
            pool.shutdown();
        }
    }

    private static void mergeSortSequencial(int[] dados, int[] aux, int inicio, int fim) {
        if (inicio >= fim) {
            return;
        }
        int meio = inicio + ((fim - inicio) >>> 1);
        mergeSortSequencial(dados, aux, inicio, meio);
        mergeSortSequencial(dados, aux, meio + 1, fim);
        intercalar(dados, aux, inicio, meio, fim);
    }

    /**
     * Intercala as duas metades ordenadas [inicio..meio] e [meio+1..fim]
     * utilizando a região disjunta correspondente no vetor auxiliar.
     */
    public static void intercalar(int[] dados, int[] aux, int inicio, int meio, int fim) {
        // Otimização: se já estiver ordenado na fronteira, evita cópia
        if (dados[meio] <= dados[meio + 1]) {
            return;
        }

        System.arraycopy(dados, inicio, aux, inicio, fim - inicio + 1);

        int i = inicio;
        int j = meio + 1;
        int k = inicio;

        while (i <= meio && j <= fim) {
            if (aux[i] <= aux[j]) {
                dados[k++] = aux[i++];
            } else {
                dados[k++] = aux[j++];
            }
        }

        while (i <= meio) {
            dados[k++] = aux[i++];
        }
        // Os elementos restantes da metade direita já estão em suas posições finais em dados[]
    }

    private static class TarefaMergeSort extends RecursiveAction {
        private final int[] dados;
        private final int[] aux;
        private final int inicio;
        private final int fim;
        private final int limiar;

        TarefaMergeSort(int[] dados, int[] aux, int inicio, int fim, int limiar) {
            this.dados = dados;
            this.aux = aux;
            this.inicio = inicio;
            this.fim = fim;
            this.limiar = limiar;
        }

        @Override
        protected void compute() {
            if (fim - inicio <= limiar) {
                mergeSortSequencial(dados, aux, inicio, fim);
            } else {
                int meio = inicio + ((fim - inicio) >>> 1);
                invokeAll(
                        new TarefaMergeSort(dados, aux, inicio, meio, limiar),
                        new TarefaMergeSort(dados, aux, meio + 1, fim, limiar)
                );
                intercalar(dados, aux, inicio, meio, fim);
            }
        }
    }
}
