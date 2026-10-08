package com.example.lodgebilling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// One bill. All the money maths happens in the constructor, so a Bill is always
// internally consistent. It also knows how to write its own receipt.
public class Bill {

    // The discount rule from the lab sheet: 5% off the ROOM charge for 5 nights or more.
    public static final int DISCOUNT_MIN_NIGHTS = 5;
    public static final double DISCOUNT_RATE = 0.05;

    // Details entered by the user
    private final String id;               // e.g. B001
    private final String guest;
    private final RoomType room;
    private final int nights;
    private final CurrencyType currency;
    private final String notes;

    // Calculated amounts (all in the chosen currency)
    private final double ratePerNight;
    private final double roomCharge;       // rate x nights
    private final double discount;         // 5% of roomCharge, or 0
    private final double extras;           // extras are never discounted
    private final double total;            // roomCharge - discount + extras
    private final double paid;
    private final double balance;          // total - paid

    // The date and time the bill was created, shown on the receipt.
    private final String issued = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH));

    public Bill(String id, String guest, RoomType room, int nights,
                CurrencyType currency, double extras, double paid, String notes) {
        this.id = id;
        this.guest = guest;
        this.room = room;
        this.nights = nights;
        this.currency = currency;
        this.extras = extras;
        this.paid = paid;
        this.notes = notes;

        // The maths, step by step:
        this.ratePerNight = currency.fromKwacha(room.getPriceKwacha());           // price in chosen currency
        this.roomCharge = ratePerNight * nights;                                  // price x nights
        this.discount = (nights >= DISCOUNT_MIN_NIGHTS) ? roomCharge * DISCOUNT_RATE : 0.0;
        this.total = roomCharge - discount + extras;                              // no tax in this lab
        this.balance = total - paid;                                              // what is still owed
    }

    // Getters: the table and the App class use these to read values.
    public String getId() { return id; }
    public String getGuest() { return guest; }
    public RoomType getRoom() { return room; }
    public int getNights() { return nights; }
    public CurrencyType getCurrency() { return currency; }
    public double getTotal() { return total; }
    public double getPaid() { return paid; }
    public double getBalance() { return balance; }

    // Builds the receipt as plain text, 36 characters wide.
    public String buildReceipt() {
        String line = "-".repeat(36) + "\n";            // a dashed divider line
        StringBuilder sb = new StringBuilder();         // StringBuilder joins text pieces efficiently

        sb.append("        LODGE BILLING LAB\n");
        sb.append("         OFFICIAL RECEIPT\n");
        sb.append(line);
        sb.append(row("Bill no", id));
        sb.append(row("Date", issued));
        sb.append(row("Guest", guest));
        sb.append(row("Room", room + " x " + nights + " night(s)"));
        sb.append(row("Currency", currency.name()));
        sb.append(line);
        sb.append(row("Rate per night", currency.format(ratePerNight)));
        sb.append(row("Room charge", currency.format(roomCharge)));
        if (discount > 0) {                              // only print the discount line if there is one
            sb.append(row(String.format("%.0f%% discount", DISCOUNT_RATE * 100),
                    "-" + currency.format(discount)));
        }
        sb.append(row("Extras", currency.format(extras)));
        sb.append(line);
        sb.append(row("TOTAL", currency.format(total)));
        sb.append(row("Paid", currency.format(paid)));
        sb.append(row("Balance", currency.format(balance)));
        // Math.abs(...) < 0.005 means "zero, allowing for tiny rounding errors".
        sb.append(row("Status", Math.abs(balance) < 0.005 ? "PAID IN FULL" : "BALANCE DUE"));
        sb.append(line);
        if (!notes.isEmpty()) {
            sb.append("Notes: ").append(notes).append("\n");
            sb.append(line);
        }
        sb.append("   Thank you for staying with us!\n");
        return sb.toString();
    }

    // Helper: one receipt line with the label on the left and the value pushed to the right.
    // %-15s = text padded to 15 characters, left-aligned. %21s = 21 characters, right-aligned.
    private static String row(String label, String value) {
        return String.format("%-15s%21s%n", label, value);
    }
}