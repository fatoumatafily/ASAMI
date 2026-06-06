package com.asami.bot.catalog;

import com.asami.bot.seller.Seller;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommercialReplyServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CommercialReplyService service =
            new CommercialReplyService(productRepository);

    @Test
    void returnsProductPriceAndStockWhenNameMatches() {
        Seller seller = seller();
        Product shoes = new Product(
                seller,
                "Chaussures",
                "Chaussures de ville",
                new BigDecimal("25000"),
                "XOF",
                4
        );
        when(productRepository.findBySellerIdAndActiveTrueOrderByName(seller.getId()))
                .thenReturn(List.of(shoes));

        String reply = service.reply(seller, "Vous avez des chaussures ?");

        assertThat(reply).isEqualTo("Chaussures coute 25000 XOF. En stock : 4.");
    }

    @Test
    void listsCatalogWhenCustomerAsksWhatIsAvailable() {
        Seller seller = seller();
        Product bag = new Product(
                seller,
                "Sac",
                null,
                new BigDecimal("15000"),
                "XOF",
                2
        );
        Product shoes = new Product(
                seller,
                "Chaussures",
                null,
                new BigDecimal("25000"),
                "XOF",
                4
        );
        when(productRepository.findBySellerIdAndActiveTrueOrderByName(seller.getId()))
                .thenReturn(List.of(shoes, bag));

        String reply = service.reply(seller, "Quels produits sont disponibles ?");

        assertThat(reply).isEqualTo(
                "Nous proposons : Chaussures, Sac. Quel produit vous interesse ?"
        );
    }

    @Test
    void repliesInWolofForWolofProductQuestion() {
        Seller seller = seller();
        Product shoes = new Product(
                seller,
                "Chaussures",
                null,
                new BigDecimal("25000"),
                "XOF",
                4
        );
        when(productRepository.findBySellerIdAndActiveTrueOrderByName(seller.getId()))
                .thenReturn(List.of(shoes));

        String reply = service.reply(seller, "Am nga chaussures ?", "wo");

        assertThat(reply).isEqualTo(
                "Chaussures dafa jar 25000 XOF. Stock bi am na 4."
        );
    }

    private Seller seller() {
        Seller seller = mock(Seller.class);
        when(seller.getId()).thenReturn(UUID.randomUUID());
        when(seller.getBusinessName()).thenReturn("Boutique Robert");
        return seller;
    }
}
