package dio.budgeting.domain;

public enum Category {
    GROCERIES("Mercado e supermercado"),
    PHARMA("Farmácia e saúde"),
    AUTO("Carro: combustível, manutenção e estacionamento"),
    RESTAURANT("Restaurantes, lanches e delivery"),
    LEISURE("Lazer e entretenimento"),
    OTHER("Outros gastos");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
