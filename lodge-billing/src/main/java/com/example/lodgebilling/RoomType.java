package com.example.lodgebilling;

// The three room types and their prices per night. Prices are stored in kwacha;
// CurrencyType converts them when the user picks US dollars.
public enum RoomType {

    STANDARD("Standard", 350.0),
    DELUXE("Deluxe", 550.0),
    FAMILY("Family", 750.0);

    private final String label;          // text shown in the drop-down and on the receipt
    private final double priceKwacha;    // price per night in kwacha

    RoomType(String label, double priceKwacha) {
        this.label = label;
        this.priceKwacha = priceKwacha;
    }

    public double getPriceKwacha() {
        return priceKwacha;
    }

    @Override
    public String toString() {
        return label;
    }
}