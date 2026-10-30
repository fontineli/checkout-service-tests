package ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ecommerce.dto.CompraDTO;
import ecommerce.dto.DisponibilidadeDTO;
import ecommerce.dto.EstoqueBaixaDTO;
import ecommerce.dto.PagamentoDTO;
import ecommerce.entity.CarrinhoDeCompras;
import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;

/**
 * Testes de unidade para CompraService.finalizarCompra usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
public class CompraServiceFinalizarCompraMockitoTest {

    @Mock
    private CarrinhoDeComprasService carrinhoService;

    @Mock
    private ClienteService clienteService;

    @Mock
    private IEstoqueExternal estoqueExternal;

    @Mock
    private IPagamentoExternal pagamentoExternal;

    private CompraService service;

    @BeforeEach
    void setUp() {
        service = new CompraService(carrinhoService, clienteService, estoqueExternal, pagamentoExternal);
    }

    @Test
    @DisplayName("Deve finalizar compra com sucesso quando há estoque, pagamento autorizado e baixa ok")
    void finalizarCompra_sucesso() {
        Long carrinhoId = 1L;
        Long clienteId = 10L;

        // Arrange: monta cliente e carrinho usando o TestHelper da própria base
        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("100.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(produto, 1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        // ids e quantidades que o service vai extrair do carrinho
        List<Long> produtosIds = Arrays.asList(produto.getId());
        List<Long> produtosQtds = Arrays.asList(item.getQuantidade());

        // Stubs dos serviços internos
        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente))
                .thenReturn(carrinho);

        // Mock do serviço de estoque
        when(estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds))
                .thenReturn(new DisponibilidadeDTO(true, List.of()));

        when(estoqueExternal.darBaixa(produtosIds, produtosQtds))
                .thenReturn(new EstoqueBaixaDTO(true));

        // Mock do serviço de pagamento
        when(pagamentoExternal.autorizarPagamento(eq(cliente.getId()), anyDouble()))
                .thenReturn(new PagamentoDTO(true, 999L));

        // Act
        CompraDTO resultado = service.finalizarCompra(carrinhoId, clienteId);

        // Assert
        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.transacaoPagamentoId()).isEqualTo(999L);
        assertThat(resultado.mensagem()).contains("Compra finalizada com sucesso");

        // Verifica interações importantes
        verify(clienteService).buscarPorId(clienteId);
        verify(carrinhoService).buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);
        verify(estoqueExternal).verificarDisponibilidade(produtosIds, produtosQtds);
        verify(estoqueExternal).darBaixa(produtosIds, produtosQtds);
        verify(pagamentoExternal).autorizarPagamento(eq(cliente.getId()), anyDouble());
        verify(pagamentoExternal, never()).cancelarPagamento(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Deve lançar exceção quando itens estão fora de estoque e não chamar pagamento")
    void finalizarCompra_itensForaDeEstoque() {
        Long carrinhoId = 1L;
        Long clienteId = 10L;

        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("100.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(produto, 1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        List<Long> produtosIds = Arrays.asList(produto.getId());
        List<Long> produtosQtds = Arrays.asList(item.getQuantidade());

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente))
                .thenReturn(carrinho);

        // Estoque indisponível
        when(estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds))
                .thenReturn(new DisponibilidadeDTO(false, List.of(produto.getId())));

        // Act + Assert
        assertThatThrownBy(() -> service.finalizarCompra(carrinhoId, clienteId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Itens fora de estoque");

        // Não deve chamar pagamento nem baixa em estoque
        verifyNoInteractions(pagamentoExternal);
        verify(estoqueExternal, never()).darBaixa(any(), any());
    }

    @Test
    @DisplayName("Deve cancelar pagamento quando a baixa de estoque falhar")
    void finalizarCompra_falhaNaBaixaEstoque_cancelaPagamento() {
        Long carrinhoId = 1L;
        Long clienteId = 10L;

        Cliente cliente = TestHelper.criarCliente(TipoCliente.BRONZE, Regiao.SUDESTE);
        Produto produto = TestHelper.criarProduto("P1", new BigDecimal("100.00"), new BigDecimal("6.0"), false);
        ItemCompra item = TestHelper.criarItem(produto, 1L);

        CarrinhoDeCompras carrinho = new CarrinhoDeCompras();
        carrinho.setItens(Arrays.asList(item));

        List<Long> produtosIds = Arrays.asList(produto.getId());
        List<Long> produtosQtds = Arrays.asList(item.getQuantidade());

        when(clienteService.buscarPorId(clienteId)).thenReturn(cliente);
        when(carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente))
                .thenReturn(carrinho);

        when(estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds))
                .thenReturn(new DisponibilidadeDTO(true, List.of()));

        Long transacaoId = 123L;
        when(pagamentoExternal.autorizarPagamento(eq(cliente.getId()), anyDouble()))
                .thenReturn(new PagamentoDTO(true, transacaoId));

        // Falha ao dar baixa no estoque
        when(estoqueExternal.darBaixa(produtosIds, produtosQtds))
                .thenReturn(new EstoqueBaixaDTO(false));

        assertThatThrownBy(() -> service.finalizarCompra(carrinhoId, clienteId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Erro ao dar baixa no estoque");

        // Verifica se o pagamento foi cancelado com os parâmetros corretos
        verify(pagamentoExternal).cancelarPagamento(cliente.getId(), transacaoId);
    }
}