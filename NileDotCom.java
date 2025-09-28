/*
Name: <Your Name>
Course: CNT 4714 – Summer 2025
Assignment title: Project 1 – An Event-driven Enterprise Simulation
Date: Sunday June 1, 2025
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
        setTitle("🛒 Nile Dot Com - Premium E-Store");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set application icon and modern styling
        setLocationRelativeTo(null); // Center on screen

        // Modern color scheme
        Color primaryColor = new Color(41, 128, 185);
        Color secondaryColor = new Color(52, 152, 219);
        Color backgroundColor = new Color(236, 240, 241);
        Color textColor = new Color(44, 62, 80);

        getContentPane().setBackground(backgroundColor);

        inventory = loadInventory("inventory.csv");

        // Create modern input panel with styling
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        inputPanel.setBackground(backgroundColor);

        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.setBackground(backgroundColor);
        JLabel titleLabel = new JLabel("🛒 Welcome to Nile Dot Com Premium Store");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(primaryColor);
        titlePanel.add(titleLabel);
        inputPanel.add(titlePanel);

        inputPanel.add(Box.createVerticalStrut(10));

        // Input fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(backgroundColor);
        fieldsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(primaryColor, 2),
                "Product Search",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                primaryColor));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Item ID field
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel itemIDLabel = new JLabel("Item ID:");
        itemIDLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        itemIDLabel.setForeground(textColor);
        fieldsPanel.add(itemIDLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        itemIDField = new JTextField(15);
        itemIDField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        itemIDField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primaryColor, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        fieldsPanel.add(itemIDField, gbc);

        // Quantity field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel quantityLabel = new JLabel("Quantity:");
        quantityLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        quantityLabel.setForeground(textColor);
        fieldsPanel.add(quantityLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        quantityField = new JTextField(15);
        quantityField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        quantityField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primaryColor, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        fieldsPanel.add(quantityField, gbc);

        inputPanel.add(fieldsPanel);
        inputPanel.add(Box.createVerticalStrut(10));

        // Item details area
        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(secondaryColor, 1),
                "Item Details",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                secondaryColor));
        detailsPanel.setBackground(backgroundColor);

        itemDetailsArea = new JTextArea(4, 30);
        itemDetailsArea.setEditable(false);
        itemDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        itemDetailsArea.setBackground(Color.WHITE);
        itemDetailsArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane detailsScroll = new JScrollPane(itemDetailsArea);
        detailsScroll.setBorder(BorderFactory.createLineBorder(secondaryColor, 1));
        detailsPanel.add(detailsScroll, BorderLayout.CENTER);

        inputPanel.add(detailsPanel);

        add(inputPanel, BorderLayout.NORTH);

        // Shopping cart area with modern styling
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                "🛒 Shopping Cart",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 14),
                new Color(46, 204, 113)));
        cartPanel.setBackground(backgroundColor);

        cartArea = new JTextArea();
        cartArea.setEditable(false);
        cartArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        cartArea.setBackground(Color.WHITE);
        cartArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        cartArea.setLineWrap(true);
        cartArea.setWrapStyleWord(true);

        JScrollPane cartScroll = new JScrollPane(cartArea);
        cartScroll.setBorder(BorderFactory.createLineBorder(new Color(46, 204, 113), 1));
        cartScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        add(cartPanel, BorderLayout.CENTER);

        // Modern button panel with styling
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        // Create buttons with modern styling
        searchButton = createStyledButton("🔍 Search Item", primaryColor, Color.WHITE);
        addButton = createStyledButton("➕ Add to Cart", new Color(46, 204, 113), Color.WHITE);
        deleteButton = createStyledButton("🗑️ Remove Item", new Color(231, 76, 60), Color.WHITE);
        checkoutButton = createStyledButton("💳 Checkout", new Color(230, 126, 34), Color.WHITE);
        resetButton = createStyledButton("🔄 Empty Cart", new Color(155, 89, 182), Color.WHITE);
        exitButton = createStyledButton("❌ Exit", new Color(149, 165, 166), Color.WHITE);

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
            if (cart.isEmpty())
                return;
            cart.remove(cart.size() - 1);
            subtotal -= subtotals.remove(subtotals.size() - 1);
            cartArea.setText("");
            cart.forEach(item -> cartArea.append(item + "\n---\n"));
            if (cart.isEmpty())
                deleteButton.setEnabled(false);
        });

        checkoutButton.addActionListener(e -> {
            double tax = subtotal * 0.06;
            double total = subtotal + tax;
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String timestamp = dtf.format(LocalDateTime.now());
            String id = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").format(LocalDateTime.now());
            StringBuilder invoice = new StringBuilder("Invoice - Transaction ID: " + id + "\nDate: " + timestamp +
                    "\n\nItems:\n");
            for (String item : cart)
                invoice.append(item).append("\n---\n");
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
                map.put(tokens[0], new String[] { tokens[1], tokens[2], tokens[3], tokens[4] });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private JButton createStyledButton(String text, Color bgColor, Color textColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(textColor);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bgColor);
                }
            }
        });

        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NileDotCom().setVisible(true));
    }
}
