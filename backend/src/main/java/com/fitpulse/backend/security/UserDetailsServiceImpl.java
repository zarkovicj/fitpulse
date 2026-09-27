package com.fitpulse.backend.security;

import com.fitpulse.backend.user.User;
import com.fitpulse.backend.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(User.normalizeEmail(email))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Korisnik nije pronađen: " + email));
    }
}
