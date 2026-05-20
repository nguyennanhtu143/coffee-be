package org.example.coffee.config;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - không cần đăng nhập
                .requestMatchers("/api/v1/user/sign-up", "/api/v1/user/log-in",
                        "/api/v1/user/verify-register-otp", "/api/v1/user/resend-register-otp",
                        "/api/v1/user/forgot-password", "/api/v1/user/verify-password-reset-otp",
                        "/api/v1/user/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/product/get-products").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/product/get-products-by-category").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/product/get-details").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/product/get-products-by-search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/category/get-categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/category/get").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/comment/get-comments").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/product-size/get").permitAll()

                // Shipping address data (public)
                .requestMatchers(HttpMethod.GET, "/api/v1/shipping/provinces").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/shipping/districts").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/shipping/wards").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/shipping/calculate-fee").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/shipping/ghn-webhook").permitAll()

                // SePay webhook + cancel unpaid
                .requestMatchers(HttpMethod.POST, "/api/v1/payment/sepay-webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/order/cancel-unpaid").permitAll()
//                .requestMatchers(HttpMethod.POST, "/api/v1/chatbot/ask").permitAll()

                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Tất cả endpoint khác cần đăng nhập
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
