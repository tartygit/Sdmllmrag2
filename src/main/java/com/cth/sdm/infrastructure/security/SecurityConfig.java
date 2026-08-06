package com.cth.sdm.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    @Value("${app.security.provider:db}")
    private String securityProvider;

    @Value("${app.security.ldap.url:ldap://localhost:8389}")
    private String ldapUrl;

    @Value("${app.security.ldap.base-dn:dc=cth,dc=com}")
    private String ldapBaseDn;

    @Value("${app.security.ldap.user-dn-pattern:uid={0},ou=users}")
    private String ldapUserDnPattern;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator);
        return expressionHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBaseDn);
        // In a real AD/LDAP environment, we might set manager user DN and password:
        // contextSource.setUserDn("cn=admin,dc=cth,dc=com");
        // contextSource.setPassword("adminpassword");
        return contextSource;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            PasswordEncoder passwordEncoder,
            LdapContextSource contextSource
    ) {
        if ("ldap".equalsIgnoreCase(securityProvider)) {
            // Configure LDAP/Active Directory Authenticator
            BindAuthenticator authenticator = new BindAuthenticator(contextSource);
            authenticator.setUserDnPatterns(new String[]{ldapUserDnPattern});

            LdapAuthenticationProvider ldapProvider = new LdapAuthenticationProvider(authenticator);
            // Optionally, we can define custom LDAP authorities populator here if needed:
            // ldapProvider.setAuthoritiesPopulator(...);

            return new ProviderManager(List.of(ldapProvider));
        } else {
            // Configure standard DB Authenticator
            DaoAuthenticationProvider dbProvider = new DaoAuthenticationProvider();
            dbProvider.setUserDetailsService(userDetailsService);
            dbProvider.setPasswordEncoder(passwordEncoder);

            return new ProviderManager(List.of(dbProvider));
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
