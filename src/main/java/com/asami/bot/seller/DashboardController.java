package com.asami.bot.seller;

import com.asami.bot.catalog.*;
import com.asami.bot.config.MetaSignupProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final SellerContext context;
    private final SellerRepository sellers;
    private final ProductRepository products;
    private final MetaSignupProperties meta;
    public DashboardController(SellerContext context, SellerRepository sellers,
                               ProductRepository products, MetaSignupProperties meta) {
        this.context = context; this.sellers = sellers; this.products = products; this.meta = meta;
    }

    @GetMapping
    public DashboardResponse dashboard(Principal principal) {
        Seller seller = context.current(principal);
        return new DashboardResponse(seller.getBusinessName(),
                seller.getWhatsappConnectionStatus(), seller.getWhatsappDisplayPhone(),
                seller.getWhatsappPhoneNumberId(),
                products.findBySellerIdAndActiveTrueOrderByName(seller.getId()).size(),
                meta.appId(), meta.configurationId(), meta.apiVersion());
    }

    @GetMapping("/products")
    public List<ProductResponse> products(Principal principal) {
        Seller seller = context.current(principal);
        return products.findBySellerIdAndActiveTrueOrderByName(seller.getId()).stream()
                .map(ProductResponse::from).toList();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest body,
                                  Principal principal) {
        Seller seller = context.current(principal);
        Product product = new Product(seller, body.name().trim(), body.description(),
                body.price(), body.currency() == null ? "XOF" : body.currency().toUpperCase(),
                body.stockQuantity());
        return ProductResponse.from(products.save(product));
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable UUID id, Principal principal) {
        Seller seller = context.current(principal);
        Product product = products.findByIdAndSellerId(id, seller.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        product.deactivate();
    }

    @PutMapping("/whatsapp")
    @Transactional
    public DashboardResponse connect(@Valid @RequestBody WhatsAppConnection body,
                                     Principal principal) {
        Seller seller = context.current(principal);
        seller.connectWhatsApp(body.phoneNumberId().trim(),
                body.businessAccountId().trim(), body.displayPhone().trim());
        sellers.save(seller);
        return dashboard(principal);
    }

    public record WhatsAppConnection(@NotBlank String phoneNumberId,
            @NotBlank String businessAccountId, @NotBlank String displayPhone) {}
    public record DashboardResponse(String businessName, String connectionStatus,
            String displayPhone, String phoneNumberId, int productCount,
            String metaAppId, String metaConfigurationId, String metaApiVersion) {}
}
