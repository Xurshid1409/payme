package uz.payme.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import uz.payme.entity.Client;
import uz.payme.repository.ClientRepository;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final ClientRepository clientRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!clientRepository.existsByPhoneNumber("paycom")) {
            clientRepository.save(new Client("paycom", passwordEncoder.encode("KEY")));
        }
    }
}
