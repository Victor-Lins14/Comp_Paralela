# -*- coding: utf-8 -*-
"""
Gera o Relatório Acadêmico Completo no padrão oficial da UNIFOR em dois formatos:
1. Relatorio_Analise_Desempenho_UNIFOR.docx (Word nativo com Arial 11/14, espaçamento 1,5,
   cabeçalho UNIFOR com logo, tabelas estatísticas e figuras embutidas)
2. RELATORIO_UNIFOR.md (Versão Markdown estruturada com links para os gráficos e códigos)
"""

import os
import zipfile
import struct
from xml.sax.saxutils import escape
import pandas as pd


def obter_dimensoes_png(caminho_png):
    """Lê largura e altura em pixels diretamente do cabeçalho IHDR do arquivo PNG."""
    with open(caminho_png, 'rb') as f:
        assinatura = f.read(8)
        if assinatura != b'\x89PNG\r\n\x1a\n':
            return 1200, 600
        _ = f.read(4)  # chunk length
        tipo = f.read(4)
        if tipo == b'IHDR':
            largura, altura = struct.unpack('>II', f.read(8))
            return largura, altura
    return 1200, 600


class ConstrutorDocxUnifor:
    """Construtor OpenXML (.docx) sem dependências externas, formatado na norma UNIFOR."""

    def __init__(self):
        self.elementos_corpo = []
        self.imagens = []  # lista de (rId, nome_arquivo_interno, caminho_origem)
        self.links = []    # lista de (rId, url)
        self.prox_rid = 10
        self.prox_docpr_id = 1

    def registrar_imagem(self, caminho_origem):
        rid = f"rId{self.prox_rid}"
        self.prox_rid += 1
        nome_interno = f"image_{len(self.imagens) + 1}.png"
        self.imagens.append((rid, nome_interno, caminho_origem))
        return rid, nome_interno

    def registrar_hyperlink(self, url):
        rid = f"rId{self.prox_rid}"
        self.prox_rid += 1
        self.links.append((rid, url))
        return rid

    def adicionar_cabecalho_unifor(self, caminho_logo):
        rid_logo, _ = self.registrar_imagem(caminho_logo)
        # Dimensões do logo em EMUs (1 inch = 914400 EMUs)
        cx = int(2.15 * 914400)
        cy = int(0.78 * 914400)
        docpr = self.prox_docpr_id
        self.prox_docpr_id += 1

        xml_tabela = f"""
        <w:tbl>
          <w:tblPr>
            <w:tblW w:w="9360" w:type="dxa"/>
            <w:tblBorders>
              <w:top w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
              <w:left w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
              <w:bottom w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
              <w:right w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
              <w:insideH w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
              <w:insideV w:val="single" w:sz="6" w:space="0" w:color="94A3B8"/>
            </w:tblBorders>
          </w:tblPr>
          <w:tblGrid>
            <w:gridCol w:w="3100"/>
            <w:gridCol w:w="6260"/>
          </w:tblGrid>
          <w:tr>
            <w:tc>
              <w:tcPr>
                <w:tcW w:w="3100" w:type="dxa"/>
                <w:vAlign w:val="center"/>
              </w:tcPr>
              <w:p>
                <w:pPr><w:jc w:val="center"/></w:pPr>
                <w:r>
                  <w:drawing>
                    <wp:inline distT="0" distB="0" distL="0" distR="0">
                      <wp:extent cx="{cx}" cy="{cy}"/>
                      <wp:docPr id="{docpr}" name="Logo UNIFOR"/>
                      <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                        <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                          <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                            <pic:nvPicPr>
                              <pic:cNvPr id="{docpr}" name="unifor_logo.png"/>
                              <pic:cNvPicPr/>
                            </pic:nvPicPr>
                            <pic:blipFill>
                              <a:blip r:embed="{rid_logo}"/>
                              <a:stretch><a:fillRect/></a:stretch>
                            </pic:blipFill>
                            <pic:spPr>
                              <a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>
                              <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                            </pic:spPr>
                          </pic:pic>
                        </a:graphicData>
                      </a:graphic>
                    </wp:inline>
                  </w:drawing>
                </w:r>
              </w:p>
            </w:tc>
            <w:tc>
              <w:tcPr>
                <w:tcW w:w="6260" w:type="dxa"/>
                <w:vAlign w:val="center"/>
              </w:tcPr>
              <w:p>
                <w:pPr><w:spacing w:after="40" w:line="280" w:lineRule="auto"/></w:pPr>
                <w:r>
                  <w:rPr>
                    <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                    <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
                  </w:rPr>
                  <w:t>UNIVERSIDADE DE FORTALEZA</w:t>
                </w:r>
              </w:p>
              <w:p>
                <w:pPr><w:spacing w:after="40" w:line="280" w:lineRule="auto"/></w:pPr>
                <w:r>
                  <w:rPr>
                    <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                    <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
                  </w:rPr>
                  <w:t>CENTRO DE CIÊNCIAS TECNOLÓGICAS</w:t>
                </w:r>
              </w:p>
              <w:p>
                <w:pPr><w:spacing w:after="0" w:line="280" w:lineRule="auto"/></w:pPr>
                <w:r>
                  <w:rPr>
                    <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                    <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
                  </w:rPr>
                  <w:t>CURSO: CIÊNCIA DA COMPUTAÇÃO</w:t>
                </w:r>
              </w:p>
            </w:tc>
          </w:tr>
        </w:tbl>
        """
        self.elementos_corpo.append(xml_tabela)

    def adicionar_titulo_principal(self, texto):
        """Arial, 14pt (sz=28), Negrito, Caixa Alta, Centralizado."""
        t = escape(texto.upper())
        xml = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="240" w:after="180" w:line="360" w:lineRule="auto"/>
            <w:jc w:val="center"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
              <w:b/><w:sz w:val="28"/><w:szCs w:val="28"/>
            </w:rPr>
            <w:t>{t}</w:t>
          </w:r>
        </w:p>
        """
        self.elementos_corpo.append(xml)

    def adicionar_titulo_secao(self, texto):
        """Título de seção em Arial 12pt Negrito, espaçamento 1,5."""
        t = escape(texto)
        xml = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="240" w:after="120" w:line="360" w:lineRule="auto"/>
            <w:keepNext/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
              <w:b/><w:sz w:val="24"/><w:szCs w:val="24"/>
            </w:rPr>
            <w:t>{t}</w:t>
          </w:r>
        </w:p>
        """
        self.elementos_corpo.append(xml)

    def adicionar_subtitulo_secao(self, texto):
        """Subtítulo em Arial 11pt Negrito, espaçamento 1,5."""
        t = escape(texto)
        xml = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="180" w:after="80" w:line="360" w:lineRule="auto"/>
            <w:keepNext/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
              <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
            </w:rPr>
            <w:t>{t}</w:t>
          </w:r>
        </w:p>
        """
        self.elementos_corpo.append(xml)

    def adicionar_paragrafo(self, texto, negrito_prefixo=None, alinhamento="both", recuo_primeira_linha=True):
        """Arial 11pt (sz=22), Espaçamento 1,5 (line=360)."""
        ind_xml = '<w:ind w:firstLine="708"/>' if recuo_primeira_linha and alinhamento == "both" else ''
        runs_xml = ""
        if negrito_prefixo:
            runs_xml += f"""
            <w:r>
              <w:rPr>
                <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
              </w:rPr>
              <w:t xml:space="preserve">{escape(negrito_prefixo)}</w:t>
            </w:r>
            """
        runs_xml += f"""
        <w:r>
          <w:rPr>
            <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
            <w:sz w:val="22"/><w:szCs w:val="22"/>
          </w:rPr>
          <w:t xml:space="preserve">{escape(texto)}</w:t>
        </w:r>
        """
        xml = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="0" w:after="120" w:line="360" w:lineRule="auto"/>
            {ind_xml}
            <w:jc w:val="{alinhamento}"/>
          </w:pPr>
          {runs_xml}
        </w:p>
        """
        self.elementos_corpo.append(xml)

    def adicionar_paragrafo_hyperlink(self, prefixo, texto_link, url):
        rid = self.registrar_hyperlink(url)
        xml = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="80" w:after="120" w:line="360" w:lineRule="auto"/>
            <w:jc w:val="both"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
              <w:b/><w:sz w:val="22"/><w:szCs w:val="22"/>
            </w:rPr>
            <w:t xml:space="preserve">{escape(prefixo)}</w:t>
          </w:r>
          <w:hyperlink r:id="{rid}" w:history="1">
            <w:r>
              <w:rPr>
                <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                <w:color w:val="2563EB"/><w:u w:val="single"/>
                <w:sz w:val="22"/><w:szCs w:val="22"/>
              </w:rPr>
              <w:t>{escape(texto_link)}</w:t>
            </w:r>
          </w:hyperlink>
        </w:p>
        """
        self.elementos_corpo.append(xml)

    def adicionar_bloco_codigo(self, codigo):
        linhas = codigo.strip("\n").split("\n")
        for linha in linhas:
            xml = f"""
            <w:p>
              <w:pPr>
                <w:shd w:val="clear" w:color="auto" w:fill="F8FAFC"/>
                <w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>
                <w:ind w:left="240" w:right="240"/>
              </w:pPr>
              <w:r>
                <w:rPr>
                  <w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:cs="Consolas"/>
                  <w:sz w:val="17"/><w:szCs w:val="17"/>
                  <w:color w:val="0F172A"/>
                </w:rPr>
                <w:t xml:space="preserve">{escape(linha)}</w:t>
              </w:r>
            </w:p>
            """
            self.elementos_corpo.append(xml)

    def adicionar_figura(self, caminho_png, legenda, largura_inches=6.1):
        if not os.path.exists(caminho_png):
            return
        rid, nome_int = self.registrar_imagem(caminho_png)
        larg_px, alt_px = obter_dimensoes_png(caminho_png)
        proporcao = alt_px / max(1.0, float(larg_px))
        cx = int(largura_inches * 914400)
        cy = int(largura_inches * proporcao * 914400)
        docpr = self.prox_docpr_id
        self.prox_docpr_id += 1

        xml_img = f"""
        <w:p>
          <w:pPr>
            <w:spacing w:before="160" w:after="60" w:line="240" w:lineRule="auto"/>
            <w:jc w:val="center"/>
          </w:pPr>
          <w:r>
            <w:drawing>
              <wp:inline distT="0" distB="0" distL="0" distR="0">
                <wp:extent cx="{cx}" cy="{cy}"/>
                <wp:docPr id="{docpr}" name="{nome_int}"/>
                <a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
                  <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                    <pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
                      <pic:nvPicPr>
                        <pic:cNvPr id="{docpr}" name="{nome_int}"/>
                        <pic:cNvPicPr/>
                      </pic:nvPicPr>
                      <pic:blipFill>
                        <a:blip r:embed="{rid}"/>
                        <a:stretch><a:fillRect/></a:stretch>
                      </pic:blipFill>
                      <pic:spPr>
                        <a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>
                        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                      </pic:spPr>
                    </pic:pic>
                  </a:graphicData>
                </a:graphic>
              </wp:inline>
            </w:drawing>
          </w:r>
        </w:p>
        <w:p>
          <w:pPr>
            <w:spacing w:before="0" w:after="160" w:line="280" w:lineRule="auto"/>
            <w:jc w:val="center"/>
          </w:pPr>
          <w:r>
            <w:rPr>
              <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
              <w:i/><w:sz w:val="19"/><w:szCs w:val="19"/>
              <w:color w:val="334155"/>
            </w:rPr>
            <w:t>{escape(legenda)}</w:t>
          </w:r>
        </w:p>
        """
        self.elementos_corpo.append(xml_img)

    def adicionar_tabela(self, cabecalhos, linhas, titulo_tabela=None):
        if titulo_tabela:
            self.elementos_corpo.append(f"""
            <w:p>
              <w:pPr>
                <w:spacing w:before="160" w:after="60" w:line="280" w:lineRule="auto"/>
                <w:jc w:val="center"/>
              </w:pPr>
              <w:r>
                <w:rPr>
                  <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                  <w:b/><w:sz w:val="20"/><w:szCs w:val="20"/>
                </w:rPr>
                <w:t>{escape(titulo_tabela)}</w:t>
              </w:r>
            </w:p>
            """)

        num_cols = len(cabecalhos)
        larg_col = int(9360 / max(1, num_cols))
        grid_xml = "".join([f'<w:gridCol w:w="{larg_col}"/>' for _ in cabecalhos])

        # Linha de cabeçalho
        celulas_cab = ""
        for h in cabecalhos:
            celulas_cab += f"""
            <w:tc>
              <w:tcPr>
                <w:tcW w:w="{larg_col}" w:type="dxa"/>
                <w:shd w:val="clear" w:color="auto" w:fill="1E293B"/>
                <w:vAlign w:val="center"/>
              </w:tcPr>
              <w:p>
                <w:pPr><w:spacing w:before="40" w:after="40"/><w:jc w:val="center"/></w:pPr>
                <w:r>
                  <w:rPr>
                    <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                    <w:b/><w:color w:val="FFFFFF"/><w:sz w:val="18"/><w:szCs w:val="18"/>
                  </w:rPr>
                  <w:t>{escape(str(h))}</w:t>
                </w:r>
              </w:p>
            </w:tc>
            """
        tr_xml = f"<w:tr>{celulas_cab}</w:tr>"

        for idx_l, linha in enumerate(linhas):
            cor_fundo = "FFFFFF" if idx_l % 2 == 0 else "F8FAFC"
            celulas_l = ""
            for valor in linha:
                celulas_l += f"""
                <w:tc>
                  <w:tcPr>
                    <w:tcW w:w="{larg_col}" w:type="dxa"/>
                    <w:shd w:val="clear" w:color="auto" w:fill="{cor_fundo}"/>
                    <w:vAlign w:val="center"/>
                  </w:tcPr>
                  <w:p>
                    <w:pPr><w:spacing w:before="30" w:after="30"/><w:jc w:val="center"/></w:pPr>
                    <w:r>
                      <w:rPr>
                        <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
                        <w:sz w:val="18"/><w:szCs w:val="18"/>
                      </w:rPr>
                      <w:t>{escape(str(valor))}</w:t>
                    </w:r>
                  </w:p>
                </w:tc>
                """
            tr_xml += f"<w:tr>{celulas_l}</w:tr>"

        xml_tbl = f"""
        <w:tbl>
          <w:tblPr>
            <w:tblW w:w="9360" w:type="dxa"/>
            <w:tblBorders>
              <w:top w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
              <w:left w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
              <w:bottom w:val="single" w:sz="6" w:space="0" w:color="64748B"/>
              <w:right w:val="single" w:sz="4" w:space="0" w:color="CBD5E1"/>
              <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
              <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
            </w:tblBorders>
          </w:tblPr>
          <w:tblGrid>{grid_xml}</w:tblGrid>
          {tr_xml}
        </w:tbl>
        <w:p><w:pPr><w:spacing w:before="0" w:after="100"/></w:pPr></w:p>
        """
        self.elementos_corpo.append(xml_tbl)

    def salvar(self, caminho_docx):
        content_types = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

        rels_raiz = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        rels_doc_itens = [
            '<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
        ]
        for rid, nome_int, _ in self.imagens:
            rels_doc_itens.append(
                f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/{nome_int}"/>'
            )
        for rid, url in self.links:
            rels_doc_itens.append(
                f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="{escape(url)}" TargetMode="External"/>'
            )

        rels_doc = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  {"".join(rels_doc_itens)}
</Relationships>"""

        styles_xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Arial" w:hAnsi="Arial" w:cs="Arial"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
        <w:lang w:val="pt-BR"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>"""

        corpo_str = "\n".join(self.elementos_corpo)
        # Margens padrão ABNT/UNIFOR: 2.5cm (~1417 dxa) laterais e superior/inferior
        document_xml = f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
  <w:body>
    {corpo_str}
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1417" w:right="1270" w:bottom="1417" w:left="1270" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>"""

        with zipfile.ZipFile(caminho_docx, 'w', zipfile.ZIP_DEFLATED) as zf:
            zf.writestr("[Content_Types].xml", content_types)
            zf.writestr("_rels/.rels", rels_raiz)
            zf.writestr("word/_rels/document.xml.rels", rels_doc)
            zf.writestr("word/styles.xml", styles_xml)
            zf.writestr("word/document.xml", document_xml)
            for _, nome_int, caminho_orig in self.imagens:
                if os.path.exists(caminho_orig):
                    zf.write(caminho_orig, f"word/media/{nome_int}")


