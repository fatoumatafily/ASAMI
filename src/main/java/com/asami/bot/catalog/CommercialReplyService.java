package com.asami.bot.catalog;

import com.asami.bot.seller.Seller;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CommercialReplyService {

    private final ProductRepository productRepository;

    public CommercialReplyService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public String reply(Seller seller, String customerText) {
        return reply(seller, customerText, detectLanguage(customerText));
    }

    public String reply(Seller seller, String customerText, String language) {
        boolean wolof = "wo".equalsIgnoreCase(language);
        List<Product> products =
                productRepository.findBySellerIdAndActiveTrueOrderByName(seller.getId());
        if (products.isEmpty()) {
            if (wolof) {
                return "Salaam, man maay ASAMI, assistant bi ci "
                        + seller.getBusinessName()
                        + ". Catalogue bi dina feese legi.";
            }
            return "Bonjour, je suis ASAMI, l'assistant de " + seller.getBusinessName()
                    + ". Le catalogue sera disponible prochainement.";
        }

        String normalizedText = normalize(customerText);
        Product matchingProduct = products.stream()
                .filter(product -> normalizedText.contains(normalize(product.getName())))
                .findFirst()
                .orElse(null);

        if (matchingProduct != null) {
            return describe(matchingProduct, wolof);
        }

        if (containsAny(
                normalizedText,
                "produit",
                "catalogue",
                "vendez",
                "disponible",
                "lan ngeen am",
                "ban produit",
                "am ngeen"
        )) {
            String names = products.stream()
                    .map(Product::getName)
                    .collect(Collectors.joining(", "));
            if (wolof) {
                return "Lii la nu am: " + names
                        + ". Ban produit nga bëgg xam?";
            }
            return "Nous proposons : " + names
                    + ". Quel produit vous interesse ?";
        }

        if (wolof) {
            return "Salaam, man maay ASAMI, assistant bi ci "
                    + seller.getBusinessName()
                    + ". Laaj ma produit, prix walla stock.";
        }
        return "Bonjour, je suis ASAMI, l'assistant de " + seller.getBusinessName()
                + ". Demandez-moi un produit, son prix ou sa disponibilite.";
    }

    private String describe(Product product, boolean wolof) {
        String stock;
        if (product.getStockQuantity() == null) {
            stock = wolof
                    ? "Dañuy seet ndax am na."
                    : "Disponibilite a confirmer.";
        } else if (product.getStockQuantity() > 0) {
            stock = wolof
                    ? "Stock bi am na " + product.getStockQuantity() + "."
                    : "En stock : " + product.getStockQuantity() + ".";
        } else {
            stock = wolof
                    ? "Léegi stock bi jeex na."
                    : "Actuellement en rupture de stock.";
        }

        if (wolof) {
            return product.getName() + " dafa jar "
                    + product.getPrice().stripTrailingZeros().toPlainString()
                    + " " + product.getCurrency() + ". " + stock;
        }
        return product.getName() + " coute "
                + product.getPrice().stripTrailingZeros().toPlainString()
                + " " + product.getCurrency() + ". " + stock;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(
                value == null ? "" : value,
                Normalizer.Form.NFD
        );
        return decomposed.replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String detectLanguage(String text) {
        String normalized = normalize(text);
        if (containsAny(
                normalized,
                "am nga",
                "am ngeen",
                "nanga",
                "jerejef",
                "waaw",
                "ndax",
                "ban produit",
                "lan ngeen"
        )) {
            return "wo";
        }
        return "fr";
    }
}
