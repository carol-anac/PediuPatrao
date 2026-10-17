package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProdutoService {

    private static final double PRECO_MAXIMO = 10000.0;

    @Autowired
    private ProdutoRepository produtoRepository;

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto novoProduto(Produto produto) {
        produto.setId(null);
        validar(produto);
        return produtoRepository.save(produto);
    }

    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public void excluir(String id) {
        if (id == null || !produtoRepository.existsById(id)) {
            throw new RegraNegocioException("Produto não encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    /** Cria (sem id) ou atualiza (com id) um produto, validando os dados. */
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Produto salvar(Produto produto) {
        if (produto.getId() != null && !produtoRepository.existsById(produto.getId())) {
            throw new RegraNegocioException("Produto não encontrado para atualização.");
        }
        validar(produto);
        return produtoRepository.save(produto);
    }

    private void validar(Produto p) {
        String nome = p.getNome() == null ? "" : p.getNome().trim();
        if (nome.length() < 2 || nome.length() > 80) {
            throw new RegraNegocioException("O nome do produto deve ter de 2 a 80 caracteres.");
        }
        String tipo = p.getTipo() == null ? "" : p.getTipo().trim();
        if (tipo.isEmpty() || tipo.length() > 40) {
            throw new RegraNegocioException("Informe o tipo do produto.");
        }
        if (p.getPreco() == null || p.getPreco().isNaN() || p.getPreco() <= 0 || p.getPreco() > PRECO_MAXIMO) {
            throw new RegraNegocioException("O preço deve ser maior que zero e no máximo " + (int) PRECO_MAXIMO + ".");
        }
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(BigDecimal.valueOf(p.getPreco()).setScale(2, RoundingMode.HALF_UP).doubleValue());
        if (p.getAtivo() == null) {
            p.setAtivo(Boolean.TRUE);
        }
    }
}