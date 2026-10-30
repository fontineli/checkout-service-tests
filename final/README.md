# Projeto dos Casos de Teste — eCommerce

## Descrição do Projeto
Este projeto implementa **cálculo do custo total** de uma compra em um sistema de e‑commerce e inclui uma suíte de **testes unitários robustos** (partições, limites, decisão e robustez) em **JUnit**. A API está estruturada em **Spring Boot**, com camadas de *controller*, *service* e *repository*, além de adaptadores externos simulados para estoque e pagamento.

## Estrutura do Projeto
```
src/
├── main/java/ecommerce/
│   ├── controller/              # REST endpoints (CompraController)
│   ├── dto/                     # Objetos de entrada/saída (CompraDTO, etc.)
│   ├── entity/                  # Entidades do domínio (Carrinho, Produto, etc.)
│   ├── external/                # Portas para serviços externos (estoque/pagamento)
│   ├── external/fake/           # Implementações simuladas
│   ├── repository/              # Persistência (Spring Data)
│   ├── service/                 # Regras de negócio (CompraService, etc.)
│   └── CompraApplication.java   # Spring Boot app
└── test/java/ecommerce/service/
    ├── CompraServiceParticaoTest.java
    ├── CompraServiceValorLimiteTest.java
    ├── CompraServiceDecisaoTest.java
    ├── CompraServiceRobustezTest.java
    └── TestHelper.java
```

## Como Executar o Projeto


### Subir a aplicação (opcional)
```bash
./mvnw spring-boot:run
```

### Compilar
```bash
./mvnw clean compile
```

### Rodar os testes
```bash
./mvn test
```

### Verificar e gerar relatórios (inclui cobertura)
```bash
./mvn verify
```
## 1. Contexto e Escopo

- **Função testada:** `CompraService#calcularCustoTotal(CarrinhoDeCompras, Regiao, TipoCliente)`
- **Domínio:** cálculo de subtotal + frete − descontos
- **Regiões e fretes fixos:**

| Região | Sigla | Frete (R$) |
|:--|:--:|:--:|
| Sudeste | SE | 15.00 |
| Sul | S | 20.00 |
| Centro-Oeste | CO | 25.00 |
| Nordeste | NE | 30.00 |
| Norte | N | 35.00 |

- **Tipos de cliente e desconto no subtotal:**

| Tipo | Desconto | Observação |
|:--|:--:|:--|
| Bronze | 0 % | sem benefício |
| Prata  | 10 % | se subtotal ≥ 100 R$ |
| Ouro  | 20 % | qualquer valor + frete zerado  |

- **Produtos frágeis:** acréscimo fixo R$ 5,00 no frete.
- **Descontos por faixa de subtotal:**

| Faixa | Limiar | Desconto Extra |
|:--|:--:|:--:|
| T1 | ≥ 100 R$ | 5 % |
| T2 | ≥ 500 R$ | 10 % |

> Estes parâmetros vêm do enunciado prático de 2025.2: combinar *frete por região + fragilidade + faixa de subtotal + perfil do cliente*.

---

## 2. Classes de Equivalência

| ID | Domínio | Classe | Tipo | Exemplo | Saída Esperada |
|:--|:--|:--|:--:|:--|:--|
| E1 | Subtotal | = 0 | inválida | carrinho vazio | erro ou total 0 |
| E2 | 0 < subtotal < 100 | válida | bronze/prata/ouro | total = subtotal + frete (– 20 % se ouro) |
| E3 | 100 ≤ subtotal < 500 | válida | bronze/prata/ouro | desconto T1 (5 %) + desconto prata/ouro |
| E4 | subtotal ≥ 500 | válida | bronze/prata/ouro | desconto T2 (10 %) + benefício do cliente |
| E5 | quantidade ≤ 0 ou preço ≤ 0 | inválida | — | lançar exceção |
| E6 | produto frágil | válida | fragil = true | + R$ 5 ao frete |

---

## 3. Análise de Valor-Limite

| Parâmetro | Limite | Valores de Teste | Propósito |
|:--|:--:|:--|:--|
| Subtotal T1 | 100 | 99.99 / 100.00 / 100.01 | entrada na faixa de 5 % |
| Subtotal T2 | 500 | 499.99 / 500.00 / 500.01 | entrada na faixa de 10 % |
| Qtd mínima | 1 | 0 / 1 / 2 | validação de quantidade |
| Frete | >= 0 | −0.01 / 0 / 5 | arredondamento/validação |

