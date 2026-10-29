package dio.budgeting.infrastructure.ai;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeToolsTest {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final DateTimeTools tools = new DateTimeTools(
            Clock.fixed(LocalDate.of(2026, 10, 28).atStartOfDay(ZONE).toInstant(), ZONE));

    @Test
    void should_describeTodayWithDayOfWeek() {
        assertThat(tools.currentDate()).isEqualTo("2026-10-28 (quarta-feira)");
    }

    @Test
    void should_listRecentDaysWithDayOfWeek_soTheModelDoesNotNeedToCalculate() {
        var calendar = tools.recentDays(8).lines().toList();

        assertThat(calendar).hasSize(8);
        assertThat(calendar.get(0)).isEqualTo("- 2026-10-28 (quarta-feira) - hoje");
        assertThat(calendar.get(1)).isEqualTo("- 2026-10-27 (terça-feira) - ontem");
        assertThat(calendar).contains("- 2026-10-24 (sábado)");
        assertThat(calendar.get(7)).isEqualTo("- 2026-10-21 (quarta-feira)");
    }
}
