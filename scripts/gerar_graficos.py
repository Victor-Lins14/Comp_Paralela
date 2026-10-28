# -*- coding: utf-8 -*-
"""
Gera todos os gráficos científicos de alta resolução a partir dos arquivos CSV
produzidos pelo benchmark em Java, além do cabeçalho visual da UNIFOR.
"""

import os
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as patches

OUT_DIR = "resultados/graficos_relatorio"
os.makedirs(OUT_DIR, exist_ok=True)

plt.rcParams['font.sans-serif'] = 'Arial'
plt.rcParams['axes.edgecolor'] = '#CBD5E1'
plt.rcParams['axes.linewidth'] = 0.9

CORES = {
    'Quick Sort': '#2563EB',
    'Merge Sort': '#10B981',
    'Counting Sort': '#F59E0B',
    'Bubble Sort': '#DC2626'
}

ROTULOS_NATUREZA = {
    'ALEATORIO': 'Aleatório\n(Uniforme)',
    'INVERSAMENTE_ORDENADO': 'Inversamente\nOrdenado',
    'QUASE_ORDENADO': 'Quase Ordenado\n(90%)',
    'POUCOS_UNICOS': 'Alta Repetição\n(Poucos Únicos)'
}


def gerar_logo_unifor():
    """Gera o logotipo institucional da UNIFOR para o cabeçalho do documento Word (.docx)."""
    fig, ax = plt.subplots(figsize=(3.6, 1.3), dpi=200)
    ax.set_xlim(0, 3.6)
    ax.set_ylim(0, 1.3)
    ax.axis('off')

    azul_unifor = '#0047FF'
    # Desenha emblema octogonal/losango estilizado da UNIFOR
    poligono_ext = patches.Polygon(
        [[0.65, 1.18], [1.18, 0.65], [0.65, 0.12], [0.12, 0.65]],
        closed=True, facecolor='none', edgecolor=azul_unifor, linewidth=5.0, joinstyle='round'
    )
    ax.add_patch(poligono_ext)

    # Arcos internos característicos da UNIFOR
    for r, y_off in [(0.28, 0.56), (0.19, 0.50), (0.10, 0.45)]:
        arco = patches.Arc((0.65, y_off), r * 2, r * 2, angle=0, theta1=0, theta2=180,
                           edgecolor=azul_unifor, linewidth=3.5)
        ax.add_patch(arco)

    # Topo e base do emblema
    ax.plot([0.32, 0.65, 0.98], [1.02, 1.22, 1.02], color=azul_unifor, linewidth=4.5, solid_capstyle='round')
    ax.plot([0.32, 0.65, 0.98], [0.28, 0.08, 0.28], color=azul_unifor, linewidth=4.5, solid_capstyle='round')

    # Texto "Unifor"
    ax.text(1.38, 0.62, "Unifor", fontsize=34, fontweight='bold', color=azul_unifor,
            va='center', ha='left', family='Arial')

    caminho = os.path.join(OUT_DIR, "unifor_logo.png")
    plt.tight_layout(pad=0.05)
    plt.savefig(caminho, dpi=200, transparent=True)
    plt.close(fig)
    return caminho