---

## 4. Tabela de Decisão

| Condição | Valores | R1 | R2 | R3 | R4 | R5 | R6 |
|:--|:--|:--:|:--:|:--:|:--:|:--:|:--:|
| subtotal ≥ 500 (T2) | V/F | F | F | V | F | V | F |
| subtotal ≥ 100 (T1) | V/F | F | V | V | V | F | F |
| tipoCliente = PRATA | V/F | F | V | F | F | V | F |
| tipoCliente = OURO | V/F | F | F | F | V | F | V |
| produto frágil | V/F | F | V | F | F | V | V |
| região | SE | S | NE | N | CO | S | SE |
| **Ações** | | | | | | | |
| Desconto subtotal (5 %/10 %) | X |  | X | X |  | X |  |
| Desconto por cliente (10 %/20 %) |  |  | X |  | X | X | X |
| Aplicar frete base | X | X | X | X | X | X |
| Acréscimo frágil |  | X |  |  | X |  | X |
| Zerar frete (Ouro) |  |  |  |  | X |  | X |
| **Resultado esperado** | | `subtotal+FR_SE` | `subtotal·0.9+FR_NE+5` | `subtotal·0.9+FR_N` | `subtotal·0.8` | `subtotal·0.85+FR_S+5` | `subtotal·0.8` |

---

## 5. MC/DC — Decisão-chave

```java
if (regiaoValida && subtotal > 0 && !itensInvalidos)
    total = (subtotal - desconto) + frete;
if (tipoCliente == OURO)
    frete = 0;
```

| Condição | Par que mostra influência | Resultado |
|:--|:--|:--|
| `regiaoValida` | (V,F) com subtotal>0 | altera cálculo do frete |
| `subtotal>0` | (V,F) com regiao fixa | muda total de >0 para 0 |
| `itensInvalidos` | (F,V) | gera erro |
| `tipoCliente==OURO` | (V,F) | zera ou não zera frete |

---

## 6. Casos de Teste

| ID | Técnica | Descrição | Entrada-chave | Saída Esperada |
|:--|:--|:--|:--|:--|
| TC-EC-001 | Equivalência | Carrinho vazio | subtotal = 0 | Erro/total 0 |
| TC-EC-002 | Equivalência | Bronze < T1 SE | subtotal = 50 | 50 + 15 = 65 |
| TC-EC-003 | Equivalência | Prata ≥ T1 NE frágil | subtotal = 120 | (120×0.9×0.95)+30+5 = **138.7 R$** |
| TC-EC-004 | Equivalência | Bronze ≥ T2 N | subtotal = 500 | (500×0.9)+35 = 485 |
| TC-EC-005 | Equivalência | Ouro qualquer região | subtotal = 200 | (200×0.8)+0 = 160 |
| TC-BVA-001 | Limite | T1 − ε | 99.99 | sem desconto |
| TC-BVA-002 | Limite | T1 | 100.00 | 5 % desc. |
| TC-BVA-003 | Limite | T2 | 500.00 | 10 % desc. |
| TC-DT-001..006 | Tabela | Regras R1–R6 | (ver tabela) | conforme expectativas |
| TC-MCDC-001..004 | MC/DC | verificação das condições independentes | — | mudança no resultado detectada |

---

## 7. Rastreabilidade

| Requisito | Casos |
|:--|:--|
| Cálculo de frete por região | TC-EC-002..004, TC-DT-001..006 |
| Desconto por faixa T1/T2 | TC-BVA-001..003, TC-DT-002..005 |
| Benefício OURO (zerar frete) | TC-EC-005, TC-DT-004/006 |
| Fragilidade impacta frete | TC-EC-003, TC-DT-002/005 |

---

## 8. Estratégia de Implementação

- Crie **três classes JUnit**:
    - `CompraServiceEquivalenceTest`
    - `CompraServiceBoundaryTest`
    - `CompraServiceDecisionTableTest`
- Utilize *builders* para criar produtos, itens e carrinhos.
- Compare `BigDecimal` com `isEqualByComparingTo`.
- Nomeie métodos com o padrão **Given–When–Then** e prefixo do **ID do caso**.

---
