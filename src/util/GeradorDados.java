package util;

import model.TipoConjuntoDados;

import java.util.Random;

/**
 * Gerador determinístico de conjuntos de dados para os experimentos de desempenho.
 * Garante que todos os algoritmos (seriais e paralelos) recebam exatamente os mesmos
 * vetores de entrada para uma mesma semente de amostra, assegurando justiça estatística.
 */
public class GeradorDados {

    private static final long SEMENTE_BASE = 20261027L;

    private GeradorDados() {
    }

    /**
     * Gera um vetor de inteiros de acordo com os parâmetros experimentais.
     *
     * @param tamanho       quantidade de elementos no vetor (N)
     * @param natureza      distribuição/natureza do conjunto de dados
     * @param intervaloMax  limite superior dos valores gerados [0 .. intervaloMax]
     * @param indiceAmostra índice da amostra (1..K) para variação controlada da semente
     * @return vetor recém-alocado pronto para ordenação
     */
    public static int[] gerarVetor(int tamanho, TipoConjuntoDados natureza, int intervaloMax, int indiceAmostra) {
        int[] vetor = new int[tamanho];
        int limite = Math.max(2, intervaloMax);
        long semente = SEMENTE_BASE + (indiceAmostra * 10007L) + (tamanho * 31L) + natureza.ordinal();
        Random random = new Random(semente);

        switch (natureza) {
            case ALEATORIO:
                for (int i = 0; i < tamanho; i++) {
                    vetor[i] = random.nextInt(limite);
                }
                break;

            case INVERSAMENTE_ORDENADO:
                for (int i = 0; i < tamanho; i++) {
                    // Escala decrescente dentro do intervalo [0 .. limite]
                    long val = ((long) (tamanho - 1 - i) * limite) / Math.max(1, tamanho - 1);
                    vetor[i] = (int) val;
                }
                break;

            case QUASE_ORDENADO:
                for (int i = 0; i < tamanho; i++) {
                    long val = ((long) i * limite) / Math.max(1, tamanho - 1);
                    vetor[i] = (int) val;
                }
                // Perturba 5% dos pares (10% dos elementos) aleatoriamente
                int trocas = Math.max(1, tamanho / 20);
                for (int k = 0; k < trocas; k++) {
                    int idx1 = random.nextInt(tamanho);
                    int idx2 = random.nextInt(tamanho);
                    int tmp = vetor[idx1];
                    vetor[idx1] = vetor[idx2];
                    vetor[idx2] = tmp;
                }
                break;

            case POUCOS_UNICOS:
                // Concentra chaves em no máximo 32 valores distintos espalhados até o intervaloMax
                int numUnicos = Math.min(32, limite);
                int passo = Math.max(1, limite / numUnicos);
                for (int i = 0; i < tamanho; i++) {
                    vetor[i] = random.nextInt(numUnicos) * passo;
                }
                break;
        }
        return vetor;
    }

    /**
     * Verifica se o vetor encontra-se em ordem não-decrescente.
     */
    public static boolean estaOrdenado(int[] vetor) {
        for (int i = 1; i < vetor.length; i++) {
            if (vetor[i - 1] > vetor[i]) {
                return false;
            }
        }
        return true;
    }
}
