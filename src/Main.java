import algoritmos.AlgoritmoOrdenacao;
import algoritmos.CountingSort;
import algoritmos.MergeSort;
import algoritmos.QuickSort;
import benchmark.ExecutorBenchmark;
import gui.BenchmarkSwingApp;
import gui.PainelGraficoDinamico;
import model.ResultadoAmostra;
import model.TipoConjuntoDados;
import util.ExportadorCSV;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Ponto de entrada principal do projeto:
 * "Análise de Desempenho de Algoritmos de Busca e Ordenação em Ambientes Concorrentes e Paralelos:
 *  Um Estudo Comparativo em Java"
 *
 * Modos de execução:
 * - Sem argumentos (padrão no IntelliJ): Abre a interface gráfica interativa em Java Swing
 *   carregando os resultados do CSV (ou executando uma bateria inicial caso o CSV ainda não exista).
 * - Com argumento "--cli": Executa toda a suíte experimental via terminal (5 amostras por teste),
 *   salva os arquivos CSV em "resultados/" e exporta os gráficos do Swing em PNG.
 */
public class Main {

    public static void main(String[] args) {
        boolean modoCli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                modoCli = true;
                break;
            }
        }

        if (modoCli) {
            executarBenchmarkViaConsole();
        } else {
            File csvPadrao = new File("resultados/benchmark_resumo.csv");
            if (!csvPadrao.exists()) {
                System.out.println("Arquivo CSV inicial não encontrado. Executando bateria base antes de abrir a GUI...");
                executarBenchmarkViaConsole();
            }
            System.out.println("Iniciando interface gráfica de Gráficos Dinâmicos em Java Swing...");
            BenchmarkSwingApp.iniciarInterface();
        }
    }

    private static void executarBenchmarkViaConsole() {
        System.out.println("================================================================================");
        System.out.println(" UNIFOR - ESTUDO COMPARATIVO DE ALGORITMOS EM AMBIENTES SERIAIS E PARALELOS");
        System.out.println(" Processadores Lógicos Disponíveis: " + Runtime.getRuntime().availableProcessors());
        System.out.println(" Versão do Java (JVM): " + System.getProperty("java.version"));
        System.out.println("================================================================================");

        ExecutorBenchmark executor = new ExecutorBenchmark();
        List<AlgoritmoOrdenacao> todosAlgoritmos = ExecutorBenchmark.obterAlgoritmosPadrao();

        // Experimento 1: Variação de Tamanho (N), Threads (1, 2, 4, 8, 16) e Natureza dos Dados (5 amostras cada)
        int[] tamanhos = new int[]{10_000, 25_000, 50_000, 100_000, 500_000, 1_000_000};
        TipoConjuntoDados[] naturezas = new TipoConjuntoDados[]{
                TipoConjuntoDados.ALEATORIO,
                TipoConjuntoDados.INVERSAMENTE_ORDENADO,
                TipoConjuntoDados.QUASE_ORDENADO,
                TipoConjuntoDados.POUCOS_UNICOS
        };
        int[] intervalos = new int[]{100_000};
        int[] threads = new int[]{1, 2, 4, 8, 16};
        int amostras = 5;

        long inicioGeral = System.currentTimeMillis();
        List<ResultadoAmostra> resultadosPrincipais = executor.executarSuite(
                todosAlgoritmos, tamanhos, naturezas, intervalos, threads, amostras,
                new ExecutorBenchmark.OuvinteProgresso() {
                    @Override
                    public void aoIniciarPasso(String mensagem, int passoAtual, int totalPassos) {
                        // Exibe progresso conciso
                    }

                    @Override
                    public void aoConcluirConfiguracao(ResultadoAmostra r, int passoAtual, int totalPassos) {
                        if (r.getTamanho() == 50_000 || r.getTamanho() == 1_000_000) {
                            System.out.printf(Locale.US,
                                    "[%3d/%3d] %-14s | %-8s (%2d thr) | N=%9d | %-12s | Média: %8.3f ms (±%6.3f) | Speedup: %5.2fx%n",
                                    passoAtual, totalPassos, r.getAlgoritmo(), r.getModo(), r.getThreads(),
                                    r.getTamanho(), r.getNaturezaDados().name(), r.getMediaMs(), r.getDesvioPadraoMs(),
                                    r.getSpeedup());
                        }
                    }
                }
        );

        // Experimento 2: Impacto do Intervalo de Valores [0..k] em função do tempo (N = 500.000)
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" Executando Experimento Complementar: Variação do Intervalo de Valores (k)");
        System.out.println("--------------------------------------------------------------------------------");
        List<AlgoritmoOrdenacao> algsRapidos = Arrays.asList(new QuickSort(), new MergeSort(), new CountingSort());
        int[] tamanhosIntervalo = new int[]{500_000};
        TipoConjuntoDados[] natAleatoria = new TipoConjuntoDados[]{TipoConjuntoDados.ALEATORIO};
        int[] intervalosVariados = new int[]{1_000, 100_000, 1_000_000, 10_000_000};
        int[] threadsIntervalo = new int[]{1, 8};

        List<ResultadoAmostra> resultadosIntervalo = executor.executarSuite(
                algsRapidos, tamanhosIntervalo, natAleatoria, intervalosVariados, threadsIntervalo, amostras,
                new ExecutorBenchmark.OuvinteProgresso() {
                    @Override
                    public void aoIniciarPasso(String mensagem, int passoAtual, int totalPassos) {
                    }

                    @Override
                    public void aoConcluirConfiguracao(ResultadoAmostra r, int passoAtual, int totalPassos) {
                        System.out.printf(Locale.US,
                                "[Intervalo k=%10d] %-14s | %-8s (%2d thr) | Média: %8.3f ms | Speedup: %5.2fx%n",
                                r.getIntervaloMax(), r.getAlgoritmo(), r.getModo(), r.getThreads(),
                                r.getMediaMs(), r.getSpeedup());
                    }
                }
        );

        try {
            File arqResumo = new File("resultados/benchmark_resumo.csv");
            File arqAmostras = new File("resultados/benchmark_amostras.csv");
            File arqIntervalos = new File("resultados/benchmark_intervalos.csv");

            ExportadorCSV.exportarResumoEstatistico(resultadosPrincipais, arqResumo);
            ExportadorCSV.exportarAmostrasDetalhadas(resultadosPrincipais, arqAmostras);
            ExportadorCSV.exportarResumoEstatistico(resultadosIntervalo, arqIntervalos);

            exportarGraficosSwingHeadless(resultadosPrincipais);
            exportarAmbiente(new File("resultados/ambiente.txt"));

            long duracaoSeg = (System.currentTimeMillis() - inicioGeral) / 1000;
            System.out.println("\n================================================================================");
            System.out.println(" Benchmark concluído com sucesso em " + duracaoSeg + "s!");
            System.out.println(" - CSV Resumo Estatístico : " + arqResumo.getAbsolutePath());
            System.out.println(" - CSV Amostras Individuais: " + arqAmostras.getAbsolutePath());
            System.out.println(" - CSV Intervalo de Valores: " + arqIntervalos.getAbsolutePath());
            System.out.println("================================================================================");
        } catch (IOException e) {
            System.err.println("Erro ao exportar arquivos CSV: " + e.getMessage());
        }
    }

    /**
     * Registra o ambiente de execução do benchmark (lido pelo gerador do relatório).
     * No Windows, PROCESSOR_IDENTIFIER descreve a família do processador; o nome comercial
     * pode ser informado em scripts/gerar_relatorio_unifor.py (constante PROCESSADOR).
     */
    private static void exportarAmbiente(File arquivo) throws IOException {
        arquivo.getParentFile().mkdirs();
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu == null || cpu.isBlank()) {
            cpu = System.getProperty("os.arch");
        }
        String conteudo = "processadores_logicos=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "processador=" + cpu + "\n"
                + "java_version=" + System.getProperty("java.version") + "\n"
                + "java_vm=" + System.getProperty("java.vm.name") + "\n"
                + "sistema_operacional=" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n"
                + "memoria_max_jvm_mb=" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "\n";
        java.nio.file.Files.writeString(arquivo.toPath(), conteudo, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void exportarGraficosSwingHeadless(List<ResultadoAmostra> resultados) throws IOException {
        File dir = new File("resultados/graficos_swing");
        dir.mkdirs();

        PainelGraficoDinamico pTam = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.TEMPO_POR_TAMANHO);
        pTam.atualizarDados(resultados);
        pTam.configurarFiltros(TipoConjuntoDados.ALEATORIO, 50_000, 8, 100_000, true, true);
        pTam.exportarParaPNG(new File(dir, "swing_tempo_por_tamanho.png"), 1200, 700);

        PainelGraficoDinamico pThr = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.TEMPO_POR_THREADS);
        pThr.atualizarDados(resultados);
        pThr.configurarFiltros(TipoConjuntoDados.ALEATORIO, 50_000, 8, 100_000, true, true);
        pThr.exportarParaPNG(new File(dir, "swing_tempo_por_threads.png"), 1200, 700);

        PainelGraficoDinamico pSpd = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.SPEEDUP_POR_THREADS);
        pSpd.atualizarDados(resultados);
        pSpd.configurarFiltros(TipoConjuntoDados.ALEATORIO, 1_000_000, 8, 100_000, false, false);
        pSpd.exportarParaPNG(new File(dir, "swing_speedup_threads.png"), 1200, 700);

        PainelGraficoDinamico pNat = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.IMPACTO_NATUREZA);
        pNat.atualizarDados(resultados);
        pNat.configurarFiltros(TipoConjuntoDados.ALEATORIO, 50_000, 8, 100_000, true, true);
        pNat.exportarParaPNG(new File(dir, "swing_impacto_natureza.png"), 1200, 700);
    }
}
