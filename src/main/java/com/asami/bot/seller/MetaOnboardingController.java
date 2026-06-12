package com.asami.bot.seller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import com.asami.bot.config.WhatsAppProperties;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
public class MetaOnboardingController {
    private final SellerContext context;
    private final MetaOnboardingService onboarding;
    private final SellerRepository sellers;
    private final WhatsAppProperties whatsApp;

    public MetaOnboardingController(
            SellerContext context,
            MetaOnboardingService onboarding,
            SellerRepository sellers,
            WhatsAppProperties whatsApp
    ) {
        this.context = context;
        this.onboarding = onboarding;
        this.sellers = sellers;
        this.whatsApp = whatsApp;
    }

    @PostMapping("/connect")
    public Map<String, String> connect(
            @Valid @RequestBody ConnectMetaRequest request,
            Principal principal
    ) {
        onboarding.connect(
                context.current(principal),
                request.code(),
                request.phoneNumberId(),
                request.businessAccountId()
        );
        return Map.of("status", "CONNECTED");
    }

    @PostMapping("/connect-test-number")
    @Transactional
    public Map<String, String> connectTestNumber(Principal principal) {
        if (isBlank(whatsApp.phoneNumberId()) || isBlank(whatsApp.accessToken())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Le numero Meta de test n'est pas configure"
            );
        }
        Seller current = context.current(principal);
        sellers.findByWhatsappPhoneNumberId(whatsApp.phoneNumberId())
                .filter(existing -> !existing.getId().equals(current.getId()))
                .ifPresent(existing -> {
                    existing.releaseWhatsAppNumber();
                    sellers.saveAndFlush(existing);
                });
        current.releaseWhatsAppNumber();
        current.connectWhatsApp(
                whatsApp.phoneNumberId(),
                "META_TEST_ACCOUNT",
                "+1 555 642 4759"
        );
        sellers.save(current);
        return Map.of(
                "status", "CONNECTED",
                "displayPhone", "+1 555 642 4759"
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ConnectMetaRequest(
            @NotBlank String code,
            @NotBlank String phoneNumberId,
            @NotBlank String businessAccountId
    ) {}
}
