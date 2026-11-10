package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

public record DashboardResumo(
        BigDecimal totalVendas,
        long quantidadePedidos,
        BigDecimal ticketMedio,
        long pedidosEmAndamento) {
}
