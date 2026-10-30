# JUnit — eCommerce — Automation

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

### Pré‑requisitos
- **Java 17+**
- **Maven 3.6+** (o projeto já inclui *Maven Wrapper*: `mvnw`/`mvnw.cmd`).

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
./mvnw test
```

### Verificar e gerar relatórios (inclui cobertura)
```bash
./mvnw verify
```

## Relatórios de Teste e Cobertura

- **Relatórios de teste (Surefire):**
  - `target/surefire-reports/` — arquivos `TEST-*.xml` e `.txt`.

- **Cobertura (JaCoCo):**
  - HTML em `target/site/jacoco/index.html`.

> Abra o arquivo HTML no navegador para ver os detalhes de cobertura por classe/método.

## Regras de Negócio (resumo)
As principais regras implementadas em `CompraService` são:

1. **Subtotal com desconto por tipo de produto**  
   - 3–4 itens do mesmo tipo → **5%**  
   - 5–7 itens do mesmo tipo → **10%**  
   - ≥ 8 itens do mesmo tipo → **15%**  

2. **Desconto por valor de carrinho (após o desconto por tipo)**  
   - Subtotal > **R$ 1000,00** → **20%**  
   - Subtotal > **R$ 500,00** e ≤ R$ 1000,00 → **10%**

3. **Cálculo de frete**
   - **Peso cúbico** = (C × L × A) / **6000**  
   - **Peso tributável** = `max(peso_físico, peso_cúbico)`  
   - Faixas:
     - 0–5 kg → **isento**
     - 5–10 kg → **R$ 2,00/kg**
     - 10–50 kg → **R$ 4,00/kg**
     - > 50 kg → **R$ 7,00/kg**
   - Se não isento, aplica **taxa mínima** de **R$ 12,00**  
   - Produtos **frágeis**: **R$ 5,00 × quantidade** (manuseio especial)  
   - **Multiplicador por região** (ex.: Sudeste 1.00, Sul 1.05, Nordeste 1.10, Centro‑Oeste 1.20, Norte 1.30)

4. **Benefício por nível do cliente (aplicado ao frete)**
   - **OURO**: 100% de desconto (frete grátis)  
   - **PRATA**: 50% de desconto  
   - **BRONZE**: sem desconto

5. **Total**
```
Total = Subtotal (após descontos) + Frete (após benefício de nível)
```
Arredondamento: **HALF_UP** com 2 casas decimais.

## Casos de Teste (visão geral)
A suíte cobre:
- **Partições de domínio** (ex.: faixas de peso, faixas de desconto por quantidade e por valor do carrinho, regiões, níveis de cliente, frágil vs. não‑frágil);
- **Valores limites** (ex.: 5,00 kg; 10,00 kg; 50,00 kg; R$ 500,00; R$ 1000,00);
- **Tabela de decisão** (combinações representativas de regras);
- **Robustez/validação** (parâmetros nulos/ inválidos no cálculo).

## Tecnologias
- **Java 17**, **Spring Boot**
- **JUnit 5**, **AssertJ**
- **Maven**, **JaCoCo**
- **H2** (testes), **Spring Data JPA**, **OpenFeign** (simulado)

## Como Contribuir
1. Faça um fork do repositório
2. Crie um *branch* para sua feature: `git checkout -b feature/minha-feature`
3. Commit: `git commit -m "feat: minha feature"`
4. Push: `git push origin feature/minha-feature`
5. Abra um **Pull Request**

## Licença
Sugestão: **MIT License**. Crie um arquivo `LICENSE` com o texto da licença.

---
> _Gerado em 2025-10-29._
