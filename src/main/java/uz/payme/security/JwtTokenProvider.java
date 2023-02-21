package uz.payme.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    public String generateJWTToken(UserDetailsImpl user) {
        return JWT.create()
                .withSubject(user.getUsername())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusMillis(SecurityConstant.TOKEN_EXPIRE_AT))
                .sign(Algorithm.HMAC256(secret));
    }

    public String getUsernameFromToken(String jwtToken) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(jwtToken)
                .getSubject();
    }

    public Boolean validateToken(String jwtToken) {
        try {
            JWT.require(Algorithm.HMAC256(secret))
                    .build()
                    .verify(jwtToken);
            return true;
        }
        catch (JWTVerificationException ex) {
            ex.printStackTrace();
        }
        return false;
    }
}
