package com.example.base_api.config;

import com.example.base_api.usuario.entity.Papel;
import com.example.base_api.usuario.entity.Usuario;
import com.example.base_api.usuario.repository.PapelRepository;
import com.example.base_api.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedAdminUser(
            UsuarioRepository usuarioRepository,
            PapelRepository papelRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (usuarioRepository.existsByUsername("admin")) {
                return;
            }

            Papel adminRole = papelRepository.findByNome("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("Papel ADMIN não encontrado. Verifique as migrations."));

            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@igreja.local");
            admin.setSenhaHash(passwordEncoder.encode("admin123"));
            admin.setAtivo(true);
            admin.setPapeis(Set.of(adminRole));
            usuarioRepository.save(admin);

            log.info("Usuário admin criado (username=admin). Altere a senha em produção.");
        };
    }
}
