package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public record ProdutoMaisVendido(
        String produtoId,
        String nomeProduto,
        long quantidadeVendida,
        BigDecimal valorVendido) {
}
