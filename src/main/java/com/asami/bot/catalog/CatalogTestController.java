package com.asami.bot.catalog;

import com.asami.bot.config.WhatsAppProperties;
import com.asami.bot.seller.Seller;
import com.asami.bot.seller.SellerRepository;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/test/catalog/products")
@ConditionalOnProperty(
        prefix = "asami.whatsapp",
        name = "test-endpoint-enabled",
        havingValue = "true"
)
public class CatalogTestController {

    private final WhatsAppProperties properties;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;

    public CatalogTestController(
            WhatsAppProperties properties,
            SellerRepository sellerRepository,
            ProductRepository productRepository
    ) {
        this.properties = properties;
        this.sellerRepository = sellerRepository;
        this.productRepository = productRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        Seller seller = currentSeller();
        Product product = new Product(
                seller,
                request.name().trim(),
                request.description(),
                request.price(),
                request.currency() == null ? "XOF" : request.currency().toUpperCase(),
                request.stockQuantity()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    @GetMapping
    public List<ProductResponse> list() {
        Seller seller = currentSeller();
        return productRepository.findBySellerIdAndActiveTrueOrderByName(seller.getId())
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    private Seller currentSeller() {
        return sellerRepository.findByWhatsappPhoneNumberId(properties.phoneNumberId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Send one WhatsApp message first to initialize the test seller"
                ));
    }
}
