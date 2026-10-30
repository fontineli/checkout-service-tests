package ecommerce.entity;

import java.math.BigDecimal;

public enum Regiao
{
	SUDESTE(BigDecimal.valueOf(1.00)),
	SUL(BigDecimal.valueOf(1.05)),
	NORDESTE(BigDecimal.valueOf(1.10)),
	CENTRO_OESTE(BigDecimal.valueOf(1.20)),
	NORTE(BigDecimal.valueOf(1.30));

	private final BigDecimal multiplicador;

	Regiao(BigDecimal multiplicador)
	{
		this.multiplicador = multiplicador;
	}

	public BigDecimal getMultiplicador()
	{
		return multiplicador;
	}
}
