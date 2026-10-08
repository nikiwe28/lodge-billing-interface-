package com.example.lodgebilling;

import java.util.Locale;

// An enum is a fixed set of named choices. The lodge accepts exactly two currencies,
// so an enum stops anyone creating a third one by accident.
public enum CurrencyType {

    // Each line below calls the constructor further down with three values:
    //   1) the text shown in the drop-down, 2) the symbol, 3) the exchange rate.
    // The rate says how many kwacha equal ONE unit of that currency.
    // PRACTICE RATE: 1 US dollar = K25.00. Change the 25.0 to use a different rate.
    ZMW("Kwacha (K)", "K", 1.0),
    USD("US Dollar ($)", "$", 25.0);

    private final String label;
    private final String symbol;
    private final double kwachaPerUnit;

    // Constructor for the enum: stores the three values given above.
    CurrencyType(String label, String symbol, double kwachaPerUnit) {
        this.label = label;
        this.symbol = symbol;
        this.kwachaPerUnit = kwachaPerUnit;
    }

    public String getSymbol() {
        return symbol;
    }

    // Converts an amount in kwacha into this currency.
    // Example: 550 kwacha in USD = 550 / 25 = 22.0 dollars. For ZMW it divides by 1, so no change.
    public double fromKwacha(double kwacha) {
        return kwacha / kwachaPerUnit;
    }

    // Turns a number into money text with 2 decimals, e.g. "K 2812.50" or "$ 112.50".
    // Locale.US forces a dot as the decimal mark, even on computers set to use a comma.
    public String format(double amount) {
        return symbol + " " + String.format(Locale.US, "%.2f", amount);
    }

    // The drop-down shows whatever toString() returns.
    @Override
    public String toString() {
        return label;
    }
}