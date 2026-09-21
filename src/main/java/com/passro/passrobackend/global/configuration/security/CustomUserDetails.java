package com.passro.passrobackend.global.configuration.security;

import com.passro.passrobackend.account.entity.Account;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.management.relation.Role;
import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Account account;

    public Long getAccountId(){
        return account.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    @Override
    public String getPassword(){
        return account.getPassword();
    }

    @Override
    public String getUsername(){
        return account.getMail();
    }
}
