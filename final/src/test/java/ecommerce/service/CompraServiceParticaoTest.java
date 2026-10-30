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

public class CompraServiceParticaoTest {

    private CompraService service;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);
    }

    @Test
    @DisplayName("PE: Bronze, Sub < 500, Qtd < 3, Peso (Faixa B), Não Frágil, Sudeste")
    public void particao_Bronze_Sub50_Qtd1_Peso6_NaoFragil_Sudeste() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("50.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(produto, 1L); 
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

        assertThat(total).as("Cenário PE: Bronze, Faixa B, Sudeste, Qtd=1")
                         .isEqualByComparingTo("74.00");
    }

    @Test
    @DisplayName("PE: Prata, Sub < 500, Qtd (3-4), Peso (Faixa C), Frágil, Nordeste")
    public void particao_Prata_Sub120_Qtd4_Peso15_Fragil_Nordeste() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.PRATA, Regiao.NORDESTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("30.00"), new BigDecimal("15.0"), true);
        ItemCompra item = TestHelper.criarItem(produto, 4L); 
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

        assertThat(total).as("Cenário PE: Prata, Faixa D (60kg), Nordeste, Qtd=4, Frágil")
                         .isEqualByComparingTo("362.60");
    }

    @Test
    @DisplayName("PE: Bronze, Sub > 500, Qtd (5-7), Peso (Faixa D), Não Frágil, Norte")
    public void particao_Bronze_Sub600_Qtd6_Peso60_NaoFragil_Norte() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.NORTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("100.00"), new BigDecimal("10.0"), false);
        ItemCompra item = TestHelper.criarItem(produto, 6L); 
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

        assertThat(total).as("Cenário PE: Bronze, Faixa D, Norte, Qtd=6, Sub>500")
                         .isEqualByComparingTo("1047.60");
    }
    
    @Test
    @DisplayName("PE: Ouro, Sub > 1000, Qtd (8+), Peso (Isento), Frágil, Sul")
    public void particao_Ouro_Sub1200_Qtd10_Peso4_Fragil_Sul() {
        Cliente cliente = TestHelper.criarCliente(TipoCliente.OURO, Regiao.SUL);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("120.00"), new BigDecimal("0.4"), true);
        ItemCompra item = TestHelper.criarItem(produto, 10L); 
        
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

        assertThat(total).as("Cenário PE: Ouro, Faixa Isenta, Sul, Qtd=10, Sub>1000")
                         .isEqualByComparingTo("816.00");
    }
}