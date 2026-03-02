package com.example.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

@Component
@Profile("!test") 
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;
    
    private static final List<String> PUBLIC_PREFIXES = List.of(
        "/scap/**",
        "/scap/api/login",
        "/scap/api/cas",
        "/scap/api/filter",
        "/scap/api/validate-usucod",
        "/scap/health",
        "/scap/api/sical",
        "/scap/api/rpc/call",
        "/scap/login"
    );
    
    // Extensions de fichiers statiques à ignorer
    private static final List<String> STATIC_EXTENSIONS = List.of(
        ".js", ".css", ".html", ".ico", ".json", ".png", ".jpg", ".jpeg", 
        ".gif", ".svg", ".woff", ".woff2", ".ttf", ".eot"
    );

    public JwtAuthFilter(JwtUtil jwt) { 
        this.jwt = jwt; 
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        System.out.println("JwtAuthFilter: Processing path=" + path + " method=" + req.getMethod());

        // 1. Ignorer les requêtes OPTIONS (CORS preflight)
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            chain.doFilter(req, res);
            return;
        }

        // 2. Ignorer les fichiers statiques (Angular)
        if (isStaticResource(path)) {
            System.out.println("JwtAuthFilter: Static resource, skipping auth for " + path);
            chain.doFilter(req, res);
            return;
        }

        // 3. Ignorer les chemins publics de l'API
        if (isPublic(path)) {
            System.out.println("JwtAuthFilter: Public path, skipping auth for " + path);
            chain.doFilter(req, res);
            return;
        }

        // 4. Vérifier JWT pour les API protégées
        System.out.println("JwtAuthFilter: Protected path, checking JWT for " + path);

        String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            System.out.println("JwtAuthFilter: Missing or invalid Authorization header");
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String token = auth.substring(7);
        try {
            String user = jwt.validateAndGetSubject(token);
            if (user == null) {
                System.out.println("JwtAuthFilter: Invalid token");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            System.out.println("JwtAuthFilter OK user=" + user + " path=" + path);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(req, res);
        } catch (Exception e) {
            System.out.println("JwtAuthFilter: Exception validating token: " + e.getMessage());
            SecurityContextHolder.clearContext();
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Vérifie si le chemin correspond à une ressource statique
     */
    private boolean isStaticResource(String path) {
        // Racine ou index.html
        if (path.equals("/") || path.equals("/scap") || path.equals("/scap/") || 
            path.endsWith("/index.html")) {
            return true;
        }
        
        // Dossier assets
        if (path.contains("/assets/")) {
            return true;
        }
        
        // Extensions de fichiers statiques
        String lowerPath = path.toLowerCase();
        return STATIC_EXTENSIONS.stream().anyMatch(lowerPath::endsWith);
    }

    /**
     * Vérifie si le chemin est une API publique
     */
    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}