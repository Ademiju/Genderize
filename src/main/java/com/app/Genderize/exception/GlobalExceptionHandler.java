package com.app.Genderize.exception;

import com.app.Genderize.response.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("MethodArgumentNotValidException", ex);
        GenericResponse<Object> response = new GenericResponse<>();
        if(ex.getBindingResult().hasErrors()) {
            StringBuilder errorBuilder = new StringBuilder();
            List<ObjectError> errors = ex.getBindingResult().getAllErrors();
            for(ObjectError error : errors) {
                if(error == errors.getLast()) {
                    errorBuilder.append(error.getDefaultMessage());
                    break;
                }
                errorBuilder.append(error.getDefaultMessage());
                errorBuilder.append(", ");
            }
            response.setMessage(errorBuilder.toString());
        }else{
            response.setMessage(ex.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    private ResponseEntity<GenericResponse<Object>> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("MissingServletRequestParameterException", ex);
        return new ResponseEntity<>(GenericResponse.builder().status("error").message(ex.getMessage()).build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Object>> handleException(Exception ex) {
        log.error("Exception", ex);
        return new ResponseEntity<>(GenericResponse.builder().status("error").message(ex.getMessage()).build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
