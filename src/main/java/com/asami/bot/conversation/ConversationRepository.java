package com.asami.bot.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findBySellerIdAndCustomerPhone(UUID sellerId, String customerPhone);
}
