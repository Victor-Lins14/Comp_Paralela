package gui;

import model.ResultadoAmostra;
import model.TipoConjuntoDados;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Componente Swing customizado para renderização de gráficos dinâmicos interativos
 * com suporte a anti-aliasing, barras de erro (desvio padrão), escala linear/logarítmica,
 * tooltips ao passar o mouse e exportação direta para PNG.
 */
public class PainelGraficoDinamico extends JPanel {

    public enum TipoGrafico {
        TEMPO_POR_TAMANHO("Tempo de Execução (ms) × Tamanho do Vetor (N)"),
        TEMPO_POR_THREADS("Tempo Médio e Desvio Padrão (ms) × Número de Threads"),
        SPEEDUP_POR_THREADS("Speedup (Sp = T1 / Tp) × Número de Threads"),
        IMPACTO_NATUREZA("Impacto da Natureza dos Dados no Tempo (ms)");

        private final String tituloPadrao;

        TipoGrafico(String tituloPadrao) {
            this.tituloPadrao = tituloPadrao;
        }

        public String getTituloPadrao() {
            return tituloPadrao;
        }
    }

    private static class PontoInterativo {
        final ShapeWrapper area;
        final String titulo;
        final String[] linhasDetalhe;
        final Color cor;

        PontoInterativo(ShapeWrapper area, String titulo, String[] linhasDetalhe, Color cor) {
            this.area = area;
            this.titulo = titulo;
            this.linhasDetalhe = linhasDetalhe;
            this.cor = cor;
        }
    }

    private static class ShapeWrapper {
        final double cx;
        final double cy;
        final Rectangle2D rect;

        ShapeWrapper(double cx, double cy, Rectangle2D rect) {
            this.cx = cx;
            this.cy = cy;
            this.rect = rect;
        }

        boolean contem(Point p) {
            return rect.contains(p.x, p.y);
        }
    }

    private final TipoGrafico tipoGrafico;
    private List<ResultadoAmostra> dados = new ArrayList<>();
    private TipoConjuntoDados filtroNatureza = TipoConjuntoDados.ALEATORIO;
    private int filtroTamanho = 100_000;
    private int filtroThreads = 8;
    private int filtroIntervalo = 100_000;
    private boolean usarEscalaLog = false;
    private boolean incluirBubbleSort = true;

    private final List<PontoInterativo> pontosInterativos = new ArrayList<>();
    private PontoInterativo pontoDestacado = null;
    private Point posicaoMouse = null;

    private static final Map<String, Color> CORES_ALGORITMOS = new LinkedHashMap<>();

    static {
        CORES_ALGORITMOS.put("Quick Sort", new Color(37, 99, 235));     // Azul Royal
        CORES_ALGORITMOS.put("Merge Sort", new Color(16, 185, 129));    // Verde Esmeralda
        CORES_ALGORITMOS.put("Counting Sort", new Color(245, 158, 11)); // Âmbar / Laranja
        CORES_ALGORITMOS.put("Bubble Sort", new Color(220, 38, 38));    // Vermelho Carmim
    }

