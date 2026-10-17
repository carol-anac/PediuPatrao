package com.umc.pediupatrao.dto;

import java.math.BigDecimal;

/** Percentual de desconto solicitado (0 a 20). O valor em reais é calculado pelo servidor. */
public record DescontoRequest(BigDecimal percentual) {
}