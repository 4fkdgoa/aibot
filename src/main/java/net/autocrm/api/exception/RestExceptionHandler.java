package net.autocrm.api.exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import net.autocrm.api.util.CustomHttpRequestBody;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
	ObjectMapper ob = new ObjectMapper();

	@SuppressWarnings("unchecked")
	private ResponseEntity<Object> buildResponseEntity(ApiError apiError, WebRequest request) {
		Map<String, Object> ret = new LinkedHashMap<>();

		Map<String, String> header = null;
		if ( request instanceof ServletWebRequest ) {
			CustomHttpRequestBody req = ((ServletWebRequest) request).getNativeRequest(CustomHttpRequestBody.class);
			if ( req != null ) {
				try {
					Map<?, ?> map = ob.readValue(req.getRawData(), Map.class);
					header = (Map<String, String>)map.get("HEADER");
				} catch (IOException e) {
					e.printStackTrace();
					header = new LinkedHashMap<>();
				}
			} else {
				header = new LinkedHashMap<>();
			}
		} else {
			header = new LinkedHashMap<>();
		}
		header.put("IF_DATE", DateFormatUtils.format(Calendar.getInstance(), "yyyy-MM-dd HH:mm:ss"));
		header.put("IF_SYSTEM", "AutoCRM");
		ret.put("HEADER", header);
		
		Map<?, ?> rtn = apiError.getRtn();
		if ( MapUtils.isEmpty( apiError.getRtn() ) ) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("IF_RST_CD", "99");
			data.put("IF_RST_MSG", "FAIL");
			data.put("CRM_MSG", apiError.getMessage());
			rtn = data;
		}

		ArrayList<Map<String, Object>> datas = new ArrayList<>();
		ret.put("DATA", datas);

		ArrayList<Map<?, ?>> results = new ArrayList<>();
		results.add(rtn);

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("RESULT", results);
		datas.add(data);

//		Map<String, Object> tail = new LinkedHashMap<>();
//		tail.put("IF_RST_CD", "99");
//		tail.put("IF_RST_MSG", "FAIL");
//		tail.put("CRM_MSG", apiError.getMessage());
//		
//		ret.put("TAIL", tail);
		
		// errors 처리는 나중에 필요하면 진행.

		HttpStatusCode status = apiError.getStatus() == null ? HttpStatus.INTERNAL_SERVER_ERROR : apiError.getStatus();

		return new ResponseEntity<>(ret, status);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ApiError apiError = null;
		if ( body != null ) {
			if ( body instanceof String ) {
				String error = (String) body;
				String errMsg = StringUtils.isEmpty(error) ? ExceptionUtils.getRootCauseMessage(ex) : error;
				apiError = new ApiError(status, errMsg, error);
			} else if ( body instanceof Map ) {
				apiError = new ApiError(status, ExceptionUtils.getRootCauseMessage(ex), (Map<?,?>) body);
			} else if ( body instanceof List ) {
				List<String> errors = (List<String>) body;
				String errMsg = CollectionUtils.isEmpty(errors) ? ExceptionUtils.getRootCauseMessage(ex) : errors.get(0);
				apiError = new ApiError(status, errMsg, errors);
			} else {
				apiError = new ApiError(status, ExceptionUtils.getRootCauseMessage(ex));
			}
		} else {
			apiError = new ApiError(status, ExceptionUtils.getRootCauseMessage(ex));
		}
		
		logger.error(ex);

		return buildResponseEntity(apiError, request);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, 
			HttpHeaders headers, 
			HttpStatusCode status, 
			WebRequest request) {
		List<String> errors = new ArrayList<String>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.add(error.getField() + ": " + error.getDefaultMessage());
		}
		for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
			errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
		}

		return handleExceptionInternal(ex, errors, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
			MissingServletRequestParameterException ex, HttpHeaders headers, 
			HttpStatusCode status, WebRequest request) {
		String error = ex.getParameterName() + " parameter is missing";

		return handleExceptionInternal(ex, error, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
	  HttpRequestMethodNotSupportedException ex, 
	  HttpHeaders headers, 
	  HttpStatusCode status, 
	  WebRequest request) {
	    StringBuilder builder = new StringBuilder();
	    builder.append(ex.getMethod());
	    builder.append(
	      " method is not supported for this request. Supported methods are ");
	    ex.getSupportedHttpMethods().forEach(t -> builder.append(t + " "));

		return handleExceptionInternal(ex, builder.toString(), headers, status, request);
	}

	@ExceptionHandler({ ConstraintViolationException.class })
	public ResponseEntity<Object> handleConstraintViolation(
			ConstraintViolationException ex, WebRequest request) {
		List<String> errors = new ArrayList<String>();
		for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
			errors.add(violation.getRootBeanClass().getName() + " " + 
			violation.getPropertyPath() + ": " + violation.getMessage());
		}

		return handleExceptionInternal(ex, errors, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler({ MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Object> handleMethodArgumentTypeMismatch(
			MethodArgumentTypeMismatchException ex, WebRequest request) {
		String error = ex.getName() + " should be of type " + ex.getRequiredType().getName();

		return handleExceptionInternal(ex, error, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler({ GenericRuntimeException.class })
	public ResponseEntity<Object> handleGenericRuntimeException(GenericRuntimeException ex, WebRequest request){
		return handleExceptionInternal(ex, ex.getRtn(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler({ Throwable.class })
	public ResponseEntity<Object> handleThrowable(Throwable ex, WebRequest request){
		ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage(ex), "error");
		return buildResponseEntity(apiError, request);
	}
}
