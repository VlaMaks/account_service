package account.service;

import account.entity.Status;
import account.entity.User;
import account.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import account.security.SecurityUser;
import javax.transaction.Transactional;


@Service("userDetailsServiceImpl")
@Transactional
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() ->
                new UsernameNotFoundException("User doesn't exists"));
        if (user.getStatus() == Status.BANNED) {
            throw new RuntimeException("User account is locked");
        }
        return SecurityUser.fromUser(user);
    }
}
