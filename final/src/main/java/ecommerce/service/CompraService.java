package ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import ecommerce.entity.TipoProduto;
import ecommerce.external.IEstoqueExternal;
import ecommerce.external.IPagamentoExternal;
import jakarta.transaction.Transactional;

@Service
public class CompraService
{

	private final CarrinhoDeComprasService carrinhoService;
	private final ClienteService clienteService;

	private final IEstoqueExternal estoqueExternal;
	private final IPagamentoExternal pagamentoExternal;

	private static final BigDecimal DIVISOR_PESO_CUBICO = BigDecimal.valueOf(6000);
	private static final BigDecimal TAXA_FRAGIL = BigDecimal.valueOf(5.00);
	private static final BigDecimal TAXA_MINIMA_FRETE = BigDecimal.valueOf(12.00);

	private static final BigDecimal FRETE_FAIXA_B = BigDecimal.valueOf(2.00);
	private static final BigDecimal FRETE_FAIXA_C = BigDecimal.valueOf(4.00);
	private static final BigDecimal FRETE_FAIXA_D = BigDecimal.valueOf(7.00);

    private static final BigDecimal DESCONTO_QTD_3 = BigDecimal.valueOf(0.05);
    private static final BigDecimal DESCONTO_QTD_5 = BigDecimal.valueOf(0.10);
    private static final BigDecimal DESCONTO_QTD_8 = BigDecimal.valueOf(0.15);

    private static final BigDecimal DESCONTO_VALOR_500 = BigDecimal.valueOf(0.10);
    private static final BigDecimal DESCONTO_VALOR_1000 = BigDecimal.valueOf(0.20);

	@Autowired
	public CompraService(CarrinhoDeComprasService carrinhoService, ClienteService clienteService,
			IEstoqueExternal estoqueExternal, IPagamentoExternal pagamentoExternal)
	{
		this.carrinhoService = carrinhoService;
		this.clienteService = clienteService;

		this.estoqueExternal = estoqueExternal;
		this.pagamentoExternal = pagamentoExternal;
	}

	@Transactional
	public CompraDTO finalizarCompra(Long carrinhoId, Long clienteId)
	{
		Cliente cliente = clienteService.buscarPorId(clienteId);
		CarrinhoDeCompras carrinho = carrinhoService.buscarPorCarrinhoIdEClienteId(carrinhoId, cliente);

		List<Long> produtosIds = carrinho.getItens().stream().map(i -> i.getProduto().getId())
				.collect(Collectors.toList());
		List<Long> produtosQtds = carrinho.getItens().stream().map(i -> i.getQuantidade()).collect(Collectors.toList());

		DisponibilidadeDTO disponibilidade = estoqueExternal.verificarDisponibilidade(produtosIds, produtosQtds);

		if (!disponibilidade.disponivel())
		{
			throw new IllegalStateException("Itens fora de estoque.");
		}

		BigDecimal custoTotal = calcularCustoTotal(carrinho, cliente.getRegiao(), cliente.getTipo());

		PagamentoDTO pagamento = pagamentoExternal.autorizarPagamento(cliente.getId(), custoTotal.doubleValue());

		if (!pagamento.autorizado())
		{
			throw new IllegalStateException("Pagamento não autorizado.");
		}

		EstoqueBaixaDTO baixaDTO = estoqueExternal.darBaixa(produtosIds, produtosQtds);

		if (!baixaDTO.sucesso())
		{
			pagamentoExternal.cancelarPagamento(cliente.getId(), pagamento.transacaoId());
			throw new IllegalStateException("Erro ao dar baixa no estoque.");
		}

		CompraDTO compraDTO = new CompraDTO(true, pagamento.transacaoId(), "Compra finalizada com sucesso.");

		return compraDTO;
	} 

