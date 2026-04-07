package com.data.service.core.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ReturnUrlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String returnUrl = "/";
        if (request.getSession(false) != null) {
            Object sessionValue = request.getSession(false).getAttribute(AuthController.RETURN_URL_SESSION_ATTRIBUTE);
            if (sessionValue instanceof String stringValue && stringValue.startsWith("/") && !stringValue.startsWith("//")) {
                returnUrl = stringValue;
            }
            request.getSession(false).removeAttribute(AuthController.RETURN_URL_SESSION_ATTRIBUTE);
        }

        response.sendRedirect(returnUrl);
    }
}
