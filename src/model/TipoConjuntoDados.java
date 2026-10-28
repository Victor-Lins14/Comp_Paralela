package model;

/**
 * Define a natureza (distribuição inicial) dos conjuntos de dados de entrada
 * utilizados na avaliação de desempenho dos algoritmos.
 */
public enum TipoConjuntoDados {
    ALEATORIO("Aleatório (Uniforme)", "Elementos distribuídos uniformemente de forma pseudoaleatória"),
    INVERSAMENTE_ORDENADO("Inversamente Ordenado", "Elementos em ordem estritamente decrescente (pior caso clássico)"),
    QUASE_ORDENADO("Quase Ordenado (90%)", "Vetor ordenado com 10% de pares trocados aleatoriamente"),
    POUCOS_UNICOS("Alta Repetição (Poucos Únicos)", "Conjunto com alta concentração de chaves duplicadas");

    private final String rotulo;
    private final String descricao;

    TipoConjuntoDados(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
