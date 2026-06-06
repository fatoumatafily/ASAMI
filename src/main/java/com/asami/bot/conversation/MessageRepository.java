package com.asami.bot.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    boolean existsByWhatsappMessageId(String whatsappMessageId);
}
