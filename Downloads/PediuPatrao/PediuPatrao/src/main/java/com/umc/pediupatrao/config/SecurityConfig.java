package com.umc.pediupatrao.config;

import com.umc.pediupatrao.service.UsuarioDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String GERENTE = "GERENTE";
    private static final String ATENDENTE = "ATENDENTE";

    private final UsuarioDetailsService usuarioDetailsService;

    public SecurityConfig(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(usuarioDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // Requisições a /api/** sem login recebem 401 (e não um redirecionamento ao login)
        RequestMatcher apiRequest = request -> request.getRequestURI().startsWith("/api/");

        http
                // CSRF permanece LIGADO (formulários Thymeleaf e fetch() enviam o token)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/dist/**", "/plugins/**", "/js/**", "/images/**").permitAll()

                        // Administração de usuários: somente ADMIN
                        .requestMatchers("/usuarios", "/usuarios/**", "/api/usuarios", "/api/usuarios/**",
                                "/admin", "/admin/**").hasRole(ADMIN)

                        // Auditoria (RF05): ADMIN e GERENTE
                        .requestMatchers("/auditoria", "/auditoria/**", "/api/auditoria", "/api/auditoria/**")
                                .hasAnyRole(ADMIN, GERENTE)

                        // Descontos e cancelamentos (RF04): SOMENTE o GERENTE
                        .requestMatchers("/api/pedidos/*/desconto", "/api/pedidos/*/cancelar").hasRole(GERENTE)

                        // Pedidos
                        .requestMatchers(HttpMethod.GET, "/pedidos", "/api/pedidos", "/api/pedidos/**")
                                .hasAnyRole(ADMIN, GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/pedidos/**").hasAnyRole(GERENTE, ATENDENTE)

                        // Clientes
                        .requestMatchers(HttpMethod.GET, "/clientes", "/api/clientes", "/api/clientes/**")
                                .hasAnyRole(GERENTE, ATENDENTE)
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole(GERENTE)
                        .requestMatchers("/clientes/novo", "/clientes/salvar", "/clientes/editar/**",
                                "/api/clientes", "/api/clientes/**").hasRole(ATENDENTE)

                        // Produtos
                        .requestMatchers(HttpMethod.GET, "/produtos", "/api/produtos", "/api/produtos/**")
                                .hasAnyRole(ADMIN, GERENTE, ATENDENTE)
                        .requestMatchers("/produtos/**", "/api/produtos", "/api/produtos/**")
                                .hasAnyRole(ADMIN, GERENTE)

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), apiRequest)
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                        .successHandler(authenticationSuccessHandler())
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // com CSRF ligado, o logout precisa ser POST
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.sendRedirect("/");
        };
    }
}