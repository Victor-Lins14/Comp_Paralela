package algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Implementação Serial e Paralela do algoritmo Quick Sort.
 * Utiliza escolha de pivô por Mediana de Três e particionamento bidirecional de Hoare
 * para evitar degradação O(n²) em vetores ordenados, inversamente ordenados ou com
 * chaves altamente repetidas.
 * A versão paralela utiliza o framework Fork/Join (RecursiveAction) com limiar (threshold)
 * adaptativo baseado no número de threads configuradas.
 */
public class QuickSort implements AlgoritmoOrdenacao {

    private static final int LIMIAR_MINIMO_SEQUENCIAL = 2048;

    @Override
    public String getNome() {
        return "Quick Sort";
    }

    @Override
    public void ordenarSerial(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        quickSortSequencial(dados, 0, dados.length - 1);
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
        int limiar = Math.max(LIMIAR_MINIMO_SEQUENCIAL, dados.length / (numThreads * 4));
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            pool.invoke(new TarefaQuickSort(dados, 0, dados.length - 1, limiar));
        } finally {
            pool.shutdown();
        }
    }

    private static void quickSortSequencial(int[] dados, int inicio, int fim) {
        while (inicio < fim) {
            if (fim - inicio < 24) {
                insertionSortCurto(dados, inicio, fim);
                return;
            }
            int indiceParticao = particionarHoare(dados, inicio, fim);
            // Cauda recursiva na maior partição para garantir pilha O(log n)
            if (indiceParticao - inicio < fim - (indiceParticao + 1)) {
                quickSortSequencial(dados, inicio, indiceParticao);
                inicio = indiceParticao + 1;
            } else {
                quickSortSequencial(dados, indiceParticao + 1, fim);
                fim = indiceParticao;
            }
        }
    }

    private static void insertionSortCurto(int[] dados, int inicio, int fim) {
        for (int i = inicio + 1; i <= fim; i++) {
            int chave = dados[i];
            int j = i - 1;
            while (j >= inicio && dados[j] > chave) {
                dados[j + 1] = dados[j];
                j--;
            }
            dados[j + 1] = chave;
        }
    }

    /**
     * Particionamento de Hoare com pivô por mediana de três (início, meio, fim).
     * Divide de forma equilibrada mesmo na presença de muitos elementos duplicados.
     */
    private static int particionarHoare(int[] dados, int inicio, int fim) {
        int meio = inicio + ((fim - inicio) >>> 1);

        // Ordena inicio, meio e fim para obter a mediana no meio
        if (dados[meio] < dados[inicio]) {
            trocar(dados, inicio, meio);
        }
        if (dados[fim] < dados[inicio]) {
            trocar(dados, inicio, fim);
        }
        if (dados[fim] < dados[meio]) {
            trocar(dados, meio, fim);
        }

        int pivo = dados[meio];
        int i = inicio - 1;
        int j = fim + 1;

        while (true) {
            do {
                i++;
            } while (dados[i] < pivo);

            do {
                j--;
            } while (dados[j] > pivo);

            if (i >= j) {
                return j;
            }
            trocar(dados, i, j);
        }
    }

    private static void trocar(int[] dados, int i, int j) {
        int temp = dados[i];
        dados[i] = dados[j];
        dados[j] = temp;
    }

    private static class TarefaQuickSort extends RecursiveAction {
        private final int[] dados;
        private final int inicio;
        private final int fim;
        private final int limiar;

        TarefaQuickSort(int[] dados, int inicio, int fim, int limiar) {
            this.dados = dados;
            this.inicio = inicio;
            this.fim = fim;
            this.limiar = limiar;
        }

        @Override
        protected void compute() {
            if (fim - inicio <= limiar) {
                quickSortSequencial(dados, inicio, fim);
            } else {
                int indiceParticao = particionarHoare(dados, inicio, fim);
                invokeAll(
                        new TarefaQuickSort(dados, inicio, indiceParticao, limiar),
                        new TarefaQuickSort(dados, indiceParticao + 1, fim, limiar)
                );
            }
        }
    }
}
