/*
Name: <Your Name>
Course: CNT 4714 – Summer 2025
Assignment title: Project 1 – An Event-driven Enterprise Simulation
Date: Sunday June 1, 2025
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class NileDotCom extends JFrame {
    private JTextField itemIDField, quantityField;
    private JTextArea itemDetailsArea, cartArea;
    private JButton searchButton, addButton, deleteButton, checkoutButton, resetButton, exitButton;

    private java.util.List<String> cart = new ArrayList<>();
    private java.util.List<Double> subtotals = new ArrayList<>();
    private final int MAX_CART_SIZE = 5;
    private double subtotal = 0;
    private Map<String, String[]> inventory;

    public NileDotCom() {
        setTitle("Nile Dot Com e-Store");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        inventory = loadInventory("inventory.csv");

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        inputPanel.add(new JLabel("Enter Item ID:"));
        itemIDField = new JTextField();
        inputPanel.add(itemIDField);

        inputPanel.add(new JLabel("Enter Quantity:"));
        quantityField = new JTextField();
        inputPanel.add(quantityField);

        itemDetailsArea = new JTextArea(5, 30);
        itemDetailsArea.setEditable(false);
        inputPanel.add(new JScrollPane(itemDetailsArea));

        add(inputPanel, BorderLayout.NORTH);

        cartArea = new JTextArea();
        cartArea.setEditable(false);
        add(new JScrollPane(cartArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        searchButton = new JButton("Search For Item #1");
        addButton = new JButton("Add Item #1 To Cart");
        deleteButton = new JButton("Delete Last Item Added To Cart");
        checkoutButton = new JButton("Check Out");
        resetButton = new JButton("Empty Cart");
        exitButton = new JButton("Exit");

        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
        checkoutButton.setEnabled(false);

        buttonPanel.add(searchButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(checkoutButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(exitButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setupListeners();
    }

    private void setupListeners() {
        searchButton.addActionListener(e -> {
            String itemID = itemIDField.getText().trim();
            String quantityStr = quantityField.getText().trim();
            if (!inventory.containsKey(itemID)) {
                JOptionPane.showMessageDialog(this, "Item not found in inventory.");
                return;
            }
            String[] data = inventory.get(itemID);
            if (!data[1].equalsIgnoreCase("true")) {
                JOptionPane.showMessageDialog(this, "Item is out of stock.");
                return;
            }
            int available = Integer.parseInt(data[2]);
            int requested = Integer.parseInt(quantityStr);
            if (requested > available) {
                JOptionPane.showMessageDialog(this, "Insufficient quantity in stock.");
                return;
            }
            double price = Double.parseDouble(data[3]);
            double discount = (requested >= 15) ? 0.2 : (requested >= 10) ? 0.15 : (requested >= 5) ? 0.1 : 0.0;
            double total = requested * price * (1 - discount);

            itemDetailsArea.setText(String.format("Item: %s\nDescription: %s\nPrice: $%.2f\nQty: %d\nSubtotal: $%.2f",
                    itemID, data[0], price, requested, total));

            addButton.setEnabled(true);
        });

        addButton.addActionListener(e -> {
            if (cart.size() >= MAX_CART_SIZE) {
                JOptionPane.showMessageDialog(this, "Cart is full.");
                return;
            }
            cart.add(itemDetailsArea.getText());
            String[] lines = itemDetailsArea.getText().split("\n");
            double itemTotal = Double.parseDouble(lines[4].split("\\$")[1]);
            subtotal += itemTotal;
            subtotals.add(itemTotal);
            cartArea.append(itemDetailsArea.getText() + "\n---\n");
            itemIDField.setText("");
            quantityField.setText("");
            itemDetailsArea.setText("");
            addButton.setEnabled(false);
            deleteButton.setEnabled(true);
            checkoutButton.setEnabled(true);
        });

        deleteButton.addActionListener(e -> {
            if (cart.isEmpty()) return;
            cart.remove(cart.size() - 1);
            subtotal -= subtotals.remove(subtotals.size() - 1);
            cartArea.setText("");
            cart.forEach(item -> cartArea.append(item + "\n---\n"));
            if (cart.isEmpty()) deleteButton.setEnabled(false);
        });

        checkoutButton.addActionListener(e -> {
            double tax = subtotal * 0.06;
            double total = subtotal + tax;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String timestamp = dtf.format(LocalDateTime.now());
            String id = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").format(LocalDateTime.now());
            StringBuilder invoice = new StringBuilder("Invoice - Transaction ID: " + id + "\nDate: " + timestamp +
                    "\n\nItems:\n");
            for (String item : cart) invoice.append(item).append("\n---\n");
            invoice.append(String.format("\nSubtotal: $%.2f\nTax: $%.2f\nTotal: $%.2f", subtotal, tax, total));
            JOptionPane.showMessageDialog(this, invoice.toString());

            try (PrintWriter out = new PrintWriter(new FileWriter("transactions.csv", true))) {
                for (String item : cart) {
                    out.println(id + "," + timestamp + "," + item.replaceAll("\n", ", "));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            cart.clear();
            subtotals.clear();
            cartArea.setText("");
            subtotal = 0;
            checkoutButton.setEnabled(false);
            deleteButton.setEnabled(false);
        });

        resetButton.addActionListener(e -> {
            cart.clear();
            subtotals.clear();
            cartArea.setText("");
            itemIDField.setText("");
            quantityField.setText("");
            itemDetailsArea.setText("");
            subtotal = 0;
            checkoutButton.setEnabled(false);
            deleteButton.setEnabled(false);
        });

        exitButton.addActionListener(e -> System.exit(0));
    }

    private Map<String, String[]> loadInventory(String filename) {
        Map<String, String[]> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                for (int i = 0; i < tokens.length; i++) {
                    tokens[i] = tokens[i].replaceAll("^\"|\"$", "").trim();
                }
                map.put(tokens[0], new String[]{tokens[1], tokens[2], tokens[3], tokens[4]});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NileDotCom().setVisible(true));
    }
}
