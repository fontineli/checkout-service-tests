package ecommerce.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;

public class CompraServiceRobustezTest {

    private CompraService service;
    private Cliente clienteValido;
    private Produto produtoValido;
    private CarrinhoDeCompras carrinhoValido;

    @BeforeEach
    public void setup() {
        service = new CompraService(null, null, null, null);
        clienteValido = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        produtoValido = TestHelper.criarProduto("Valido", BigDecimal.TEN, BigDecimal.ONE, false);
        
        carrinhoValido = new CarrinhoDeCompras();
        carrinhoValido.setItens(Arrays.asList(TestHelper.criarItem(produtoValido, 1L)));
    }

    @Test
    @DisplayName("Deve lançar exceção quando Carrinho for nulo")
    public void calcularCustoTotal_quandoCarrinhoNulo_entaoLancaIllegalArgumentException() {
        assertThatThrownBy(() -> service.calcularCustoTotal(null, Regiao.SUDESTE, TipoCliente.BRONZE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Carrinho não pode ser nulo ou vazio.");
    }
    
    @Test
    @DisplayName("Deve lançar exceção quando a Lista de Itens for nula")
    public void calcularCustoTotal_quandoItensNulos_entaoLancaIllegalArgumentException() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(null); // Lista de itens nula

        assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Carrinho não pode ser nulo ou vazio.");
    }

    @Test
    @DisplayName("Deve lançar exceção quando a Lista de Itens for vazia")
    public void calcularCustoTotal_quandoItensVazios_entaoLancaIllegalArgumentException() {
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Collections.emptyList()); // Lista vazia

        assertThatThrownBy(() -> service.calcularCustoTotal(carrinho, Regiao.SUDESTE, TipoCliente.BRONZE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Carrinho não pode ser nulo ou vazio.");
    }
    
    @Test
    @DisplayName("Deve lançar exceção quando Regiao for nula")
    public void calcularCustoTotal_quandoRegiaoNula_entaoLancaIllegalArgumentException() {
        assertThatThrownBy(() -> service.calcularCustoTotal(carrinhoValido, null, TipoCliente.BRONZE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Região não pode ser nula.");
    }
    
    @Test
    @DisplayName("Deve lançar exceção quando TipoCliente for nulo")
    public void calcularCustoTotal_quandoTipoClienteNulo_entaoLancaIllegalArgumentException() {
        assertThatThrownBy(() -> service.calcularCustoTotal(carrinhoValido, Regiao.SUDESTE, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Tipo de cliente não pode ser nulo.");
    }

    @Test
    @DisplayName("Deve calcular corretamente quando 'fragil' for nulo (não adicionar taxa)")
    public void robustez_quandoProdutoFragilNulo_naoAdicionaTaxa() {
        // 1. Given
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto produtoComFragilNulo = TestHelper.criarProduto("Produto", new BigDecimal("50.00"), new BigDecimal("6.0"), false);
        produtoComFragilNulo.setFragil(null); // Força o campo 'fragil' a ser nulo

        ItemCompra item = TestHelper.criarItem(produtoComFragilNulo, 1L);
        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // 2. When
        // Subtotal: 50.00
        // Frete: Peso 6kg, SE, Bronze.
        //   (6*2) + 12 = 24.00
        //   Taxa Frágil deve ser 0 (pois isFragil == null)
        // Total: 50.00 + 24.00 = 74.00
        BigDecimal total = service.calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

        // 3. Then
        assertThat(total)
            .as("Não deve adicionar taxa de R$5 se 'fragil' for nulo, cobrindo o '!= null' check")
            .isEqualByComparingTo("74.00");
    }
}