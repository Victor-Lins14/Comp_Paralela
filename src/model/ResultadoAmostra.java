package model;

import java.util.Arrays;

/**
 * Armazena os resultados de uma bateria de testes (pelo menos 5 amostras)
 * para uma combinação específica de algoritmo, modo (Serial/Paralelo),
 * quantidade de threads, tamanho do vetor, natureza dos dados e intervalo máximo.
 */
public class ResultadoAmostra {
    private final String algoritmo;
    private final String modo; // "Serial" ou "Paralelo"
    private final int threads;
    private final int tamanho;
    private final TipoConjuntoDados naturezaDados;
    private final int intervaloMax;
    private final double[] amostrasMs;
    private final double mediaMs;
    private final double desvioPadraoMs;
    private final double medianaMs;
    private final double minMs;
    private final double maxMs;
    private double speedup;
    private double eficiencia;

    public ResultadoAmostra(
            String algoritmo,
            String modo,
            int threads,
            int tamanho,
            TipoConjuntoDados naturezaDados,
            int intervaloMax,
            double[] amostrasMs) {
        this.algoritmo = algoritmo;
        this.modo = modo;
        this.threads = threads;
        this.tamanho = tamanho;
        this.naturezaDados = naturezaDados;
        this.intervaloMax = intervaloMax;
        this.amostrasMs = Arrays.copyOf(amostrasMs, amostrasMs.length);

        double soma = 0.0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double t : this.amostrasMs) {
            soma += t;
            if (t < min) min = t;
            if (t > max) max = t;
        }
        this.mediaMs = soma / this.amostrasMs.length;
        this.minMs = min;
        this.maxMs = max;

        double somaQuadrados = 0.0;
        for (double t : this.amostrasMs) {
            double diff = t - this.mediaMs;
            somaQuadrados += diff * diff;
        }
        this.desvioPadraoMs = this.amostrasMs.length > 1
                ? Math.sqrt(somaQuadrados / (this.amostrasMs.length - 1))
                : 0.0;

        double[] ordenadas = Arrays.copyOf(this.amostrasMs, this.amostrasMs.length);
        Arrays.sort(ordenadas);
        int meio = ordenadas.length / 2;
        if (ordenadas.length % 2 == 0) {
            this.medianaMs = (ordenadas[meio - 1] + ordenadas[meio]) / 2.0;
        } else {
            this.medianaMs = ordenadas[meio];
        }

        this.speedup = 1.0;
        this.eficiencia = 1.0 / threads;
    }

    public void calcularSpeedup(double tempoSerialReferenciaMs) {
        if (this.mediaMs > 0 && tempoSerialReferenciaMs > 0) {
            this.speedup = tempoSerialReferenciaMs / this.mediaMs;
            this.eficiencia = this.speedup / this.threads;
        }
    }

    public String getAlgoritmo() {
        return algoritmo;
    }

    public String getModo() {
        return modo;
    }

    public int getThreads() {
        return threads;
    }

    public int getTamanho() {
        return tamanho;
    }

    public TipoConjuntoDados getNaturezaDados() {
        return naturezaDados;
    }

    public int getIntervaloMax() {
        return intervaloMax;
    }

    public double[] getAmostrasMs() {
        return Arrays.copyOf(amostrasMs, amostrasMs.length);
    }

    public double getMediaMs() {
        return mediaMs;
    }

    public double getDesvioPadraoMs() {
        return desvioPadraoMs;
    }

    public double getMedianaMs() {
        return medianaMs;
    }

    public double getMinMs() {
        return minMs;
    }

    public double getMaxMs() {
        return maxMs;
    }

    public double getSpeedup() {
        return speedup;
    }

    public double getEficiencia() {
        return eficiencia;
    }
}
