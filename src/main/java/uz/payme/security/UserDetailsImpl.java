package uz.payme.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.payme.entity.User;
import java.util.Collection;
import java.util.List;

public class

UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String phoneNumber;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    public UserDetailsImpl(Long id, String phoneNumber, String password, Collection<? extends GrantedAuthority> authorities) {

        this.id = id;
        this.phoneNumber = phoneNumber;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().getName()));
        return new UserDetailsImpl(
                user.getId(),
                user.getPhoneNumber(),
                user.getPassword(),
                authorities);
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
