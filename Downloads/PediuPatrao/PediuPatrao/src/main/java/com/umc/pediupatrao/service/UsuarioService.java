package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Service;

@Service
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioService {

    private static final Pattern USERNAME_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    public Iterable<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    /** Só copia username, senha e perfil: id e outros campos enviados são ignorados. */
    public Usuario criarUsuario(Usuario dados) {
        String username = validarUsername(dados.getUsername());
        validarSenha(dados.getPassword());
        Perfil perfil = exigirPerfil(dados.getRole());

        if (usuarioRepository.existsByUsername(username)) {
            throw new RegraNegocioException("Já existe um usuário com este username.");
        }

        Usuario novo = new Usuario();
        novo.setUsername(username);
        novo.setPassword(passwordEncoder.encode(dados.getPassword()));
        novo.setRole(perfil);
        return usuarioRepository.save(novo);
    }

    public Usuario atualizarUsuario(String id, Usuario dados) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        String username = validarUsername(dados.getUsername());
        Perfil novoPerfil = exigirPerfil(dados.getRole());
        boolean proprioUsuario = existente.getUsername().equals(usuarioLogado());

        // Proteção contra autoelevação
        if (proprioUsuario && novoPerfil != existente.getRole()) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio perfil.");
        }
        if (proprioUsuario && !username.equals(existente.getUsername())) {
            throw new RegraNegocioException("Você não pode alterar o seu próprio username.");
        }
        // Nunca ficar sem nenhum ADMIN
        if (existente.getRole() == Perfil.ADMIN && novoPerfil != Perfil.ADMIN
                && usuarioRepository.countByRole(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa manter ao menos um ADMIN.");
        }
        if (!username.equals(existente.getUsername()) && usuarioRepository.existsByUsername(username)) {
            throw new RegraNegocioException("Já existe um usuário com este username.");
        }

        existente.setUsername(username);
        existente.setRole(novoPerfil);

        // Só altera a senha se uma nova foi informada
        if (dados.getPassword() != null && !dados.getPassword().isBlank()) {
            validarSenha(dados.getPassword());
            existente.setPassword(passwordEncoder.encode(dados.getPassword()));
        }
        return usuarioRepository.save(existente);
    }

    public void deletarUsuario(String id) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));

        if (existente.getUsername().equals(usuarioLogado())) {
            throw new RegraNegocioException("Você não pode excluir o seu próprio usuário.");
        }
        if (existente.getRole() == Perfil.ADMIN && usuarioRepository.countByRole(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa manter ao menos um ADMIN.");
        }
        usuarioRepository.deleteById(id);
    }

    // ---------- validações e utilitários ----------

    private String usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private String validarUsername(String username) {
        if (username == null || !USERNAME_VALIDO.matcher(username.trim()).matches()) {
            throw new RegraNegocioException(
                    "Username inválido: use de 3 a 30 caracteres (letras, números, ponto, hífen ou sublinhado).");
        }
        return username.trim();
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
    }

    private Perfil exigirPerfil(Perfil perfil) {
        if (perfil == null) {
            throw new RegraNegocioException("O perfil é obrigatório.");
        }
        return perfil;
    }
}