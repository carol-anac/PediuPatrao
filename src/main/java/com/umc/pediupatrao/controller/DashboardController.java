package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardResumo;
import com.umc.pediupatrao.dto.PedidoPorStatus;
import com.umc.pediupatrao.dto.ProdutoMaisVendido;
import com.umc.pediupatrao.dto.VendaPorPeriodo;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public DashboardResumo resumo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboardService.resumo(inicio, fim);
    }

    @GetMapping("/vendas")
    public List<VendaPorPeriodo> vendas(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                        @RequestParam(defaultValue = "dia") String agrupamento) {
        return dashboardService.vendasPorPeriodo(inicio, fim, agrupamento);
    }

    @GetMapping("/pedidos-por-status")
    public List<PedidoPorStatus> pedidosPorStatus(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return dashboardService.pedidosPorStatus(inicio, fim);
    }

    @GetMapping("/produtos-mais-vendidos")
    public List<ProdutoMaisVendido> produtosMaisVendidos(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                                          @RequestParam(defaultValue = "5") int limite) {
        return dashboardService.produtosMaisVendidos(inicio, fim, limite);
    }
}
