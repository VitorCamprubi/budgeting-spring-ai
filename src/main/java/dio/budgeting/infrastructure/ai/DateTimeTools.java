package dio.budgeting.infrastructure.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * O modelo de linguagem não sabe que dia é hoje. Esta tool dá esse contexto para ele conseguir
 * transformar expressões como "esse mês" ou "ontem" em datas reais antes de consultar os gastos.
 */
@Component
public class DateTimeTools {
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final Clock clock;

    public DateTimeTools(Clock clock) {
        this.clock = clock;
    }

    @Tool(name = "current-date",
            description = "Retorna a data de hoje no formato yyyy-MM-dd e o dia da semana. "
                    + "Use antes de calcular períodos como 'esse mês', 'ontem' ou 'semana passada'.")
    public String currentDate() {
        return describe(LocalDate.now(clock));
    }

    /**
     * Lista os últimos dias com o dia da semana de cada um. Modelos de linguagem erram contas de calendário
     * ("no sábado" virava uma quinta-feira nos testes reais); com a lista, basta consultar.
     */
    public String recentDays(int days) {
        var today = LocalDate.now(clock);

        return IntStream.range(0, days)
                .mapToObj(offset -> {
                    var date = today.minusDays(offset);
                    var label = switch (offset) {
                        case 0 -> " - hoje";
                        case 1 -> " - ontem";
                        default -> "";
                    };
                    return "- " + describe(date) + label;
                })
                .collect(Collectors.joining("\n"));
    }

    private static String describe(LocalDate date) {
        return date + " (" + date.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR) + ")";
    }
}
