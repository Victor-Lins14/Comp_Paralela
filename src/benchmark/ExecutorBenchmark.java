package benchmark;

import algoritmos.AlgoritmoOrdenacao;
import algoritmos.BubbleSort;
import algoritmos.CountingSort;
import algoritmos.MergeSort;
import algoritmos.QuickSort;
import model.ResultadoAmostra;
import model.TipoConjuntoDados;
import util.GeradorDados;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Framework de teste e avaliação experimental de desempenho para os algoritmos
 * em versões seriais e paralelas.
 */
public class ExecutorBenchmark {

    public interface OuvinteProgresso {
        void aoIniciarPasso(String mensagem, int passoAtual, int totalPassos);
        void aoConcluirConfiguracao(ResultadoAmostra resultado, int passoAtual, int totalPassos);
    }

    private static final int LIMITE_TAMANHO_BUBBLE_SORT = 50_000;

    public static List<AlgoritmoOrdenacao> obterAlgoritmosPadrao() {
        return Arrays.asList(
                new QuickSort(),
                new MergeSort(),
                new CountingSort(),
                new BubbleSort()
        );
    }

    /**
     * Executa uma bateria parametrizada de testes com pelo menos 5 amostras por configuração.
     */
    public List<ResultadoAmostra> executarSuite(
            List<AlgoritmoOrdenacao> algoritmos,
            int[] tamanhos,
            TipoConjuntoDados[] naturezas,
            int[] intervalosMax,
            int[] contagensThreads,
            int numAmostras,
            OuvinteProgresso ouvinte) {

        int amostrasReais = Math.max(5, numAmostras);
        aquecerJVM(algoritmos);

        // Calcula total de combinações válidas
        int totalConfiguracoes = 0;
        for (AlgoritmoOrdenacao alg : algoritmos) {
            for (TipoConjuntoDados nat : naturezas) {
                for (int intervalo : intervalosMax) {
                    for (int tam : tamanhos) {
                        if (alg.isQuadratico() && tam > LIMITE_TAMANHO_BUBBLE_SORT) {
                            continue;
                        }
                        totalConfiguracoes += contagensThreads.length;
                    }
                }
            }
        }

        List<ResultadoAmostra> todosResultados = new ArrayList<>(totalConfiguracoes);
        Map<String, Double> mapaTemposSeriais = new HashMap<>();
        int passo = 0;

        for (TipoConjuntoDados natureza : naturezas) {
            for (int intervaloMax : intervalosMax) {
                for (int tamanho : tamanhos) {
                    // Pré-gera os vetores-base para as K amostras desta combinação (tamanho, natureza, intervalo)
                    // garantindo que todos os algoritmos e contagens de threads processem dados idênticos
                    int[][] vetoresBase = new int[amostrasReais][];
                    for (int a = 0; a < amostrasReais; a++) {
                        vetoresBase[a] = GeradorDados.gerarVetor(tamanho, natureza, intervaloMax, a + 1);
                    }

                    for (AlgoritmoOrdenacao alg : algoritmos) {
                        if (alg.isQuadratico() && tamanho > LIMITE_TAMANHO_BUBBLE_SORT) {
                            continue;
                        }

                        for (int threads : contagensThreads) {
                            passo++;
                            String modo = (threads <= 1) ? "Serial" : "Paralelo";
                            String msg = String.format(Locale.US,
                                    "[%d/%d] %s (%s - %d thread%s) | N=%d | %s | Max=%d",
                                    passo, totalConfiguracoes, alg.getNome(), modo, threads,
                                    threads > 1 ? "s" : "", tamanho, natureza.getRotulo(), intervaloMax);

                            if (ouvinte != null) {
                                ouvinte.aoIniciarPasso(msg, passo, totalConfiguracoes);
                            }

                            double[] temposMs = new double[amostrasReais];
                            for (int a = 0; a < amostrasReais; a++) {
                                int[] copia = Arrays.copyOf(vetoresBase[a], vetoresBase[a].length);

                                long inicioNs = System.nanoTime();
                                if (threads <= 1) {
                                    alg.ordenarSerial(copia);
                                } else {
                                    alg.ordenarParalelo(copia, threads);
                                }
                                long fimNs = System.nanoTime();

                                if (!GeradorDados.estaOrdenado(copia)) {
                                    throw new IllegalStateException(
                                            "Falha de verificação de ordenação no algoritmo: " + alg.getNome());
                                }

                                temposMs[a] = (fimNs - inicioNs) / 1_000_000.0;
                            }

                            ResultadoAmostra resultado = new ResultadoAmostra(
                                    alg.getNome(),
                                    modo,
                                    threads,
                                    tamanho,
                                    natureza,
                                    intervaloMax,
                                    temposMs
                            );

                            String chaveSerial = gerarChaveSerial(alg.getNome(), tamanho, natureza, intervaloMax);
                            if (threads <= 1) {
                                mapaTemposSeriais.put(chaveSerial, resultado.getMediaMs());
                                resultado.calcularSpeedup(resultado.getMediaMs());
                            } else {
                                Double tempoSerial = mapaTemposSeriais.get(chaveSerial);
                                if (tempoSerial != null) {
                                    resultado.calcularSpeedup(tempoSerial);
                                }
                            }

                            todosResultados.add(resultado);
                            if (ouvinte != null) {
                                ouvinte.aoConcluirConfiguracao(resultado, passo, totalConfiguracoes);
                            }
                        }
                    }
                }
            }
        }

        // Segunda passagem caso alguma execução paralela tenha ocorrido antes da serial
        for (ResultadoAmostra r : todosResultados) {
            String chave = gerarChaveSerial(r.getAlgoritmo(), r.getTamanho(), r.getNaturezaDados(), r.getIntervaloMax());
            Double tSerial = mapaTemposSeriais.get(chave);
            if (tSerial != null) {
                r.calcularSpeedup(tSerial);
            }
        }

        return todosResultados;
    }

    private static String gerarChaveSerial(String alg, int tam, TipoConjuntoDados nat, int intervalo) {
        return alg + "|" + tam + "|" + nat.name() + "|" + intervalo;
    }

    /**
     * Realiza o aquecimento (Warmup) da JVM para que o compilador JIT (HotSpot C2)
     * compile os métodos críticos antes do início da coleta de tempos.
     */
    private void aquecerJVM(List<AlgoritmoOrdenacao> algoritmos) {
        int[] base = GeradorDados.gerarVetor(2500, TipoConjuntoDados.ALEATORIO, 10000, 0);
        for (int i = 0; i < 3; i++) {
            for (AlgoritmoOrdenacao alg : algoritmos) {
                int[] c1 = Arrays.copyOf(base, base.length);
                alg.ordenarSerial(c1);
                int[] c2 = Arrays.copyOf(base, base.length);
                alg.ordenarParalelo(c2, 4);
            }
        }
    }
}
