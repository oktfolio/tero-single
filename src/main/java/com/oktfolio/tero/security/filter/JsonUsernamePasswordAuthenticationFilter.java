package com.oktfolio.tero.security.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oktfolio.tero.security.exception.ContentTypeNullException;
import com.oktfolio.tero.security.exception.MethodNotSupportedException;
import com.oktfolio.tero.security.model.UsernamePassword;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author oktfolio oktfolio@gmail.com
 * @date 2020/06/11
 */
public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JsonUsernamePasswordAuthenticationFilter.class);

    // ~ Static fields/initializers
    // =====================================================================================

    private static final AntPathRequestMatcher DEFAULT_ANT_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher("/login", "POST");

    private boolean postOnly = true;

    // ~ Constructors
    // ===================================================================================================

    public JsonUsernamePasswordAuthenticationFilter() {
        super();
    }

    public JsonUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
        super.setAuthenticationManager(authenticationManager);
    }

    // ~ Methods
    // ========================================================================================================

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) throws AuthenticationException {
        if (postOnly && !HttpMethod.POST.name().equals(request.getMethod())) {
            throw new MethodNotSupportedException(
                    "Authentication method not supported: " + request.getMethod());
        }

        if (request.getContentType() == null) {
            throw new ContentTypeNullException(
                    "ContentType not supported: " + request.getContentType());
        }

        if (request.getContentType().contains(MediaType.APPLICATION_JSON_VALUE)) {
            logger.info("a json login request, continue");
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken("", "");
            try (InputStream inputStream = request.getInputStream()) {
                JsonParser parser = new ObjectMapper().createParser(inputStream);
                UsernamePassword usernamePassword = parser.readValueAs(UsernamePassword.class);
                if (usernamePassword != null && StringUtils.isNoneEmpty(usernamePassword.getUsername())) {
                    authRequest = new UsernamePasswordAuthenticationToken(
                            StringUtils.trim(usernamePassword.getUsername()),
                            usernamePassword.getPassword());
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("failed to obtain json from request, {}", e.getMessage());
                authRequest = new UsernamePasswordAuthenticationToken(
                        "", "");
            }
            setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);
        } else {
            // if not JSON request，continue with attemptAuthentication
            logger.info("not a json request, use default UsernamePasswordAuthenticationFilter");
            return super.attemptAuthentication(request, response);
        }
    }

    /**
     * Provided so that subclasses may configure what is put into the authentication
     * request's details property.
     *
     * @param request     that an authentication request is being created for
     * @param authRequest the authentication request object that should have its details
     *                    set
     */
    @Override
    protected void setDetails(HttpServletRequest request,
                              UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

    /**
     * Defines whether only HTTP POST requests will be allowed by this filter. If set to
     * true, and an authentication request is received which is not a POST request, an
     * exception will be raised immediately and authentication will not be attempted. The
     * <tt>unsuccessfulAuthentication()</tt> method will be called as if handling a failed
     * authentication.
     * <p>
     * Defaults to <tt>true</tt> but may be overridden by subclasses.
     */
    @Override
    public void setPostOnly(boolean postOnly) {
        this.postOnly = postOnly;
    }

}