	public BigDecimal calcularCustoTotal(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		
		if (carrinho == null || carrinho.getItens() == null || carrinho.getItens().isEmpty()) {
			throw new IllegalArgumentException("Carrinho não pode ser nulo ou vazio.");
		}
		if (regiao == null) {
			throw new IllegalArgumentException("Região não pode ser nula.");
		}
		if (tipoCliente == null) {
			throw new IllegalArgumentException("Tipo de cliente não pode ser nulo.");
		}

		BigDecimal subtotalComDescontoTipo = calcularSubtotalComDescontoPorTipo(carrinho);
		BigDecimal subtotalComDescontoValor = aplicarDescontoPorValorCarrinho(subtotalComDescontoTipo);
		BigDecimal frete = calcularFrete(carrinho, regiao, tipoCliente);

		BigDecimal total = subtotalComDescontoValor.add(frete);
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calcularSubtotalComDescontoPorTipo(CarrinhoDeCompras carrinho) {
		Map<TipoProduto, List<ItemCompra>> itensPorTipo = carrinho.getItens().stream()
				.collect(Collectors.groupingBy(item -> item.getProduto().getTipo()));

		BigDecimal subtotalGeral = BigDecimal.ZERO;

		for (Map.Entry<TipoProduto, List<ItemCompra>> entry : itensPorTipo.entrySet()) {
			List<ItemCompra> itens = entry.getValue();

			BigDecimal subtotalTipo = BigDecimal.ZERO;
			int quantidadeTotalTipo = 0;

			for (ItemCompra item : itens) {
				BigDecimal preco = item.getProduto().getPreco();
				BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade());
				subtotalTipo = subtotalTipo.add(preco.multiply(qtd));
				quantidadeTotalTipo += item.getQuantidade();
			}

			BigDecimal desconto = obterDescontoPorQuantidade(quantidadeTotalTipo);
			BigDecimal subtotalComDesconto = subtotalTipo.multiply(BigDecimal.ONE.subtract(desconto));

			subtotalGeral = subtotalGeral.add(subtotalComDesconto);
		}

		return subtotalGeral;
	}

	private BigDecimal obterDescontoPorQuantidade(int quantidade) {
		if (quantidade >= 8)
			return DESCONTO_QTD_8;
		if (quantidade >= 5)
			return DESCONTO_QTD_5;
		if (quantidade >= 3)
			return DESCONTO_QTD_3;
		return BigDecimal.ZERO;
	}

	private BigDecimal aplicarDescontoPorValorCarrinho(BigDecimal subtotal) {
		if (subtotal.compareTo(BigDecimal.valueOf(1000)) > 0) {
			return subtotal.multiply(BigDecimal.ONE.subtract(DESCONTO_VALOR_1000));
		} else if (subtotal.compareTo(BigDecimal.valueOf(500)) > 0) {
			return subtotal.multiply(BigDecimal.ONE.subtract(DESCONTO_VALOR_500));
		} else {
			return subtotal;
		}
	}

	private BigDecimal calcularPesoTributavel(Produto produto) {
		BigDecimal pesoCubico = produto.getComprimento()
				.multiply(produto.getLargura())
				.multiply(produto.getAltura())
				.divide(DIVISOR_PESO_CUBICO, 2, RoundingMode.HALF_UP);
		return produto.getPesoFisico().max(pesoCubico);
	}

	private BigDecimal calcularFrete(CarrinhoDeCompras carrinho, Regiao regiao, TipoCliente tipoCliente) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
		BigDecimal taxaFragilTotal = BigDecimal.ZERO;

		for (ItemCompra item : carrinho.getItens()) {
			BigDecimal qtd = BigDecimal.valueOf(item.getQuantidade());
			BigDecimal pesoItem = calcularPesoTributavel(item.getProduto()).multiply(qtd);
			pesoTotal = pesoTotal.add(pesoItem);

			if (item.getProduto().isFragil() != null && item.getProduto().isFragil()) {
				taxaFragilTotal = taxaFragilTotal.add(TAXA_FRAGIL.multiply(qtd));
			}
		}

		BigDecimal valorPorKg = BigDecimal.ZERO;
		boolean temTaxaMinima = false;

		if (pesoTotal.compareTo(BigDecimal.valueOf(5)) <= 0) {
			valorPorKg = BigDecimal.ZERO;
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(10)) <= 0) {
			valorPorKg = FRETE_FAIXA_B;
			temTaxaMinima = true;
		} else if (pesoTotal.compareTo(BigDecimal.valueOf(50)) <= 0) {
			valorPorKg = FRETE_FAIXA_C;
			temTaxaMinima = true;
		} else {
			valorPorKg = FRETE_FAIXA_D;
			temTaxaMinima = true;
		}

		BigDecimal freteBase = valorPorKg.multiply(pesoTotal);

		if (temTaxaMinima) {
			freteBase = freteBase.add(TAXA_MINIMA_FRETE);
		}

		freteBase = freteBase.add(taxaFragilTotal);

		freteBase = freteBase.multiply(regiao.getMultiplicador());

		switch (tipoCliente) {
			case OURO -> freteBase = BigDecimal.ZERO;
			case PRATA -> freteBase = freteBase.multiply(BigDecimal.valueOf(0.5));
			case BRONZE -> {
			}
		}

		return freteBase;
	}
}
