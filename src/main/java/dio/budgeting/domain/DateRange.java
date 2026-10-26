package dio.budgeting.domain;

import java.time.LocalDate;

/**
 * Período fechado de datas (início e fim inclusos), usado nas consultas financeiras.
 */
public record DateRange(LocalDate from, LocalDate to) {
    public DateRange {
        if (from == null || to == null) {
            throw new InvalidDateRangeException("As datas de início e fim do período são obrigatórias");
        }
        if (from.isAfter(to)) {
            throw new InvalidDateRangeException("A data inicial não pode ser posterior à data final");
        }
    }

    /**
     * Do primeiro dia do mês até a data informada. É o período padrão quando o usuário não informa datas.
     */
    public static DateRange monthUntil(LocalDate date) {
        return new DateRange(date.withDayOfMonth(1), date);
    }

    /**
     * Completa as datas que não foram informadas: o fim vira "hoje" e o início vira o primeiro dia do mês do fim.
     */
    public static DateRange resolve(LocalDate from, LocalDate to, LocalDate today) {
        var end = to != null ? to : today;
        var start = from != null ? from : end.withDayOfMonth(1);
        return new DateRange(start, end);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
