package org.rostislav.curiokeep.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rostislav.curiokeep.user.AppUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SetupModeFilter extends OncePerRequestFilter {

    private final AppUserRepository users;

    public SetupModeFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        boolean setupRequired = !users.existsByIsAdminTrue();

        if (!setupRequired) {
            chain.doFilter(req, res);
            return;
        }

        String path = req.getRequestURI();

        boolean allowed =
                path.startsWith("/api/setup/") ||
                        path.equals("/") || path.equals("/index.html") ||
                        path.startsWith("/assets/") || path.startsWith("/css/") || path.startsWith("/js/") ||
                        path.startsWith("/images/") || path.equals("/favicon.ico");

        if (allowed) {
            chain.doFilter(req, res);
            return;
        }

        if (path.startsWith("/api/")) {
            res.setStatus(403);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"SETUP_REQUIRED\"}");
            return;
        }

        res.sendRedirect("/");
    }
}
