package org.ai5590.devopsagent.security;

import org.ai5590.devopsagent.db.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<String> hash = userRepository.getPasswordHash(username);
        if (hash.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new User(username, hash.get(), Collections.emptyList());
    }
}