def formatar_linha_tabela(r):
    cv = (r['DesvioPadraoMs'] / r['MediaMs'] * 100.0) if r['MediaMs'] > 0 else 0.0
    return [
        r['Algoritmo'],
        r['Modo'],
        str(int(r['Threads'])),
        f"{r['MediaMs']:.3f}",
        f"±{r['DesvioPadraoMs']:.3f}",
        f"{r['MedianaMs']:.3f}",
        f"{cv:.1f}%",
        f"{r['Speedup']:.2f}x",
        f"{r['Eficiencia'] * 100.0:.1f}%"
    ]


def construir_relatorios():
    df_resumo = pd.read_csv("resultados/benchmark_resumo.csv")
    df_intervalos = pd.read_csv("resultados/benchmark_intervalos.csv")

    # Extrai tabelas para o relatório
    sub_50k = df_resumo[(df_resumo['NaturezaDados'] == 'ALEATORIO') & (df_resumo['Tamanho'] == 50000)]
    linhas_50k = [formatar_linha_tabela(row) for _, row in sub_50k.iterrows()]

    sub_1m = df_resumo[(df_resumo['NaturezaDados'] == 'ALEATORIO') & (df_resumo['Tamanho'] == 1000000)]
    linhas_1m = [formatar_linha_tabela(row) for _, row in sub_1m.iterrows()]

    linhas_int = []
    for _, row in df_intervalos.iterrows():
        linhas_int.append([
            row['Algoritmo'],
            row['Modo'],
            str(int(row['Threads'])),
            f"{int(row['IntervaloMax']):,}".replace(",", "."),
            f"{row['MediaMs']:.3f}",
            f"±{row['DesvioPadraoMs']:.3f}",
            f"{row['Speedup']:.2f}x"
        ])

    # Valores-chave reais para citação precisa no texto
    def obter_metrica(alg, tam, thr, nat='ALEATORIO', col='MediaMs'):
        f = df_resumo[(df_resumo['Algoritmo'] == alg) &
                      (df_resumo['Tamanho'] == tam) &
                      (df_resumo['Threads'] == thr) &
                      (df_resumo['NaturezaDados'] == nat)]
        return float(f[col].values[0]) if not f.empty else 0.0

    qs_1m_t1 = obter_metrica('Quick Sort', 1000000, 1, col='MediaMs')
    qs_1m_t16 = obter_metrica('Quick Sort', 1000000, 16, col='MediaMs')
    qs_1m_spd = obter_metrica('Quick Sort', 1000000, 16, col='Speedup')

    ms_1m_t1 = obter_metrica('Merge Sort', 1000000, 1, col='MediaMs')
    ms_1m_t16 = obter_metrica('Merge Sort', 1000000, 16, col='MediaMs')
    ms_1m_spd = obter_metrica('Merge Sort', 1000000, 16, col='Speedup')

    cs_1m_t1 = obter_metrica('Counting Sort', 1000000, 1, col='MediaMs')
    cs_1m_t8 = obter_metrica('Counting Sort', 1000000, 8, col='MediaMs')

    bs_50k_t1 = obter_metrica('Bubble Sort', 50000, 1, col='MediaMs')
    bs_50k_t16 = obter_metrica('Bubble Sort', 50000, 16, col='MediaMs')
    bs_50k_spd = obter_metrica('Bubble Sort', 50000, 16, col='Speedup')

    docx = ConstrutorDocxUnifor()
    docx.adicionar_cabecalho_unifor("resultados/graficos_relatorio/unifor_logo.png")
    docx.adicionar_titulo_principal(
        "ANÁLISE DE DESEMPENHO DE ALGORITMOS DE BUSCA EM AMBIENTES CONCORRENTES E PARALELOS: UM ESTUDO COMPARATIVO EM JAVA"
    )

    docx.adicionar_paragrafo("Victor Lins.", negrito_prefixo="Autor 1: ", alinhamento="left", recuo_primeira_linha=False)
    docx.adicionar_paragrafo("Integrante da Dupla (Inserir Nome).", negrito_prefixo="Autor 2: ", alinhamento="left", recuo_primeira_linha=False)
    docx.adicionar_paragrafo(
        "Computação Paralela. Algoritmos de Ordenação e Busca. Java ForkJoinPool. Análise Estatística de Desempenho. Speedup.",
        negrito_prefixo="Palavras-chave: ", alinhamento="left", recuo_primeira_linha=False
    )

    # 1. RESUMO
    docx.adicionar_titulo_secao("Resumo")
    texto_resumo = (
        "Este trabalho realiza uma análise experimental e estatística detalhada do desempenho de quatro algoritmos "
        "fundamentais para estruturação e busca eficiente de dados — Quick Sort, Merge Sort, Counting Sort e Bubble Sort — "
        "em ambientes de execução seriais e paralelos utilizando a plataforma Java (JDK 26). A busca por eficiência "
        "computacional em arquiteturas multicore modernas exige compreender não apenas a complexidade assintótica teórica, "
        "mas também os custos práticos de sincronização de threads, localidade de cache e contenção de memória. "
        "Foi desenvolvido um framework de avaliação automatizado em Java, acompanhado de uma interface gráfica interativa "
        "em Java Swing com gráficos dinâmicos em tempo real e integração com Jupyter Notebook. Os experimentos variaram o "
        "tamanho dos conjuntos de dados (de 10.000 a 1.000.000 de elementos), a natureza da distribuição de entrada "
        "(aleatória, inversamente ordenada, quase ordenada e com alta repetição de chaves), a amplitude do intervalo de "
        "valores e o número de threads alocadas (1, 2, 4, 8 e 16 núcleos lógicos), coletando-se cinco amostras independentes "
        "para cada configuração após aquecimento da máquina virtual (JIT Warmup). Os resultados registrados em arquivos CSV "
        f"demonstram que o Merge Sort e o Quick Sort alcançaram expressiva escalabilidade paralela em vetores de 1.000.000 "
        f"de elementos (atingindo Speedups de até {ms_1m_spd:.2f}x e {qs_1m_spd:.2f}x, respectivamente), enquanto o Counting Sort "
        "apresentou o menor tempo absoluto em intervalos moderados, mas revelou sensibilidade direta à expansão da amplitude "
        "de valores k e à largura de banda da memória principal."
    )
    docx.adicionar_paragrafo(texto_resumo)

    # 2. INTRODUÇÃO
    docx.adicionar_titulo_secao("Introdução")
    p_intro_1 = (
        "A eficiência na recuperação, indexação e busca de informações constitui um dos pilares centrais da Ciência da "
        "Computação. Em sistemas de grande escala, a viabilidade de algoritmos de busca rápida — tais como a busca binária "
        "em tempo logarítmico O(log n), buscas por intervalo e consultas analíticas — depende diretamente da prévia "
        "ordenação estruturada dos dados. Com a transição da indústria de processadores para arquiteturas multicore "
        "(múltiplos núcleos físicos e lógicos compartilhando a hierarquia de memória), explorar o paralelismo de dados "
        "e de tarefas tornou-se indispensável para reduzir o tempo de resposta em cargas de trabalho intensivas."
    )
    p_intro_2 = (
        "Neste estudo comparativo em Java, foram selecionados quatro algoritmos que representam diferentes paradigmas "
        "de projeto e classes de complexidade assintótica: (i) Quick Sort, algoritmo de divisão e conquista in-place "
        "baseado em particionamento ao redor de um pivô, com complexidade média O(n log n); (ii) Merge Sort, algoritmo "
        "estável de divisão e conquista baseado em intercalação (merge) com garantia determinística O(n log n) no pior caso; "
        "(iii) Counting Sort, método não-comparativo de endereçamento direto por contagem de frequências, cuja complexidade "
        "linear O(n + k) depende do tamanho da entrada n e da amplitude do intervalo de valores k; e (iv) Bubble Sort, "
        "algoritmo elementar baseado em trocas sucessivas de elementos adjacentes com complexidade quadrática O(n²)."
    )
    p_intro_3 = (
        "A abordagem adotada para a paralelização em Java baseia-se nas melhores práticas de concorrência moderna introduzidas "
        "no pacote java.util.concurrent, com destaque para o framework Fork/Join (ForkJoinPool e RecursiveAction), que "
        "implementa o escalonamento por roubo de trabalho (work-stealing). Para os algoritmos recursivos (Quick Sort e Merge Sort), "
        "as subtarefas independentes são subdivididas assincronamente até atingirem um limiar de granularidade (threshold) "
        "proporcional ao número de threads, evitando a sobrecarga excessiva de criação de objetos na JVM. Para o Counting Sort, "
        "adotou-se a estratégia de histogramas locais por thread seguida de redução paralela por faixas de valores, eliminando "
        "totalmente a contenção de bloqueios (locks) ou disputas de linhas de cache (false sharing). Para o Bubble Sort, "
        "implementou-se a decomposição em sub-blocos paralelos combinada à intercalação hierárquica (além da variante clássica "
        "Odd-Even Transposition Sort sincronizada por Phaser), permitindo investigar o efeito da redução do termo quadrático "
        "em múltiplos núcleos."
    )
    docx.adicionar_paragrafo(p_intro_1)
    docx.adicionar_paragrafo(p_intro_2)
    docx.adicionar_paragrafo(p_intro_3)

    # 3. METODOLOGIA
    docx.adicionar_titulo_secao("Metodologia")
    p_met_1 = (
        "Para assegurar rigor científico, reprodutibilidade e isenção estatística, foi projetado e implementado um "
        "framework modular de testes em Java (classes ExecutorBenchmark, GeradorDados e ExportadorCSV). O ambiente "
        "experimental de execução é composto por uma estação de trabalho equipada com processador de 16 núcleos lógicos "
        "(Runtime.getRuntime().availableProcessors() = 16), executando sob o sistema operacional Windows e Máquina Virtual "
        "Java HotSpot 64-Bit Server VM (JDK 26). Antes do início da coleta cronometrada, o framework executa ciclos de "
        "aquecimento (JVM Warmup) sobre todos os algoritmos seriais e paralelos, garantindo que o compilador Just-In-Time "
        "(HotSpot C2 JIT) realize a compilação nativa e otimização inlining dos métodos críticos antes da medição."
    )
    p_met_2 = (
        "O protocolo experimental seguiu um projeto fatorial abrangendo quatro dimensões de variação: (1) Número de "
        "Threads: execuções Seriais (1 thread) e Paralelas variando-se exatamente em 2, 4, 8 e 16 threads; (2) Tamanho "
        "do Conjunto de Dados (N): vetores de 10.000, 25.000, 50.000, 100.000, 500.000 e 1.000.000 de números inteiros "
        "(limitando-se o algoritmo quadrático Bubble Sort até N = 50.000 para viabilidade temporal nas 4 distribuições); "
        "(3) Natureza dos Dados de Entrada: Aleatório Uniforme (ALEATORIO), Inversamente Ordenado/Decrescente "
        "(INVERSAMENTE_ORDENADO), Quase Ordenado com 90% de ordenação prévia (QUASE_ORDENADO) e Alta Repetição/Poucos "
        "Únicos (POUCOS_UNICOS); e (4) Intervalo de Valores [0..k] em função do tempo: variação do limite superior k "
        "em {1.000, 100.000, 1.000.000, 10.000.000} para N = 500.000 elementos."
    )
    p_met_3 = (
        "Para cada configuração avaliada, foram executadas 5 amostras independentes utilizando sementes pseudoaleatórias "
        "determinísticas (SEMENTE_BASE = 20261027L), garantindo que todos os algoritmos e contagens de threads recebam "
        "exatamente os mesmos 5 vetores de entrada. A medição de tempo utilizou o relógio de alta precisão System.nanoTime(), "
        "convertido para milissegundos (ms), seguida de validação automática da integridade da ordenação. A análise "
        "estatística dos resultados obtidos contempla o cálculo da Média Aritmética, Desvio Padrão Amostral (s), Mediana, "
        "Valores Mínimo e Máximo, Coeficiente de Variação (CV = s / média), além das métricas clássicas de avaliação "
        "paralela: Speedup (Sp = T_serial / T_paralelo(p)) e Eficiência Paralela (Ep = Sp / p), interpretadas à luz da "
        "Lei de Amdahl. Todos os dados brutos e consolidados foram exportados em arquivos CSV e visualizados tanto na "
        "aplicação desktop de gráficos dinâmicos em Java Swing (Ponto Extra) quanto em Jupyter Notebook."
    )
    docx.adicionar_paragrafo(p_met_1)
    docx.adicionar_paragrafo(p_met_2)
    docx.adicionar_paragrafo(p_met_3)

    # 4. RESULTADOS E DISCUSSÃO
    docx.adicionar_titulo_secao("Resultados e Discussão")

    docx.adicionar_subtitulo_secao("4.1. Escalabilidade em Função do Tamanho do Conjunto de Dados (N)")
    p_res_1 = (
        "A primeira análise investiga o crescimento do tempo de execução conforme o volume de dados N escala de 10.000 "
        "até 1.000.000 de elementos. Conforme ilustrado na Figura 1, observa-se uma separação nítida de ordens de grandeza "
        "entre as três classes de complexidade estudadas. No modo Serial (1 thread), para N = 50.000 elementos aleatórios, "
        f"o Bubble Sort demandou em média {bs_50k_t1:.3f} ms, ao passo que o Merge Sort concluiu em "
        f"{obter_metrica('Merge Sort', 50000, 1):.3f} ms, o Quick Sort em {obter_metrica('Quick Sort', 50000, 1):.3f} ms e o "
        f"Counting Sort em apenas {obter_metrica('Counting Sort', 50000, 1):.3f} ms."
    )
    docx.adicionar_paragrafo(p_res_1)
    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig1_escalabilidade_tamanho.png",
        "Figura 1 — Escalabilidade do tempo médio de execução (ms) em função do tamanho N (Serial vs Paralelo 8 Threads)."
    )

    cab_tabela = ["Algoritmo", "Modo", "Threads", "Média (ms)", "Desvio (ms)", "Mediana", "CV (%)", "Speedup", "Eficiência"]
    docx.adicionar_tabela(
        cab_tabela, linhas_50k,
        "Tabela 1 — Análise Estatística Comparativa dos 4 Algoritmos para N = 50.000 (Natureza Aleatória, 5 Amostras)"
    )

    docx.adicionar_subtitulo_secao("4.2. Desempenho Paralelo, Speedup e Eficiência por Número de Threads")
    p_res_2 = (
        "Na avaliação com cargas de alto volume (N = 1.000.000 de elementos, apresentada na Tabela 2 e nas Figuras 2 e 3), "
        "o impacto do paralelismo via ForkJoinPool mostra-se expressivo para os algoritmos de divisão e conquista. "
        f"O Merge Sort reduziu seu tempo médio de execução de {ms_1m_t1:.3f} ms (Serial) para {ms_1m_t16:.3f} ms com 16 threads, "
        f"alcançando um Speedup de {ms_1m_spd:.2f}x. Esse excelente escalonamento deve-se ao uso de um único vetor auxiliar "
        "pré-alocado compartilhado por faixas disjuntas, eliminando pausas do Garbage Collector durante as etapas recursivas "
        f"de intercalação. Similarmente, o Quick Sort reduziu seu tempo médio de {qs_1m_t1:.3f} ms (Serial) para "
        f"{qs_1m_t16:.3f} ms em 16 threads (Speedup de {qs_1m_spd:.2f}x), sustentado pela boa localidade espacial de cache "
        "do particionamento bidirecional de Hoare."
    )
    p_res_3 = (
        f"Por outro lado, o Counting Sort apresentou tempo serial extremamente reduzido ({cs_1m_t1:.3f} ms para N = 1.000.000 "
        f"e k = 100.000), enquanto sua versão paralela com 8 threads registrou {cs_1m_t8:.3f} ms. Esse fenômeno ilustra um "
        "princípio fundamental da Computação Paralela: para algoritmos memory-bound de complexidade linear O(n + k) cujo "
        "tempo serial já é da ordem de poucos milissegundos, o custo fixo de criação de tarefas, alocação de múltiplos "
        "histogramas locais e saturação do barramento de memória RAM supera o ganho aritmético da distribuição entre núcleos. "
        f"No extremo oposto, o Bubble Sort particionado em blocos (N = 50.000) caiu de {bs_50k_t1:.3f} ms (Serial) para "
        f"{bs_50k_t16:.3f} ms em 16 threads (fator de aceleração de {bs_50k_spd:.2f}x), pois ao dividir um vetor de tamanho N "
        "em p sub-blocos independentes, o trabalho quadrático por bloco reduz-se proporcionalmente a (N/p)², substituindo "
        "comparações quadráticas globais por etapas lineares O(N) de intercalação paralela."
    )
    docx.adicionar_paragrafo(p_res_2)
    docx.adicionar_paragrafo(p_res_3)

    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig2_tempo_por_threads.png",
        "Figura 2 — Tempo médio de execução e barras de erro (±1 desvio padrão das 5 amostras) por número de threads."
    )
    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig3_speedup_eficiencia.png",
        "Figura 3 — Curvas de Speedup (Sp) e Eficiência Paralela (Ep) em função do número de threads (1 a 16 núcleos lógicos)."
    )
    docx.adicionar_tabela(
        cab_tabela, linhas_1m,
        "Tabela 2 — Análise Estatística de Alta Escala para N = 1.000.000 (Natureza Aleatória, 5 Amostras)"
    )

    docx.adicionar_subtitulo_secao("4.3. Impacto da Natureza dos Dados e do Intervalo de Valores (k)")
    p_res_4 = (
        "A Figura 4 demonstra o comportamento dos algoritmos frente à mudança na natureza dos dados (Aleatório, Inversamente "
        "Ordenado, Quase Ordenado e Poucos Únicos). Graças à seleção de pivô por mediana de três combinada ao particionamento "
        "de Hoare, o Quick Sort manteve estabilidade O(n log n) mesmo para entradas inversamente ordenadas e com alta repetição "
        "de chaves (que degradariam implementações ingênuas com pivô fixo e particionamento de Lomuto para O(n²)). O Merge Sort "
        "beneficiou-se visivelmente nos cenários Quase Ordenado e Poucos Únicos em razão da verificação de fronteira "
        "(dados[meio] <= dados[meio + 1]), que evita cópias desnecessárias quando subvetores adjacentes já estão ordenados."
    )
    p_res_5 = (
        "Na Figura 5 e na Tabela 3, analisa-se o impacto direto do intervalo de valores [0..k] em função do tempo para "
        "N = 500.000 elementos. Enquanto o Quick Sort e o Merge Sort mantêm tempos de execução praticamente constantes "
        "independentemente de k variar de 1.000 a 10.000.000 (pois baseiam-se exclusivamente em comparações entre chaves), "
        "o Counting Sort exibe forte dependência em relação a k: em k = 1.000 e k = 100.000, o Counting Sort supera "
        "amplamente os demais algoritmos; contudo, quando k atinge 10.000.000, o custo de alocação e travessia do vetor de "
        "frequências O(n + k) eleva drasticamente seu tempo de execução, demonstrando que o Counting Sort é ideal apenas "
        "quando a amplitude k é moderada em relação a N."
    )
    docx.adicionar_paragrafo(p_res_4)
    docx.adicionar_paragrafo(p_res_5)

    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig4_impacto_natureza.png",
        "Figura 4 — Impacto da natureza inicial do conjunto de dados no tempo médio de execução (N = 100.000)."
    )
    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig5_impacto_intervalo_valores.png",
        "Figura 5 — Variação do intervalo de valores [0..k] em função do tempo para N = 500.000 elementos."
    )

    cab_int = ["Algoritmo", "Modo", "Threads", "Intervalo Máximo (k)", "Média (ms)", "Desvio (ms)", "Speedup"]
    docx.adicionar_tabela(
        cab_int, linhas_int,
        "Tabela 3 — Impacto do Intervalo de Valores [0..k] em Função do Tempo (N = 500.000, 5 Amostras)"
    )

    docx.adicionar_subtitulo_secao("4.4. Estabilidade Estatística das Amostras e Gráficos Dinâmicos em Java Swing")
    p_res_6 = (
        "A Figura 6 apresenta o diagrama de caixa (Boxplot) das 5 amostras coletadas para cada algoritmo em N = 1.000.000, "
        "confirmando baixo Coeficiente de Variação (CV tipicamente inferior a 10%) devido ao aquecimento prévio da JVM. "
        "Além das análises no Jupyter Notebook, foi construída a aplicação desktop interativa em Java Swing (Figura 7), "
        "que permite filtrar em tempo real por natureza dos dados, tamanho do vetor, número de threads e escala logarítmica, "
        "exibindo tooltips estatísticos interativos sobre cada ponto experimental."
    )
    docx.adicionar_paragrafo(p_res_6)
    docx.adicionar_figura(
        "resultados/graficos_relatorio/fig6_estabilidade_amostras_boxplot.png",
        "Figura 6 — Distribuição estatística das 5 amostras coletadas em N = 1.000.000 (Boxplot)."
    )
    docx.adicionar_figura(
        "resultados/graficos_swing/swing_tempo_por_tamanho.png",
        "Figura 7 — Captura renderizada diretamente pelo componente de Gráficos Dinâmicos em Java Swing (Ponto Extra)."
    )

    # 5. CONCLUSÃO
    docx.adicionar_titulo_secao("Conclusão")
    p_conc_1 = (
        "Os experimentos conduzidos neste estudo permitiram comprovar empiricamente as relações entre complexidade "
        "algorítmica, arquitetura de memória e escalabilidade paralela em Java. Conclui-se que: (1) Para conjuntos de "
        "dados de grande porte (N >= 100.000) e amplitude arbitrária de chaves, as versões paralelas do Merge Sort e do "
        "Quick Sort baseadas em ForkJoinPool oferecem o melhor equilíbrio entre desempenho absoluto, estabilidade e "
        f"escalabilidade multicore, atingindo Speedups de até {ms_1m_spd:.2f}x e {qs_1m_spd:.2f}x em 16 threads; "
        "(2) O Counting Sort é imbatível em execuções seriais ou com poucas threads quando o intervalo de chaves inteiras k "
        "é pequeno ou proporcional a N (O(n + k)), mas perde eficiência quando k cresce para dezenas de milhões devido ao "
        "gargalo de largura de banda de memória e alocação de histogramas; e (3) O aumento indiscriminado do número de "
        "threads para cargas pequenas (N <= 25.000) pode introduzir overhead de gerenciamento de threads superior ao tempo "
        "útil de processamento, reforçando a importância de limiares adaptativos (thresholds) na programação concorrente."
    )
    docx.adicionar_paragrafo(p_conc_1)

    # 6. REFERÊNCIAS
    docx.adicionar_titulo_secao("Referências")
    referencias = [
        "AMDAL, Gene M. Validity of the single processor approach to achieving large scale computing capabilities. In: AFIPS CONFERENCE PROCEEDINGS, v. 30, p. 483-485, 1967.",
        "CORMEN, Thomas H.; LEISERSON, Charles E.; RIVEST, Ronald L.; STEIN, Clifford. Algoritmos: Teoria e Prática. 4. ed. Rio de Janeiro: GEN LTC, 2024.",
        "GOETZ, Brian et al. Java Concurrency in Practice. Boston: Addison-Wesley Professional, 2006.",
        "LEA, Doug. A Java Fork/Join Framework. In: PROCEEDINGS OF THE ACM 2000 CONFERENCE ON JAVA GRANDE, p. 36-43, 2000.",
        "ORACLE. Java Platform, Standard Edition & Java Development Kit Version 26 API Specification (java.util.concurrent). Disponível em: https://docs.oracle.com/en/java/javase/. Acesso em: 27 out. 2026.",
        "SEDGEWICK, Robert; WAYNE, Kevin. Algorithms. 4. ed. Boston: Addison-Wesley Professional, 2011."
    ]
    for ref in referencias:
        docx.adicionar_paragrafo(ref, alinhamento="left", recuo_primeira_linha=False)

    # 7. ANEXOS
    docx.adicionar_titulo_secao("Anexos — Códigos das Implementações e Link do GitHub")
    docx.adicionar_paragrafo_hyperlink(
        "Repositório Oficial no GitHub (Código-Fonte Completo, CSVs, GUI Java Swing e Jupyter Notebook): ",
        "https://github.com/Victor-Lins14/Comp_Paralela",
        "https://github.com/Victor-Lins14/Comp_Paralela"
    )

    arquivos_codigo = [
        ("Anexo A — Quick Sort Serial e Paralelo (src/algoritmos/QuickSort.java)", "src/algoritmos/QuickSort.java"),
        ("Anexo B — Merge Sort Serial e Paralelo (src/algoritmos/MergeSort.java)", "src/algoritmos/MergeSort.java"),
        ("Anexo C — Counting Sort Serial e Paralelo (src/algoritmos/CountingSort.java)", "src/algoritmos/CountingSort.java"),
        ("Anexo D — Bubble Sort Serial e Paralelo (src/algoritmos/BubbleSort.java)", "src/algoritmos/BubbleSort.java"),
    ]
    for titulo_anexo, caminho_arq in arquivos_codigo:
        docx.adicionar_subtitulo_secao(titulo_anexo)
        if os.path.exists(caminho_arq):
            with open(caminho_arq, "r", encoding="utf-8") as f:
                docx.adicionar_bloco_codigo(f.read())

    caminho_docx = "Relatorio_Analise_Desempenho_UNIFOR.docx"
    docx.salvar(caminho_docx)
    print("Documento Word (.docx) UNIFOR gerado com sucesso em:", caminho_docx)

    # Também gera RELATORIO_UNIFOR.md
    gerar_markdown_relatorio(
        linhas_50k, linhas_1m, linhas_int,
        qs_1m_t1, qs_1m_t16, qs_1m_spd,
        ms_1m_t1, ms_1m_t16, ms_1m_spd,
        cs_1m_t1, cs_1m_t8,
        bs_50k_t1, bs_50k_t16, bs_50k_spd
    )


