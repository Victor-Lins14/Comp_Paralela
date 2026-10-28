package gui;

import algoritmos.AlgoritmoOrdenacao;
import algoritmos.BubbleSort;
import algoritmos.CountingSort;
import algoritmos.MergeSort;
import algoritmos.QuickSort;
import benchmark.ExecutorBenchmark;
import model.ResultadoAmostra;
import model.TipoConjuntoDados;
import util.ExportadorCSV;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Interface Gráfica Desktop interativa em Java Swing (Ponto Extra) para execução,
 * visualização de gráficos dinâmicos e exportação de dados CSV/PNG.
 */
public class BenchmarkSwingApp extends JFrame {

    private final PainelGraficoDinamico graficoTamanho;
    private final PainelGraficoDinamico graficoThreads;
    private final PainelGraficoDinamico graficoSpeedup;
    private final PainelGraficoDinamico graficoNatureza;
    private final DefaultTableModel modeloTabela;

    private final JComboBox<TipoConjuntoDados> comboNatureza;
    private final JComboBox<Integer> comboTamanho;
    private final JComboBox<Integer> comboThreads;
    private final JCheckBox checkEscalaLog;
    private final JCheckBox checkMostrarBubble;

    private final JCheckBox checkQuick;
    private final JCheckBox checkMerge;
    private final JCheckBox checkCounting;
    private final JCheckBox checkBubble;
    private final JSpinner spinnerAmostras;

    private final JProgressBar barraProgresso;
    private final JLabel labelStatus;
    private final JButton btnExecutarRapido;
    private final JButton btnExecutarCompleto;

    private List<ResultadoAmostra> resultadosAtuais = new ArrayList<>();

    public BenchmarkSwingApp() {
        super("UNIFOR - Análise de Desempenho de Algoritmos em Ambientes Seriais e Paralelos (Java Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(241, 245, 249));
        setContentPane(root);

        // Cabeçalho institucional
        root.add(criarPainelCabecalho(), BorderLayout.NORTH);

        // Painel lateral de controle e execução
        JPanel painelLateral = new JPanel();
        painelLateral.setLayout(new BoxLayout(painelLateral, BoxLayout.Y_AXIS));
        painelLateral.setPreferredSize(new Dimension(280, 600));
        painelLateral.setBackground(Color.WHITE);
        painelLateral.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel boxAlgs = new JPanel();
        boxAlgs.setLayout(new BoxLayout(boxAlgs, BoxLayout.Y_AXIS));
        boxAlgs.setBackground(Color.WHITE);
        boxAlgs.setBorder(new TitledBorder("1. Algoritmos Selecionados"));
        checkQuick = new JCheckBox("Quick Sort (O(n log n))", true);
        checkMerge = new JCheckBox("Merge Sort (O(n log n))", true);
        checkCounting = new JCheckBox("Counting Sort (O(n + k))", true);
        checkBubble = new JCheckBox("Bubble Sort (O(n²))", true);
        for (JCheckBox cb : new JCheckBox[]{checkQuick, checkMerge, checkCounting, checkBubble}) {
            cb.setBackground(Color.WHITE);
            cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
            boxAlgs.add(cb);
        }
        boxAlgs.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelLateral.add(boxAlgs);
        painelLateral.add(Box.createVerticalStrut(10));

        JPanel boxConfig = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boxConfig.setBackground(Color.WHITE);
        boxConfig.setBorder(new TitledBorder("2. Amostragem Estatística"));
        boxConfig.add(new JLabel("Amostras por teste (mín. 5):"));
        spinnerAmostras = new JSpinner(new SpinnerNumberModel(5, 5, 20, 1));
        boxConfig.add(spinnerAmostras);
        boxConfig.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelLateral.add(boxConfig);
        painelLateral.add(Box.createVerticalStrut(10));

        JPanel boxAcoes = new JPanel();
        boxAcoes.setLayout(new BoxLayout(boxAcoes, BoxLayout.Y_AXIS));
        boxAcoes.setBackground(Color.WHITE);
        boxAcoes.setBorder(new TitledBorder("3. Execução e Exportação"));

        btnExecutarRapido = criarBotaoAcao("Executar Benchmark Rápido", new Color(37, 99, 235), Color.WHITE);
        btnExecutarRapido.addActionListener(e -> iniciarBenchmarkEmBackground(false));

        btnExecutarCompleto = criarBotaoAcao("Executar Suite Completa", new Color(15, 23, 42), Color.WHITE);
        btnExecutarCompleto.addActionListener(e -> iniciarBenchmarkEmBackground(true));

        JButton btnCarregarCsv = criarBotaoAcao("Carregar Arquivo CSV...", new Color(241, 245, 249), new Color(15, 23, 42));
        btnCarregarCsv.addActionListener(e -> acaoCarregarCSV());

        JButton btnExportarCsv = criarBotaoAcao("Exportar Resultados CSV", new Color(241, 245, 249), new Color(15, 23, 42));
        btnExportarCsv.addActionListener(e -> acaoExportarCSV());

        JButton btnExportarPng = criarBotaoAcao("Salvar Gráficos em PNG", new Color(16, 185, 129), Color.WHITE);
        btnExportarPng.addActionListener(e -> acaoExportarGraficosPNG());

        for (JButton b : new JButton[]{btnExecutarRapido, btnExecutarCompleto, btnCarregarCsv, btnExportarCsv, btnExportarPng}) {
            boxAcoes.add(b);
            boxAcoes.add(Box.createVerticalStrut(8));
        }
        boxAcoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelLateral.add(boxAcoes);
        painelLateral.add(Box.createVerticalGlue());

        root.add(painelLateral, BorderLayout.WEST);

        // Área Central: Filtros de Visualização + Abas de Gráficos Dinâmicos e Tabela
        JPanel painelCentral = new JPanel(new BorderLayout(8, 8));
        painelCentral.setOpaque(false);

        JPanel barraFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        barraFiltros.setBackground(Color.WHITE);
        barraFiltros.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));

