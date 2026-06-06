package com.asami.bot.seller;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_accounts")
public class SellerAccount {
    @Id @UuidGenerator private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false, unique = true)
    private Seller seller;
    @Column(nullable = false, unique = true) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    protected SellerAccount() {}
    public SellerAccount(Seller seller, String email, String passwordHash) {
        this.seller = seller; this.email = email; this.passwordHash = passwordHash;
    }
    public Seller getSeller() { return seller; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
