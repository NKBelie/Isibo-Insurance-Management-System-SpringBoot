package exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class UiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler({ValidationException.class, BusinessRuleException.class, DuplicateResourceException.class})
    public String handleBusinessIssue(RuntimeException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/500";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("message", ex.getMessage() == null ? "An unexpected error occurred." : ex.getMessage());
        return "error/500";
    }
}
