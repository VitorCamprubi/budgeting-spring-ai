package dio.budgeting.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    @Test
    void should_createRange_when_fromIsBeforeTo() {
        var range = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThat(range.contains(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(range.contains(LocalDate.of(2026, 11, 1))).isFalse();
    }

    @Test
    void should_acceptSingleDayRange() {
        var day = LocalDate.of(2026, 10, 25);

        assertThat(new DateRange(day, day).contains(day)).isTrue();
    }

    @Test
    void should_rejectRange_when_fromIsAfterTo() {
        assertThatThrownBy(() -> new DateRange(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)))
                .isInstanceOf(InvalidDateRangeException.class)
                .hasMessage("A data inicial não pode ser posterior à data final");
    }

    @Test
    void should_rejectRange_when_dateIsMissing() {
        assertThatThrownBy(() -> new DateRange(null, LocalDate.of(2026, 10, 1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void should_startAtFirstDayOfMonth_when_usingMonthUntil() {
        var range = DateRange.monthUntil(LocalDate.of(2026, 10, 25));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 10, 25));
    }
}
