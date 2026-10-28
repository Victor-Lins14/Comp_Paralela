package algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * Implementação Serial e Paralela do algoritmo Counting Sort.
 * Complexidade de tempo O(n + k), onde n é o tamanho do vetor e k é a amplitude
 * do intervalo de valores (max - min + 1).
 *
 * A versão paralela utiliza o framework ForkJoinPool em quatro fases:
 * (1) busca paralela de mínimo e máximo; (2) histogramas locais por thread, sem
 * contenção de locks; (3) redução paralela dos histogramas por faixa de valores;
 * (4) reconstrução paralela do vetor ordenado.
 *
 * Granularidade adaptativa: cada thread aloca um histograma de tamanho k, logo a
 * memória extra é O(p·k). Quando k é grande em relação a n, alocar e reduzir p
 * histogramas custa mais do que contar os n elementos. Por isso o número de threads
 * da contagem é limitado a 2n/k; se esse limite for 1, a contagem é feita
 * sequencialmente (o que evita alocar, por exemplo, 8 × 10.000.000 inteiros).
 */
public class CountingSort implements AlgoritmoOrdenacao {

    private static final int TAMANHO_MINIMO_PARALELO = 4096;

    @Override
    public String getNome() {
        return "Counting Sort";
    }

    @Override
    public void ordenarSerial(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }

        int min = dados[0];
        int max = dados[0];
        for (int i = 1; i < dados.length; i++) {
            int val = dados[i];
            if (val < min) min = val;
            else if (val > max) max = val;
        }
        contarEReconstruirSerial(dados, min, max - min + 1);
    }

    private static void contarEReconstruirSerial(int[] dados, int min, int amplitude) {
        int[] contagem = new int[amplitude];
        for (int valor : dados) {
            contagem[valor - min]++;
        }

        int indice = 0;
        for (int v = 0; v < amplitude; v++) {
            int freq = contagem[v];
            int valorReal = v + min;
            while (freq-- > 0) {
                dados[indice++] = valorReal;
            }
        }
    }

    @Override
    public void ordenarParalelo(int[] dados, int numThreads) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        if (numThreads <= 1 || dados.length < TAMANHO_MINIMO_PARALELO) {
            ordenarSerial(dados);
            return;
        }

        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            final int n = dados.length;

            // Fase 1: Descoberta paralela de mínimo e máximo
            int blocoMinMax = (n + numThreads - 1) / numThreads;
            int threadsMinMax = (n + blocoMinMax - 1) / blocoMinMax;
            int[][] minMaxPorThread = new int[threadsMinMax][2];
            ForkJoinTask<?>[] tarefasMinMax = new ForkJoinTask<?>[threadsMinMax];
            for (int t = 0; t < threadsMinMax; t++) {
                final int id = t;
                final int inicio = t * blocoMinMax;
                final int fim = Math.min(n, inicio + blocoMinMax);
                tarefasMinMax[t] = pool.submit(() -> {
                    int localMin = dados[inicio];
                    int localMax = dados[inicio];
                    for (int i = inicio + 1; i < fim; i++) {
                        int v = dados[i];
                        if (v < localMin) localMin = v;
                        else if (v > localMax) localMax = v;
                    }
                    minMaxPorThread[id][0] = localMin;
                    minMaxPorThread[id][1] = localMax;
                });
            }
            for (ForkJoinTask<?> task : tarefasMinMax) {
                task.join();
            }

            int minGlobal = minMaxPorThread[0][0];
            int maxGlobal = minMaxPorThread[0][1];
            for (int t = 1; t < threadsMinMax; t++) {
                if (minMaxPorThread[t][0] < minGlobal) minGlobal = minMaxPorThread[t][0];
                if (minMaxPorThread[t][1] > maxGlobal) maxGlobal = minMaxPorThread[t][1];
            }

            final int min = minGlobal;
            final int amplitude = maxGlobal - minGlobal + 1;

            // Granularidade adaptativa: limita o número de histogramas locais a 2n/k
            long limitePorMemoria = Math.max(1L, (2L * n) / amplitude);
            final int threadsAtivas = (int) Math.min(threadsMinMax, limitePorMemoria);
            if (threadsAtivas <= 1) {
                contarEReconstruirSerial(dados, min, amplitude);
                return;
            }
            final int tamanhoBloco = (n + threadsAtivas - 1) / threadsAtivas;

            // Fase 2: Alocação e contagem paralela de histogramas locais em cada worker
            final int[][] contagensLocais = new int[threadsAtivas][];
            ForkJoinTask<?>[] tarefasContagem = new ForkJoinTask<?>[threadsAtivas];
            for (int t = 0; t < threadsAtivas; t++) {
                final int id = t;
                final int inicio = t * tamanhoBloco;
                final int fim = Math.min(n, inicio + tamanhoBloco);
                tarefasContagem[t] = pool.submit(() -> {
                    int[] histLocal = new int[amplitude];
                    for (int i = inicio; i < fim; i++) {
                        histLocal[dados[i] - min]++;
                    }
                    contagensLocais[id] = histLocal;
                });
            }
            for (ForkJoinTask<?> task : tarefasContagem) {
                task.join();
            }

            // Fase 3: Redução paralela por faixa de valores
            final int blocoAmplitude = (amplitude + threadsAtivas - 1) / threadsAtivas;
            final int[] somaPorFaixa = new int[threadsAtivas];
            final int[] histBase = contagensLocais[0];
            ForkJoinTask<?>[] tarefasReducao = new ForkJoinTask<?>[threadsAtivas];
            for (int t = 0; t < threadsAtivas; t++) {
                final int idFaixa = t;
                final int vInicio = t * blocoAmplitude;
                final int vFim = Math.min(amplitude, vInicio + blocoAmplitude);
                tarefasReducao[t] = pool.submit(() -> {
                    int totalFaixa = 0;
                    for (int v = vInicio; v < vFim; v++) {
                        int soma = histBase[v];
                        for (int th = 1; th < threadsAtivas; th++) {
                            soma += contagensLocais[th][v];
                        }
                        histBase[v] = soma;
                        totalFaixa += soma;
                    }
                    somaPorFaixa[idFaixa] = totalFaixa;
                });
            }
            for (ForkJoinTask<?> task : tarefasReducao) {
                task.join();
            }

            // Cálculo dos offsets iniciais de escrita de cada faixa (soma de prefixos)
            int[] offsetInicioFaixa = new int[threadsAtivas];
            int acumulado = 0;
            for (int t = 0; t < threadsAtivas; t++) {
                offsetInicioFaixa[t] = acumulado;
                acumulado += somaPorFaixa[t];
            }

            // Fase 4: Escrita paralela no vetor de saída (faixas disjuntas)
            ForkJoinTask<?>[] tarefasEscrita = new ForkJoinTask<?>[threadsAtivas];
            for (int t = 0; t < threadsAtivas; t++) {
                final int vInicio = t * blocoAmplitude;
                final int vFim = Math.min(amplitude, vInicio + blocoAmplitude);
                final int indiceInicial = offsetInicioFaixa[t];
                tarefasEscrita[t] = pool.submit((Runnable) () -> {
                    int idx = indiceInicial;
                    for (int v = vInicio; v < vFim; v++) {
                        int freq = histBase[v];
                        int valorReal = v + min;
                        while (freq-- > 0) {
                            dados[idx++] = valorReal;
                        }
                    }
                });
            }
            for (ForkJoinTask<?> task : tarefasEscrita) {
                task.join();
            }
        } finally {
            pool.shutdown();
        }
    }
}