        barraFiltros.add(new JLabel("Natureza:"));
        comboNatureza = new JComboBox<>(TipoConjuntoDados.values());
        comboNatureza.addActionListener(e -> atualizarFiltrosGraficos());
        barraFiltros.add(comboNatureza);

        barraFiltros.add(new JLabel("Tamanho (N):"));
        comboTamanho = new JComboBox<>(new Integer[]{10_000, 25_000, 50_000, 100_000, 500_000, 1_000_000});
        comboTamanho.setSelectedItem(50_000);
        comboTamanho.addActionListener(e -> atualizarFiltrosGraficos());
        barraFiltros.add(comboTamanho);

        barraFiltros.add(new JLabel("Threads:"));
        comboThreads = new JComboBox<>(new Integer[]{1, 2, 4, 8, 16});
        comboThreads.setSelectedItem(8);
        comboThreads.addActionListener(e -> atualizarFiltrosGraficos());
        barraFiltros.add(comboThreads);

        checkEscalaLog = new JCheckBox("Escala Log10", true);
        checkEscalaLog.setBackground(Color.WHITE);
        checkEscalaLog.addActionListener(e -> atualizarFiltrosGraficos());
        barraFiltros.add(checkEscalaLog);

        checkMostrarBubble = new JCheckBox("Exibir Bubble Sort", true);
        checkMostrarBubble.setBackground(Color.WHITE);
        checkMostrarBubble.addActionListener(e -> atualizarFiltrosGraficos());
        barraFiltros.add(checkMostrarBubble);

