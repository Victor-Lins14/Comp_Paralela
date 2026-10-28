package algoritmos;

import java.util.concurrent.Phaser;

/**
 * Implementação Serial e Paralela do algoritmo Bubble Sort.
 *
 * Ambas as versões utilizam o Odd-Even Transposition Sort (Brick Sort), a formulação
 * paralela clássica do Bubble Sort: a versão serial executa as fases em 1 thread e a
 * paralela distribui cada fase entre p threads. O algoritmo alterna fases pares (compara os pares
 * (0,1), (2,3), (4,5)...) e fases ímpares (compara (1,2), (3,4), (5,6)...).
 * Dentro de uma mesma fase os pares são disjuntos, portanto podem ser comparados e
 * trocados simultaneamente por threads diferentes sem condição de corrida. As fases
 * são separadas por uma barreira de sincronização (Phaser).
 *
 * Assim como o Bubble Sort serial, o algoritmo continua sendo O(n²) em trabalho total
 * (até n fases de n/2 comparações), de modo que o Speedup medido reflete apenas o
 * ganho do paralelismo, e não uma mudança de complexidade algorítmica.
 * A parada antecipada também é preservada: o algoritmo termina quando uma fase par e
 * uma fase ímpar consecutivas não realizam nenhuma troca.
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

    /**
     * Odd-Even Transposition Sort sequencial (1 thread), com parada antecipada.
     * Executa exatamente as mesmas fases e comparações da versão paralela, de modo
     * que o Speedup Sp = T1 / Tp compare o MESMO algoritmo e meça apenas o efeito
     * do paralelismo (e não uma diferença entre algoritmos).
     */
    @Override
    public void ordenarSerial(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        int n = dados.length;
        int fasesSemTroca = 0;
        for (int fase = 0; fase < n && fasesSemTroca < 2; fase++) {
            boolean trocou = false;
            for (int idx = fase & 1; idx + 1 < n; idx += 2) {
                if (dados[idx] > dados[idx + 1]) {
                    int tmp = dados[idx];
                    dados[idx] = dados[idx + 1];
                    dados[idx + 1] = tmp;
                    trocou = true;
                }
            }
            fasesSemTroca = trocou ? 0 : fasesSemTroca + 1;
        }
    }

    /**
     * Bubble Sort clássico (passagens sucessivas da esquerda para a direita com
     * parada antecipada). Mantido apenas como referência didática; não é usado
     * no benchmark, pois sua cadeia de dependências entre iterações consecutivas
     * (o elemento "borbulhado" é lido logo após ser escrito) o torna mais lento que
     * o Odd-Even mesmo em 1 thread, o que distorceria o cálculo do Speedup.
     */
    public void ordenarSerialClassico(int[] dados) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        int n = dados.length;
        for (int i = 0; i < n - 1; i++) {
            boolean trocou = false;
            int limite = n - 1 - i;
            for (int j = 0; j < limite; j++) {
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

    /**
     * Odd-Even Transposition Sort paralelo.
     * Cada thread recebe uma faixa contígua de pares (boa localidade de cache) e
     * processa apenas a sua faixa em cada fase. O Phaser garante que nenhuma thread
     * inicie a fase seguinte antes de todas concluírem a fase atual, e o método
     * onAdvance (executado uma única vez por fase) decide a parada antecipada.
     */
    @Override
    public void ordenarParalelo(int[] dados, int numThreads) {
        if (dados == null || dados.length <= 1) {
            return;
        }
        if (numThreads <= 1) {
            ordenarSerial(dados);
            return;
        }

        final int n = dados.length;
        final int paresTotais = n / 2;
        final int threads = Math.max(1, Math.min(numThreads, paresTotais));
        final int paresPorThread = (paresTotais + threads - 1) / threads;

        // Cada thread sinaliza se realizou alguma troca na fase corrente.
        // A visibilidade entre threads é garantida pela barreira (Phaser).
        final boolean[] trocouNaFase = new boolean[threads];

        final Phaser barreira = new Phaser(threads) {
            private int fasesSemTroca = 0;

            @Override
            protected boolean onAdvance(int fase, int partesRegistradas) {
                boolean houveTroca = false;
                for (int t = 0; t < trocouNaFase.length; t++) {
                    if (trocouNaFase[t]) {
                        houveTroca = true;
                        trocouNaFase[t] = false;
                    }
                }
                fasesSemTroca = houveTroca ? 0 : fasesSemTroca + 1;
                // Uma fase par e uma ímpar consecutivas sem trocas => vetor ordenado.
                // O limite de n fases garante a terminação no pior caso.
                return fasesSemTroca >= 2 || fase + 1 >= n;
            }
        };

        Thread[] trabalhadores = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int id = t;
            final int parInicio = t * paresPorThread;
            final int parFim = Math.min(paresTotais, parInicio + paresPorThread);
            trabalhadores[t] = new Thread(() -> {
                int fase = 0;
                while (!barreira.isTerminated()) {
                    int deslocamento = fase & 1; // 0 = fase par, 1 = fase ímpar
                    boolean trocou = false;
                    for (int p = parInicio; p < parFim; p++) {
                        int idx = (p << 1) + deslocamento;
                        if (idx + 1 < n && dados[idx] > dados[idx + 1]) {
                            int tmp = dados[idx];
                            dados[idx] = dados[idx + 1];
                            dados[idx + 1] = tmp;
                            trocou = true;
                        }
                    }
                    if (trocou) {
                        trocouNaFase[id] = true;
                    }
                    barreira.arriveAndAwaitAdvance();
                    fase++;
                }
            }, "OddEven-" + t);
            trabalhadores[t].start();
        }

        for (Thread w : trabalhadores) {
            try {
                w.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
