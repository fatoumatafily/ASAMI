package com.asami.bot.seller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;
import java.util.UUID;

public interface SellerAccountRepository extends JpaRepository<SellerAccount, UUID> {
    @EntityGraph(attributePaths = "seller")
    Optional<SellerAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
