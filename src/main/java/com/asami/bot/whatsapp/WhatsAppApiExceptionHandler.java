package com.asami.bot.whatsapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = WhatsAppTestController.class)
public class WhatsAppApiExceptionHandler {

    @ExceptionHandler(WhatsAppApiException.class)
    ProblemDetail handleWhatsAppApiException(WhatsAppApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problem.setTitle("WhatsApp API error");
        problem.setDetail(exception.getMessage());
        return problem;
    }
}
