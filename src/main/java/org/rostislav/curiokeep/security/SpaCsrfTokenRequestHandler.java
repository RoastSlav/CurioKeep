package org.rostislav.curiokeep.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * CSRF handling for a single-page app that keeps the token in a cookie and echoes it in a header.
 * <p>
 * The token is generated on every response so the cookie is always present before the first state-changing request. A token
 * sent in the header is the raw cookie value; a token sent any other way must be the masked form, which protects against BREACH.
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        csrfToken.get(); // deferred by default; reading it is what writes the cookie
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        boolean fromHeader = StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()));
        return (fromHeader ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
