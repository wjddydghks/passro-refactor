package com.passro.passrobackend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.enums.AccountRole;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.global.jwt.JwtProvider;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected DeliveryRepository deliveryRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired
    protected EntityManager entityManager;

    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("passro_test")
            .withUsername("passro")
            .withPassword("passro");

    protected static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        Startables.deepStart(Stream.of(MYSQL, REDIS)).join();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", REDIS::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    protected Account createAccount(String prefix) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String cleanPrefix = prefix.replace("-", "");
        String phoneDigits = String.format("%08d", Math.abs(suffix.hashCode()) % 100000000);

        return accountRepository.saveAndFlush(Account.builder()
                .mail(prefix + "-" + suffix + "@passro.test")
                .password(passwordEncoder.encode("Passro123!"))
                .nickname(cleanPrefix + suffix)
                .name(prefix)
                .phoneNumber("010-" + phoneDigits.substring(0, 4) + "-" + phoneDigits.substring(4))
                .birth(LocalDate.of(2000, 1, 1))
                .certified(true)
                .point(0L)
                .role(AccountRole.USER)
                .build());
    }

    protected String accessToken(Account account) {
        return jwtProvider.createAccessToken(account.getId(), account.getRole().name());
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected long createDelivery(String token, String goodName, long sourceStationId, long destinationStationId) throws Exception {
        String body = """
                {
                  "sourceStationId":%d,
                  "destinationStationId":%d,
                  "name":"%s",
                  "size":"M",
                  "picture":"good.png",
                  "memo":"Handle with care"
                }
                """.formatted(sourceStationId, destinationStationId, goodName);

        MvcResult result = mockMvc.perform(post("/sender")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/result").asLong();
    }
}
