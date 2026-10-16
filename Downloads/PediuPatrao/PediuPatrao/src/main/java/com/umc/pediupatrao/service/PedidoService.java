package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private static final int MAX_LINHAS = 50;
    private static final int MAX_QUANTIDADE = 99;
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
    }

    /**
     * Registra um novo pedido. Nome e preço dos itens vêm do cadastro de produtos
     * e o total é calculado aqui: nada disso é aceito do cliente HTTP.
     */
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido criarPedido(PedidoRequest requisicao) {
        if (requisicao == null) {
            throw new RegraNegocioException("Dados do pedido não informados.");
        }

        String clienteId = requisicao.clienteId();
        if (clienteId == null || clienteId.isBlank() || !clienteRepository.existsById(clienteId)) {
            throw new RegraNegocioException("Cliente não encontrado.");
        }

        List<PedidoRequest.ItemRequest> linhas = requisicao.itens();
        if (linhas == null || linhas.isEmpty()) {
            throw new RegraNegocioException("O pedido deve ter pelo menos um item.");
        }
        if (linhas.size() > MAX_LINHAS) {
            throw new RegraNegocioException("O pedido pode ter no máximo " + MAX_LINHAS + " itens.");
        }

        // Valida cada linha e agrupa linhas repetidas do mesmo produto
        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (PedidoRequest.ItemRequest linha : linhas) {
            if (linha == null || linha.produtoId() == null || linha.produtoId().isBlank()) {
                throw new RegraNegocioException("Há um item sem produto informado.");
            }
            Integer quantidade = linha.quantidade();
            if (quantidade == null || quantidade < 1 || quantidade > MAX_QUANTIDADE) {
                throw new RegraNegocioException(
                        "A quantidade de cada item deve estar entre 1 e " + MAX_QUANTIDADE + ".");
            }
            quantidades.merge(linha.produtoId(), quantidade, Integer::sum);
        }

        List<ItemPedido> itens = new ArrayList<>();
        for (Map.Entry<String, Integer> entrada : quantidades.entrySet()) {
            Produto produto = produtoRepository.findById(entrada.getKey())
                    .orElseThrow(() -> new RegraNegocioException("Produto não encontrado."));

            if (Boolean.FALSE.equals(produto.getAtivo())) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' está inativo.");
            }
            if (produto.getPreco() == null || produto.getPreco() <= 0) {
                throw new RegraNegocioException("O produto '" + produto.getNome() + "' não possui preço válido.");
            }
            int quantidade = entrada.getValue();
            if (quantidade > MAX_QUANTIDADE) {
                throw new RegraNegocioException(
                        "A quantidade total de '" + produto.getNome() + "' não pode passar de " + MAX_QUANTIDADE + ".");
            }

            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);

            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(preco);
            item.setSubtotal(preco.multiply(BigDecimal.valueOf(quantidade)));
            itens.add(item);
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        pedido.setItens(itens);
        pedido.setDescontoPercentual(BigDecimal.ZERO.setScale(2));
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setDataEntrada(LocalDateTime.now());
        pedido.setResponsavelEntrada(usuarioLogado());
        recalcularTotais(pedido);

        return pedidoRepository.save(pedido);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Optional<Pedido> buscarPorId(String id) {
        return pedidoRepository.findById(id);
    }

    /**
     * Provisório (RF02): as transições válidas e os registros de saída serão feitos no RF03,
     * e o cancelamento terá operação própria, exclusiva do GERENTE (RF04).
     */
    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Pedido não encontrado."));

        StatusPedido novoStatus;
        try {
            novoStatus = StatusPedido.valueOf(status == null ? "" : status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RegraNegocioException("Status inválido.");
        }
        if (novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("O cancelamento é uma operação própria, exclusiva do GERENTE.");
        }

        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }

    // ---------- cálculo e utilitários ----------

    /** Recalcula subtotal, desconto e total a partir dos itens. O RF04 reutilizará este método. */
    private void recalcularTotais(Pedido pedido) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.getItens()) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal percentual = pedido.getDescontoPercentual() != null
                ? pedido.getDescontoPercentual() : BigDecimal.ZERO;
        BigDecimal desconto = subtotal.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_UP);

        pedido.setSubtotal(subtotal);
        pedido.setValorDesconto(desconto);
        pedido.setTotal(subtotal.subtract(desconto));
    }

    private String usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}