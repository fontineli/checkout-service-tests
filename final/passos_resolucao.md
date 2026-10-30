# 🧩 Guia de Resolução — Projeto de Testes de Software (Finalização de Compra)

Este checklist segue o enunciado do trabalho e as técnicas vistas em aula  
(**Particionamento**, **Valor-Limite**, **Tabela de Decisão**, **MC/DC**)  
e o ciclo **TDD (Red → Green → Refactor)**.

---

## 🏁 Etapa 1 — Entendimento do problema

- [ ] Ler o enunciado completo e destacar **as seis etapas do cálculo**:
    - [ ] Subtotal
    - [ ] Desconto por tipo de produto
    - [ ] Desconto por valor de carrinho
    - [ ] Cálculo do frete base
    - [ ] Benefício de fidelidade
    - [ ] Total (arredondamento final)
- [ ] Anotar todas as **faixas, limites e condições** (500, 1000, 5, 10, 50, etc.)
- [ ] Criar um resumo textual com a **ordem de cálculo e exceções**
- [ ] Confirmar o **escopo do projeto**: apenas o método `calcularCustoTotal` da camada `service`
- [ ] Criar uma pasta `docs/` para armazenar planilhas, tabelas e CFG

---

## 🧠 Etapa 2 — Modelagem mínima do domínio

- [ ] Definir as **entidades essenciais**:
    - [ ] `Cliente` (nível de fidelidade + região)
    - [ ] `ItemCompra` (quantidade, preço, tipo, dimensões, peso, frágil)
    - [ ] `Carrinho` (lista de itens)
- [ ] Criar uma enum `Regiao` com multiplicadores
- [ ] Criar um método `calcularCustoTotal(Cliente, Carrinho)`
- [ ] Documentar as **entradas e saídas esperadas** em forma de contrato

---

## 🧪 Etapa 3 — Planejamento dos testes funcionais (caixa-preta)

### 3.1 Classes de Equivalência
- [ ] Listar partições válidas e inválidas para cada regra:
    - [ ] Desconto por tipo: <3, 3–4, 5–7, ≥8
    - [ ] Desconto por valor: ≤500, (500–1000], >1000
    - [ ] Faixa de peso: 0–5, (5–10], (10–50], >50
    - [ ] Fragilidade: sim / não
    - [ ] Região: SE, S, NE, CO, N
    - [ ] Fidelidade: Ouro, Prata, Bronze
    - [ ] Entradas inválidas: quantidade ≤0, preço <0, cliente nulo, etc.
- [ ] Criar tabela `docs/testes-particoes.md` com:
  | ID | Entrada | Resultado Esperado | Partição Coberta |

### 3.2 Valor-Limite
- [ ] Identificar valores críticos (500, 1000, 5, 10, 50, 2→3, 4→5, 7→8)
- [ ] Elaborar tabela `docs/testes-limites.md`:
  | ID | Variável | Valor de Teste | Resultado Esperado | Limite |

### 3.3 Tabela de Decisão
- [ ] Definir condições e ações relevantes
- [ ] Eliminar combinações impossíveis
- [ ] Criar `docs/tabela-decisao.md`:
  | Regra | Condições (T/F) | Ação Esperada |
- [ ] Selecionar um conjunto mínimo de regras representativas

---

## ⚙️ Etapa 4 — Planejamento dos testes estruturais (caixa-branca)

- [ ] Desenhar o **grafo de fluxo de controle (CFG)** do método
- [ ] Calcular a **complexidade ciclomática** `V(G) = E - N + 2P`
- [ ] Listar o **mínimo de casos independentes (≥ V(G))**
- [ ] Salvar o grafo em `docs/cfg-custo-total.png`
- [ ] Escolher a **decisão composta mais complexa** e montar a tabela MC/DC:
  | Caso | Condição A | Condição B | Condição C | Resultado |
- [ ] Garantir que cada condição pode, isoladamente, inverter o resultado
- [ ] Salvar como `docs/mcdc-custo-total.md`

---

## 🔁 Etapa 5 — Criação do projeto e estrutura de pastas

- [ ] Criar projeto Maven:
  ```bash
  mvn archetype:generate -DgroupId=com.ecommerce -DartifactId=checkout-service-tests -DinteractiveMode=false