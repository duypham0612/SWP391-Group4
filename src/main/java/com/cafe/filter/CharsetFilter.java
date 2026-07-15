package com.cafe.filter;

import jakarta.servlet.*;

import java.io.IOException;

/**
 * Ép UTF-8 cho request/response. Đăng ký qua web.xml (không dùng @WebFilter
 * để tránh đăng ký trùng và để kiểm soát thứ tự filter).
 */
public class CharsetFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig config) {
        String e = config.getInitParameter("encoding");
        if (e != null && !e.isBlank()) encoding = e;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding(encoding);
        }
        resp.setCharacterEncoding(encoding);
        chain.doFilter(req, resp);
    }
}