    public PainelGraficoDinamico(TipoGrafico tipoGrafico) {
        this.tipoGrafico = tipoGrafico;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(860, 480));

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                posicaoMouse = e.getPoint();
                PontoInterativo encontrado = null;
                for (int i = pontosInterativos.size() - 1; i >= 0; i--) {
                    PontoInterativo pi = pontosInterativos.get(i);
                    if (pi.area.contem(posicaoMouse)) {
                        encontrado = pi;
                        break;
                    }
                }
                if (encontrado != pontoDestacado || encontrado != null) {
                    pontoDestacado = encontrado;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (pontoDestacado != null) {
                    pontoDestacado = null;
                    repaint();
                }
            }
        };
        addMouseMotionListener(mouseHandler);
        addMouseListener(mouseHandler);
    }

    public void atualizarDados(List<ResultadoAmostra> novosDados) {
        this.dados = new ArrayList<>(novosDados);
        repaint();
    }

    public void configurarFiltros(
            TipoConjuntoDados natureza,
            int tamanho,
            int threads,
            int intervalo,
            boolean escalaLog,
            boolean mostrarBubble) {
        this.filtroNatureza = natureza;
        this.filtroTamanho = tamanho;
        this.filtroThreads = threads;
        this.filtroIntervalo = intervalo;
        this.usarEscalaLog = escalaLog;
        this.incluirBubbleSort = mostrarBubble;
        repaint();
    }

    public void exportarParaPNG(File arquivo, int largura, int altura) throws IOException {
        BufferedImage img = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Dimension tamanhoOriginal = getSize();
        setSize(largura, altura);
        paintComponent(g2);
        setSize(tamanhoOriginal);
        g2.dispose();
        if (arquivo.getParentFile() != null) {
            arquivo.getParentFile().mkdirs();
        }
        ImageIO.write(img, "png", arquivo);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        pontosInterativos.clear();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Fundo suave com borda de cartão
        g2.setColor(new Color(248, 250, 252));
        g2.fillRect(0, 0, w, h);

        int margemCard = 14;
        RoundRectangle2D card = new RoundRectangle2D.Double(
                margemCard, margemCard, w - 2 * margemCard, h - 2 * margemCard, 16, 16);
        g2.setColor(Color.WHITE);
        g2.fill(card);
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(card);

        if (dados == null || dados.isEmpty()) {
            desenharMensagemVazia(g2, w, h);
            g2.dispose();
            return;
        }

        switch (tipoGrafico) {
            case TEMPO_POR_TAMANHO:
                desenharGraficoTempoPorTamanho(g2, w, h);
                break;
            case TEMPO_POR_THREADS:
                desenharGraficoTempoPorThreads(g2, w, h);
                break;
            case SPEEDUP_POR_THREADS:
                desenharGraficoSpeedup(g2, w, h);
                break;
            case IMPACTO_NATUREZA:
                desenharGraficoImpactoNatureza(g2, w, h);
                break;
        }

        if (pontoDestacado != null && posicaoMouse != null) {
            desenharTooltip(g2, pontoDestacado, posicaoMouse, w, h);
        }

        g2.dispose();
    }

    private void desenharMensagemVazia(Graphics2D g2, int w, int h) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(new Color(100, 116, 139));
        String msg = "Nenhum dado carregado. Clique em 'Executar Benchmark' ou 'Carregar CSV'.";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    private void desenharCabecalhoELegenda(Graphics2D g2, int w, String titulo, String subtitulo, Set<String> algoritmosPresentes) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(new Color(15, 23, 42));
        g2.drawString(titulo, 36, 44);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(100, 116, 139));
        g2.drawString(subtitulo, 36, 64);

        // Legenda no canto superior direito
        int xLeg = w - 36;
        int yLeg = 42;
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        List<String> listaAlg = new ArrayList<>(algoritmosPresentes);
        Collections.reverse(listaAlg);
        for (String alg : listaAlg) {
            if (!incluirBubbleSort && alg.equals("Bubble Sort")) continue;
            Color cor = obterCorAlgoritmo(alg);
            int largTexto = fm.stringWidth(alg);
            xLeg -= (largTexto + 26);
            g2.setColor(cor);
            g2.fillRoundRect(xLeg, yLeg - 10, 14, 14, 4, 4);
            g2.setColor(new Color(51, 65, 85));
            g2.drawString(alg, xLeg + 18, yLeg + 2);
            xLeg -= 14;
        }
    }

    private void desenharGraficoTempoPorTamanho(Graphics2D g2, int w, int h) {
        List<ResultadoAmostra> filtrados = new ArrayList<>();
        Set<Integer> tamanhosSet = new TreeSet<>();
        Set<String> algsSet = new TreeSet<>();

        for (ResultadoAmostra r : dados) {
            if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
            if (r.getNaturezaDados() == filtroNatureza
                    && r.getThreads() == filtroThreads
                    && r.getIntervaloMax() == filtroIntervalo) {
                filtrados.add(r);
                tamanhosSet.add(r.getTamanho());
                algsSet.add(r.getAlgoritmo());
            }
        }

        // Fallback caso o filtro exato não tenha amostras
        if (filtrados.isEmpty()) {
            for (ResultadoAmostra r : dados) {
                if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
                if (r.getThreads() == filtroThreads) {
                    filtrados.add(r);
                    tamanhosSet.add(r.getTamanho());
                    algsSet.add(r.getAlgoritmo());
                }
            }
        }

        String modoTexto = filtroThreads <= 1 ? "Serial (1 Thread)" : "Paralelo (" + filtroThreads + " Threads)";
        String subtitulo = String.format("Modo: %s | Natureza: %s | Escala: %s",
                modoTexto, filtroNatureza.getRotulo(), usarEscalaLog ? "Logarítmica (log10)" : "Linear");
        desenharCabecalhoELegenda(g2, w, tipoGrafico.getTituloPadrao(), subtitulo, CORES_ALGORITMOS.keySet());

        if (filtrados.isEmpty() || tamanhosSet.isEmpty()) {
            return;
        }

        int padEsq = 88;
        int padDir = 42;
        int padTopo = 86;
        int padBase = 64;
        int areaW = w - padEsq - padDir;
        int areaH = h - padTopo - padBase;

        double maxVal = 0.001;
        double minVal = Double.MAX_VALUE;
        for (ResultadoAmostra r : filtrados) {
            maxVal = Math.max(maxVal, r.getMediaMs() + r.getDesvioPadraoMs());
            minVal = Math.min(minVal, Math.max(0.001, r.getMediaMs()));
        }
        maxVal *= 1.12;

        desenharGradeEixosY(g2, padEsq, padTopo, areaW, areaH, minVal, maxVal, "Tempo Médio (ms)");

        List<Integer> tamanhos = new ArrayList<>(tamanhosSet);
        int nTam = tamanhos.size();

        // Rótulos eixo X
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.setColor(new Color(51, 65, 85));
        for (int i = 0; i < nTam; i++) {
            double x = padEsq + (nTam == 1 ? areaW / 2.0 : (i * (double) areaW / (nTam - 1)));
            g2.setColor(new Color(226, 232, 240));
            g2.drawLine((int) x, padTopo, (int) x, padTopo + areaH);

            String rotulo = formatarInteiro(tamanhos.get(i));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(new Color(51, 65, 85));
            g2.drawString(rotulo, (int) x - fm.stringWidth(rotulo) / 2, padTopo + areaH + 22);
        }

        String tituloX = "Tamanho do Conjunto de Dados (N elementos)";
        FontMetrics fmX = g2.getFontMetrics();
        g2.drawString(tituloX, padEsq + (areaW - fmX.stringWidth(tituloX)) / 2, padTopo + areaH + 44);

        // Desenha linhas por algoritmo
        for (String alg : CORES_ALGORITMOS.keySet()) {
            if (!incluirBubbleSort && alg.equals("Bubble Sort")) continue;
            Color cor = obterCorAlgoritmo(alg);

            List<Point.Double> coords = new ArrayList<>();
            List<ResultadoAmostra> amostrasAlg = new ArrayList<>();

            for (int i = 0; i < nTam; i++) {
                int tam = tamanhos.get(i);
                ResultadoAmostra encontrado = null;
                for (ResultadoAmostra r : filtrados) {
                    if (r.getAlgoritmo().equals(alg) && r.getTamanho() == tam) {
                        encontrado = r;
                        break;
                    }
                }
                if (encontrado != null) {
                    double x = padEsq + (nTam == 1 ? areaW / 2.0 : (i * (double) areaW / (nTam - 1)));
                    double y = converterValorParaY(encontrado.getMediaMs(), minVal, maxVal, padTopo, areaH);
                    coords.add(new Point.Double(x, y));
                    amostrasAlg.add(encontrado);
                }
            }

            if (coords.size() >= 2) {
                Path2D caminho = new Path2D.Double();
                caminho.moveTo(coords.get(0).x, coords.get(0).y);
                for (int i = 1; i < coords.size(); i++) {
                    caminho.lineTo(coords.get(i).x, coords.get(i).y);
                }
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(caminho);
            }

            for (int i = 0; i < coords.size(); i++) {
                Point.Double pt = coords.get(i);
                ResultadoAmostra r = amostrasAlg.get(i);

                // Barra de erro de desvio padrão
                double ySuperior = converterValorParaY(r.getMediaMs() + r.getDesvioPadraoMs(), minVal, maxVal, padTopo, areaH);
                double yInferior = converterValorParaY(Math.max(minVal, r.getMediaMs() - r.getDesvioPadraoMs()), minVal, maxVal, padTopo, areaH);
                g2.setColor(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 150));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine((int) pt.x, (int) ySuperior, (int) pt.x, (int) yInferior);
                g2.drawLine((int) pt.x - 4, (int) ySuperior, (int) pt.x + 4, (int) ySuperior);
                g2.drawLine((int) pt.x - 4, (int) yInferior, (int) pt.x + 4, (int) yInferior);

                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(pt.x - 5.5, pt.y - 5.5, 11, 11));
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2.4f));
                g2.draw(new Ellipse2D.Double(pt.x - 5.5, pt.y - 5.5, 11, 11));

                pontosInterativos.add(new PontoInterativo(
                        new ShapeWrapper(pt.x, pt.y, new Rectangle2D.Double(pt.x - 10, pt.y - 10, 20, 20)),
                        r.getAlgoritmo() + " (" + r.getModo() + ")",
                        criarDetalhesTooltip(r),
                        cor
                ));
            }
        }
    }

    private void desenharGraficoTempoPorThreads(Graphics2D g2, int w, int h) {
        List<ResultadoAmostra> filtrados = filtrarPorTamanhoENatureza();
        String subtitulo = String.format("Tamanho N = %s | Natureza: %s | Barras de erro = ±1 Desvio Padrão (5 amostras)",
                formatarInteiro(obterTamanhoEfetivo(filtrados)), filtroNatureza.getRotulo());
        desenharCabecalhoELegenda(g2, w, tipoGrafico.getTituloPadrao(), subtitulo, CORES_ALGORITMOS.keySet());

        if (filtrados.isEmpty()) return;

        Set<Integer> threadsSet = new TreeSet<>();
        List<String> algoritmosAtivos = new ArrayList<>();
        for (String alg : CORES_ALGORITMOS.keySet()) {
            if (!incluirBubbleSort && alg.equals("Bubble Sort")) continue;
            for (ResultadoAmostra r : filtrados) {
                if (r.getAlgoritmo().equals(alg)) {
                    if (!algoritmosAtivos.contains(alg)) algoritmosAtivos.add(alg);
                    threadsSet.add(r.getThreads());
                }
            }
        }

        int padEsq = 88;
        int padDir = 42;
        int padTopo = 86;
        int padBase = 64;
        int areaW = w - padEsq - padDir;
        int areaH = h - padTopo - padBase;

        double maxVal = 0.001;
        double minVal = Double.MAX_VALUE;
        for (ResultadoAmostra r : filtrados) {
            maxVal = Math.max(maxVal, r.getMediaMs() + r.getDesvioPadraoMs());
            minVal = Math.min(minVal, Math.max(0.001, r.getMediaMs()));
        }
        maxVal *= 1.15;

        desenharGradeEixosY(g2, padEsq, padTopo, areaW, areaH, minVal, maxVal, "Tempo Médio (ms)");

        List<Integer> listaThreads = new ArrayList<>(threadsSet);
        int numGrupos = listaThreads.size();
        int numBarrasPorGrupo = Math.max(1, algoritmosAtivos.size());
        double largGrupo = (double) areaW / numGrupos;
        double largBarra = Math.min(36.0, (largGrupo * 0.72) / numBarrasPorGrupo);
        double espacoGrupo = largBarra * numBarrasPorGrupo;

        for (int gIdx = 0; gIdx < numGrupos; gIdx++) {
            int th = listaThreads.get(gIdx);
            double centroGrupoX = padEsq + (gIdx + 0.5) * largGrupo;
            double inicioGrupoX = centroGrupoX - espacoGrupo / 2.0;

            for (int bIdx = 0; bIdx < algoritmosAtivos.size(); bIdx++) {
                String alg = algoritmosAtivos.get(bIdx);
                Color cor = obterCorAlgoritmo(alg);
                ResultadoAmostra r = buscarResultado(filtrados, alg, th);
                if (r == null) continue;

                double xBarra = inicioGrupoX + bIdx * largBarra;
                double yTopo = converterValorParaY(r.getMediaMs(), minVal, maxVal, padTopo, areaH);
                double altBarra = Math.max(2.0, (padTopo + areaH) - yTopo);

                GradientPaint gp = new GradientPaint(
                        (float) xBarra, (float) yTopo, cor,
                        (float) xBarra, (float) (padTopo + areaH), cor.darker());
                g2.setPaint(gp);
                RoundRectangle2D rectBarra = new RoundRectangle2D.Double(
                        xBarra + 1, yTopo, largBarra - 2, altBarra, 6, 6);
                g2.fill(rectBarra);

                // Barra de erro de desvio padrão
                double cx = xBarra + largBarra / 2.0;
                double yErrSup = converterValorParaY(r.getMediaMs() + r.getDesvioPadraoMs(), minVal, maxVal, padTopo, areaH);
                double yErrInf = converterValorParaY(Math.max(minVal, r.getMediaMs() - r.getDesvioPadraoMs()), minVal, maxVal, padTopo, areaH);
                g2.setColor(new Color(30, 41, 59));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawLine((int) cx, (int) yErrSup, (int) cx, (int) yErrInf);
                g2.drawLine((int) cx - 4, (int) yErrSup, (int) cx + 4, (int) yErrSup);
                g2.drawLine((int) cx - 4, (int) yErrInf, (int) cx + 4, (int) yErrInf);

                pontosInterativos.add(new PontoInterativo(
                        new ShapeWrapper(cx, yTopo, new Rectangle2D.Double(xBarra, yTopo - 4, largBarra, altBarra + 8)),
                        r.getAlgoritmo() + " (" + th + (th == 1 ? " Thread - Serial" : " Threads - Paralelo") + ")",
                        criarDetalhesTooltip(r),
                        cor
                ));
            }

            // Rótulo do eixo X
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(51, 65, 85));
            String rotuloTh = th == 1 ? "1 (Serial)" : th + " Threads";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rotuloTh, (int) centroGrupoX - fm.stringWidth(rotuloTh) / 2, padTopo + areaH + 22);
        }

        String tituloX = "Configuração de Concorrência / Número de Threads";
        FontMetrics fmX = g2.getFontMetrics();
        g2.drawString(tituloX, padEsq + (areaW - fmX.stringWidth(tituloX)) / 2, padTopo + areaH + 44);
    }

    private void desenharGraficoSpeedup(Graphics2D g2, int w, int h) {
        List<ResultadoAmostra> filtrados = filtrarPorTamanhoENatureza();
        String subtitulo = String.format("Tamanho N = %s | Natureza: %s | Speedup S(p) = Tempo Serial / Tempo Paralelo(p)",
                formatarInteiro(obterTamanhoEfetivo(filtrados)), filtroNatureza.getRotulo());
        desenharCabecalhoELegenda(g2, w, tipoGrafico.getTituloPadrao(), subtitulo, CORES_ALGORITMOS.keySet());

        if (filtrados.isEmpty()) return;

        Set<Integer> threadsSet = new TreeSet<>();
        double maxSpeedup = 2.0;
        for (ResultadoAmostra r : filtrados) {
            threadsSet.add(r.getThreads());
            maxSpeedup = Math.max(maxSpeedup, r.getSpeedup());
        }
        List<Integer> listaThreads = new ArrayList<>(threadsSet);
        maxSpeedup = Math.max(maxSpeedup * 1.15, 4.0);

        int padEsq = 88;
        int padDir = 42;
        int padTopo = 86;
        int padBase = 64;
        int areaW = w - padEsq - padDir;
        int areaH = h - padTopo - padBase;

        boolean backupLog = this.usarEscalaLog;
        this.usarEscalaLog = false; // Speedup sempre em escala linear clara
        desenharGradeEixosY(g2, padEsq, padTopo, areaW, areaH, 0.0, maxSpeedup, "Speedup (Sp)");

        int nTh = listaThreads.size();

        // Linha de referência de Speedup = 1.0x (Serial baseline)
        double yBase1 = converterValorParaY(1.0, 0.0, maxSpeedup, padTopo, areaH);
        Stroke strokeAntigo = g2.getStroke();
        g2.setColor(new Color(148, 163, 184));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6f, 6f}, 0));
        g2.drawLine(padEsq, (int) yBase1, padEsq + areaW, (int) yBase1);
        g2.setStroke(strokeAntigo);

        for (int i = 0; i < nTh; i++) {
            double x = padEsq + (nTh == 1 ? areaW / 2.0 : (i * (double) areaW / (nTh - 1)));
            g2.setColor(new Color(226, 232, 240));
            g2.drawLine((int) x, padTopo, (int) x, padTopo + areaH);

            int th = listaThreads.get(i);
            String rotulo = th == 1 ? "1 (Serial)" : th + " Threads";
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(51, 65, 85));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rotulo, (int) x - fm.stringWidth(rotulo) / 2, padTopo + areaH + 22);
        }

        String tituloX = "Número de Threads (Núcleos Lógicos Alocados)";
        FontMetrics fmX = g2.getFontMetrics();
        g2.drawString(tituloX, padEsq + (areaW - fmX.stringWidth(tituloX)) / 2, padTopo + areaH + 44);

        for (String alg : CORES_ALGORITMOS.keySet()) {
            if (!incluirBubbleSort && alg.equals("Bubble Sort")) continue;
            Color cor = obterCorAlgoritmo(alg);

            List<Point.Double> coords = new ArrayList<>();
            List<ResultadoAmostra> amostrasAlg = new ArrayList<>();

            for (int i = 0; i < nTh; i++) {
                int th = listaThreads.get(i);
                ResultadoAmostra r = buscarResultado(filtrados, alg, th);
                if (r != null) {
                    double x = padEsq + (nTh == 1 ? areaW / 2.0 : (i * (double) areaW / (nTh - 1)));
                    double y = converterValorParaY(r.getSpeedup(), 0.0, maxSpeedup, padTopo, areaH);
                    coords.add(new Point.Double(x, y));
                    amostrasAlg.add(r);
                }
            }

            if (coords.size() >= 2) {
                Path2D caminho = new Path2D.Double();
                caminho.moveTo(coords.get(0).x, coords.get(0).y);
                for (int i = 1; i < coords.size(); i++) {
                    caminho.lineTo(coords.get(i).x, coords.get(i).y);
                }
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(caminho);
            }

            for (int i = 0; i < coords.size(); i++) {
                Point.Double pt = coords.get(i);
                ResultadoAmostra r = amostrasAlg.get(i);
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(pt.x - 5.5, pt.y - 5.5, 11, 11));
                g2.setColor(cor);
                g2.setStroke(new BasicStroke(2.4f));
                g2.draw(new Ellipse2D.Double(pt.x - 5.5, pt.y - 5.5, 11, 11));

                pontosInterativos.add(new PontoInterativo(
                        new ShapeWrapper(pt.x, pt.y, new Rectangle2D.Double(pt.x - 10, pt.y - 10, 20, 20)),
                        r.getAlgoritmo() + " (" + r.getThreads() + " Thread(s))",
                        criarDetalhesTooltip(r),
                        cor
                ));
            }
        }
        this.usarEscalaLog = backupLog;
    }

    private void desenharGraficoImpactoNatureza(Graphics2D g2, int w, int h) {
        List<ResultadoAmostra> filtrados = new ArrayList<>();
        for (ResultadoAmostra r : dados) {
            if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
            if (r.getTamanho() == filtroTamanho && r.getThreads() == filtroThreads) {
                filtrados.add(r);
            }
        }
        if (filtrados.isEmpty()) {
            for (ResultadoAmostra r : dados) {
                if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
                if (r.getThreads() == filtroThreads) {
                    filtrados.add(r);
                }
            }
        }

        String subtitulo = String.format("Tamanho N = %s | Threads = %d (%s)",
                formatarInteiro(obterTamanhoEfetivo(filtrados)),
                filtroThreads,
                filtroThreads <= 1 ? "Serial" : "Paralelo");
        desenharCabecalhoELegenda(g2, w, tipoGrafico.getTituloPadrao(), subtitulo, CORES_ALGORITMOS.keySet());

        if (filtrados.isEmpty()) return;

        List<TipoConjuntoDados> naturezasPresentes = new ArrayList<>();
        for (TipoConjuntoDados nat : TipoConjuntoDados.values()) {
            for (ResultadoAmostra r : filtrados) {
                if (r.getNaturezaDados() == nat && !naturezasPresentes.contains(nat)) {
                    naturezasPresentes.add(nat);
                }
            }
        }

        List<String> algoritmosAtivos = new ArrayList<>();
        for (String alg : CORES_ALGORITMOS.keySet()) {
            if (!incluirBubbleSort && alg.equals("Bubble Sort")) continue;
            algoritmosAtivos.add(alg);
        }

        int padEsq = 88;
        int padDir = 42;
        int padTopo = 86;
        int padBase = 64;
        int areaW = w - padEsq - padDir;
        int areaH = h - padTopo - padBase;

        double maxVal = 0.001;
        double minVal = Double.MAX_VALUE;
        for (ResultadoAmostra r : filtrados) {
            maxVal = Math.max(maxVal, r.getMediaMs() + r.getDesvioPadraoMs());
            minVal = Math.min(minVal, Math.max(0.001, r.getMediaMs()));
        }
        maxVal *= 1.15;

        desenharGradeEixosY(g2, padEsq, padTopo, areaW, areaH, minVal, maxVal, "Tempo Médio (ms)");

        int numGrupos = Math.max(1, naturezasPresentes.size());
        int numBarras = Math.max(1, algoritmosAtivos.size());
        double largGrupo = (double) areaW / numGrupos;
        double largBarra = Math.min(36.0, (largGrupo * 0.72) / numBarras);
        double espacoGrupo = largBarra * numBarras;

        for (int gIdx = 0; gIdx < naturezasPresentes.size(); gIdx++) {
            TipoConjuntoDados nat = naturezasPresentes.get(gIdx);
            double centroGrupoX = padEsq + (gIdx + 0.5) * largGrupo;
            double inicioGrupoX = centroGrupoX - espacoGrupo / 2.0;

            for (int bIdx = 0; bIdx < algoritmosAtivos.size(); bIdx++) {
                String alg = algoritmosAtivos.get(bIdx);
                Color cor = obterCorAlgoritmo(alg);
                ResultadoAmostra r = null;
                for (ResultadoAmostra item : filtrados) {
                    if (item.getAlgoritmo().equals(alg) && item.getNaturezaDados() == nat) {
                        r = item;
                        break;
                    }
                }
                if (r == null) continue;

                double xBarra = inicioGrupoX + bIdx * largBarra;
                double yTopo = converterValorParaY(r.getMediaMs(), minVal, maxVal, padTopo, areaH);
                double altBarra = Math.max(2.0, (padTopo + areaH) - yTopo);

                g2.setColor(cor);
                RoundRectangle2D rectBarra = new RoundRectangle2D.Double(
                        xBarra + 1, yTopo, largBarra - 2, altBarra, 6, 6);
                g2.fill(rectBarra);

                double cx = xBarra + largBarra / 2.0;
                pontosInterativos.add(new PontoInterativo(
                        new ShapeWrapper(cx, yTopo, new Rectangle2D.Double(xBarra, yTopo - 4, largBarra, altBarra + 8)),
                        r.getAlgoritmo() + " - " + nat.getRotulo(),
                        criarDetalhesTooltip(r),
                        cor
                ));
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.setColor(new Color(51, 65, 85));
            String rotuloNat = nat.getRotulo();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(rotuloNat, (int) centroGrupoX - fm.stringWidth(rotuloNat) / 2, padTopo + areaH + 22);
        }

        String tituloX = "Natureza / Distribuição Inicial do Conjunto de Dados";
        FontMetrics fmX = g2.getFontMetrics();
        g2.drawString(tituloX, padEsq + (areaW - fmX.stringWidth(tituloX)) / 2, padTopo + areaH + 44);
    }

    private void desenharGradeEixosY(
            Graphics2D g2, int padEsq, int padTopo, int areaW, int areaH,
            double minVal, double maxVal, String rotuloEixoY) {

        int divisoes = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i <= divisoes; i++) {
            double fracao = i / (double) divisoes;
            int y = padTopo + areaH - (int) (fracao * areaH);

            g2.setColor(new Color(226, 232, 240));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(padEsq, y, padEsq + areaW, y);

            double valorLinha;
            if (usarEscalaLog) {
                double logMin = Math.log10(Math.max(0.001, minVal * 0.8));
                double logMax = Math.log10(Math.max(0.01, maxVal));
                valorLinha = Math.pow(10, logMin + fracao * (logMax - logMin));
            } else {
                valorLinha = fracao * maxVal;
            }

            String textoValor = valorLinha >= 100
                    ? String.format(Locale.US, "%.0f", valorLinha)
                    : String.format(Locale.US, "%.2f", valorLinha);
            g2.setColor(new Color(71, 85, 105));
            g2.drawString(textoValor, padEsq - fm.stringWidth(textoValor) - 10, y + 4);
        }

        // Eixos principais
        g2.setColor(new Color(148, 163, 184));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(padEsq, padTopo, padEsq, padTopo + areaH);
        g2.drawLine(padEsq, padTopo + areaH, padEsq + areaW, padTopo + areaH);

        // Título rotacionado do eixo Y
        Graphics2D gRot = (Graphics2D) g2.create();
        gRot.setFont(new Font("SansSerif", Font.BOLD, 12));
        gRot.setColor(new Color(51, 65, 85));
        gRot.rotate(-Math.PI / 2);
        FontMetrics fmRot = gRot.getFontMetrics();
        int centroY = padTopo + areaH / 2;
        gRot.drawString(rotuloEixoY, -centroY - fmRot.stringWidth(rotuloEixoY) / 2, 32);
        gRot.dispose();
    }

    private double converterValorParaY(double valor, double minVal, double maxVal, int padTopo, int areaH) {
        double fracao;
        if (usarEscalaLog) {
            double logMin = Math.log10(Math.max(0.001, minVal * 0.8));
            double logMax = Math.log10(Math.max(0.01, maxVal));
            double logVal = Math.log10(Math.max(0.001, valor));
            fracao = (logVal - logMin) / Math.max(0.0001, logMax - logMin);
        } else {
            fracao = valor / Math.max(0.0001, maxVal);
        }
        fracao = Math.max(0.0, Math.min(1.0, fracao));
        return padTopo + areaH - fracao * areaH;
    }

    private void desenharTooltip(Graphics2D g2, PontoInterativo pt, Point mouse, int w, int h) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fmBold = g2.getFontMetrics();
        int largMax = fmBold.stringWidth(pt.titulo);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmPlain = g2.getFontMetrics();
        for (String linha : pt.linhasDetalhe) {
            largMax = Math.max(largMax, fmPlain.stringWidth(linha));
        }

        int boxW = largMax + 24;
        int boxH = 30 + pt.linhasDetalhe.length * 17;
        int x = mouse.x + 16;
        int y = mouse.y - boxH / 2;
        if (x + boxW > w - 20) x = mouse.x - boxW - 16;
        if (y < 20) y = 20;
        if (y + boxH > h - 20) y = h - boxH - 20;

        g2.setColor(new Color(15, 23, 42, 235));
        g2.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g2.setColor(pt.cor);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawRoundRect(x, y, boxW, boxH, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(Color.WHITE);
        g2.drawString(pt.titulo, x + 12, y + 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(new Color(226, 232, 240));
        for (int i = 0; i < pt.linhasDetalhe.length; i++) {
            g2.drawString(pt.linhasDetalhe[i], x + 12, y + 38 + i * 17);
        }
    }

    private List<ResultadoAmostra> filtrarPorTamanhoENatureza() {
        List<ResultadoAmostra> filtrados = new ArrayList<>();
        for (ResultadoAmostra r : dados) {
            if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
            if (r.getTamanho() == filtroTamanho
                    && r.getNaturezaDados() == filtroNatureza
                    && r.getIntervaloMax() == filtroIntervalo) {
                filtrados.add(r);
            }
        }
        if (filtrados.isEmpty()) {
            for (ResultadoAmostra r : dados) {
                if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
                if (r.getTamanho() == filtroTamanho && r.getNaturezaDados() == filtroNatureza) {
                    filtrados.add(r);
                }
            }
        }
        if (filtrados.isEmpty() && !dados.isEmpty()) {
            int tamFallback = dados.get(0).getTamanho();
            TipoConjuntoDados natFallback = dados.get(0).getNaturezaDados();
            for (ResultadoAmostra r : dados) {
                if (!incluirBubbleSort && r.getAlgoritmo().equals("Bubble Sort")) continue;
                if (r.getTamanho() == tamFallback && r.getNaturezaDados() == natFallback) {
                    filtrados.add(r);
                }
            }
        }
        return filtrados;
    }

    private int obterTamanhoEfetivo(List<ResultadoAmostra> lista) {
        return lista.isEmpty() ? filtroTamanho : lista.get(0).getTamanho();
    }

    private ResultadoAmostra buscarResultado(List<ResultadoAmostra> lista, String alg, int threads) {
        for (ResultadoAmostra r : lista) {
            if (r.getAlgoritmo().equals(alg) && r.getThreads() == threads) {
                return r;
            }
        }
        return null;
    }

    private static String[] criarDetalhesTooltip(ResultadoAmostra r) {
        return new String[]{
                String.format(Locale.US, "Tamanho (N): %s | Intervalo: [0..%s]",
                        formatarInteiro(r.getTamanho()), formatarInteiro(r.getIntervaloMax())),
                String.format(Locale.US, "Média (5 amostras): %.3f ms (±%.3f ms)",
                        r.getMediaMs(), r.getDesvioPadraoMs()),
                String.format(Locale.US, "Mediana: %.3f ms | Min: %.3f | Max: %.3f",
                        r.getMedianaMs(), r.getMinMs(), r.getMaxMs()),
                String.format(Locale.US, "Speedup: %.2fx | Eficiência: %.1f%%",
                        r.getSpeedup(), r.getEficiencia() * 100.0)
        };
    }

    private static Color obterCorAlgoritmo(String alg) {
        return CORES_ALGORITMOS.getOrDefault(alg, new Color(99, 102, 241));
    }

    private static String formatarInteiro(int valor) {
        return String.format(Locale.GERMAN, "%,d", valor);
    }
}
