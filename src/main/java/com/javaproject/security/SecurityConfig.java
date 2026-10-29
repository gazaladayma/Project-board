package com.javaproject.security;

import javax.sql.DataSource;

import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    public ApplicationRunner initializeSecurityUsers(
            JdbcTemplate jdbcTemplate,
            JdbcUserDetailsManager userDetailsManager,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.seed-demo-users:false}") boolean seedDemoUsers) {

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

            if (seedDemoUsers) {
                if (!userDetailsManager.userExists("bugs")) {
                    userDetailsManager.createUser(
                        User.withUsername("bugs")
                            .password(passwordEncoder.encode("bunny"))
                            .roles("USER")
                            .build()
                    );
                }

                if (!userDetailsManager.userExists("daffy")) {
                    userDetailsManager.createUser(
                        User.withUsername("daffy")
                            .password(passwordEncoder.encode("duck"))
                            .roles("USER", "MANAGER")
                            .build()
                    );
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            LoggingAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Block the embedded database console.
                .requestMatchers("/h2-console/**").denyAll()

                // Manager-only page and write operations.
                .requestMatchers("/manager", "/manager/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET, "/secured/addBoardGame")
                    .hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET, "/*/reviews/*")
                    .hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/boardgameAdded")
                    .hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/deleteReview/**")
                    .hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/boardgames")
                    .hasRole("MANAGER")

                // Authenticated user pages and review creation.
                .requestMatchers("/user", "/user/**", "/secured/**")
                    .hasAnyRole("USER", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/reviewAdded")
                    .hasAnyRole("USER", "MANAGER")

                // Public pages and read-only REST endpoints.
                .requestMatchers(
                    "/", "/newUser", "/addUser", "/login",
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
