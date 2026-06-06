package com.asami.bot.seller;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SellerRepository sellers;
    private final SellerAccountRepository accounts;
    private final PasswordEncoder encoder;
    public AuthController(SellerRepository sellers, SellerAccountRepository accounts,
                          PasswordEncoder encoder) {
        this.sellers = sellers; this.accounts = accounts; this.encoder = encoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, String> register(@Valid @RequestBody RegisterRequest body,
                                        HttpServletRequest request) throws ServletException {
        String email = body.email().trim().toLowerCase();
        if (accounts.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise");
        }
        Seller seller = sellers.save(new Seller(body.businessName().trim(), null));
        accounts.save(new SellerAccount(seller, email, encoder.encode(body.password())));
        request.login(email, body.password());
        return Map.of("businessName", seller.getBusinessName(), "email", email);
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest body,
                                     HttpServletRequest request) throws ServletException {
        try { request.login(body.email().trim().toLowerCase(), body.password()); }
        catch (ServletException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        return Map.of("email", body.email().trim().toLowerCase());
    }

    @GetMapping("/me")
    public Map<String, String> me(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        SellerAccount account = accounts.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        return Map.of("email", account.getEmail(),
                "businessName", account.getSeller().getBusinessName());
    }

    public record RegisterRequest(@NotBlank String businessName, @Email String email,
                                  @Size(min = 8) String password) {}
    public record LoginRequest(@Email String email, @NotBlank String password) {}
}
