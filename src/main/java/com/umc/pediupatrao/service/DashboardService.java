package com.umc.pediupatrao.service;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("yyyy-MM");
    private final PedidoRepository pedidoRepository;

    public DashboardService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public DashboardResumo resumo(LocalDate inicio, LocalDate fim) {
        List<Pedido> pedidos = pedidosDoPeriodo(inicio, fim).stream()
                .filter(this::contaComoVenda)
                .toList();
        BigDecimal vendas = pedidos.stream()
                .map(Pedido::getTotal)
                .filter(valor -> valor != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        long emAndamento = pedidos.stream()
                .filter(pedido -> pedido.getStatus() != null && pedido.getStatus() != StatusPedido.FINALIZADO)
                .count();
        BigDecimal ticket = pedidos.isEmpty()
                ? ZERO
                : vendas.divide(BigDecimal.valueOf(pedidos.size()), 2, RoundingMode.HALF_UP);
        return new DashboardResumo(vendas, pedidos.size(), ticket, emAndamento);
    }

    public List<VendaPorPeriodo> vendasPorPeriodo(LocalDate inicio, LocalDate fim, String agrupamento) {
        Periodo periodo = normalizarPeriodo(inicio, fim);
        String modo = agrupamento == null || agrupamento.isBlank() ? "dia" : agrupamento.trim().toLowerCase();
        if (!modo.equals("dia") && !modo.equals("mes")) {
            throw new RegraNegocioException("O agrupamento deve ser 'dia' ou 'mes'.");
        }

        Map<String, Acumulador> acumulados = new TreeMap<>();
        for (Pedido pedido : pedidosDoPeriodo(periodo.inicio(), periodo.fim())) {
            if (!contaComoVenda(pedido) || pedido.getTotal() == null) continue;
            LocalDate dia = pedido.getDataEntrada().toLocalDate();
            String chave = modo.equals("dia") ? dia.format(DIA) : YearMonth.from(dia).format(MES);
            acumulados.computeIfAbsent(chave, ignorado -> new Acumulador()).adicionar(pedido.getTotal());
        }

        List<VendaPorPeriodo> resultado = new ArrayList<>();
        if (modo.equals("dia")) {
            for (LocalDate dia = periodo.inicio(); !dia.isAfter(periodo.fim()); dia = dia.plusDays(1)) {
                adicionarPeriodo(resultado, acumulados, dia.format(DIA));
            }
        } else {
            YearMonth primeiro = YearMonth.from(periodo.inicio());
            YearMonth ultimo = YearMonth.from(periodo.fim());
            for (YearMonth mes = primeiro; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
                adicionarPeriodo(resultado, acumulados, mes.format(MES));
            }
        }
        return resultado;
    }

    public List<PedidoPorStatus> pedidosPorStatus(LocalDate inicio, LocalDate fim) {
        Map<StatusPedido, Long> contagem = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) contagem.put(status, 0L);
        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            if (pedido.getStatus() != null) contagem.compute(pedido.getStatus(), (chave, valor) -> valor + 1);
        }
        return contagem.entrySet().stream()
                .map(item -> new PedidoPorStatus(item.getKey(), item.getValue()))
                .toList();
    }

    public List<ProdutoMaisVendido> produtosMaisVendidos(LocalDate inicio, LocalDate fim, int limite) {
        if (limite < 1 || limite > 20) {
            throw new RegraNegocioException("O limite deve estar entre 1 e 20.");
        }
        Map<String, ProdutoAcumulado> produtos = new LinkedHashMap<>();
        for (Pedido pedido : pedidosDoPeriodo(inicio, fim)) {
            if (!contaComoVenda(pedido) || pedido.getItens() == null) continue;
            for (ItemPedido item : pedido.getItens()) {
                if (item == null) continue;
                String id = item.getProdutoId() == null || item.getProdutoId().isBlank()
                        ? item.getNomeProduto() : item.getProdutoId();
                if (id == null || id.isBlank()) continue;
                ProdutoAcumulado acumulado = produtos.computeIfAbsent(id,
                        ignorado -> new ProdutoAcumulado(item.getProdutoId(), item.getNomeProduto()));
                acumulado.quantidade += item.getQuantidade();
                if (item.getSubtotal() != null) acumulado.valor = acumulado.valor.add(item.getSubtotal());
            }
        }
        return produtos.values().stream()
                .sorted((a, b) -> {
                    int porQuantidade = Long.compare(b.quantidade, a.quantidade);
                    return porQuantidade != 0 ? porQuantidade : a.nome.compareToIgnoreCase(b.nome);
                })
                .limit(limite)
                .map(item -> new ProdutoMaisVendido(item.id, item.nome, item.quantidade,
                        item.valor.setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<Pedido> pedidosDoPeriodo(LocalDate inicio, LocalDate fim) {
        Periodo periodo = normalizarPeriodo(inicio, fim);
        return pedidoRepository.findAll().stream()
                .filter(pedido -> pedido.getDataEntrada() != null)
                .filter(pedido -> {
                    LocalDate dia = pedido.getDataEntrada().toLocalDate();
                    return !dia.isBefore(periodo.inicio()) && !dia.isAfter(periodo.fim());
                })
                .toList();
    }

    private Periodo normalizarPeriodo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now();
        LocalDate dataFim = fim == null ? (inicio == null ? hoje : inicio.plusDays(29)) : fim;
        LocalDate dataInicio = inicio == null ? dataFim.minusDays(29) : inicio;
        if (dataInicio.isAfter(dataFim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à data final.");
        }
        if (ChronoUnit.DAYS.between(dataInicio, dataFim) > 365) {
            throw new RegraNegocioException("O período consultado não pode ultrapassar 366 dias.");
        }
        return new Periodo(dataInicio, dataFim);
    }

    private boolean contaComoVenda(Pedido pedido) {
        return pedido.getStatus() != StatusPedido.CANCELADO;
    }

    private void adicionarPeriodo(List<VendaPorPeriodo> resultado, Map<String, Acumulador> acumulados, String chave) {
        Acumulador acumulador = acumulados.getOrDefault(chave, new Acumulador());
        resultado.add(new VendaPorPeriodo(chave, acumulador.total.setScale(2, RoundingMode.HALF_UP), acumulador.pedidos));
    }

    private record Periodo(LocalDate inicio, LocalDate fim) { }

    private static final class Acumulador {
        private BigDecimal total = BigDecimal.ZERO;
        private long pedidos;
        private void adicionar(BigDecimal valor) {
            total = total.add(valor);
            pedidos++;
        }
    }

    private static final class ProdutoAcumulado {
        private final String id;
        private final String nome;
        private long quantidade;
        private BigDecimal valor = BigDecimal.ZERO;
        private ProdutoAcumulado(String id, String nome) {
            this.id = id;
            this.nome = nome == null || nome.isBlank() ? "Produto sem nome" : nome;
        }
    }
}
