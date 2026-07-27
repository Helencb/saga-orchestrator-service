package helen.com.sagaorchestratorservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * NOTA: requer a dependência spring-boot-starter-security no pom.xml/build.gradle
 * (o zip enviado não continha um arquivo de build - adicione manualmente).
 *
 * Antes: management.endpoints.web.exposure=* sem NENHUMA autenticação -
 * qualquer pessoa na rede conseguia ler env vars, beans, heapdump etc.
 * Agora: actuator exige Basic Auth; endpoints de negócio (/api/v1/sagas/**)
 * também exigem autenticação, exceto o health check.
 *
 * /api/v1/sagas/** aceita duas formas de autenticação: Basic Auth (acesso direto/
 * local) ou os headers X-User-Id/X-User-Role propagados pelo api_gateway
 * (GatewayHeaderAuthenticationFilter) - assim o gateway não precisa reautenticar
 * via Basic Auth depois de já ter validado o JWT do usuário.
 */
@Configuration
public class SecurityConfig {

    @Value("${security.actuator.username}")
    private String actuatorUsername;

    @Value("${security.actuator.password}")
    private String actuatorPassword;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.withUsername(actuatorUsername)
                .password(encoder.encode(actuatorPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter() {
        return new GatewayHeaderAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http, GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless, sem sessão de browser
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/sagas/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(gatewayHeaderAuthenticationFilter, BasicAuthenticationFilter.class)
                .httpBasic(basic -> {});

        return http.build();
    }
}