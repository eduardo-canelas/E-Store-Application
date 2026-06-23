/*
Name: Eduardo Canelas
Course: CNT 4714 – Summer 2025
Assignment title: Project 1 – An Event-driven Enterprise Simulation
Date: Sunday June 1, 2025
*/

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NileDotCom extends JFrame {

    /* ------------------------------------------------------------------ *
     *  Design tokens — mill3.studio palette (sampled from its stylesheet)*
     * ------------------------------------------------------------------ */
    static final Color PAPER    = new Color(0xF3, 0xF2, 0xEF); // warm canvas
    static final Color SURFACE  = new Color(0xFB, 0xFA, 0xF7); // block fill
    static final Color INK      = new Color(0x0A, 0x08, 0x08); // near-black
    static final Color INK_SOFT = new Color(0x14, 0x12, 0x12);
    static final Color MUTED    = new Color(0x6A, 0x66, 0x60); // warm gray
    static final Color FAINT    = new Color(0x9C, 0x97, 0x8E);
    static final Color HAIRLINE = new Color(0xDD, 0xD8, 0xCE);
    static final Color LIME     = new Color(0xC0, 0xFF, 0x0D); // signature
    static final Color LIME_DK  = new Color(0xAD, 0xE6, 0x00);
    static final Color ORANGE   = new Color(0xFF, 0x68, 0x2C);
    static final Color SUCCESS  = new Color(0x14, 0x2A, 0x1F); // deep green ink

    static final DecimalFormat MONEY = new DecimalFormat("$#,##0.00");
    static String GROTESK = pickFont(new String[]{
            "Basel Grotesk", "Helvetica Neue", "Arimo", "Inter", "Segoe UI", "Arial" });
    static String SERIF = pickFont(new String[]{
            "Times Now", "Georgia", "Times New Roman", "Times", "Serif" });
    static String MONO = pickFont(new String[]{
            "Menlo", "SF Mono", "Monaco", "Roboto Mono", "Courier New", "Monospaced" });

    /* business state */
    private final List<CartItem> cart = new ArrayList<CartItem>();
    private static final int MAX_CART = 5;
    private static final double TAX_RATE = 0.06;
    private Map<String, String[]> inventory;
    private CartItem pending;

    /* widgets */
    private RoundField idField, qtyField;
    private JPanel detailHost;
    private RoundButton addBtn;
    private JPanel cartList;
    private JLabel cartCount, sumSub, sumTax, sumTotal;
    private RoundButton checkoutBtn, emptyBtn;
    private CartPill cartPill;

    public NileDotCom() {
        setTitle("Nile dot com — Premium E-Store");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 800));
        setSize(1180, 880);
        setLocationRelativeTo(null);

        inventory = loadInventory("inventory.csv");

        JPanel rootBg = new JPanel(new BorderLayout());
        rootBg.setBackground(PAPER);
        setContentPane(rootBg);

        rootBg.add(buildHeader(), BorderLayout.NORTH);
        rootBg.add(buildBody(), BorderLayout.CENTER);

        refreshCart();
        renderEmptyDetail();
    }

    /* ============================ HEADER ============================ */
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(INK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // lime baseline rule
                g2.setColor(LIME);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 34, 24, 34));
        header.setPreferredSize(new Dimension(0, 104));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.add(new LogoMark());
        brand.add(Box.createHorizontalStrut(16));

        JPanel words = new JPanel();
        words.setOpaque(false);
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        JLabel kicker = new JLabel("(E-STORE)");
        kicker.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.14f));
        kicker.setForeground(LIME);
        kicker.setAlignmentX(LEFT_ALIGNMENT);
        JLabel mark = new JLabel("Nile dot com");
        mark.setFont(tracked(serif(Font.BOLD, 28), -0.02f));
        mark.setForeground(PAPER);
        mark.setAlignmentX(LEFT_ALIGNMENT);
        words.add(kicker);
        words.add(Box.createVerticalStrut(2));
        words.add(mark);
        brand.add(words);
        header.add(brand, BorderLayout.WEST);

        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
        JLabel meta = new JLabel("PREMIUM COMMERCE © 2026");
        meta.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.12f));
        meta.setForeground(new Color(255, 255, 255, 150));
        JPanel metaWrap = new JPanel(new GridBagLayout());
        metaWrap.setOpaque(false);
        metaWrap.add(meta);
        east.add(metaWrap);
        east.add(Box.createHorizontalStrut(20));
        cartPill = new CartPill();
        JPanel pillWrap = new JPanel(new GridBagLayout());
        pillWrap.setOpaque(false);
        pillWrap.add(cartPill);
        east.add(pillWrap);
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /* ============================= BODY ============================= */
    private JComponent buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(28, 30, 30, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.weightx = 0.40;
        c.insets = new Insets(0, 0, 0, 12);
        body.add(buildLeftColumn(), c);
        c.gridx = 1;
        c.weightx = 0.60;
        c.insets = new Insets(0, 12, 0, 0);
        body.add(buildCartCard(), c);
        return body;
    }

    private JComponent buildLeftColumn() {
        // BorderLayout: search card keeps its full preferred height (NORTH never
        // compresses); the detail card fills the remaining height (CENTER).
        JPanel col = new JPanel(new BorderLayout(0, 16));
        col.setOpaque(false);
        col.add(buildSearchCard(), BorderLayout.NORTH);
        col.add(buildDetailCard(), BorderLayout.CENTER);
        return col;
    }

    private JComponent buildSearchCard() {
        Block card = new Block(24);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(eyebrow("01 / FIND A PRODUCT"));
        card.add(Box.createVerticalStrut(8));
        card.add(heading("Search the catalogue"));
        card.add(Box.createVerticalStrut(22));

        card.add(fieldLabel("ITEM ID"));
        card.add(Box.createVerticalStrut(8));
        idField = new RoundField("e.g. 11");
        card.add(idField);
        card.add(Box.createVerticalStrut(18));

        card.add(fieldLabel("QUANTITY"));
        card.add(Box.createVerticalStrut(8));
        qtyField = new RoundField("e.g. 2");
        card.add(qtyField);
        card.add(Box.createVerticalStrut(22));

        RoundButton search = new RoundButton("Search item →", RoundButton.INK);
        search.setFullWidth(true);
        ActionListener go = new ActionListener() {
            public void actionPerformed(ActionEvent e) { doSearch(); }
        };
        search.addActionListener(go);
        idField.addActionListener(go);
        qtyField.addActionListener(go);
        card.add(search);

        card.add(Box.createVerticalStrut(18));
        card.add(new Divider());
        card.add(Box.createVerticalStrut(14));
        card.add(discountHint());
        return card;
    }

    private JComponent discountHint() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel l = new JLabel("BULK SAVINGS");
        l.setFont(tracked(mono(Font.PLAIN, 10), 0.1f));
        l.setForeground(MUTED);
        p.add(l);
        p.add(new Pill("5+ / 10%", Pill.OUTLINE));
        p.add(new Pill("10+ / 15%", Pill.OUTLINE));
        p.add(new Pill("15+ / 20%", Pill.OUTLINE));
        return p;
    }

    private JComponent buildDetailCard() {
        Block card = new Block(24);
        card.setLayout(new BorderLayout());
        card.setAlignmentX(LEFT_ALIGNMENT);
        detailHost = new JPanel(new BorderLayout());
        detailHost.setOpaque(false);
        card.add(detailHost, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildCartCard() {
        Block card = new Block(26);
        card.setLayout(new BorderLayout(0, 16));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(eyebrow("02 / YOUR ORDER"));
        titleWrap.add(Box.createVerticalStrut(8));
        titleWrap.add(heading("Shopping cart"));
        head.add(titleWrap, BorderLayout.WEST);

        cartCount = new JLabel();
        cartCount.setFont(tracked(mono(Font.PLAIN, 11), 0.08f));
        cartCount.setForeground(MUTED);
        JPanel countWrap = new JPanel(new GridBagLayout());
        countWrap.setOpaque(false);
        countWrap.add(cartCount);
        head.add(countWrap, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        cartList = new JPanel();
        cartList.setOpaque(false);
        cartList.setLayout(new BoxLayout(cartList, BoxLayout.Y_AXIS));
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.setOpaque(false);
        listHolder.add(cartList, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(listHolder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll.getVerticalScrollBar());
        card.add(scroll, BorderLayout.CENTER);

        card.add(buildSummary(), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildSummary() {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(new HardRule());
        wrap.add(Box.createVerticalStrut(14));

        sumSub = new JLabel();
        sumTax = new JLabel();
        sumTotal = new JLabel();
        wrap.add(summaryRow("Subtotal", sumSub, false));
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(summaryRow("Sales tax (6%)", sumTax, false));
        wrap.add(Box.createVerticalStrut(12));
        wrap.add(summaryRow("Total", sumTotal, true));
        wrap.add(Box.createVerticalStrut(18));

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        emptyBtn = new RoundButton("Empty cart", RoundButton.GHOST);
        emptyBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { emptyCart(); }
        });
        checkoutBtn = new RoundButton("Checkout →", RoundButton.LIME);
        checkoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { checkout(); }
        });
        actions.add(emptyBtn);
        actions.add(checkoutBtn);
        wrap.add(actions);
        return wrap;
    }

    private JComponent summaryRow(String label, JLabel value, boolean strong) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, strong ? 42 : 22));
        JLabel l = new JLabel(label);
        if (strong) {
            l.setFont(tracked(serif(Font.BOLD, 20), -0.01f));
            l.setForeground(INK);
        } else {
            l.setFont(font(Font.PLAIN, 13));
            l.setForeground(MUTED);
        }
        value.setFont(strong ? tracked(serif(Font.BOLD, 28), -0.02f) : font(Font.BOLD, 13.5f));
        value.setForeground(INK);
        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    /* ========================= INTERACTIONS ========================= */
    private void doSearch() {
        String id = idField.getText().trim();
        String qs = qtyField.getText().trim();
        if (id.isEmpty() || qs.isEmpty()) {
            toast("Enter an item ID and quantity.", Toast.WARN);
            return;
        }
        if (!inventory.containsKey(id)) {
            toast("No item matches ID \"" + id + "\".", Toast.ERROR);
            renderEmptyDetail();
            return;
        }
        int requested;
        try {
            requested = Integer.parseInt(qs);
        } catch (NumberFormatException ex) {
            toast("Quantity must be a whole number.", Toast.WARN);
            return;
        }
        if (requested <= 0) {
            toast("Quantity must be at least 1.", Toast.WARN);
            return;
        }
        String[] d = inventory.get(id);            // [desc, inStock, qty, price]
        if (!"true".equalsIgnoreCase(d[1]) || Integer.parseInt(d[2]) <= 0) {
            toast("That item is currently out of stock.", Toast.ERROR);
            renderEmptyDetail();
            return;
        }
        int available = Integer.parseInt(d[2]);
        if (requested > available) {
            toast("Only " + available + " in stock — reduce the quantity.", Toast.ERROR);
            return;
        }
        double price = Double.parseDouble(d[3]);
        double discount = requested >= 15 ? 0.20 : requested >= 10 ? 0.15 : requested >= 5 ? 0.10 : 0.0;
        pending = new CartItem(id, d[0], requested, price, discount, available);
        renderProductDetail(pending);
    }

    private void addPendingToCart() {
        if (pending == null) return;
        if (cart.size() >= MAX_CART) {
            toast("Cart is full — max " + MAX_CART + " line items.", Toast.WARN);
            return;
        }
        cart.add(pending);
        toast("Added " + pending.qty + " × \"" + clip(pending.desc, 26) + "\".", Toast.SUCCESS);
        pending = null;
        idField.setText("");
        qtyField.setText("");
        renderEmptyDetail();
        refreshCart();
    }

    private void removeAt(int i) {
        if (i < 0 || i >= cart.size()) return;
        cart.remove(i);
        refreshCart();
    }

    private void emptyCart() {
        if (cart.isEmpty()) { toast("Cart is already empty.", Toast.WARN); return; }
        cart.clear();
        refreshCart();
        toast("Cart emptied.", Toast.SUCCESS);
    }

    private void checkout() {
        if (cart.isEmpty()) { toast("Add an item before checking out.", Toast.WARN); return; }
        double sub = subtotal();
        double tax = sub * TAX_RATE;
        double total = sub + tax;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String timestamp = dtf.format(LocalDateTime.now());
        String txId = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").format(LocalDateTime.now());

        List<CartItem> snapshot = new ArrayList<CartItem>(cart);
        try (PrintWriter out = new PrintWriter(new FileWriter("transactions.csv", true))) {
            for (CartItem it : snapshot) {
                out.println(txId + "," + timestamp + ",Item: " + it.id
                        + ", Description: " + it.desc
                        + ", Price: " + MONEY.format(it.price)
                        + ", Qty: " + it.qty
                        + ", Subtotal: " + MONEY.format(it.lineTotal()));
            }
        } catch (IOException ex) {
            toast("Could not write transaction log.", Toast.ERROR);
        }

        cart.clear();
        refreshCart();
        renderReceipt(txId, timestamp, snapshot, sub, tax, total);
        toast("Order placed — receipt ready.", Toast.SUCCESS);
    }

    private double subtotal() {
        double s = 0;
        for (CartItem it : cart) s += it.lineTotal();
        return s;
    }

    /* ========================== RENDERING ========================== */
    private void refreshCart() {
        cartList.removeAll();
        if (cart.isEmpty()) {
            cartList.add(emptyState("Your cart is empty",
                    "Search a product and add it to start an order."));
        } else {
            for (int i = 0; i < cart.size(); i++) {
                cartList.add(new CartRow(i + 1, cart.get(i), i));
                if (i < cart.size() - 1) cartList.add(Box.createVerticalStrut(10));
            }
        }
        cartList.revalidate();
        cartList.repaint();

        double sub = subtotal();
        double tax = sub * TAX_RATE;
        sumSub.setText(MONEY.format(sub));
        sumTax.setText(MONEY.format(tax));
        sumTotal.setText(MONEY.format(sub + tax));
        cartCount.setText("[ " + cart.size() + " / " + MAX_CART + " ]");

        boolean has = !cart.isEmpty();
        checkoutBtn.setEnabled(has);
        emptyBtn.setEnabled(has);
        if (cartPill != null) cartPill.update(cart.size(), sub + tax);
    }

    private void renderEmptyDetail() {
        detailHost.removeAll();
        detailHost.add(emptyState("No product selected",
                "Search results will appear here."), BorderLayout.CENTER);
        detailHost.revalidate();
        detailHost.repaint();
    }

    private void renderProductDetail(CartItem it) {
        detailHost.removeAll();
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        top.setOpaque(false);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        top.add(new Pill("#" + it.id, Pill.SOLID_INK));
        top.add(new Pill("IN STOCK · " + it.available, Pill.OUTLINE));
        if (it.discount > 0)
            top.add(new Pill("SAVE " + (int) Math.round(it.discount * 100) + "%", Pill.SOLID_LIME));
        p.add(top);
        p.add(Box.createVerticalStrut(14));

        JLabel name = new JLabel(clip(it.desc, 42));
        name.setFont(tracked(serif(Font.BOLD, 22), -0.015f));
        name.setForeground(INK);
        name.setAlignmentX(LEFT_ALIGNMENT);
        p.add(name);
        p.add(Box.createVerticalStrut(6));

        JLabel unit = new JLabel(MONEY.format(it.price) + " each   ·   qty " + it.qty);
        unit.setFont(font(Font.PLAIN, 13));
        unit.setForeground(MUTED);
        unit.setAlignmentX(LEFT_ALIGNMENT);
        p.add(unit);
        p.add(Box.createVerticalStrut(14));
        p.add(new Divider());
        p.add(Box.createVerticalStrut(12));

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setAlignmentX(LEFT_ALIGNMENT);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JLabel tl = new JLabel("LINE SUBTOTAL");
        tl.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.1f));
        tl.setForeground(MUTED);
        JLabel tv = new JLabel(MONEY.format(it.lineTotal()));
        tv.setFont(tracked(serif(Font.BOLD, 28), -0.02f));
        tv.setForeground(INK);
        totalRow.add(tl, BorderLayout.WEST);
        totalRow.add(tv, BorderLayout.EAST);
        p.add(totalRow);

        if (it.discount > 0) {
            JLabel saved = new JLabel("You save "
                    + MONEY.format(it.price * it.qty - it.lineTotal()) + " on this line");
            saved.setFont(font(Font.BOLD, 12));
            saved.setForeground(new Color(0x2E, 0x7D, 0x32));
            saved.setAlignmentX(LEFT_ALIGNMENT);
            p.add(Box.createVerticalStrut(6));
            p.add(saved);
        }
        p.add(Box.createVerticalStrut(20));

        addBtn = new RoundButton("Add to cart →", RoundButton.LIME);
        addBtn.setFullWidth(true);
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { addPendingToCart(); }
        });
        p.add(addBtn);

        detailHost.add(p, BorderLayout.NORTH);
        detailHost.revalidate();
        detailHost.repaint();
    }

    /** Inline post-checkout receipt — itemized paper slip, rendered in place. */
    private void renderReceipt(String txId, String date, List<CartItem> items,
                              double sub, double tax, double total) {
        detailHost.removeAll();

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(eyebrow("03 / RECEIPT"));
        p.add(Box.createVerticalStrut(10));

        JPanel headRow = new JPanel();
        headRow.setOpaque(false);
        headRow.setLayout(new BoxLayout(headRow, BoxLayout.X_AXIS));
        headRow.setAlignmentX(LEFT_ALIGNMENT);
        headRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JComponent check = new JComponent() {
            public Dimension getPreferredSize() { return new Dimension(30, 30); }
            public Dimension getMaximumSize() { return new Dimension(30, 30); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(LIME);
                g2.fillRect(0, 0, 30, 30);
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(8, 16, 13, 21);
                g2.drawLine(13, 21, 23, 9);
            }
        };
        JPanel checkWrap = new JPanel(new GridBagLayout());
        checkWrap.setOpaque(false);
        checkWrap.setMaximumSize(new Dimension(30, 34));
        checkWrap.add(check);
        headRow.add(checkWrap);
        headRow.add(Box.createHorizontalStrut(12));
        JLabel h = new JLabel("Order confirmed");
        h.setFont(tracked(serif(Font.BOLD, 24), -0.025f));
        h.setForeground(INK);
        JPanel hWrap = new JPanel(new GridBagLayout());
        hWrap.setOpaque(false);
        hWrap.add(h);
        headRow.add(hWrap);
        headRow.add(Box.createHorizontalGlue());
        p.add(headRow);
        p.add(Box.createVerticalStrut(8));

        JLabel meta = new JLabel("TXN " + txId + "  ·  " + date);
        meta.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.06f));
        meta.setForeground(MUTED);
        meta.setAlignmentX(LEFT_ALIGNMENT);
        p.add(meta);
        p.add(Box.createVerticalStrut(18));

        ReceiptSlip slip = new ReceiptSlip();
        JLabel store = new JLabel("NILE DOT COM · PREMIUM E-STORE");
        store.setFont(tracked(mono(Font.PLAIN, 9.5f), 0.14f));
        store.setForeground(MUTED);
        store.setAlignmentX(LEFT_ALIGNMENT);
        slip.add(store);
        slip.add(Box.createVerticalStrut(12));
        slip.add(new Divider());
        slip.add(Box.createVerticalStrut(12));
        for (int i = 0; i < items.size(); i++) {
            slip.add(receiptItemRow(items.get(i)));
            if (i < items.size() - 1) slip.add(Box.createVerticalStrut(10));
        }
        slip.add(Box.createVerticalStrut(14));
        slip.add(new Divider());
        slip.add(Box.createVerticalStrut(12));
        slip.add(receiptTotal("Subtotal", MONEY.format(sub), false));
        slip.add(Box.createVerticalStrut(7));
        slip.add(receiptTotal("Sales tax (6%)", MONEY.format(tax), false));
        slip.add(Box.createVerticalStrut(12));
        slip.add(new HardRule());
        slip.add(Box.createVerticalStrut(12));
        slip.add(receiptTotal("Total paid", MONEY.format(total), true));
        p.add(slip);

        double savedAmt = 0;
        for (CartItem it : items) savedAmt += it.price * it.qty - it.lineTotal();
        if (savedAmt > 0) {
            p.add(Box.createVerticalStrut(12));
            JLabel sv = new JLabel("You saved " + MONEY.format(savedAmt) + " with bulk pricing");
            sv.setFont(font(Font.BOLD, 12));
            sv.setForeground(new Color(0x2E, 0x7D, 0x32));
            sv.setAlignmentX(LEFT_ALIGNMENT);
            p.add(sv);
        }
        p.add(Box.createVerticalStrut(10));
        JLabel note = new JLabel("RECEIPT APPENDED TO TRANSACTIONS.CSV");
        note.setFont(tracked(mono(Font.PLAIN, 9), 0.1f));
        note.setForeground(FAINT);
        note.setAlignmentX(LEFT_ALIGNMENT);
        p.add(note);
        p.add(Box.createVerticalStrut(20));

        RoundButton again = new RoundButton("Start new order →", RoundButton.LIME);
        again.setFullWidth(true);
        again.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { renderEmptyDetail(); }
        });
        p.add(again);

        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(p, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(holder,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null);
        sc.setOpaque(false);
        sc.getViewport().setOpaque(false);
        sc.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(sc.getVerticalScrollBar());
        detailHost.add(sc, BorderLayout.CENTER);
        detailHost.revalidate();
        detailHost.repaint();
    }

    private JComponent receiptItemRow(CartItem it) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(it.qty + " × " + clip(it.desc, 28));
        name.setFont(font(Font.BOLD, 13.5f));
        name.setForeground(INK);
        name.setAlignmentX(LEFT_ALIGNMENT);
        String m = MONEY.format(it.price) + " each"
                + (it.discount > 0 ? "   ·   −" + (int) Math.round(it.discount * 100) + "%" : "");
        JLabel mlab = new JLabel(m);
        mlab.setFont(tracked(mono(Font.PLAIN, 10), 0.04f));
        mlab.setForeground(MUTED);
        mlab.setAlignmentX(LEFT_ALIGNMENT);
        mid.add(name);
        mid.add(Box.createVerticalStrut(3));
        mid.add(mlab);
        r.add(mid, BorderLayout.CENTER);

        JLabel tot = new JLabel(MONEY.format(it.lineTotal()));
        tot.setFont(tracked(mono(Font.BOLD, 13.5f), 0.02f));
        tot.setForeground(INK);
        JPanel tw = new JPanel(new GridBagLayout());
        tw.setOpaque(false);
        tw.add(tot);
        r.add(tw, BorderLayout.EAST);
        return r;
    }

    private JComponent receiptTotal(String label, String value, boolean strong) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, strong ? 40 : 22));
        JLabel l = new JLabel(label);
        JLabel v = new JLabel(value);
        if (strong) {
            l.setFont(tracked(serif(Font.BOLD, 19), -0.01f)); l.setForeground(INK);
            v.setFont(tracked(serif(Font.BOLD, 24), -0.02f)); v.setForeground(INK);
        } else {
            l.setFont(font(Font.PLAIN, 12.5f)); l.setForeground(MUTED);
            v.setFont(tracked(mono(Font.BOLD, 12.5f), 0.02f)); v.setForeground(INK);
        }
        r.add(l, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private JComponent emptyState(String title, String sub) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(30, 14, 30, 14));

        JComponent glyph = new JComponent() {
            public Dimension getPreferredSize() { return new Dimension(54, 54); }
            public Dimension getMaximumSize() { return new Dimension(54, 54); }
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawRect(0, 0, 53, 53);
                g2.setColor(LIME);
                g2.fillRect(0, 0, 14, 14);
                g2.setColor(INK);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRect(17, 22, 20, 16);
                g2.drawLine(17, 27, 37, 27);
            }
        };
        glyph.setAlignmentX(CENTER_ALIGNMENT);
        p.add(glyph);
        p.add(Box.createVerticalStrut(16));

        JLabel t = new JLabel(title);
        t.setFont(tracked(serif(Font.BOLD, 17), -0.01f));
        t.setForeground(INK);
        t.setAlignmentX(CENTER_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(6));

        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12.5f));
        s.setForeground(MUTED);
        s.setAlignmentX(CENTER_ALIGNMENT);
        p.add(s);
        return p;
    }

    /* ===================== SMALL LABEL HELPERS ===================== */
    private JLabel eyebrow(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.12f));
        l.setForeground(MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private JLabel heading(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(serif(Font.BOLD, 24), -0.025f));
        l.setForeground(INK);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private JLabel fieldLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.1f));
        l.setForeground(INK_SOFT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    /* ========================= TOAST SYSTEM ======================== */
    private void toast(String msg, int type) { Toast.show(this, msg, type); }

    /* ========================== DATA LOAD ========================== */
    private Map<String, String[]> loadInventory(String filename) {
        Map<String, String[]> map = new HashMap<String, String[]>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] t = line.split(",");
                if (t.length < 5) continue;
                for (int i = 0; i < t.length; i++) t[i] = t[i].replaceAll("^\"|\"$", "").trim();
                map.put(t[0], new String[] { t[1], t[2], t[3], t[4] });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /* =========================== MODEL ============================= */
    static class CartItem {
        final String id, desc;
        final int qty, available;
        final double price, discount;
        CartItem(String id, String desc, int qty, double price, double discount, int available) {
            this.id = id; this.desc = desc; this.qty = qty;
            this.price = price; this.discount = discount; this.available = available;
        }
        double lineTotal() { return qty * price * (1 - discount); }
    }

    /* ====================== CUSTOM COMPONENTS ====================== */

    /** Flat, hard-bordered block (sharp corners) — the mill3 module look. */
    static class Block extends JPanel {
        Block(int pad) {
            setOpaque(false);
            setBorder(new EmptyBorder(pad, pad, pad, pad));
            setAlignmentX(LEFT_ALIGNMENT);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(SURFACE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(0, 0, w - 1, h - 1);
            super.paintComponent(g);
        }
    }

    /** Receipt slip — white paper, hard border, perforated bottom edge. */
    static class ReceiptSlip extends JPanel {
        ReceiptSlip() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(18, 18, 24, 18));
            setAlignmentX(LEFT_ALIGNMENT);
        }
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, super.getPreferredSize().height);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            int tooth = 12;
            int baseY = h - 9;
            java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
            path.moveTo(0, 0);
            path.lineTo(w - 1, 0);
            path.lineTo(w - 1, baseY);
            float x = w - 1;
            while (x > 0) {
                path.lineTo(x - tooth / 2f, baseY + 7);
                path.lineTo(x - tooth, baseY);
                x -= tooth;
            }
            path.lineTo(0, baseY);
            path.closePath();
            g2.setColor(Color.WHITE);
            g2.fill(path);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(path);
            super.paintComponent(g);
        }
    }

    /** Full-pill button. LIME (accent), INK (dark), GHOST (outline). Inverts on hover. */
    static class RoundButton extends JButton {
        static final int LIME = 0, INK = 1, GHOST = 2;
        private final int variant;
        private boolean hover, press, fullWidth;
        RoundButton(String text, int variant) {
            super(text);
            this.variant = variant;
            setFont(tracked(font(Font.BOLD, 12.5f), 0.04f));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(14, 22, 14, 22));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e)  { hover = false; press = false; repaint(); }
                public void mousePressed(MouseEvent e) { press = true; repaint(); }
                public void mouseReleased(MouseEvent e){ press = false; repaint(); }
            });
        }
        void setFullWidth(boolean f) {
            fullWidth = f;
            setAlignmentX(LEFT_ALIGNMENT);
            if (f) setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        }
        public Dimension getMaximumSize() {
            return fullWidth ? new Dimension(Integer.MAX_VALUE, 50) : super.getMaximumSize();
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight(), arc = h; // full pill
            boolean on = isEnabled();
            Color fill, txt, line = null;
            if (!on) {
                fill = new Color(0xE3, 0xE0, 0xD8); txt = FAINT;
            } else if (variant == GHOST) {
                if (hover) { fill = NileDotCom.INK; txt = PAPER; }
                else { fill = null; txt = NileDotCom.INK; line = NileDotCom.INK; }
            } else if (variant == INK) {
                if (hover) { fill = NileDotCom.LIME; txt = NileDotCom.INK; }
                else { fill = NileDotCom.INK; txt = PAPER; }
            } else { // LIME
                if (hover) { fill = NileDotCom.INK; txt = NileDotCom.LIME; }
                else { fill = NileDotCom.LIME; txt = NileDotCom.INK; }
            }
            int dy = (on && press) ? 1 : 0;
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, dy, w, h - 1, arc, arc);
            }
            if (line != null) {
                g2.setColor(line);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, dy, w - 1, h - 2, arc, arc);
            }
            g2.setColor(txt);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String t = getText().toUpperCase();
            // re-apply tracking to uppercased text
            g2.setFont(tracked(font(Font.BOLD, 12.5f), 0.06f));
            fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(t)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent() + dy;
            g2.drawString(t, tx, ty);
        }
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }
    }

    /** Flat field, hard border, sharp corners; focus ring turns lime. */
    static class RoundField extends JTextField {
        private final String placeholder;
        RoundField(String placeholder) {
            this.placeholder = placeholder;
            setFont(font(Font.PLAIN, 14));
            setForeground(INK);
            setOpaque(false);
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setCaretColor(INK);
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { repaint(); }
                public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 46); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            boolean focus = isFocusOwner();
            if (focus) {
                g2.setColor(LIME);
                g2.fillRect(0, 0, 4, h);
            }
            g2.setColor(focus ? INK : new Color(0xC9, 0xC4, 0xBA));
            g2.setStroke(new BasicStroke(focus ? 1.8f : 1.2f));
            g2.drawRect(0, 0, w - 1, h - 1);
            super.paintComponent(g);
            if (getText().isEmpty() && !focus) {
                g2.setColor(FAINT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                Insets in = getInsets();
                g2.drawString(placeholder, in.left, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        }
    }

    /** Status pill / tag. OUTLINE, SOLID_INK, SOLID_LIME. */
    static class Pill extends JComponent {
        static final int OUTLINE = 0, SOLID_INK = 1, SOLID_LIME = 2;
        private final String text; private final int style;
        Pill(String text, int style) {
            this.text = text.toUpperCase(); this.style = style;
            setFont(tracked(mono(Font.PLAIN, 10), 0.08f));
        }
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 24, 24);
        }
        public Dimension getMaximumSize() { return getPreferredSize(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            Color bg, fg, br = null;
            if (style == SOLID_INK) { bg = INK; fg = PAPER; }
            else if (style == SOLID_LIME) { bg = LIME; fg = INK; }
            else { bg = null; fg = INK; br = new Color(0xBC, 0xB7, 0xAC); }
            if (bg != null) { g2.setColor(bg); g2.fillRoundRect(0, 0, w, h, h, h); }
            if (br != null) { g2.setColor(br); g2.drawRoundRect(0, 0, w - 1, h - 1, h, h); }
            g2.setColor(fg);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 12, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    /** Cart line-item row — bordered hard block. */
    class CartRow extends JPanel {
        CartRow(int index, final CartItem it, final int modelIndex) {
            setOpaque(false);
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(13, 14, 13, 12));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

            JComponent badge = new JComponent() {
                public Dimension getPreferredSize() { return new Dimension(32, 32); }
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = aa(g);
                    g2.setColor(INK);
                    g2.fillRect(0, 0, 32, 32);
                    g2.setColor(LIME);
                    g2.setFont(tracked(mono(Font.BOLD, 12), 0.04f));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = (index < 10 ? "0" : "") + index;
                    g2.drawString(s, (32 - fm.stringWidth(s)) / 2, (32 - fm.getHeight()) / 2 + fm.getAscent());
                }
            };
            JPanel badgeWrap = new JPanel(new GridBagLayout());
            badgeWrap.setOpaque(false);
            badgeWrap.add(badge);
            add(badgeWrap, BorderLayout.WEST);

            JPanel mid = new JPanel();
            mid.setOpaque(false);
            mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(clip(it.desc, 32));
            name.setFont(font(Font.BOLD, 14));
            name.setForeground(INK);
            name.setAlignmentX(LEFT_ALIGNMENT);
            String sub = it.qty + " × " + MONEY.format(it.price)
                    + (it.discount > 0 ? "   ·   −" + (int) Math.round(it.discount * 100) + "%" : "");
            JLabel meta = new JLabel(sub);
            meta.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.04f));
            meta.setForeground(MUTED);
            meta.setAlignmentX(LEFT_ALIGNMENT);
            mid.add(name);
            mid.add(Box.createVerticalStrut(4));
            mid.add(meta);
            add(mid, BorderLayout.CENTER);

            JPanel right = new JPanel();
            right.setOpaque(false);
            right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
            JLabel total = new JLabel(MONEY.format(it.lineTotal()));
            total.setFont(font(Font.BOLD, 15));
            total.setForeground(INK);
            right.add(total);
            right.add(Box.createHorizontalStrut(12));
            right.add(new RemoveButton(new Runnable() {
                public void run() { removeAt(modelIndex); }
            }));
            JPanel rightWrap = new JPanel(new GridBagLayout());
            rightWrap.setOpaque(false);
            rightWrap.add(right);
            add(rightWrap, BorderLayout.EAST);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(0xCD, 0xC8, 0xBE));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    /** Square × remove button (mill3 hard edges). */
    static class RemoveButton extends JComponent {
        private boolean hover;
        RemoveButton(final Runnable onClick) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
        }
        public Dimension getPreferredSize() { return new Dimension(28, 28); }
        public Dimension getMaximumSize() { return new Dimension(28, 28); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(hover ? ORANGE : Color.WHITE);
            g2.fillRect(0, 0, 27, 27);
            g2.setColor(hover ? ORANGE : new Color(0xC9, 0xC4, 0xBA));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRect(0, 0, 27, 27);
            g2.setColor(hover ? Color.WHITE : MUTED);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(9, 9, 18, 18);
            g2.drawLine(18, 9, 9, 18);
        }
    }

    /** Brand logo mark — lime square with ink monogram. */
    static class LogoMark extends JComponent {
        public Dimension getPreferredSize() { return new Dimension(48, 48); }
        public Dimension getMaximumSize() { return new Dimension(48, 48); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(LIME);
            g2.fillRect(0, 0, 48, 48);
            g2.setColor(INK);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.drawLine(15, 34, 15, 14);
            g2.drawLine(15, 14, 33, 34);
            g2.drawLine(33, 34, 33, 14);
        }
    }

    /** Live cart indicator — lime pill, ink text. */
    static class CartPill extends JComponent {
        private int count = 0; private double total = 0;
        CartPill() { setFont(font(Font.BOLD, 14)); }
        void update(int count, double total) { this.count = count; this.total = total; revalidate(); repaint(); }
        public Dimension getPreferredSize() { return new Dimension(Math.max(170, textW() + 70), 44); }
        public Dimension getMaximumSize() { return getPreferredSize(); }
        private int textW() {
            FontMetrics fm = getFontMetrics(font(Font.BOLD, 15));
            return fm.stringWidth(MONEY.format(total));
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(LIME);
            g2.fillRoundRect(0, 0, w, h, h, h);
            // count chip
            g2.setColor(INK);
            g2.fillOval(7, (h - 30) / 2, 30, 30);
            g2.setColor(LIME);
            g2.setFont(tracked(mono(Font.BOLD, 13), 0.02f));
            FontMetrics fb = g2.getFontMetrics();
            String c = String.valueOf(count);
            g2.drawString(c, 7 + (30 - fb.stringWidth(c)) / 2, (h - fb.getHeight()) / 2 + fb.getAscent());
            // labels
            g2.setColor(new Color(0x2A, 0x33, 0x06));
            g2.setFont(tracked(mono(Font.PLAIN, 9), 0.12f));
            g2.drawString(count == 1 ? "ITEM" : "ITEMS", 46, 18);
            g2.setColor(INK);
            g2.setFont(font(Font.BOLD, 15));
            g2.drawString(MONEY.format(total), 46, 35);
        }
    }

    /** Light dotted-feel divider. */
    static class Divider extends JComponent {
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 1); }
        public Dimension getPreferredSize() { return new Dimension(10, 1); }
        protected void paintComponent(Graphics g) { g.setColor(HAIRLINE); g.fillRect(0, 0, getWidth(), 1); }
    }
    /** Solid ink rule. */
    static class HardRule extends JComponent {
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 2); }
        public Dimension getPreferredSize() { return new Dimension(10, 2); }
        protected void paintComponent(Graphics g) { g.setColor(INK); g.fillRect(0, 0, getWidth(), 2); }
    }

    /** Slide-in toast — ink block, lime marker, sharp corners. */
    static class Toast extends JComponent {
        static final int SUCCESS = 0, ERROR = 1, WARN = 2;
        private final String msg; private final int type;
        Toast(String msg, int type) { this.msg = msg; this.type = type; setFont(font(Font.BOLD, 13)); }
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(Math.min(470, fm.stringWidth(msg) + 80), 50);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(INK);
            g2.fillRect(0, 0, w - 4, h - 4);
            Color mark = type == ERROR ? ORANGE : type == WARN ? new Color(0xFF, 0xC8, 0x3D) : NileDotCom.LIME;
            g2.setColor(mark);
            g2.fillRect(0, 0, 6, h - 4);
            g2.fillRect(20, h / 2 - 7, 9, 9);
            g2.setColor(PAPER);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, 42, (h - 4 - fm.getHeight()) / 2 + fm.getAscent());
        }
        static void show(JFrame frame, String msg, int type) {
            final JLayeredPane lp = frame.getLayeredPane();
            final Toast t = new Toast(msg, type);
            final Dimension ps = t.getPreferredSize();
            final int x = (frame.getWidth() - ps.width) / 2;
            final int endY = 120;
            t.setBounds(x, endY - 26, ps.width, ps.height);
            lp.add(t, JLayeredPane.POPUP_LAYER);
            lp.repaint();
            final long t0 = System.currentTimeMillis();
            final Timer in = new Timer(12, null);
            in.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    float p = Math.min(1f, (System.currentTimeMillis() - t0) / 180f);
                    float e2 = 1 - (1 - p) * (1 - p);
                    t.setBounds(x, (int) (endY - 26 + 26 * e2), ps.width, ps.height);
                    if (p >= 1f) in.stop();
                }
            });
            in.start();
            Timer out = new Timer(2300, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ((Timer) e.getSource()).stop();
                    final long s0 = System.currentTimeMillis();
                    final Timer fade = new Timer(12, null);
                    fade.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ev) {
                            float p = Math.min(1f, (System.currentTimeMillis() - s0) / 160f);
                            t.setBounds(x, (int) (endY - 26 * p), ps.width, ps.height);
                            if (p >= 1f) { fade.stop(); lp.remove(t); lp.repaint(); }
                        }
                    });
                    fade.start();
                }
            });
            out.setRepeats(false);
            out.start();
        }
    }

    /* =========================== UTIL ============================== */
    static void styleScrollBar(JScrollBar bar) {
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(8, 0));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            protected void configureScrollBarColors() {
                thumbColor = INK; trackColor = PAPER;
            }
            protected JButton createDecreaseButton(int o) { return zeroButton(); }
            protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                return b;
            }
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = aa(g);
                g2.setColor(thumbColor);
                g2.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
            }
        });
    }

    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }
    static Font font(int style, float size)  { return new Font(GROTESK, style, Math.round(size)).deriveFont(size); }
    static Font serif(int style, float size) { return new Font(SERIF, style, Math.round(size)).deriveFont(size); }
    static Font mono(int style, float size)  { return new Font(MONO, style, Math.round(size)).deriveFont(size); }
    static Font tracked(Font base, float tracking) {
        java.util.Map<java.awt.font.TextAttribute, Object> a =
                new java.util.HashMap<java.awt.font.TextAttribute, Object>();
        a.put(java.awt.font.TextAttribute.TRACKING, Float.valueOf(tracking));
        return base.deriveFont(a);
    }
    static String pickFont(String[] prefs) {
        java.util.Set<String> have = new java.util.HashSet<String>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            have.add(f);
        for (String p : prefs) if (have.contains(p)) return p;
        return prefs[prefs.length - 1];
    }
    static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new NileDotCom().setVisible(true); }
        });
    }
}
