package com.asami.bot.seller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@Component
public class SellerContext {
    private final SellerAccountRepository accounts;
    public SellerContext(SellerAccountRepository accounts) { this.accounts = accounts; }
    public Seller current(Principal principal) {
        if (principal == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return accounts.findByEmailIgnoreCase(principal.getName())
                .map(SellerAccount::getSeller)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
