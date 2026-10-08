package user.controller;

import exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import user.domain.User;
import user.service.UserService;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registration(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user,
                           @RequestParam String passwordConfirmation,
                           RedirectAttributes redirectAttributes) {
        try {
            if (user.getPasswordHash() == null || !user.getPasswordHash().equals(passwordConfirmation)) {
                throw new ValidationException("Password confirmation does not match.");
            }
            userService.registerInsuranceOfficerAccount(user);
            redirectAttributes.addFlashAttribute("successMessage", "Account created. You can now sign in.");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            user.setPasswordHash(null);
            redirectAttributes.addFlashAttribute("user", user);
            return "redirect:/register";
        }
    }
}
