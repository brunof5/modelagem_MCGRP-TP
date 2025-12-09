# Modelagem Matemática para NEARP e NEARP-TP

Este repositório contém a implementação dos modelos matemáticos desenvolvidos no Trabalho de Conclusão de Curso "O Problema de Roteamento em Nós, Arestas e Arcos com Penalidades de Conversão (NEARP / NEARP-TP)".

A modelagem reúne as formulações exatas para o **Problema de Roteamento Geral Misto Capacitado** (MCGRP / NEARP) e sua variante com penalidades de conversão (MCGRP-TP / NEARP-TP), permitindo resolver instâncias reais de coleta seletiva a partir da malha urbana da cidade de Lavras/MG.

As instâncias usadas pelo modelo são geradas diretamente pelo protótipo geoespacial. Aquelas instâncias mencionadas e utilizadas no TCC estão armazenadas na pasta `datasets`.

## 🔗 Repositórios Relacionados

Este é o 2º componente do conjunto de três repositórios que compõem a solução completa do TCC:

1. [Protótipo geoespacial](https://github.com/brunof5/geracao_instancia_MCGRP-TP) (este): Gera instâncias MCGRP/MCGRP-TP a partir de dados reais.

2. [Modelagem Matemática](https://github.com/brunof5/modelagem_MCGRP-TP) (NEARP / NEARP-TP): Implementa os modelos exatos utilizados para análise comparativa.

3. [Meta-Heurística HGS-CARP](https://github.com/brunof5/HGS-CARP): Implementação do algoritmo HGS-CARP adaptado para lidar com penalidades de conversão.

## 🚀 Funcionalidades

* **Leitura** das instâncias .dat produzidas pelo protótipo geoespacial.
* Geração de **estruturas auxiliares** para acesso rápido aos dados.
* **Classes** diversas que representam cada componente da formulação, como `Edge`, `Node`, `Route`, etc.
* **Formulação** NEARP e NEARP-TP seguindo a estrutura de programação inteira mista proposta no TCC.

## 🛠️ Solver

As implementações foram desenvolvidas em Java 17 e o modelo matemático foi resolvido por meio do solver IBM ILOG CPLEX 22.1.2.

## ⚙️ Instalação e Execução

Siga os passos abaixo para configurar o ambiente e executar a aplicação.

### Pré-requisitos

* **Python 3.9** ou superior.
* **Git**.
* **Maven**.

### Passos

1.  **Clone o repositório:**
    ```bash
    # Clone este repositório
    cd modelagem_MCGRP-TP
    ```
    
2.  **Compile o projeto:**
    ```bash
    mvn clean package
    ```

---

É possível executar um conjunto de instâncias sequencialmente, ou escolher qual instância deseja-se executar.

*  **Execute a aplicação (dataset):**
    ```bash
    python run_tcc.py <source_folder> <input_folder> <output_folder> <inputType>
    ```

*  **Execute a aplicação (manual):**
    ```bash
    java -Djava.library.path=<Djava.library.path> -jar target\tcc-1.0.jar <inputType> <input_file> <output_file>
    ```

---

## 📚 Artigo / TCC (Base Teórica)

FERREIRA, B. C. **O Problema de Roteamento em Nós, Arestas e Arcos com Penalidades de Conversão: Um Estudo no Contexto da Coleta Seletiva de Lixo**. TCC (Bacharelado) — Faculdade de Ciência da Computação, Universidade Federal de Lavras. Lavras, p. 81. 2025.
