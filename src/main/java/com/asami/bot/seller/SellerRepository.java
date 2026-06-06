package com.asami.bot.seller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByWhatsappPhoneNumberId(String whatsappPhoneNumberId);
}
