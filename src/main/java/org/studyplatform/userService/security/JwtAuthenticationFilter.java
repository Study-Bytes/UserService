package ru.example.userService.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.example.userService.repository.UserRepository;

import java.io.IOException;

//фильтр извлекает Access токен из заголовка Authentication и при валидном ставит Authentication в SecurityContext

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtUtil.validateToken(token)) {
                    Claims claims = jwtUtil.parseClaims(token);
                    String email = claims.getSubject();
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        var userDetails = userDetailsService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        logger.debug("Authenticated user {} via JWT", email);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("cannot set user authentication: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
