package org.kkobi.exception;

import lombok.extern.log4j.Log4j2;
import org.kkobi.users.dto.response.MessageResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@Log4j2
public class CommonExceptionAdvice {

    // DTO 필드 검증 실패 메시지를 JSON으로 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError == null ? "요청 값을 확인해 주세요." : fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    // 이메일 또는 닉네임 중복 오류를 JSON으로 반환
    @ExceptionHandler(DuplicateUserException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleDuplicateUser(DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 동시에 들어온 가입 요청이 DB 고유 제약조건과 충돌한 경우 JSON으로 반환
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleDuplicateKey(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new MessageResponse("이미 사용 중인 이메일 또는 닉네임입니다."));
    }

    // 비밀번호 등 비즈니스 규칙 검증 오류를 JSON으로 반환
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }

    // KIS Open API 호출 실패를 JSON으로 반환
    @ExceptionHandler(KisApiException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleKisApi(KisApiException ex) {
        log.warn("KIS API 호출 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new MessageResponse(ex.getMessage()));
    }

    // 요청한 종목이 존재하지 않을 때 JSON으로 반환
    @ExceptionHandler(SecurityNotFoundException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleSecurityNotFound(SecurityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse(ex.getMessage()));
    }

    // @RequestParam·@PathVariable 타입 변환 실패를 JSON으로 반환
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName();
        Object value = ex.getValue();
        return ResponseEntity.badRequest()
                .body(new MessageResponse(field + " 파라미터 값이 올바르지 않습니다: " + value));
    }

    // @ModelAttribute 바인딩 실패(예: enum 변환 실패)를 JSON으로 반환
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public ResponseEntity<MessageResponse> handleBind(BindException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message;
        if (fieldError == null) {
            message = "요청 값을 확인해 주세요.";
        } else {
            message = fieldError.getField() + " 파라미터 값이 올바르지 않습니다: "
                    + fieldError.getRejectedValue();
        }
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public String except(Exception ex, Model model){

        log.error("Exception ...." + ex.getMessage());
        model.addAttribute("exception", ex);
        log.error(model);
        return "error_page";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle404(NoHandlerFoundException ex, Model model, HttpServletRequest request) {
        log.error(ex);
        model.addAttribute("uri", request.getRequestURI());
        return "custom404";
    }
}
