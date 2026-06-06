package com.asami.bot.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findBySellerIdAndActiveTrueOrderByName(UUID sellerId);
    java.util.Optional<Product> findByIdAndSellerId(UUID id, UUID sellerId);
}
