package com.winllc.acme.server.service.filter;

import com.winllc.acme.server.persistence.DirectoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

//@Component
public class AcmeServletFilter implements Filter {

    public static String directoryKey = "directoryData";

    @Autowired
    private DirectoryPersistence directoryPersistence;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        //TODO use this somehow

        /*
        Section 6.2
        Because client requests in ACME carry JWS objects in the Flattened JSON Serialization,
        they must have the Content-Type header field set to “application/jose+json”. If a request
        does not meet this requirement, then the server MUST return a response with status code
        415 (Unsupported Media Type).
         */
        HttpServletRequest req = (HttpServletRequest) request;
        final String contentType = req.getContentType();
/*
        if (contentType == null || !contentType.equals("application/jose+json")) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported media type.");
        }else {

            AcmeURL acmeURL = new AcmeURL(req);
            DirectoryData directoryData = directoryPersistence.getByName(acmeURL.getDirectoryIdentifier());
            request.setAttribute(directoryKey, directoryData);
            chain.doFilter(request, response);
        }

 */
    }

}
