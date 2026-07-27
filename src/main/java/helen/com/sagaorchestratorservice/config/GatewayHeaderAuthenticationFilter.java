package helen.com.sagaorchestratorservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Confia nos headers X-User-Id/X-User-Role já validados pelo api_gateway (que
 * autentica via JWT Bearer token e propaga esses headers), em vez de exigir
 * Basic Auth de novo para requisições que já passaram pelo gateway.
 *
 * ATENÇÃO: só é seguro enquanto este serviço não estiver acessível diretamente
 * da rede externa - qualquer cliente que fale direto com a porta 8087 poderia
 * forjar esses headers e se autenticar como ADMIN. A rede precisa isolar o
 * serviço para que só o gateway consiga alcançá-lo.
 *
 * Se os headers não vierem (ex.: chamada direta/local), o request segue sem
 * autenticação por aqui e cai no Basic Auth, que continua ativo como fallback.
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {
    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);

        if (userId != null && !userId.isBlank() && role != null && !role.isBlank()) {
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
