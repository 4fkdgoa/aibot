package net.autocrm.api.exception;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {

    private HttpStatusCode status;
    private String message;
    private List<String> errors;
    private Map<?,?> rtn;

    ApiError(HttpStatusCode status, String message, Map<?,?> rtn) {
        super();
        this.status = status;
        this.message = message;
        this.rtn = rtn;
    }

    ApiError(HttpStatusCode status, String message, List<String> errors) {
        super();
        this.status = status;
        this.message = message;
        this.errors = errors;
    }

    ApiError(HttpStatusCode status, String message, String error) {
        super();
        this.status = status;
        this.message = message;
        errors = Arrays.asList(error);
    }

    ApiError(HttpStatusCode status, String message) {
    	this(status, message, message);
    }
}