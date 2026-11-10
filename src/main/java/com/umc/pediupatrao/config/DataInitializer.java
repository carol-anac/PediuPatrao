package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo,
                                   PasswordEncoder encoder,
                                   @Value("${app.seed.admin-password:teste123}") String senhaAdmin,
                                   @Value("${app.seed.gerente-password:gerente123}") String senhaGerente,
                                   @Value("${app.seed.atendente-password:atendente123}") String senhaAtendente) {
        return args -> {
            garantirUsuario(repo, encoder, "admin", senhaAdmin, Perfil.ADMIN);
            garantirUsuario(repo, encoder, "gerente", senhaGerente, Perfil.GERENTE);
            garantirUsuario(repo, encoder, "atendente", senhaAtendente, Perfil.ATENDENTE);
        };
    }

    private void garantirUsuario(UsuarioRepository repo, PasswordEncoder encoder,
                                 String username, String senha, Perfil perfil) {
        var existente = repo.findByUsername(username);

        if (existente.isEmpty()) {
            Usuario novo = new Usuario();
            novo.setUsername(username);
            novo.setPassword(encoder.encode(senha));
            novo.setRole(perfil);
            repo.save(novo);
            log.info("Usuário '{}' criado com o perfil {}.", username, perfil);
        } else if (existente.get().getRole() == null) {
            Usuario antigo = existente.get();
            antigo.setRole(perfil);
            repo.save(antigo);
            log.info("Usuário '{}' sem perfil: atribuído {}.", username, perfil);
        }
    }
}