def gerar_markdown_relatorio(
        linhas_50k, linhas_1m, linhas_int,
        qs_1m_t1, qs_1m_t16, qs_1m_spd,
        ms_1m_t1, ms_1m_t16, ms_1m_spd,
        cs_1m_t1, cs_1m_t8,
        bs_50k_t1, bs_50k_t16, bs_50k_spd):

    def tabela_md(cab, linhas):
        s = "| " + " | ".join(cab) + " |\n"
        s += "| " + " | ".join(["---"] * len(cab)) + " |\n"
        for l in linhas:
            s += "| " + " | ".join(l) + " |\n"
        return s

    cab_padrao = ["Algoritmo", "Modo", "Threads", "Média (ms)", "Desvio (ms)", "Mediana", "CV (%)", "Speedup", "Eficiência"]
    cab_int = ["Algoritmo", "Modo", "Threads", "Intervalo Máximo (k)", "Média (ms)", "Desvio (ms)", "Speedup"]

    md = f"""| ![Unifor](resultados/graficos_relatorio/unifor_logo.png) | **UNIVERSIDADE DE FORTALEZA**<br>**CENTRO DE CIÊNCIAS TECNOLÓGICAS**<br>**CURSO: CIÊNCIA DA COMPUTAÇÃO** |
| :---: | :--- |

# ANÁLISE DE DESEMPENHO DE ALGORITMOS DE BUSCA EM AMBIENTES CONCORRENTES E PARALELOS: UM ESTUDO COMPARATIVO EM JAVA

**Autor 1:** Victor Lins.  
**Autor 2:** Integrante da Dupla (Inserir Nome).  
**Palavras-chave:** Computação Paralela. Algoritmos de Ordenação e Busca. Java ForkJoinPool. Análise Estatística de Desempenho. Speedup.

---

## Resumo
Este trabalho realiza uma análise experimental e estatística detalhada do desempenho de quatro algoritmos fundamentais para estruturação e busca eficiente de dados — *Quick Sort*, *Merge Sort*, *Counting Sort* e *Bubble Sort* — em ambientes de execução seriais e paralelos utilizando a linguagem de programação Java (JDK 26). A busca por eficiência computacional em arquiteturas *multicore* modernas exige compreender não apenas a complexidade assintótica teórica, mas também os custos práticos de sincronização de *threads*, localidade de cache e contenção de memória. Foi desenvolvido um *framework* de avaliação automatizado em Java, acompanhado de uma interface gráfica interativa em **Java Swing** com gráficos dinâmicos em tempo real e integração com **Jupyter Notebook**. Os experimentos variaram o tamanho dos conjuntos de dados (de $10.000$ a $1.000.000$ de elementos), a natureza da distribuição de entrada (aleatória, inversamente ordenada, quase ordenada e com alta repetição de chaves), a amplitude do intervalo de valores e o número de *threads* alocadas ($1, 2, 4, 8$ e $16$ núcleos lógicos), coletando-se $5$ amostras independentes para cada configuração após aquecimento da máquina virtual (*JIT Warmup*). Os resultados registrados em arquivos CSV demonstram que o *Merge Sort* e o *Quick Sort* alcançaram expressiva escalabilidade paralela em vetores de $1.000.000$ de elementos (atingindo *Speedups* de até **{ms_1m_spd:.2f}x** e **{qs_1m_spd:.2f}x**, respectivamente), enquanto o *Counting Sort* apresentou o menor tempo absoluto em intervalos moderados, mas revelou sensibilidade direta à expansão da amplitude de valores $k$ e à largura de banda da memória principal.

---

## Introdução
A eficiência na recuperação, indexação e busca de informações constitui um dos pilares centrais da Ciência da Computação. Em sistemas de grande escala, a viabilidade de algoritmos de busca rápida — tais como a busca binária em tempo logarítmico $O(\\log n)$, buscas por intervalo e consultas analíticas — depende diretamente da prévia ordenação estruturada dos dados. Com a transição da indústria de processadores para arquiteturas *multicore*, explorar o paralelismo de dados e de tarefas tornou-se indispensável para reduzir o tempo de resposta em cargas de trabalho intensivas.

Neste estudo comparativo em Java, foram selecionados quatro algoritmos que representam diferentes paradigmas de projeto e classes de complexidade assintótica:
1. **Quick Sort**: algoritmo de divisão e conquista *in-place* baseado em particionamento bidirecional de Hoare e pivô por mediana de três, com complexidade média $O(n \\log n)$.
2. **Merge Sort**: algoritmo estável de divisão e conquista baseado em intercalação (*merge*) com garantia determinística $O(n \\log n)$ no pior caso e vetor auxiliar pré-alocado.
3. **Counting Sort**: método não-comparativo de endereçamento direto por contagem de frequências, cuja complexidade linear $O(n + k)$ depende do tamanho da entrada $n$ e da amplitude do intervalo de valores $k$.
4. **Bubble Sort**: algoritmo elementar baseado em trocas sucessivas de elementos adjacentes com complexidade quadrática $O(n^2)$.

A paralelização em Java utilizou o framework `ForkJoinPool` (`RecursiveAction` e *work-stealing*), aplicando limiares adaptativos de granularidade (*thresholds*) para os métodos recursivos, histogramas locais sem contenção de bloqueios para o *Counting Sort*, e decomposição em sub-blocos concorrentes seguida de intercalação paralela (além da variante *Odd-Even Transposition Sort*) para o *Bubble Sort*.

---

## Metodologia
O *framework* experimental foi executado em uma estação equipada com **16 processadores lógicos** (`Runtime.getRuntime().availableProcessors() = 16`) sob Windows e **Java HotSpot 64-Bit Server VM (JDK 26)**. Antes da coleta de tempos, ciclos de *warmup* foram aplicados para estabilizar as otimizações do compilador *Just-In-Time* (HotSpot C2).

As variáveis experimentais controladas foram:
- **Modos e Threads**: Serial ($1$ thread) e Paralelo ($2, 4, 8$ e $16$ threads).
- **Tamanhos de Entrada ($N$)**: $10.000, 25.000, 50.000, 100.000, 500.000$ e $1.000.000$ de inteiros.
- **Natureza dos Conjuntos de Dados**: `ALEATORIO`, `INVERSAMENTE_ORDENADO`, `QUASE_ORDENADO` e `POUCOS_UNICOS`.
- **Intervalo de Valores ($k$)**: $k \\in \\{{1.000, 100.000, 1.000.000, 10.000.000\\}}$ em função do tempo para $N = 500.000$.
- **Amostragem e Estatística**: **5 amostras independentes** por configuração (mesmas sementes determinísticas entre algoritmos), registrando-se Média, Desvio Padrão ($s$), Mediana, Coeficiente de Variação ($CV$), *Speedup* ($S_p = T_{{serial}} / T_{{paralelo}}$) e Eficiência ($E_p = S_p / p$).

---

## Resultados e Discussão

### 4.1. Escalabilidade por Tamanho do Conjunto de Dados ($N$)
![Figura 1](resultados/graficos_relatorio/fig1_escalabilidade_tamanho.png)

**Tabela 1 — Análise Estatística Comparativa dos 4 Algoritmos para $N = 50.000$ (Natureza Aleatória, 5 Amostras)**

{tabela_md(cab_padrao, linhas_50k)}

### 4.2. Desempenho Paralelo, Speedup e Eficiência por Número de Threads
Em vetores de $N = 1.000.000$ de elementos:
- O **Merge Sort** reduziu seu tempo médio de **{ms_1m_t1:.3f} ms** (Serial) para **{ms_1m_t16:.3f} ms** (16 threads), atingindo **Speedup de {ms_1m_spd:.2f}x**.
- O **Quick Sort** reduziu seu tempo médio de **{qs_1m_t1:.3f} ms** (Serial) para **{qs_1m_t16:.3f} ms** (16 threads), atingindo **Speedup de {qs_1m_spd:.2f}x**.
- O **Counting Sort** concluiu a execução serial em apenas **{cs_1m_t1:.3f} ms**, evidenciando que para algoritmos *memory-bound* ultrarrápidos o custo de criação de múltiplas estruturas locais de contagem satura a largura de banda de memória.
- O **Bubble Sort** em blocos ($N = 50.000$) reduziu seu tempo de **{bs_50k_t1:.3f} ms** (Serial) para **{bs_50k_t16:.3f} ms** (16 threads), um fator de aceleração de **{bs_50k_spd:.2f}x** decorrente da redução quadrática $(N/p)^2$ em cada partição.

![Figura 2](resultados/graficos_relatorio/fig2_tempo_por_threads.png)

![Figura 3](resultados/graficos_relatorio/fig3_speedup_eficiencia.png)

**Tabela 2 — Análise Estatística de Alta Escala para $N = 1.000.000$ (Natureza Aleatória, 5 Amostras)**

{tabela_md(cab_padrao, linhas_1m)}

### 4.3. Impacto da Natureza dos Dados e do Intervalo de Valores ($k$)
![Figura 4](resultados/graficos_relatorio/fig4_impacto_natureza.png)

![Figura 5](resultados/graficos_relatorio/fig5_impacto_intervalo_valores.png)

**Tabela 3 — Impacto do Intervalo de Valores $[0..k]$ em Função do Tempo ($N = 500.000$, 5 Amostras)**

{tabela_md(cab_int, linhas_int)}

### 4.4. Estabilidade Estatística das 5 Amostras e Gráficos Dinâmicos (Java Swing)
![Figura 6](resultados/graficos_relatorio/fig6_estabilidade_amostras_boxplot.png)

![Figura 7](resultados/graficos_swing/swing_tempo_por_tamanho.png)

---

## Conclusão
Os resultados obtidos comprovam experimentalmente que a paralelização em Java por meio do `ForkJoinPool` proporciona ganhos substanciais de desempenho em algoritmos de divisão e conquista (*Merge Sort* e *Quick Sort*) quando aplicados a conjuntos de dados volumosos ($N \\ge 100.000$), alcançando *Speedups* de até **{ms_1m_spd:.2f}x** e **{qs_1m_spd:.2f}x**. Demonstrou-se ainda que o *Counting Sort* oferece o menor tempo de resposta absoluto quando o domínio de valores inteiros $k$ é restrito, mas sofre degradação proporcional a $k$ quando o intervalo atinge a casa de dezenas de milhões ($k = 10^7$), momento em que os algoritmos baseados em comparação $O(n \\log n)$ tornam-se mais vantajosos.

---

## Referências
- AMDAHL, Gene M. Validity of the single processor approach to achieving large scale computing capabilities. In: **AFIPS Conference Proceedings**, v. 30, p. 483-485, 1967.
- CORMEN, Thomas H.; LEISERSON, Charles E.; RIVEST, Ronald L.; STEIN, Clifford. **Algoritmos: Teoria e Prática**. 4. ed. Rio de Janeiro: GEN LTC, 2024.
- GOETZ, Brian et al. **Java Concurrency in Practice**. Boston: Addison-Wesley Professional, 2006.
- LEA, Doug. A Java Fork/Join Framework. In: **Proceedings of the ACM 2000 Conference on Java Grande**, p. 36-43, 2000.
- ORACLE. **Java Platform, Standard Edition & JDK 26 API Specification**. Disponível em: https://docs.oracle.com/en/java/javase/. Acesso em: 27 out. 2026.
- SEDGEWICK, Robert; WAYNE, Kevin. **Algorithms**. 4. ed. Boston: Addison-Wesley Professional, 2011.

---

## Anexos — Códigos das Implementações e Link do GitHub
- **Repositório GitHub do Projeto:** [https://github.com/Victor-Lins14/Comp_Paralela](https://github.com/Victor-Lins14/Comp_Paralela)
- Código-fonte das implementações:
  - [`src/algoritmos/QuickSort.java`](src/algoritmos/QuickSort.java)
  - [`src/algoritmos/MergeSort.java`](src/algoritmos/MergeSort.java)
  - [`src/algoritmos/CountingSort.java`](src/algoritmos/CountingSort.java)
  - [`src/algoritmos/BubbleSort.java`](src/algoritmos/BubbleSort.java)
  - [`src/benchmark/ExecutorBenchmark.java`](src/benchmark/ExecutorBenchmark.java)
  - [`src/gui/BenchmarkSwingApp.java`](src/gui/BenchmarkSwingApp.java)
  - [`src/gui/PainelGraficoDinamico.java`](src/gui/PainelGraficoDinamico.java)
"""
    with open("RELATORIO_UNIFOR.md", "w", encoding="utf-8") as f:
        f.write(md)
    print("Relatório Markdown gerado com sucesso em: RELATORIO_UNIFOR.md")


if __name__ == "__main__":
    construir_relatorios()
