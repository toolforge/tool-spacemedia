package org.wikimedia.commons.donvip.spacemedia.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UnsupportedOperationException.class)
    public void springHandleNotImplemented(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_IMPLEMENTED.value());
    }
}
