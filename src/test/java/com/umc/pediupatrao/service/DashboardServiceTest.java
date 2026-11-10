package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.DashboardResumo;
import com.umc.pediupatrao.dto.PedidoPorStatus;
import com.umc.pediupatrao.dto.ProdutoMaisVendido;
import com.umc.pediupatrao.dto.VendaPorPeriodo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(pedidoRepository);
    }

    @Test
    void resumoSomaVendasIgnoraCanceladosECalculaTicketMedio() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.RECEBIDO, "100.00", "2026-11-10"),
                pedido(StatusPedido.FINALIZADO, "50.00", "2026-11-10"),
                pedido(StatusPedido.CANCELADO, "999.00", "2026-11-10")));

        DashboardResumo resumo = service.resumo(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"));

        assertEquals(new BigDecimal("150.00"), resumo.totalVendas());
        assertEquals(2, resumo.quantidadePedidos());
        assertEquals(new BigDecimal("75.00"), resumo.ticketMedio());
        assertEquals(1, resumo.pedidosEmAndamento());
    }

    @Test
    void vendasPorDiaIncluiDatasSemPedidosEExcluiCancelados() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.PRONTO, "42.50", "2026-11-10"),
                pedido(StatusPedido.CANCELADO, "300.00", "2026-11-11")));

        List<VendaPorPeriodo> vendas = service.vendasPorPeriodo(
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-12"), "dia");

        assertEquals(3, vendas.size());
        assertEquals("2026-11-10", vendas.get(0).periodo());
        assertEquals(new BigDecimal("42.50"), vendas.get(0).totalVendas());
        assertEquals(new BigDecimal("0.00"), vendas.get(1).totalVendas());
        assertEquals(new BigDecimal("0.00"), vendas.get(2).totalVendas());
    }

    @Test
    void rankingAgrupaItensDoMesmoProdutoEOrdenaPorQuantidade() {
        Pedido primeiro = pedido(StatusPedido.PRONTO, "100.00", "2026-11-10");
        primeiro.setItens(List.of(item("pizza", "Pizza de Calabresa", 2, "50.00"),
                item("suco", "Suco", 3, "30.00")));
        Pedido segundo = pedido(StatusPedido.FINALIZADO, "75.00", "2026-11-10");
        segundo.setItens(List.of(item("pizza", "Pizza de Calabresa", 3, "75.00")));
        when(pedidoRepository.findAll()).thenReturn(List.of(primeiro, segundo));

        List<ProdutoMaisVendido> ranking = service.produtosMaisVendidos(
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"), 5);

        assertEquals("Pizza de Calabresa", ranking.get(0).nomeProduto());
        assertEquals(5, ranking.get(0).quantidadeVendida());
        assertEquals(new BigDecimal("125.00"), ranking.get(0).valorVendido());
    }

    @Test
    void contagemPorStatusRetornaTodosOsStatusMesmoSemPedidos() {
        when(pedidoRepository.findAll()).thenReturn(List.of(
                pedido(StatusPedido.RECEBIDO, "25.00", "2026-11-10"),
                pedido(StatusPedido.RECEBIDO, "30.00", "2026-11-10"),
                pedido(StatusPedido.CANCELADO, "10.00", "2026-11-10")));

        List<PedidoPorStatus> contagem = service.pedidosPorStatus(
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"));

        assertEquals(StatusPedido.values().length, contagem.size());
        assertEquals(2, contagem.stream().filter(item -> item.status() == StatusPedido.RECEBIDO)
                .findFirst().orElseThrow().quantidade());
        assertEquals(0, contagem.stream().filter(item -> item.status() == StatusPedido.PRONTO)
                .findFirst().orElseThrow().quantidade());
    }

    @Test
    void rejeitaIntervaloInvertidoEAgrupamentoDesconhecido() {
        assertThrows(RegraNegocioException.class, () -> service.resumo(
                LocalDate.parse("2026-11-11"), LocalDate.parse("2026-11-10")));
        assertThrows(RegraNegocioException.class, () -> service.vendasPorPeriodo(
                LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-10"), "semana"));
    }

    private Pedido pedido(StatusPedido status, String total, String data) {
        Pedido pedido = new Pedido();
        pedido.setStatus(status);
        pedido.setTotal(new BigDecimal(total));
        pedido.setDataEntrada(LocalDateTime.parse(data + "T12:00:00"));
        return pedido;
    }

    private ItemPedido item(String id, String nome, int quantidade, String subtotal) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(id);
        item.setNomeProduto(nome);
        item.setQuantidade(quantidade);
        item.setSubtotal(new BigDecimal(subtotal));
        return item;
    }
}
