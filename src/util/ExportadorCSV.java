package util;

import model.ResultadoAmostra;
import model.TipoConjuntoDados;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilitário responsável por persistir e carregar os resultados dos benchmarks
 * em arquivos CSV padronizados (UTF-8), permitindo visualização gráfica em Java Swing,
 * Jupyter Notebook ou planilhas externas.
 */
public class ExportadorCSV {

    private ExportadorCSV() {
    }

    /**
     * Salva o arquivo CSV detalhado contendo cada uma das amostras individuais.
     */
    public static void exportarAmostrasDetalhadas(List<ResultadoAmostra> resultados, File arquivoDestino) throws IOException {
        if (arquivoDestino.getParentFile() != null) {
            arquivoDestino.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivoDestino), StandardCharsets.UTF_8))) {
            writer.write("Algoritmo,Modo,Threads,Tamanho,NaturezaDados,IntervaloMax,Amostra,TempoMs,SpeedupConfig,EficienciaConfig");
            writer.newLine();

            for (ResultadoAmostra r : resultados) {
                double[] amostras = r.getAmostrasMs();
                for (int i = 0; i < amostras.length; i++) {
                    writer.write(String.format(Locale.US,
                            "%s,%s,%d,%d,%s,%d,%d,%.4f,%.4f,%.4f",
                            escaparCsv(r.getAlgoritmo()),
                            escaparCsv(r.getModo()),
                            r.getThreads(),
                            r.getTamanho(),
                            r.getNaturezaDados().name(),
                            r.getIntervaloMax(),
                            (i + 1),
                            amostras[i],
                            r.getSpeedup(),
                            r.getEficiencia()
                    ));
                    writer.newLine();
                }
            }
        }
    }

    /**
     * Salva o arquivo CSV consolidado com estatísticas (Média, Desvio Padrão, Mediana,
     * Mínimo, Máximo, Speedup, Eficiência) e as colunas das amostras individuais.
     */
    public static void exportarResumoEstatistico(List<ResultadoAmostra> resultados, File arquivoDestino) throws IOException {
        if (arquivoDestino.getParentFile() != null) {
            arquivoDestino.getParentFile().mkdirs();
        }
        int maxAmostras = 5;
        for (ResultadoAmostra r : resultados) {
            maxAmostras = Math.max(maxAmostras, r.getAmostrasMs().length);
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(arquivoDestino), StandardCharsets.UTF_8))) {
            StringBuilder cabecalho = new StringBuilder(
                    "Algoritmo,Modo,Threads,Tamanho,NaturezaDados,IntervaloMax,MediaMs,DesvioPadraoMs,MedianaMs,MinMs,MaxMs,Speedup,Eficiencia");
            for (int i = 1; i <= maxAmostras; i++) {
                cabecalho.append(",Amostra").append(i).append("Ms");
            }
            writer.write(cabecalho.toString());
            writer.newLine();

            for (ResultadoAmostra r : resultados) {
                StringBuilder linha = new StringBuilder(String.format(Locale.US,
                        "%s,%s,%d,%d,%s,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                        escaparCsv(r.getAlgoritmo()),
                        escaparCsv(r.getModo()),
                        r.getThreads(),
                        r.getTamanho(),
                        r.getNaturezaDados().name(),
                        r.getIntervaloMax(),
                        r.getMediaMs(),
                        r.getDesvioPadraoMs(),
                        r.getMedianaMs(),
                        r.getMinMs(),
                        r.getMaxMs(),
                        r.getSpeedup(),
                        r.getEficiencia()
                ));
                double[] am = r.getAmostrasMs();
                for (int i = 0; i < maxAmostras; i++) {
                    double val = i < am.length ? am[i] : r.getMediaMs();
                    linha.append(String.format(Locale.US, ",%.4f", val));
                }
                writer.write(linha.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Carrega resultados a partir de um arquivo CSV de resumo estatístico previamente salvo.
     */
    public static List<ResultadoAmostra> carregarResumoEstatistico(File arquivoOrigem) throws IOException {
        List<ResultadoAmostra> lista = new ArrayList<>();
        if (!arquivoOrigem.exists()) {
            return lista;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(arquivoOrigem), StandardCharsets.UTF_8))) {
            String cabecalho = reader.readLine();
            if (cabecalho == null) {
                return lista;
            }
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;
                String[] partes = linha.split(",");
                if (partes.length < 13) continue;

                String algoritmo = partes[0].trim();
                String modo = partes[1].trim();
                int threads = Integer.parseInt(partes[2].trim());
                int tamanho = Integer.parseInt(partes[3].trim());
                TipoConjuntoDados natureza = TipoConjuntoDados.valueOf(partes[4].trim());
                int intervaloMax = Integer.parseInt(partes[5].trim());
                double speedup = Double.parseDouble(partes[11].trim());
                double media = Double.parseDouble(partes[6].trim());

                int qtdAmostras = Math.max(1, partes.length - 13);
                double[] amostras = new double[qtdAmostras];
                if (partes.length > 13) {
                    for (int i = 13; i < partes.length; i++) {
                        amostras[i - 13] = Double.parseDouble(partes[i].trim());
                    }
                } else {
                    amostras[0] = media;
                }

                ResultadoAmostra item = new ResultadoAmostra(
                        algoritmo, modo, threads, tamanho, natureza, intervaloMax, amostras
                );
                if (speedup > 0) {
                    item.calcularSpeedup(media * speedup);
                }
                lista.add(item);
            }
        }
        return lista;
    }

    private static String escaparCsv(String texto) {
        if (texto.contains(",") || texto.contains("\"")) {
            return "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
