package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;

public record PedidoPorStatus(StatusPedido status, long quantidade) {
}
