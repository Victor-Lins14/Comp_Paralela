package algoritmos;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Phaser;
import java.util.concurrent.RecursiveAction;

/**
 * Implementação Serial e Paralela do algoritmo Bubble Sort.
 * Na versão serial, aplica o Bubble Sort clássico com detecção de parada antecipada.
 * Na versão paralela, particiona o conjunto de dados em blocos independentes,
 * aplica o Bubble Sort concorrentemente em cada bloco através de múltiplas threads
 * (reduzindo o custo quadrático de cada partição para O((n/p)²)) e realiza a
 * intercalação hierárquica em paralelo dos blocos ordenados.
 * Também disponibiliza a implementação do Odd-Even Transposition Sort (Brick Sort)
 * para fins de estudo das barreiras de sincronização.
 */
public class BubbleSort implements AlgoritmoOrdenacao {

    @Override
    public String getNome() {
        return "Bubble Sort";
    }

    @Override
    public boolean isQuadratico() {
        return true;
    }

    @Override
    public void ordenarSerial(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        bubbleSortSubvetor(dados, 0, dados.length - 1);
    }

    /**
     * Ordena o subvetor [inicio..fim] in-place utilizando o algoritmo Bubble Sort
     * com flag de interrupção antecipada caso nenhuma troca ocorra na passagem.
     */
    private static void bubbleSortSubvetor(int[] dados, int inicio, int fim) {
        for (int i = inicio; i < fim; i++) {
            boolean trocou = false;
            int limite = fim - (i - inicio);
            for (int j = inicio; j < limite; j++) {
                if (dados[j] > dados[j + 1]) {
                    int temp = dados[j];
                    dados[j] = dados[j + 1];
                    dados[j + 1] = temp;
                    trocou = true;
                }
            }
            if (!trocou) {
                break;
            }
        }
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
        // Divide em numThreads blocos para que cada thread execute Bubble Sort em um bloco
        int tamanhoBloco = Math.max(256, (dados.length + numThreads - 1) / numThreads);
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            pool.invoke(new TarefaBubbleSortBloco(dados, aux, 0, dados.length - 1, tamanhoBloco));
        } finally {
            pool.shutdown();
        }
    }

    /**
     * Variante alternativa: Odd-Even Transposition Sort (Brick Sort) paralelo.
     * Alterna entre fases pares e ímpares sincronizadas por Phaser.
     */
    public void ordenarParaleloOddEven(int[] dados, int numThreads) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        if (numThreads <= 1) {
            ordenarSerial(dados);
            return;
        }
        final int n = dados.length;
        final int threads = Math.min(numThreads, n / 2);
        final Phaser barreira = new Phaser(threads);
        Thread[] workers = new Thread[threads];
        int paresTotais = n / 2;
        int paresPorThread = (paresTotais + threads - 1) / threads;

        for (int t = 0; t < threads; t++) {
            final int parInicio = t * paresPorThread;
            final int parFim = Math.min(paresTotais, parInicio + paresPorThread);
            workers[t] = new Thread(() -> {
                for (int fase = 0; fase < n; fase++) {
                    int deslocamento = fase & 1; // 0 para fase par, 1 para fase ímpar
                    for (int p = parInicio; p < parFim; p++) {
                        int idx = (p << 1) + deslocamento;
                        if (idx + 1 < n && dados[idx] > dados[idx + 1]) {
                            int tmp = dados[idx];
                            dados[idx] = dados[idx + 1];
                            dados[idx + 1] = tmp;
                        }
                    }
                    barreira.arriveAndAwaitAdvance();
                }
            });
            workers[t].start();
        }

        for (Thread w : workers) {
            try {
                w.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class TarefaBubbleSortBloco extends RecursiveAction {
        private final int[] dados;
        private final int[] aux;
        private final int inicio;
        private final int fim;
        private final int tamanhoBloco;

        TarefaBubbleSortBloco(int[] dados, int[] aux, int inicio, int fim, int tamanhoBloco) {
            this.dados = dados;
            this.aux = aux;
            this.inicio = inicio;
            this.fim = fim;
            this.tamanhoBloco = tamanhoBloco;
        }

        @Override
        protected void compute() {
            if (fim - inicio + 1 <= tamanhoBloco) {
                bubbleSortSubvetor(dados, inicio, fim);
            } else {
                int meio = inicio + ((fim - inicio) >>> 1);
                invokeAll(
                        new TarefaBubbleSortBloco(dados, aux, inicio, meio, tamanhoBloco),
                        new TarefaBubbleSortBloco(dados, aux, meio + 1, fim, tamanhoBloco)
                );
                MergeSort.intercalar(dados, aux, inicio, meio, fim);
            }
        }
    }
}
