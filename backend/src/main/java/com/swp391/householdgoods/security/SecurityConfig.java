package com.swp391.householdgoods.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final SecurityResponseHandlers handlers;
    private final CredentialSessionFilter credentialSessionFilter;

    public SecurityConfig(SecurityResponseHandlers handlers, CredentialSessionFilter credentialSessionFilter) {
        this.handlers = handlers;
        this.credentialSessionFilter = credentialSessionFilter;
    }

    @Bean
    org.springframework.boot.web.servlet.FilterRegistrationBean<CredentialSessionFilter> credentialFilterRegistration() {
        var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(credentialSessionFilter);
        registration.setEnabled(false); // Runs once inside Spring Security, after context is loaded.
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        csrfRepository.setCookieCustomizer(
                cookie -> cookie.path("/").sameSite("Lax"));

        CsrfTokenRequestAttributeHandler csrfRequestHandler =
                new CsrfTokenRequestAttributeHandler();

        csrfRequestHandler.setCsrfRequestAttributeName("_csrf");

        http.addFilterAfter(credentialSessionFilter,
                org.springframework.security.web.context.SecurityContextHolderFilter.class);
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(authorize -> authorize

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.svg",
                                "/assets/**",
                                "/error",
                                "/actuator/health")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/catalog/**",
                                "/api/v1/products/**",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/me",
                                "/api/v1/auth/verify-email")
                        .permitAll()
                        .requestMatchers("/api/v1/products/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/resend-verification")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/products",
                                "/api/v1/products/**")
                        .denyAll()
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/products",
                                "/api/v1/products/**")
                        .denyAll()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/products",
                                "/api/v1/products/**")
                        .denyAll()
                        .requestMatchers("/api/v1/cart", "/api/v1/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/account/profile").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/purchasing/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/staff/**")
                        .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/api/**")
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(handlers.loginSuccess())
                        .failureHandler(handlers.loginFailure())
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .logoutSuccessHandler(
                                handlers.logoutSuccess()))

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                handlers.authenticationEntryPoint())
                        .accessDeniedHandler(
                                handlers.accessDenied()))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(
                                Customizer.withDefaults()));

        return http.build();
    }
}
