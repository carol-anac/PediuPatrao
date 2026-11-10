package com.umc.pediupatrao.entity;

import java.util.List;

/**
 * Estados de um pedido e suas transições válidas (RF03).
 *
 * RECEBIDO -> EM_PREPARACAO -> PRONTO -> (SAIU_PARA_ENTREGA | RETIRADO) -> FINALIZADO
 * CANCELADO é excepcional: só é alcançado pela operação de cancelamento (RF04),
 * nunca por uma transição comum, e só antes da saída do pedido.
 * FINALIZADO e CANCELADO são estados finais.
 */
public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    /** Próximos estados permitidos por transição comum (não inclui CANCELADO). */
    public List<StatusPedido> proximos() {
        return switch (this) {
            case RECEBIDO -> List.of(EM_PREPARACAO);
            case EM_PREPARACAO -> List.of(PRONTO);
            case PRONTO -> List.of(SAIU_PARA_ENTREGA, RETIRADO);
            case SAIU_PARA_ENTREGA, RETIRADO -> List.of(FINALIZADO);
            case FINALIZADO, CANCELADO -> List.of();
        };
    }

    public boolean podeIrPara(StatusPedido destino) {
        return destino != null && proximos().contains(destino);
    }

    /** Verdadeiro enquanto o pedido ainda não saiu (permite desconto e cancelamento). */
    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }

    /** Verdadeiro para o registro de saída (entrega ou retirada). */
    public boolean ehSaida() {
        return this == SAIU_PARA_ENTREGA || this == RETIRADO;
    }
}