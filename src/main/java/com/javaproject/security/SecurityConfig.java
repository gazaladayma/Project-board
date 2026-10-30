package com.javaproject.security;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final String ROLE_MANAGER = "MANAGER";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public ApplicationRunner initializeSecurityUsers(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(50) NOT NULL PRIMARY KEY,
                    password VARCHAR(500) NOT NULL,
                    enabled BOOLEAN NOT NULL
                )
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS authorities (
                    username VARCHAR(50) NOT NULL,
                    authority VARCHAR(50) NOT NULL,
                    CONSTRAINT fk_authorities_users
                        FOREIGN KEY (username) REFERENCES users(username)
                )
                """);

            jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username
                ON authorities (username, authority)
                """);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoggingAccessDeniedHandler accessDeniedHandler) {

        http
            .authorizeHttpRequests(auth -> auth
                // Block the embedded database console.
                .requestMatchers("/h2-console/**").denyAll()

                // Manager-only user administration and write operations.
                .requestMatchers("/newUser").hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.POST, "/addUser").hasRole(ROLE_MANAGER)
                .requestMatchers("/manager", "/manager/**").hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.GET, "/secured/addBoardGame")
                    .hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.GET, "/*/reviews/*")
                    .hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.POST, "/boardgameAdded")
                    .hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.POST, "/deleteReview/**")
                    .hasRole(ROLE_MANAGER)
                .requestMatchers(HttpMethod.POST, "/boardgames")
                    .hasRole(ROLE_MANAGER)

                // Authenticated user pages and review creation.
                .requestMatchers("/user", "/user/**", "/secured/**")
                    .hasAnyRole("USER", ROLE_MANAGER)
                .requestMatchers(HttpMethod.POST, "/reviewAdded")
                    .hasAnyRole("USER", ROLE_MANAGER)

                // Public pages and read-only REST endpoints.
                .requestMatchers(
                    "/", "/login",
                    "/permission-denied", "/error",
                    "/css/**", "/js/**", "/images/**",
                    "/favicon.ico"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/boardgames", "/boardgames/**")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/{id}")
                    .permitAll()
                .requestMatchers(HttpMethod.GET, "/{id}/reviews")
                    .permitAll()

                // No broad permitAll fallback.
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/secured")
                .permitAll()
            )
            .logout(logout -> logout
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}
