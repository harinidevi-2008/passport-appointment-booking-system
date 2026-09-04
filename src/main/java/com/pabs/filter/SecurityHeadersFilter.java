package com.pabs.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse httpResponse) {
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
            httpResponse.setHeader("Content-Security-Policy",
                    "default-src 'self'; style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; "
                            + "script-src 'self' https://cdn.jsdelivr.net; img-src 'self' data:; "
                            + "font-src 'self' https://cdn.jsdelivr.net; frame-ancestors 'self'");
        }
        chain.doFilter(request, response);
    }
}
