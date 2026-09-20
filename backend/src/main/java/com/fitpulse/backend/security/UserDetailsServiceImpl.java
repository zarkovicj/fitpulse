package com.fitpulse.backend.security;

import com.fitpulse.backend.user.KorisnikRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final KorisnikRepository korisnikRepository;

    public UserDetailsServiceImpl(KorisnikRepository korisnikRepository) {
        this.korisnikRepository = korisnikRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String mail) throws UsernameNotFoundException {
        return korisnikRepository.findByMail(mail)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik nije pronađen: " + mail));
    }
}
