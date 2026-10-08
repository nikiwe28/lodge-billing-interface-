package com.example.lodgebilling;

// Imports: each line pulls in a class used below.
import javafx.application.Application;               // base class of every JavaFX program
import javafx.application.Platform;                  // lets us close the app (File > Exit)
import javafx.beans.property.SimpleStringProperty;   // a watchable text value for table cells
import javafx.collections.FXCollections;             // creates observable lists
import javafx.collections.ObservableList;            // a list the table watches
import javafx.geometry.Insets;                       // padding
import javafx.scene.Scene;                           // the content of a window
import javafx.scene.control.*;                       // Button, Label, TextField, ComboBox, TableView, Alert...
import javafx.scene.layout.*;                        // GridPane, HBox, VBox, BorderPane, Priority
import javafx.stage.Stage;                           // the window itself

import java.util.function.Function;                  // used by the column() helper method

public class App extends Application {

    // ---- Data ----
    // Every saved bill lives here. The table is connected to it, so adding or removing
    // a bill updates the screen automatically. It is cleared when the app closes.
    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private int nextBillNumber = 1;      // used to make the ids B001, B002, B003...

    // ---- Controls ----
    // These are fields (not local variables) because several methods below need to read them.
    private final ComboBox<CurrencyType> currencyBox =
            new ComboBox<>(FXCollections.observableArrayList(CurrencyType.values()));
    private final TextField nameField = new TextField();
    private final ComboBox<RoomType> roomBox =
            new ComboBox<>(FXCollections.observableArrayList(RoomType.values()));
    private final TextField nightsField = new TextField();
    private final TextField extrasField = new TextField();
    private final TextField paidField = new TextField();
    private final TextArea notesArea = new TextArea();
    private final Label statusLabel = new Label();            // green/red message under the buttons
    private final VBox priceList = new VBox(4);               // holds the room price lines
    private final TableView<Bill> table = new TableView<>(bills);
    private Label extrasLabel;                                // their text changes with the currency
    private Label paidLabel;

    // JavaFX calls start() when the program opens. "stage" is the main window.
    // Pieces 5b to 5e all go inside this method.
    @Override
    public void start(Stage stage) {
        // ---- Menu bar: File and Help ----
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());               // closes the app
        Menu fileMenu = new Menu("_File", null, exitItem);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Lodge Billing Lab\nBills are kept only until you close the app.").showAndWait());
        Menu helpMenu = new Menu("_Help", null, aboutItem);

        MenuBar menuBar = new MenuBar(fileMenu, helpMenu);

        // ---- Toolbar: New guest | View receipt | Delete selected bill ----
        Button newGuestBtn = new Button("New guest");
        Button viewReceiptBtn = new Button("View receipt");
        Button deleteBtn = new Button("Delete selected bill");
        ToolBar toolBar = new ToolBar(newGuestBtn, viewReceiptBtn, deleteBtn);