def gerar_graficos():
    df_resumo = pd.read_csv("resultados/benchmark_resumo.csv")
    df_amostras = pd.read_csv("resultados/benchmark_amostras.csv")
    df_intervalos = pd.read_csv("resultados/benchmark_intervalos.csv")

    # -------------------------------------------------------------------------
    # FIGURA 1: Escalabilidade por Tamanho do Vetor (N) - Serial vs Paralelo (8 Threads)
    # -------------------------------------------------------------------------
    fig, axes = plt.subplots(1, 2, figsize=(12.5, 5.0), dpi=180)
    for idx, thr in enumerate([1, 8]):
        ax = axes[idx]
        sub = df_resumo[(df_resumo['NaturezaDados'] == 'ALEATORIO') & (df_resumo['Threads'] == thr)]
        for alg in ['Quick Sort', 'Merge Sort', 'Counting Sort', 'Bubble Sort']:
            g = sub[sub['Algoritmo'] == alg].sort_values('Tamanho')
            if g.empty:
                continue
            ax.errorbar(
                g['Tamanho'], g['MediaMs'], yerr=g['DesvioPadraoMs'],
                label=alg, color=CORES[alg], marker='o', linewidth=2.2, markersize=6, capsize=4
            )
        modo_txt = "Execução Serial (1 Thread)" if thr == 1 else f"Execução Paralela ({thr} Threads)"
        ax.set_title(f"{modo_txt} — Natureza Aleatória", fontsize=11.5, fontweight='bold', pad=10)
        ax.set_xlabel("Tamanho do Conjunto de Dados (N elementos)", fontsize=10)
        ax.set_ylabel("Tempo Médio de Execução (ms) [Escala Log10]", fontsize=10)
        ax.set_xscale('log')
        ax.set_yscale('log')
        ax.grid(True, which='both', linestyle='--', alpha=0.4)
        ax.legend(frameon=True, facecolor='white', fontsize=9)

    plt.suptitle("Figura 1 — Escalabilidade do Tempo de Execução em Função do Tamanho da Entrada (N)",
                 fontsize=13, fontweight='bold', y=1.02)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig1_escalabilidade_tamanho.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)

    # -------------------------------------------------------------------------
    # FIGURA 2: Tempo Médio e Desvio Padrão por Número de Threads (Subplots 2x2)
    # -------------------------------------------------------------------------
    fig, axes = plt.subplots(2, 2, figsize=(12.5, 8.2), dpi=180)
    configs_painel = [
        ('Quick Sort', 1000000, axes[0, 0]),
        ('Merge Sort', 1000000, axes[0, 1]),
        ('Counting Sort', 1000000, axes[1, 0]),
        ('Bubble Sort', 50000, axes[1, 1]),
    ]
    for alg, tam, ax in configs_painel:
        sub = df_resumo[(df_resumo['NaturezaDados'] == 'ALEATORIO') &
                        (df_resumo['Algoritmo'] == alg) &
                        (df_resumo['Tamanho'] == tam)].sort_values('Threads')
        x_pos = np.arange(len(sub))
        barras = ax.bar(
            x_pos, sub['MediaMs'], yerr=sub['DesvioPadraoMs'],
            color=CORES[alg], alpha=0.88, capsize=5, width=0.55,
            edgecolor='#1E293B', linewidth=0.8
        )
        ax.set_xticks(x_pos)
        ax.set_xticklabels([f"{t}\n(Serial)" if t == 1 else f"{t}\nThreads" for t in sub['Threads']], fontsize=9)
        ax.set_title(f"{alg} (N = {tam:,} elementos)".replace(",", "."), fontsize=11, fontweight='bold')
        ax.set_ylabel("Tempo Médio ± Desvio Padrão (ms)", fontsize=9.5)
        ax.grid(axis='y', linestyle='--', alpha=0.45)

        max_y = (sub['MediaMs'] + sub['DesvioPadraoMs']).max()
        ax.set_ylim(0, max_y * 1.28)
        for rect, media, std, spd in zip(barras, sub['MediaMs'], sub['DesvioPadraoMs'], sub['Speedup']):
            altura = rect.get_height() + std
            ax.text(rect.get_x() + rect.get_width() / 2.0, altura + max_y * 0.025,
                    f"{media:.2f} ms\n({spd:.2f}x)", ha='center', va='bottom', fontsize=8, fontweight='bold')

    plt.suptitle("Figura 2 — Tempo Médio de Execução e Desvio Padrão (5 Amostras) por Número de Threads",
                 fontsize=13, fontweight='bold', y=1.01)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig2_tempo_por_threads.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)

    # -------------------------------------------------------------------------
    # FIGURA 3: Speedup (Sp) e Eficiência Paralela (Ep) × Número de Threads
    # -------------------------------------------------------------------------
    fig, axes = plt.subplots(1, 2, figsize=(12.5, 5.0), dpi=180)

    # Painel A: Speedup para N = 1.000.000 (Quick, Merge, Counting) e N = 100.000 (Quick, Merge)
    ax1 = axes[0]
    sub_1m = df_resumo[(df_resumo['NaturezaDados'] == 'ALEATORIO') & (df_resumo['Tamanho'] == 1000000)]
    for alg in ['Quick Sort', 'Merge Sort', 'Counting Sort']:
        g = sub_1m[sub_1m['Algoritmo'] == alg].sort_values('Threads')
        ax1.plot(g['Threads'], g['Speedup'], label=f"{alg} (N=1.000.000)",
                 color=CORES[alg], marker='o', linewidth=2.4, markersize=6)

    ax1.axhline(1.0, color='#64748B', linestyle='--', linewidth=1.2, label='Referência Serial (1.0x)')
    ax1.set_title("Speedup (Sp = T1 / Tp) — Algoritmos Rápidos (N = 1.000.000)", fontsize=11, fontweight='bold')
    ax1.set_xlabel("Número de Threads (p)", fontsize=10)
    ax1.set_ylabel("Speedup (Sp)", fontsize=10)
    ax1.set_xticks([1, 2, 4, 8, 16])
    ax1.grid(True, linestyle='--', alpha=0.45)
    ax1.legend(fontsize=9)

    # Painel B: Eficiência Paralela (%) para N = 1.000.000 + Speedup de Bloco do Bubble Sort (N = 100.000)
    ax2 = axes[1]
    for alg in ['Quick Sort', 'Merge Sort', 'Counting Sort']:
        g = sub_1m[sub_1m['Algoritmo'] == alg].sort_values('Threads')
        ax2.plot(g['Threads'], g['Eficiencia'] * 100.0, label=f"Eficiência {alg}",
                 color=CORES[alg], marker='s', linewidth=2.2, markersize=6)

    ax2.set_title("Eficiência Paralela Ep = (Sp / p) × 100% (N = 1.000.000)", fontsize=11, fontweight='bold')
    ax2.set_xlabel("Número de Threads (p)", fontsize=10)
    ax2.set_ylabel("Eficiência por Núcleo (%)", fontsize=10)
    ax2.set_xticks([1, 2, 4, 8, 16])
    ax2.set_ylim(0, 110)
    ax2.grid(True, linestyle='--', alpha=0.45)
    ax2.legend(fontsize=9)

    plt.suptitle("Figura 3 — Análise de Escalabilidade Forte: Speedup (Sp) e Eficiência Paralela (Ep)",
                 fontsize=13, fontweight='bold', y=1.02)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig3_speedup_eficiencia.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)

    # -------------------------------------------------------------------------
    # FIGURA 4: Impacto da Natureza dos Conjuntos de Dados (N = 100.000)
    # -------------------------------------------------------------------------
    fig, axes = plt.subplots(1, 2, figsize=(12.5, 5.2), dpi=180)
    ordem_nat = ['ALEATORIO', 'INVERSAMENTE_ORDENADO', 'QUASE_ORDENADO', 'POUCOS_UNICOS']

    for idx, thr in enumerate([1, 8]):
        ax = axes[idx]
        sub = df_resumo[(df_resumo['Tamanho'] == 100000) &
                        (df_resumo['Threads'] == thr) &
                        (df_resumo['Algoritmo'] != 'Bubble Sort')]
        x = np.arange(len(ordem_nat))
        larg = 0.25
        for b_idx, alg in enumerate(['Quick Sort', 'Merge Sort', 'Counting Sort']):
            tempos = []
            erros = []
            for nat in ordem_nat:
                linha = sub[(sub['Algoritmo'] == alg) & (sub['NaturezaDados'] == nat)]
                tempos.append(linha['MediaMs'].values[0] if not linha.empty else 0)
                erros.append(linha['DesvioPadraoMs'].values[0] if not linha.empty else 0)
            ax.bar(x + (b_idx - 1) * larg, tempos, width=larg, yerr=erros,
                   label=alg, color=CORES[alg], capsize=3, alpha=0.9)

        ax.set_xticks(x)
        ax.set_xticklabels([ROTULOS_NATUREZA[n] for n in ordem_nat], fontsize=8.5)
        modo_str = "Serial (1 Thread)" if thr == 1 else "Paralelo (8 Threads)"
        ax.set_title(f"Impacto da Natureza dos Dados — {modo_str} (N = 100.000)", fontsize=11, fontweight='bold')
        ax.set_ylabel("Tempo Médio de Execução (ms)", fontsize=10)
        ax.grid(axis='y', linestyle='--', alpha=0.45)
        ax.legend(fontsize=9)

    plt.suptitle("Figura 4 — Comportamento dos Algoritmos sob Diferentes Naturezas de Dados de Entrada",
                 fontsize=13, fontweight='bold', y=1.02)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig4_impacto_natureza.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)

    # -------------------------------------------------------------------------
    # FIGURA 5: Impacto do Intervalo de Valores [0..k] em Função do Tempo (N = 500.000)
    # -------------------------------------------------------------------------
    fig, ax = plt.subplots(figsize=(9.5, 4.8), dpi=180)
    for alg in ['Quick Sort', 'Merge Sort', 'Counting Sort']:
        g_ser = df_intervalos[(df_intervalos['Algoritmo'] == alg) & (df_intervalos['Threads'] == 1)].sort_values('IntervaloMax')
        g_par = df_intervalos[(df_intervalos['Algoritmo'] == alg) & (df_intervalos['Threads'] == 8)].sort_values('IntervaloMax')
        ax.plot(g_ser['IntervaloMax'], g_ser['MediaMs'], label=f"{alg} — Serial (1 Thread)",
                color=CORES[alg], marker='o', linewidth=2.3)
        ax.plot(g_par['IntervaloMax'], g_par['MediaMs'], label=f"{alg} — Paralelo (8 Threads)",
                color=CORES[alg], marker='^', linestyle='--', linewidth=2.0)

    ax.set_xscale('log')
    ax.set_yscale('log')
    ax.set_title("Figura 5 — Tempo de Execução em Função da Amplitude do Intervalo de Valores [0..k] (N = 500.000)",
                 fontsize=11.5, fontweight='bold', pad=10)
    ax.set_xlabel("Limite Superior do Intervalo de Valores (k) [Escala Log10]", fontsize=10)
    ax.set_ylabel("Tempo Médio de Execução (ms) [Escala Log10]", fontsize=10)
    ax.grid(True, which='both', linestyle='--', alpha=0.45)
    ax.legend(ncol=2, fontsize=8.8, frameon=True, facecolor='white')
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig5_impacto_intervalo_valores.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)

    # -------------------------------------------------------------------------
    # FIGURA 6: Boxplot de Estabilidade das 5 Amostras (N = 1.000.000, Serial vs 8 Threads)
    # -------------------------------------------------------------------------
    fig, axes = plt.subplots(1, 2, figsize=(12.0, 4.8), dpi=180)
    sub_am = df_amostras[(df_amostras['NaturezaDados'] == 'ALEATORIO') &
                         (df_amostras['Tamanho'] == 1000000) &
                         (df_amostras['Algoritmo'].isin(['Quick Sort', 'Merge Sort', 'Counting Sort']))]

    for idx, thr in enumerate([1, 8]):
        ax = axes[idx]
        algs_ordem = ['Quick Sort', 'Merge Sort', 'Counting Sort']
        dados_box = [
            sub_am[(sub_am['Algoritmo'] == a) & (sub_am['Threads'] == thr)]['TempoMs'].values
            for a in algs_ordem
        ]
        bp = ax.boxplot(dados_box, tick_labels=algs_ordem, patch_artist=True, widths=0.45)
        for patch, a in zip(bp['boxes'], algs_ordem):
            patch.set_facecolor(CORES[a])
            patch.set_alpha(0.75)
        for median in bp['medians']:
            median.set_color('#0F172A')
            median.set_linewidth(2.0)

        # Plota pontos individuais das 5 amostras
        for i, a in enumerate(algs_ordem, start=1):
            y_vals = dados_box[i - 1]
            x_vals = np.random.normal(i, 0.04, size=len(y_vals))
            ax.scatter(x_vals, y_vals, color='#0F172A', s=22, zorder=5, alpha=0.85)

        modo_str = "Serial (1 Thread)" if thr == 1 else "Paralelo (8 Threads)"
        ax.set_title(f"Dispersão das 5 Amostras — {modo_str} (N = 1.000.000)", fontsize=11, fontweight='bold')
        ax.set_ylabel("Tempo de Execução (ms)", fontsize=10)
        ax.grid(axis='y', linestyle='--', alpha=0.45)

    plt.suptitle("Figura 6 — Análise Estatística de Dispersão das 5 Amostras por Execução (Boxplot)",
                 fontsize=13, fontweight='bold', y=1.02)
    plt.tight_layout()
    plt.savefig(os.path.join(OUT_DIR, "fig6_estabilidade_amostras_boxplot.png"), bbox_inches='tight', dpi=180)
    plt.close(fig)


if __name__ == "__main__":
    gerar_logo_unifor()
    gerar_graficos()
    print("Gráficos do relatório e logo UNIFOR gerados com sucesso em:", OUT_DIR)
