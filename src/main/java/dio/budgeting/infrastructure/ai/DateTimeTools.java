package dio.budgeting.infrastructure.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

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
        var today = LocalDate.now(clock);
        var dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
        return today + " (" + dayOfWeek + ")";
    }
}
