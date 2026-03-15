package com.timeless.app.controller;

import com.timeless.app.entity.Role;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.util.SecurityUtils;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final UserAccountRepository userAccountRepository;

    @ModelAttribute
    public void populateCommonModel(Model model) {
        java.util.Optional<UserPrincipal> currentUser = SecurityUtils.getCurrentUserOptional();
        model.addAttribute("currentUser", currentUser.orElse(null));
        model.addAttribute("isAuthenticated", currentUser.isPresent());
        model.addAttribute("currentRole", currentUser.map(user -> user.getRole().name()).orElse(null));
        model.addAttribute("cartCount", 0L);
        model.addAttribute("wishlistCount", 0L);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminUsersPage(@RequestParam(required = false) String role, Model model) {
        List<UserResponseView> users =
                (role == null || role.isBlank()
                        ? userAccountRepository.findAll()
                        : userAccountRepository.findByRole(Role.valueOf(role.toUpperCase(Locale.ROOT))))
                        .stream()
                        .sorted(Comparator.comparing(com.timeless.app.entity.UserAccount::getCreatedAt).reversed())
                        .map(UserResponseView::from)
                        .toList();

        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role == null ? "" : role);
        return "admin/users";
    }

    public record UserResponseView(
            Long id,
            String email,
            String fullName,
            String role,
            boolean enabled,
            java.time.LocalDateTime createdAt
    ) {
        public static UserResponseView from(com.timeless.app.entity.UserAccount user) {
            return new UserResponseView(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole().name(),
                    user.isEnabled(),
                    user.getCreatedAt()
            );
        }
    }
}