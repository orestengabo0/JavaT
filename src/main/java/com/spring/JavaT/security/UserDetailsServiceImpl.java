package com.spring.JavaT.security;

import com.spring.JavaT.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link com.spring.JavaT.user.User} from the database by username (email).
 *
 * <p>Spring Security calls this during form-login and JWT filter authentication.
 * We look up by email because that is the unique identifier users log in with.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads the user by email address.
     *
     * @param email the email submitted in the login request
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));
    }
}
