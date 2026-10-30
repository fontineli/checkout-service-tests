package ecommerce.service;

import java.math.BigDecimal;

import ecommerce.entity.Cliente;
import ecommerce.entity.ItemCompra;
import ecommerce.entity.Produto;
import ecommerce.entity.Regiao;
import ecommerce.entity.TipoCliente;
import ecommerce.entity.TipoProduto;

/**
 * Classe auxiliar estática para criar dados de teste (Mocks/Stubs).
 */
public class TestHelper {

    /**
     * Cria um cliente com os dados especificados.
     */
    public static Cliente criarCliente(TipoCliente tipo, Regiao regiao) {
        return new Cliente(1L, "Cliente Teste", regiao, tipo);
    }

    /**
     * Cria um produto com dados padrão, permitindo sobrepor o essencial.
     * * @param preco Preço unitário.
     * @param pesoFisico Peso em KG. Usar > 5 para ativar frete.
     * @param fragil Se o item é frágil (adiciona taxa).
     * @return Um objeto Produto.
     */
    public static Produto criarProduto(String nome, BigDecimal preco, BigDecimal pesoFisico, boolean fragil) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setTipo(TipoProduto.ALIMENTO); // Tipo padrão
        produto.setPesoFisico(pesoFisico);
        produto.setComprimento(BigDecimal.valueOf(10)); // (C*L*A)/6000 = 0.0016 kg (irrelevante)
        produto.setLargura(BigDecimal.valueOf(10));
        produto.setAltura(BigDecimal.valueOf(1));
        produto.setFragil(fragil);
        return produto;
    }

    /**
     * Cria um item de compra.
     */
    public static ItemCompra criarItem(Produto produto, long quantidade) {
        return new ItemCompra(1L, produto, quantidade);
    }
}