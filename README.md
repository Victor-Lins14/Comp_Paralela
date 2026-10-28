# Análise de Desempenho de Algoritmos de Busca e Ordenação em Ambientes Concorrentes e Paralelos: Um Estudo Comparativo em Java

**Universidade de Fortaleza (UNIFOR) — Centro de Ciências Tecnológicas (CCT) — Curso: Ciência da Computação**

Repositório oficial do trabalho experimental de **Programação Concorrente e Paralela**, contendo a implementação em Java (Serial e Paralela) de quatro algoritmos clássicos, framework de testes estatísticos com geração de arquivos CSV, **interface gráfica interativa de gráficos dinâmicos em Java Swing (Ponto Extra)**, **Jupyter Notebook** e relatório acadêmico no template da UNIFOR (`.docx` e `.md`).

---

## Estrutura do Projeto

- [`src/Main.java`](src/Main.java): Ponto de entrada principal (abre a interface gráfica em Java Swing ou executa o benchmark via console com `--cli`).
- [`src/algoritmos/`](src/algoritmos/):
  - [`AlgoritmoOrdenacao.java`](src/algoritmos/AlgoritmoOrdenacao.java): Interface comum para execuções seriais e paralelas.
  - [`QuickSort.java`](src/algoritmos/QuickSort.java): Quick Sort Serial e Paralelo (`ForkJoinPool` + `RecursiveAction`, pivô por mediana de três e particionamento de Hoare).
  - [`MergeSort.java`](src/algoritmos/MergeSort.java): Merge Sort Serial e Paralelo (`ForkJoinPool` + `RecursiveAction`, vetor auxiliar único pré-alocado).
  - [`CountingSort.java`](src/algoritmos/CountingSort.java): Counting Sort Serial e Paralelo (`ForkJoinPool` com histogramas locais por thread sem contenção de locks).
  - [`BubbleSort.java`](src/algoritmos/BubbleSort.java): Bubble Sort Serial e Paralelo (decomposição em blocos concorrentes + intercalação hierárquica e variante *Odd-Even Transposition Sort* com `Phaser`).
- [`src/benchmark/ExecutorBenchmark.java`](src/benchmark/ExecutorBenchmark.java): Framework de avaliação com *JVM Warmup*, variação de tamanhos ($N$), naturezas de dados, intervalos de valores ($k$), contagens de threads ($1, 2, 4, 8, 16$) e coleta de 5 amostras por configuração.
- [`src/gui/`](src/gui/):
  - [`BenchmarkSwingApp.java`](src/gui/BenchmarkSwingApp.java): Aplicação desktop interativa em Java Swing.
  - [`PainelGraficoDinamico.java`](src/gui/PainelGraficoDinamico.java): Componente customizado (`Graphics2D`) de gráficos dinâmicos com barras de erro, escala linear/logarítmica, tooltips interativos e exportação PNG.
- [`src/util/`](src/util/):
  - [`GeradorDados.java`](src/util/GeradorDados.java): Gerador determinístico de vetores (`ALEATORIO`, `INVERSAMENTE_ORDENADO`, `QUASE_ORDENADO`, `POUCOS_UNICOS`).
  - [`ExportadorCSV.java`](src/util/ExportadorCSV.java): Exportação e leitura dos arquivos CSV.
- [`resultados/`](resultados/):
  - `benchmark_resumo.csv`: Estatísticas consolidadas (Média, Desvio Padrão, Mediana, Mínimo, Máximo, Speedup e Eficiência).
  - `benchmark_amostras.csv`: Medições individuais das 5 amostras de cada teste.
  - `benchmark_intervalos.csv`: Avaliação do intervalo de valores $[0..k]$ em função do tempo.
  - `graficos_swing/` e `graficos_relatorio/`: Gráficos gerados em alta resolução (`.png`).
- [`analise_desempenho.ipynb`](analise_desempenho.ipynb): Jupyter Notebook com análises estatísticas e visualizações adicionais.
- [`Relatorio_Analise_Desempenho_UNIFOR.docx`](Relatorio_Analise_Desempenho_UNIFOR.docx) e [`RELATORIO_UNIFOR.md`](RELATORIO_UNIFOR.md): Relatório completo formatado segundo o modelo oficial da UNIFOR.

---

## Como Executar

### 1. Executar a Interface Gráfica de Gráficos Dinâmicos (Java Swing)
Basta executar a classe `Main` diretamente pela IDE (IntelliJ IDEA) ou via linha de comando:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
java -cp out Main
```

### 2. Executar toda a Bateria de Testes via Console (Geração de CSVs)
```powershell
java -cp out "-Djava.awt.headless=true" Main --cli
```

### 3. Atualizar Gráficos e Relatório Word (.docx)
```powershell
python scripts/gerar_graficos.py
python scripts/gerar_relatorio_unifor.py
```
