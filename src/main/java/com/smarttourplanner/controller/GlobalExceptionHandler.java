package com.smarttourplanner.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DistrictNotFoundException.class)
    public ModelAndView handleNotFound() {
        ModelAndView notFound = new ModelAndView("not-found");
        notFound.setStatus(HttpStatus.NOT_FOUND);
        return notFound;
    }
}
