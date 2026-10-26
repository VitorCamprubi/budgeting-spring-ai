package dio.budgeting.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Expõe o relógio da aplicação como bean. Assim os casos de uso não chamam LocalDate.now() direto
 * e os testes conseguem fixar a data de "hoje".
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    Clock clock(@Value("${budgeting.time-zone:America/Sao_Paulo}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
