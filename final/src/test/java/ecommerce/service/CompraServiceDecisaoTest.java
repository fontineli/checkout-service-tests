package ecommerce.service;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

public class CompraServiceDecisaoTest {

    private CompraService service;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);
    }

    @Test
    @DisplayName("Regra 1: Bronze, Sub < 500, Não Frágil, Sudeste, Frete Faixa B")
    public void decisao_Regra_Bronze_FaixaB_NaoFragil_SE() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto p = TestHelper.criarProduto("P1", new BigDecimal("50.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(p, 1L);
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // Subtotal: 50.00
        // Frete: Peso 6kg (Faixa B), SE, Bronze -> (6*2)+12 = 24.00
        // Total: 50.00 + 24.00 = 74.00
        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());
        assertThat(total).as("DT Regra 1: Bronze, Faixa B, SE, Não Frágil")
                         .isEqualByComparingTo("74.00");
    }

    @Test
    @DisplayName("Regra 2: Prata, Sub < 500, Frágil, Nordeste, Frete Faixa B")
    public void decisao_Regra_Prata_FaixaB_Fragil_NE() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.PRATA, Regiao.NORDESTE);
        Produto p = TestHelper.criarProduto("P1", new BigDecimal("120.00"), new BigDecimal("6.0"), true);
        ItemCompra item = TestHelper.criarItem(p, 1L);
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // Subtotal: 120.00
        // Frete: Peso 6kg (Faixa B), Frágil, NE, Prata
        //   (6*2)+12 = 24.00
        //   + 5.00 (frágil) = 29.00
        //   * 1.10 (NE) = 31.90
        //   * 0.5 (Prata) = 15.95
        // Total: 120.00 + 15.95 = 135.95
        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());
        assertThat(total).as("DT Regra 2: Prata, Faixa B, NE, Frágil")
                         .isEqualByComparingTo("135.95");
    }

    @Test
    @DisplayName("Regra 3: Ouro, Sub > 500, Qtd 8+, Frete Isento")
    public void decisao_Regra_Ouro_Sub1000_Qtd8_FreteZero() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.OURO, Regiao.NORTE);
        Produto p = TestHelper.criarProduto("P1", new BigDecimal("200.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(p, 8L); // Qtd=8 (Desc 15%)
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // Subtotal Bruto: 200 * 8 = 1600.00
        // Desconto Tipo (15%): 1600 * (1 - 0.15) = 1360.00
        // Desconto Valor (20%): 1360 * (1 - 0.20) = 1088.00
        // Frete: Ouro -> 0.00
        // Total: 1088.00
        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());
        assertThat(total).as("DT Regra 3: Ouro, Descontos Qtd e Valor, Frete Zero")
                         .isEqualByComparingTo("1088.00");
    }

    @Test
    @DisplayName("Regra 4: Combinação Descontos Qtd(10%) e Valor(10%) com Frete Bronze")
    public void decisao_Regra_Bronze_DescontoQtd10_DescontoValor10_FreteFaixaC() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUL);
        Produto p = TestHelper.criarProduto("P1", new BigDecimal("100.00"), new BigDecimal("3.0"), true);
        ItemCompra item = TestHelper.criarItem(p, 6L); // Qtd=6 (Desc 10%), PesoTotal=18 (Faixa C)
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // Subtotal Bruto: 100 * 6 = 600.00
        // Desconto Tipo (10%): 600 * (1 - 0.10) = 540.00
        // Desconto Valor (10%): 540 * (1 - 0.10) = 486.00
        // Frete: Peso 18kg (Faixa C), Frágil, Sul, Bronze
        //   (18*4)+12 = 84.00
        //   + (5 * 6) (frágil) = 114.00
        //   * 1.05 (Sul) = 119.70
        //   Bronze -> 119.70
        // Total: 486.00 + 119.70 = 605.70
        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());
        assertThat(total).as("DT Regra 4: Combinação de descontos e Frete Bronze")
                         .isEqualByComparingTo("605.70");
    }
}