        // These two buttons are greyed out until a row is selected in the table.
        viewReceiptBtn.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull());
        deleteBtn.disableProperty().bind(
                table.getSelectionModel().selectedItemProperty().isNull());

        // Each button calls one of the action methods written in piece 5g.
        newGuestBtn.setOnAction(e -> onNewGuest());
        viewReceiptBtn.setOnAction(e -> onViewReceipt());
        deleteBtn.setOnAction(e -> onDelete());
        // ---- Page heading ----
        Label title = new Label("Lodge Billing Lab");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label subtitle = new Label("ICT261 Lecture 3  |  Mr E Nyirenda  |  Mulungushi University");

        // ---- Form settings ----
        currencyBox.setValue(CurrencyType.ZMW);                    // start with kwacha selected
        currencyBox.setOnAction(e -> updateCurrencyLabels());      // when changed, refresh labels and prices
        currencyBox.setMaxWidth(Double.MAX_VALUE);                 // let the drop-downs stretch full width
        roomBox.setMaxWidth(Double.MAX_VALUE);
        roomBox.setPromptText("Select room type");
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);

        // These two labels are kept in fields because their text ("in K" / "in $") changes.
        extrasLabel = labelFor("_Extras in K:", extrasField);
        paidLabel = labelFor("Amount _paid in K:", paidField);

        // ---- Form layout: labels in column 0, inputs in column 1 ----
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        ColumnConstraints labelColumn = new ColumnConstraints(140);   // fixed width for labels
        ColumnConstraints inputColumn = new ColumnConstraints();
        inputColumn.setHgrow(Priority.ALWAYS);                        // inputs take the remaining width
        form.getColumnConstraints().addAll(labelColumn, inputColumn);

        form.addRow(0, labelFor("C_urrency:", currencyBox), currencyBox);
        form.addRow(1, labelFor("_Guest name:", nameField), nameField);
        form.addRow(2, labelFor("_Room type:", roomBox), roomBox);
        form.addRow(3, labelFor("N_ights:", nightsField), nightsField);
        form.addRow(4, extrasLabel, extrasField);
        form.addRow(5, paidLabel, paidField);
        form.addRow(6, labelFor("No_tes:", notesArea), notesArea);

        // ---- Calculate and Save buttons ----
        Button calculateBtn = new Button("_Calculate");
        Button saveBtn = new Button("_Save bill");
        saveBtn.setDefaultButton(true);                            // Enter key presses Save
        calculateBtn.setOnAction(e -> onCalculate());
        saveBtn.setOnAction(e -> onSave());
        HBox actionButtons = new HBox(10, calculateBtn, saveBtn);

        // ---- Right-hand panel: prices and rules ----
        Label priceTitle = new Label("Practice room prices");
        priceTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label ruleDiscount = new Label(String.format("%d nights or more: %.0f%% off the room charge.",
                Bill.DISCOUNT_MIN_NIGHTS, Bill.DISCOUNT_RATE * 100));
        Label ruleExtras = new Label("Extras get no discount. No tax in this lab.");
        Label ruleSession = new Label("Saved bills last only until you close the app.");
        ruleDiscount.setWrapText(true);                            // long text wraps onto new lines
        ruleExtras.setWrapText(true);
        ruleSession.setWrapText(true);

        VBox pricePanel = new VBox(10, priceTitle, priceList, new Separator(),
                ruleDiscount, ruleExtras, ruleSession);
        pricePanel.setPadding(new Insets(15));
        pricePanel.setPrefWidth(260);
        pricePanel.setStyle("-fx-background-color: #e8f0fb; -fx-background-radius: 8;");

        // Put the form on the left and the price panel on the right.
        HBox formRow = new HBox(20, form, pricePanel);
        HBox.setHgrow(form, Priority.ALWAYS);
        // ---- Saved bills table ----
        // column(...) is a helper written in piece 5g. The second part says what text to show.
        table.getColumns().add(column("Bill", Bill::getId));
        table.getColumns().add(column("Guest", Bill::getGuest));
        table.getColumns().add(column("Room", b -> b.getRoom().toString()));
        table.getColumns().add(column("Nights", b -> String.valueOf(b.getNights())));
        table.getColumns().add(column("Total", b -> b.getCurrency().format(b.getTotal())));
        table.getColumns().add(column("Paid", b -> b.getCurrency().format(b.getPaid())));
        table.getColumns().add(column("Balance", b -> b.getCurrency().format(b.getBalance())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No bills saved yet."));    // shown while the table is empty
        table.setPrefHeight(150);

        Label savedLabel = new Label("Saved bills");
        savedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        // Fill in the "in K" labels and the price list for the starting currency.
        updateCurrencyLabels();

        // ---- Put everything in the window ----
        VBox content = new VBox(10, title, subtitle, formRow, actionButtons,
                statusLabel, savedLabel, table);
        content.setPadding(new Insets(15));
        VBox.setVgrow(table, Priority.ALWAYS);                     // the table takes spare height

        BorderPane root = new BorderPane();
        root.setTop(new VBox(menuBar, toolBar));                   // menu bar above toolbar
        root.setCenter(content);

        stage.setTitle("Lodge Billing Lab");
        stage.setScene(new Scene(root, 900, 700));
        stage.show();
    }   // <- end of the start() method
    // ===== Helper methods (they go below start(), still inside the App class) =====

    // Runs at startup and whenever the currency changes. It updates the "in K" labels
    // and shows the room prices converted into the chosen currency.
    private void updateCurrencyLabels() {
        CurrencyType currency = currencyBox.getValue();
        extrasLabel.setText("_Extras in " + currency.getSymbol() + ":");
        paidLabel.setText("Amount _paid in " + currency.getSymbol() + ":");

        priceList.getChildren().clear();                           // remove the old price lines
        for (RoomType room : RoomType.values()) {                  // loop through Standard, Deluxe, Family
            double price = currency.fromKwacha(room.getPriceKwacha());
            priceList.getChildren().add(new Label(room + ":  " + currency.format(price) + " per night"));
        }
    }

    // Reads the form, checks every input, and builds a Bill.
    // Returns null (after showing a red message) if anything is wrong.
    // Both Calculate and Save use this, so the checks are written only once.
    private Bill readBill(String id) {
        String guest = nameField.getText().trim();
        RoomType room = roomBox.getValue();
        CurrencyType currency = currencyBox.getValue();

        // Check 1: guest name
        if (guest.isEmpty()) {
            showError("Guest name is required.", nameField);
            return null;
        }
        // Check 2: a room type must be chosen
        if (room == null) {
            showError("Please select a room type.", roomBox);
            return null;
        }
        // Check 3: nights must be a whole number of at least 1
        int nights;
        try {
            nights = Integer.parseInt(nightsField.getText().trim());
        } catch (NumberFormatException ex) {                       // text like "abc" or "2.5" lands here
            showError("Nights must be a whole number, e.g. 3.", nightsField);
            return null;
        }
        if (nights < 1) {
            showError("Nights must be at least 1.", nightsField);
            return null;
        }
        // Check 4 and 5: extras and amount paid must be numbers that are 0 or more
        Double extras = parseAmount(extrasField.getText());
        if (extras == null) {
            showError("Extras must be a number, 0 or more.", extrasField);
            return null;
        }
        Double paid = parseAmount(paidField.getText());
        if (paid == null) {
            showError("Amount paid must be a number, 0 or more.", paidField);
            return null;
        }

        // All inputs are valid, so build the bill (the maths happens inside Bill).
        Bill bill = new Bill(id, guest, room, nights, currency, extras, paid, notesArea.getText().trim());

        // Check 6: the guest cannot pay more than the total.
        if (paid > bill.getTotal() + 0.004) {
            showError("Amount paid is more than the total (" + currency.format(bill.getTotal()) + ").", paidField);
            return null;
        }
        return bill;
    }

    // Turns text into a number. Blank text counts as 0. Returns null if it is not a valid
    // number or is negative.
    private Double parseAmount(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0.0;
        try {
            double value = Double.parseDouble(trimmed);
            if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) return null;
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Shows a red message and puts the cursor in the field that needs fixing.
    private void showError(String message, Control controlToFocus) {
        statusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        statusLabel.setText(message);
        controlToFocus.requestFocus();
    }

    // Shows a green message.
    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        statusLabel.setText(message);
    }

    // Empties the form for the next guest. The currency choice is kept.
    private void clearForm() {
        nameField.clear();
        roomBox.setValue(null);
        nightsField.clear();
        extrasField.clear();
        paidField.clear();
        notesArea.clear();
    }
    // ===== Action methods (called by the buttons) =====

    // Calculate: shows the total and balance WITHOUT saving anything.
    private void onCalculate() {
        Bill preview = readBill("PREVIEW");                        // a temporary bill, never stored
        if (preview == null) return;                               // validation failed, message already shown
        CurrencyType c = preview.getCurrency();
        showSuccess("Total: " + c.format(preview.getTotal())
                + "    Balance: " + c.format(preview.getBalance()));
    }

    // Save bill: validate, store the bill, then show its receipt.
    private void onSave() {
        // String.format("B%03d", 3) gives "B003": B, then the number padded with zeros to 3 digits.
        Bill bill = readBill(String.format("B%03d", nextBillNumber));
        if (bill == null) return;

        nextBillNumber++;
        bills.add(bill);                                           // the table updates by itself
        showSuccess("Saved " + bill.getId() + ": Balance: "
                + bill.getCurrency().format(bill.getBalance()));
        clearForm();
        showReceipt(bill);                                         // the receipt pops up straight away
    }

    // New guest: clear the form and message.
    private void onNewGuest() {
        clearForm();
        statusLabel.setText("");
        nameField.requestFocus();
    }

    // View receipt: reopen the receipt of the selected row.
    private void onViewReceipt() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) showReceipt(selected);
    }

    // Delete selected bill: ask first, delete only on Yes.
    private void onDelete() {
        Bill selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete bill " + selected.getId() + " for " + selected.getGuest() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm deletion");
        confirm.showAndWait()
                .filter(b -> b == ButtonType.YES)
                .ifPresent(b -> bills.remove(selected));
    }

    // Shows a receipt in a pop-up window. A read-only text area with a monospaced font
    // keeps the columns of the receipt lined up.
    private void showReceipt(Bill bill) {
        TextArea receiptText = new TextArea(bill.buildReceipt());
        receiptText.setEditable(false);
        receiptText.setWrapText(true);
        receiptText.setStyle("-fx-font-family: 'Monospaced';");
        receiptText.setPrefColumnCount(38);
        receiptText.setPrefRowCount(20);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Receipt");
        dialog.setHeaderText("Receipt for bill " + bill.getId());
        dialog.getDialogPane().setContent(receiptText);
        dialog.showAndWait();                                      // waits until the user closes it
    }

    // Makes a label with a keyboard shortcut. The underscore marks the shortcut letter
    // (Alt + that letter), and setLabelFor sends the cursor to the matching control.
    private Label labelFor(String text, Control control) {
        Label label = new Label(text);
        label.setMnemonicParsing(true);
        label.setLabelFor(control);
        return label;
    }

    // Builds one table column. "getter" is a small function that, given a Bill,
    // returns the text to show in this column.
    private TableColumn<Bill, String> column(String title, Function<Bill, String> getter) {
        TableColumn<Bill, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
        return col;
    }

    // Entry point of App. Launcher calls this, and launch() starts JavaFX, which calls start().
    public static void main(String[] args) {
        launch(args);
    }
}   // <- end of the App class