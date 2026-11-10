package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public record VendaPorPeriodo(String periodo, BigDecimal totalVendas, long quantidadePedidos) {
}
