package ecommerce.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

public class CompraServiceValorLimiteTest {

    private CompraService service;
    private Cliente clienteBronzeSE;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);
        clienteBronzeSE = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
    }

    static Stream<Arguments> subtotalLimiteArgs() {
        return Stream.of(
            // subtotal, totalEsperado (arredondado), cenario
            Arguments.of("500.00", "500.00", "Limite 500.00 (sem desconto)"),
            Arguments.of("500.01", "450.01", "Limite 500.01 (10% desconto)"),
            Arguments.of("1000.00", "900.00", "Limite 1000.00 (10% desconto)"),
            Arguments.of("1000.01", "800.01", "Limite 1000.01 (20% desconto)")
        );
    }

    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("subtotalLimiteArgs")
    @DisplayName("BVA: Testa limites de desconto por valor do subtotal")
    public void bva_SubtotalLimites(String subtotalStr, String totalEsperadoStr, String cenario) {
        BigDecimal subtotal = new BigDecimal(subtotalStr);
        
        Produto p = TestHelper.criarProduto("P1", subtotal, BigDecimal.ONE, false); // Peso 1kg (Frete 0)
        ItemCompra item = TestHelper.criarItem(p, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, clienteBronzeSE.getRegiao(), clienteBronzeSE.getTipo());

        // compara direto com o total final esperado
        assertThat(total).as(cenario).isEqualByComparingTo(totalEsperadoStr);
    }

    // --- Limites de Peso ---
    @Test
    @DisplayName("BVA: Peso 5.00kg (Limite Isento)")
    public void bva_PesoLimiteIsento() {
        Produto p = TestHelper.criarProduto("P1", BigDecimal.TEN, new BigDecimal("5.00"), false);
        ItemCompra item = TestHelper.criarItem(p, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, clienteBronzeSE.getRegiao(), clienteBronzeSE.getTipo());
        assertThat(total).as("Peso 5.00kg deve ser isento").isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("BVA: Peso 5.01kg (Limite Faixa B)")
    public void bva_PesoLimiteFaixaB() {
        Produto p = TestHelper.criarProduto("P1", BigDecimal.TEN, new BigDecimal("5.01"), false);
        ItemCompra item = TestHelper.criarItem(p, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, clienteBronzeSE.getRegiao(), clienteBronzeSE.getTipo());
        assertThat(total).as("Peso 5.01kg deve ativar Faixa B").isEqualByComparingTo("32.02");
    }

    // --- Limites de Quantidade (Desconto por Tipo) ---
    @Test
    @DisplayName("BVA: Qtd 2 (Limite Sem Desconto)")
    public void bva_QuantidadeLimiteSemDesconto() {
        Produto p = TestHelper.criarProduto("P1", BigDecimal.TEN, BigDecimal.ONE, false); // Frete 0
        ItemCompra item = TestHelper.criarItem(p, 2L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, clienteBronzeSE.getRegiao(), clienteBronzeSE.getTipo());
        assertThat(total).as("Qtd 2 não deve ter desconto").isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("BVA: Qtd 3 (Limite Desconto 5%)")
    public void bva_QuantidadeLimiteDesconto5() {
        Produto p = TestHelper.criarProduto("P1", BigDecimal.TEN, BigDecimal.ONE, false); // Frete 0
        ItemCompra item = TestHelper.criarItem(p, 3L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, clienteBronzeSE.getRegiao(), clienteBronzeSE.getTipo());
        assertThat(total).as("Qtd 3 deve ter 5% desconto").isEqualByComparingTo("28.50");
    }
}