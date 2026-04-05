package com.timeless.app.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class ErrorPageController implements ErrorController {

    @GetMapping("/error/401")
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public String unauthorizedPage() {
        return "error/401";
    }

    @GetMapping("/error/403")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbiddenPage() {
        return "error/403";
    }

    @GetMapping("/error/500")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverErrorPage() {
        return "error/500";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == 401) {
                return "error/401";
            }
            if (statusCode == 403) {
                return "error/403";
            }
            if (statusCode == 404) {
                return "error/404";
            }
            if (statusCode == 500) {
                return "error/500";
            }
        }

        return "error/500";
    }
}