        painelCentral.add(barraFiltros, BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(new Font("SansSerif", Font.BOLD, 12));

        graficoTamanho = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.TEMPO_POR_TAMANHO);
        graficoThreads = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.TEMPO_POR_THREADS);
        graficoSpeedup = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.SPEEDUP_POR_THREADS);
        graficoNatureza = new PainelGraficoDinamico(PainelGraficoDinamico.TipoGrafico.IMPACTO_NATUREZA);

        abas.addTab("1. Escalabilidade (Tempo × Tamanho N)", graficoTamanho);
        abas.addTab("2. Paralelismo (Tempo × Threads)", graficoThreads);
        abas.addTab("3. Ganho de Speedup (Sp × Threads)", graficoSpeedup);
        abas.addTab("4. Impacto da Natureza dos Dados", graficoNatureza);

        String[] colunas = {
                "Algoritmo", "Modo", "Threads", "Tamanho (N)", "Natureza", "Intervalo Máx",
                "Média (ms)", "Desvio Padrão (ms)", "Mediana (ms)", "Mín (ms)", "Máx (ms)",
                "Speedup (Sp)", "Eficiência (%)"
        };
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable tabela = new JTable(modeloTabela);
        tabela.setRowHeight(24);
        tabela.setAutoCreateRowSorter(true);
        abas.addTab("5. Tabela Estatística e Amostras", new JScrollPane(tabela));

        painelCentral.add(abas, BorderLayout.CENTER);

        // Barra inferior de status e progresso
        JPanel painelStatus = new JPanel(new BorderLayout(10, 4));
        painelStatus.setBackground(Color.WHITE);
        painelStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(6, 12, 6, 12)
        ));
        labelStatus = new JLabel("Pronto. Passe o mouse sobre os pontos ou barras nos gráficos para inspecionar estatísticas.");
        labelStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        barraProgresso = new JProgressBar(0, 100);
        barraProgresso.setPreferredSize(new Dimension(260, 18));
        barraProgresso.setStringPainted(true);

        painelStatus.add(labelStatus, BorderLayout.CENTER);
        painelStatus.add(barraProgresso, BorderLayout.EAST);
        painelCentral.add(painelStatus, BorderLayout.SOUTH);

        root.add(painelCentral, BorderLayout.CENTER);

        carregarCsvPadraoSeExistir();
        atualizarFiltrosGraficos();
    }

    private JPanel criarPainelCabecalho() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(new Color(15, 23, 42));
        painel.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel titulo = new JLabel("UNIFOR — Análise Comparativa de Algoritmos Seriais e Paralelos em Java");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 17));
        titulo.setForeground(Color.WHITE);

        int nucleos = Runtime.getRuntime().availableProcessors();
        JLabel infoHardware = new JLabel(String.format(
                "Processadores Lógicos Detectados na JVM: %d núcleos | Java %s",
                nucleos, System.getProperty("java.version")));
        infoHardware.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoHardware.setForeground(new Color(148, 163, 184));

        painel.add(titulo, BorderLayout.WEST);
        painel.add(infoHardware, BorderLayout.EAST);
        return painel;
    }

    private JButton criarBotaoAcao(String texto, Color fundo, Color corTexto) {
        JButton btn = new JButton(texto);
        btn.setMaximumSize(new Dimension(250, 36));
        btn.setPreferredSize(new Dimension(240, 34));
        btn.setBackground(fundo);
        btn.setForeground(corTexto);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private void carregarCsvPadraoSeExistir() {
        File arquivoPadrao = new File("resultados/benchmark_resumo.csv");
        if (arquivoPadrao.exists()) {
            try {
                List<ResultadoAmostra> carregados = ExportadorCSV.carregarResumoEstatistico(arquivoPadrao);
                if (!carregados.isEmpty()) {
                    definirResultados(carregados);
                    labelStatus.setText("Resultados carregados automaticamente de: " + arquivoPadrao.getPath()
                            + " (" + carregados.size() + " configurações).");
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void atualizarFiltrosGraficos() {
        TipoConjuntoDados nat = (TipoConjuntoDados) comboNatureza.getSelectedItem();
        Integer tam = (Integer) comboTamanho.getSelectedItem();
        Integer th = (Integer) comboThreads.getSelectedItem();
        boolean log = checkEscalaLog.isSelected();
        boolean bubble = checkMostrarBubble.isSelected();

        if (nat == null) nat = TipoConjuntoDados.ALEATORIO;
        if (tam == null) tam = 100_000;
        if (th == null) th = 8;

        for (PainelGraficoDinamico p : new PainelGraficoDinamico[]{
                graficoTamanho, graficoThreads, graficoSpeedup, graficoNatureza}) {
            p.configurarFiltros(nat, tam, th, 100_000, log, bubble);
        }
    }

    private void definirResultados(List<ResultadoAmostra> lista) {
        this.resultadosAtuais = new ArrayList<>(lista);
        modeloTabela.setRowCount(0);

        Set<Integer> tamanhosPresentes = new TreeSet<>();
        for (ResultadoAmostra r : lista) {
            tamanhosPresentes.add(r.getTamanho());
            modeloTabela.addRow(new Object[]{
                    r.getAlgoritmo(),
                    r.getModo(),
                    r.getThreads(),
                    String.format(Locale.GERMAN, "%,d", r.getTamanho()),
                    r.getNaturezaDados().getRotulo(),
                    String.format(Locale.GERMAN, "%,d", r.getIntervaloMax()),
                    String.format(Locale.US, "%.4f", r.getMediaMs()),
                    String.format(Locale.US, "%.4f", r.getDesvioPadraoMs()),
                    String.format(Locale.US, "%.4f", r.getMedianaMs()),
                    String.format(Locale.US, "%.4f", r.getMinMs()),
                    String.format(Locale.US, "%.4f", r.getMaxMs()),
                    String.format(Locale.US, "%.2fx", r.getSpeedup()),
                    String.format(Locale.US, "%.1f%%", r.getEficiencia() * 100.0)
            });
        }

        for (PainelGraficoDinamico p : new PainelGraficoDinamico[]{
                graficoTamanho, graficoThreads, graficoSpeedup, graficoNatureza}) {
            p.atualizarDados(resultadosAtuais);
        }
    }

    private void iniciarBenchmarkEmBackground(boolean modoCompleto) {
        List<AlgoritmoOrdenacao> selecionados = new ArrayList<>();
        if (checkQuick.isSelected()) selecionados.add(new QuickSort());
        if (checkMerge.isSelected()) selecionados.add(new MergeSort());
        if (checkCounting.isSelected()) selecionados.add(new CountingSort());
        if (checkBubble.isSelected()) selecionados.add(new BubbleSort());

        if (selecionados.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione ao menos um algoritmo para executar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int numAmostras = (Integer) spinnerAmostras.getValue();
        int[] tamanhos = modoCompleto
                ? new int[]{10_000, 25_000, 50_000, 100_000, 500_000, 1_000_000}
                : new int[]{10_000, 50_000, 100_000};
        TipoConjuntoDados[] naturezas = modoCompleto
                ? TipoConjuntoDados.values()
                : new TipoConjuntoDados[]{TipoConjuntoDados.ALEATORIO, TipoConjuntoDados.INVERSAMENTE_ORDENADO};
        int[] intervalos = new int[]{100_000};
        int[] threads = new int[]{1, 2, 4, 8, 16};

        btnExecutarRapido.setEnabled(false);
        btnExecutarCompleto.setEnabled(false);
        resultadosAtuais.clear();
        modeloTabela.setRowCount(0);

        SwingWorker<List<ResultadoAmostra>, ResultadoAmostra> worker = new SwingWorker<>() {
            @Override
            protected List<ResultadoAmostra> doInBackground() {
                ExecutorBenchmark executor = new ExecutorBenchmark();
                return executor.executarSuite(
                        selecionados, tamanhos, naturezas, intervalos, threads, numAmostras,
                        new ExecutorBenchmark.OuvinteProgresso() {
                            @Override
                            public void aoIniciarPasso(String mensagem, int passoAtual, int totalPassos) {
                                SwingUtilities.invokeLater(() -> {
                                    int pct = (int) ((passoAtual - 1) * 100.0 / Math.max(1, totalPassos));
                                    barraProgresso.setValue(pct);
                                    labelStatus.setText("Executando: " + mensagem);
                                });
                            }

                            @Override
                            public void aoConcluirConfiguracao(ResultadoAmostra resultado, int passoAtual, int totalPassos) {
                                publish(resultado);
                                SwingUtilities.invokeLater(() -> {
                                    int pct = (int) (passoAtual * 100.0 / Math.max(1, totalPassos));
                                    barraProgresso.setValue(pct);
                                });
                            }
                        });
            }

            @Override
            protected void process(List<ResultadoAmostra> novos) {
                resultadosAtuais.addAll(novos);
                definirResultados(resultadosAtuais);
            }

            @Override
            protected void done() {
                btnExecutarRapido.setEnabled(true);
                btnExecutarCompleto.setEnabled(true);
                try {
                    List<ResultadoAmostra> finais = get();
                    definirResultados(finais);
                    File arqResumo = new File("resultados/benchmark_resumo.csv");
                    File arqAmostras = new File("resultados/benchmark_amostras.csv");
                    ExportadorCSV.exportarResumoEstatistico(finais, arqResumo);
                    ExportadorCSV.exportarAmostrasDetalhadas(finais, arqAmostras);
                    barraProgresso.setValue(100);
                    labelStatus.setText("Benchmark finalizado! CSVs salvos em resultados/benchmark_resumo.csv e benchmark_amostras.csv.");
                } catch (Exception ex) {
                    labelStatus.setText("Erro na execução: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void acaoCarregarCSV() {
        JFileChooser chooser = new JFileChooser(new File("."));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                List<ResultadoAmostra> lidos = ExportadorCSV.carregarResumoEstatistico(chooser.getSelectedFile());
                definirResultados(lidos);
                labelStatus.setText("Carregados " + lidos.size() + " registros de " + chooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao ler CSV: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void acaoExportarCSV() {
        if (resultadosAtuais.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há resultados para exportar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            File fResumo = new File("resultados/benchmark_resumo.csv");
            File fAmostras = new File("resultados/benchmark_amostras.csv");
            ExportadorCSV.exportarResumoEstatistico(resultadosAtuais, fResumo);
            ExportadorCSV.exportarAmostrasDetalhadas(resultadosAtuais, fAmostras);
            JOptionPane.showMessageDialog(this,
                    "Arquivos CSV gerados com sucesso:\n- " + fResumo.getAbsolutePath()
                            + "\n- " + fAmostras.getAbsolutePath(),
                    "Exportação Concluída", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar CSV: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void acaoExportarGraficosPNG() {
        try {
            File dir = new File("resultados/graficos_swing");
            dir.mkdirs();
            graficoTamanho.exportarParaPNG(new File(dir, "swing_tempo_por_tamanho.png"), 1200, 700);
            graficoThreads.exportarParaPNG(new File(dir, "swing_tempo_por_threads.png"), 1200, 700);
            graficoSpeedup.exportarParaPNG(new File(dir, "swing_speedup_threads.png"), 1200, 700);
            graficoNatureza.exportarParaPNG(new File(dir, "swing_impacto_natureza.png"), 1200, 700);
            JOptionPane.showMessageDialog(this,
                    "Gráficos exportados em alta definição para:\n" + dir.getAbsolutePath(),
                    "Gráficos Salvos", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar PNG: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void iniciarInterface() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            BenchmarkSwingApp app = new BenchmarkSwingApp();
            app.setVisible(true);
        });
    }
}
