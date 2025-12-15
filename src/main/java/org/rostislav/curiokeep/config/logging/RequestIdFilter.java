package org.rostislav.curiokeep.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String rid = req.getHeader("X-Request-Id");
        if (rid == null || rid.isBlank()) rid = UUID.randomUUID().toString();

        MDC.put("rid", rid);
        res.setHeader("X-Request-Id", rid);

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.remove("rid");
        }
    }
}
