package org.maggus.mikedb.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SimpleCORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, HEAD, PATCH, CONNECT");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Accept, Origin, X-Requested-With, Content-Type, " +
                "Last-Modified, Content-Length, API_KEY, SESSION_ID, " +
                "Upgrade, Sec-WebSocket-Extensions, Sec-WebSocket-Key, Sec-WebSocket-Version");
        chain.doFilter(req, res);
    }

    @Override
    public void destroy() {

    }
}