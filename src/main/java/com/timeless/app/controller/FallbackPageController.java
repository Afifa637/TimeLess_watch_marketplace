package com.timeless.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class FallbackPageController {

    @RequestMapping({
            "/{path:[^\\.]*}",
            "/{path:[^\\.]*}/{subpath:[^\\.]*}",
            "/{path:[^\\.]*}/{subpath:[^\\.]*}/{subpath2:[^\\.]*}"
    })
    public String fallback(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/")
                || uri.startsWith("/buyer/")
                || uri.startsWith("/seller/")
                || uri.startsWith("/admin/")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/uploads/")
                || uri.startsWith("/error")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return "error/404";
    }
}