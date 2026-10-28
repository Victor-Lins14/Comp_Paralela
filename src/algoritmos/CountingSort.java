package algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Implementação Serial e Paralela do algoritmo Counting Sort.
 * Complexidade de tempo O(n + k), onde n é o tamanho do vetor e k é a amplitude
 * do intervalo de valores (max - min + 1).
 * A versão paralela utiliza o framework ForkJoinPool, onde cada thread aloca e
 * popula seu histograma local em paralelo (evitando contenção de locks e distribuindo
 * a alocação de memória pelos canais dos núcleos) e reconstrói as faixas do vetor
 * ordenado concorrentemente.
 */
public class CountingSort implements AlgoritmoOrdenacao {

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

        int amplitude = max - min + 1;
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
        if (numThreads <= 1 || dados.length < 4096) {
            ordenarSerial(dados);
            return;
        }

        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            int n = dados.length;
            int tamanhoBloco = (n + numThreads - 1) / numThreads;
            int threadsAtivas = (n + tamanhoBloco - 1) / tamanhoBloco;

            // Fase 1: Descoberta paralela de Mínimo e Máximo
            int[][] minMaxPorThread = new int[threadsAtivas][2];
            ForkJoinTask<?>[] tarefasMinMax = new ForkJoinTask<?>[threadsAtivas];
            for (int t = 0; t < threadsAtivas; t++) {
                final int id = t;
                final int inicio = t * tamanhoBloco;
                final int fim = Math.min(n, inicio + tamanhoBloco);
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
            for (int t = 1; t < threadsAtivas; t++) {
                if (minMaxPorThread[t][0] < minGlobal) minGlobal = minMaxPorThread[t][0];
                if (minMaxPorThread[t][1] > maxGlobal) maxGlobal = minMaxPorThread[t][1];
            }

            final int min = minGlobal;
            final int amplitude = maxGlobal - minGlobal + 1;

            // Fase 2: Alocação e contagem paralela de histogramas locais dentro de cada worker thread
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
            int blocoAmplitude = (amplitude + threadsAtivas - 1) / threadsAtivas;
            int[] somaPorFaixa = new int[threadsAtivas];
            ForkJoinTask<?>[] tarefasReducao = new ForkJoinTask<?>[threadsAtivas];
            int[] histBase = contagensLocais[0];

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

            // Cálculo dos offsets iniciais de escrita de cada faixa
            int[] offsetInicioFaixa = new int[threadsAtivas];
            int acumulado = 0;
            for (int t = 0; t < threadsAtivas; t++) {
                offsetInicioFaixa[t] = acumulado;
                acumulado += somaPorFaixa[t];
            }

            // Fase 4: Escrita paralela no vetor de saída
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
