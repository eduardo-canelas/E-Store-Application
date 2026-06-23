/*
Nile dot com — Premium E-Store (v2)

Java Swing storefront over an extracted, unit-tested domain (com.nilecom.domain),
a repository-backed SQLite store (com.nilecom.persistence) and a natural-language
catalogue search (com.nilecom.search). UI language is a mill3.studio-inspired
brutalist-editorial system: warm paper canvas, hard ink borders, serif display,
mono micro-labels, single acid-lime accent, full-pill buttons that invert on hover.
Light/dark theming via Theme tokens. Custom-painted, no UI dependencies.
*/
package com.nilecom.ui;

import com.nilecom.domain.Cart;
import com.nilecom.domain.CartItem;
import com.nilecom.domain.Order;
import com.nilecom.domain.Pricing;
import com.nilecom.domain.Product;
import com.nilecom.persistence.CsvTransactionLog;
import com.nilecom.persistence.InventoryRepository;
import com.nilecom.persistence.OrderRepository;
import com.nilecom.search.NaturalLanguageSearch;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NileDotCom extends JFrame {

    static final DecimalFormat MONEY = new DecimalFormat("$#,##0.00");
    static String GROTESK = pickFont(new String[]{
            "Basel Grotesk", "Helvetica Neue", "Arimo", "Inter", "Segoe UI", "Arial" });
    static String SERIF = pickFont(new String[]{
            "Times Now", "Georgia", "Times New Roman", "Times", "Serif" });
    static String MONO = pickFont(new String[]{
            "Menlo", "SF Mono", "Monaco", "Roboto Mono", "Courier New", "Monospaced" });

    /* dependencies */
    private final InventoryRepository inventory;
    private final OrderRepository orders;
    private final CsvTransactionLog txLog;

    /* state */
    private final Cart cart = new Cart();
    private enum View { EMPTY, RESULTS, DETAIL, RECEIPT, HISTORY }
    private View view = View.EMPTY;
    private List<Product> lastResults = new java.util.ArrayList<>();
    private Product detailProduct;
    private Order receiptOrder;

    /* widgets (recreated by rebuild()) */
    private JPanel detailHost;
    private JPanel cartList;
    private JLabel cartCount, sumSub, sumTax, sumTotal;
    private RoundButton checkoutBtn, emptyBtn;
    private CartPill cartPill;
    private RoundField searchField;

    /* detail live-preview refs */
    private RoundField qtyField;
    private JPanel detailPills;
    private JLabel detailLineVal, detailSaved;

    public NileDotCom(InventoryRepository inventory, OrderRepository orders, CsvTransactionLog txLog) {
        this.inventory = inventory;
        this.orders = orders;
        this.txLog = txLog;
        setTitle("Nile dot com — Premium E-Store");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 820));
        setSize(1200, 900);
        setLocationRelativeTo(null);
        rebuild();
    }

    /** Recreate the whole content pane (used on launch and on theme toggle). */
    private void rebuild() {
        JPanel rootBg = new JPanel(new BorderLayout());
        rootBg.setBackground(Theme.canvas());
        setContentPane(rootBg);
        rootBg.add(buildHeader(), BorderLayout.NORTH);
        rootBg.add(buildBody(), BorderLayout.CENTER);
        refreshCart();
        renderCurrentView();
        revalidate();
        repaint();
    }

    /* ============================ HEADER ============================ */
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(Theme.inkBlock());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.accent());
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
        kicker.setForeground(Theme.accent());
        kicker.setAlignmentX(LEFT_ALIGNMENT);
        JLabel mark = new JLabel("Nile dot com");
        mark.setFont(tracked(serif(Font.BOLD, 28), -0.02f));
        mark.setForeground(Theme.onInk());
        mark.setAlignmentX(LEFT_ALIGNMENT);
        words.add(kicker);
        words.add(Box.createVerticalStrut(2));
        words.add(mark);
        brand.add(words);
        header.add(brand, BorderLayout.WEST);

        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
        east.add(wrapCenter(new HeaderLink(Theme.isDark() ? "Light" : "Dark", new Runnable() {
            public void run() { Theme.toggle(); rebuild(); }
        })));
        east.add(Box.createHorizontalStrut(10));
        east.add(wrapCenter(new HeaderLink("History", new Runnable() {
            public void run() { showHistory(); }
        })));
        east.add(Box.createHorizontalStrut(16));
        cartPill = new CartPill();
        east.add(wrapCenter(cartPill));
        header.add(east, BorderLayout.EAST);
        return header;
    }

    /* ============================= BODY ============================= */
    private JComponent buildBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(28, 30, 30, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.gridx = 0; c.weightx = 0.42; c.insets = new Insets(0, 0, 0, 12);
        body.add(buildLeftColumn(), c);
        c.gridx = 1; c.weightx = 0.58; c.insets = new Insets(0, 12, 0, 0);
        body.add(buildCartCard(), c);
        return body;
    }

    private JComponent buildLeftColumn() {
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
        card.add(Box.createVerticalStrut(20));

        card.add(fieldLabel("WHAT ARE YOU AFTER?"));
        card.add(Box.createVerticalStrut(8));
        searchField = new RoundField("Try: cheap usb cable under $10");
        card.add(searchField);
        card.add(Box.createVerticalStrut(16));

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        RoundButton browse = new RoundButton("Browse all", RoundButton.GHOST);
        browse.addActionListener(e -> browseAll());
        RoundButton search = new RoundButton("Search →", RoundButton.INK);
        search.addActionListener(e -> doSearch());
        searchField.addActionListener(e -> doSearch());
        actions.add(browse);
        actions.add(search);
        card.add(actions);

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
        l.setForeground(Theme.muted());
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
        cartCount.setForeground(Theme.muted());
        head.add(wrapCenter(cartCount), BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        cartList = new JPanel();
        cartList.setOpaque(false);
        cartList.setLayout(new BoxLayout(cartList, BoxLayout.Y_AXIS));
        JPanel listHolder = new JPanel(new BorderLayout());
        listHolder.setOpaque(false);
        listHolder.add(cartList, BorderLayout.NORTH);
        JScrollPane scroll = scrollOf(listHolder);
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
        sumSub = new JLabel(); sumTax = new JLabel(); sumTotal = new JLabel();
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
        emptyBtn.addActionListener(e -> emptyCart());
        checkoutBtn = new RoundButton("Checkout →", RoundButton.LIME);
        checkoutBtn.addActionListener(e -> checkout());
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
        if (strong) { l.setFont(tracked(serif(Font.BOLD, 20), -0.01f)); l.setForeground(Theme.text()); }
        else { l.setFont(font(Font.PLAIN, 13)); l.setForeground(Theme.muted()); }
        value.setFont(strong ? tracked(serif(Font.BOLD, 28), -0.02f) : font(Font.BOLD, 13.5f));
        value.setForeground(Theme.text());
        row.add(l, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    /* ========================= VIEW DISPATCH ========================= */
    private void renderCurrentView() {
        switch (view) {
            case RESULTS:  renderResults(); break;
            case DETAIL:   if (detailProduct != null) renderDetail(detailProduct); else renderEmptyDetail(); break;
            case RECEIPT:  if (receiptOrder != null) renderReceipt(receiptOrder, false); else renderEmptyDetail(); break;
            case HISTORY:  renderHistory(); break;
            default:       renderEmptyDetail();
        }
    }

    /* ========================= INTERACTIONS ========================= */
    private void doSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { toast("Type what you're looking for.", Toast.WARN); return; }
        lastResults = NaturalLanguageSearch.search(q, inventory.findAll());
        view = View.RESULTS;
        renderResults();
    }

    private void browseAll() {
        lastResults = inventory.findAll();
        view = View.RESULTS;
        renderResults();
    }

    private void selectProduct(Product p) {
        detailProduct = p;
        view = View.DETAIL;
        renderDetail(p);
    }

    private void addToCart() {
        if (detailProduct == null) return;
        if (!detailProduct.isAvailable()) { toast("That item is out of stock.", Toast.ERROR); return; }
        int qty;
        try { qty = Integer.parseInt(qtyField.getText().trim()); }
        catch (NumberFormatException ex) { toast("Quantity must be a whole number.", Toast.WARN); return; }
        if (qty <= 0) { toast("Quantity must be at least 1.", Toast.WARN); return; }
        if (qty > detailProduct.quantityAvailable()) {
            toast("Only " + detailProduct.quantityAvailable() + " in stock.", Toast.ERROR); return;
        }
        if (cart.isFull()) { toast("Cart is full — max " + Cart.MAX_ITEMS + " line items.", Toast.WARN); return; }
        cart.add(new CartItem(detailProduct, qty));
        toast("Added " + qty + " × \"" + clip(detailProduct.description(), 24) + "\".", Toast.SUCCESS);
        refreshCart();
        view = View.RESULTS;
        renderResults();
    }

    private void removeAt(int i) { cart.removeAt(i); refreshCart(); }

    private void emptyCart() {
        if (cart.isEmpty()) { toast("Cart is already empty.", Toast.WARN); return; }
        cart.clear();
        refreshCart();
        toast("Cart emptied.", Toast.SUCCESS);
    }

    private void checkout() {
        if (cart.isEmpty()) { toast("Add an item before checking out.", Toast.WARN); return; }
        String timestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
        String txId = DateTimeFormatter.ofPattern("ddMMyyyyHHmmss").format(LocalDateTime.now());
        Order order = Order.fromCart(txId, timestamp, cart);
        orders.save(order);
        try { txLog.append(order); }
        catch (IOException ex) { toast("Could not write transaction log.", Toast.ERROR); }
        cart.clear();
        refreshCart();
        receiptOrder = order;
        view = View.RECEIPT;
        renderReceipt(order, false);
        toast("Order placed — receipt ready.", Toast.SUCCESS);
    }

    private void showHistory() { view = View.HISTORY; renderHistory(); }

    /* ========================== RENDERING ========================== */
    private void refreshCart() {
        cartList.removeAll();
        if (cart.isEmpty()) {
            cartList.add(emptyState("Your cart is empty",
                    "Search a product and add it to start an order."));
        } else {
            List<CartItem> items = cart.items();
            for (int i = 0; i < items.size(); i++) {
                cartList.add(new CartRow(i + 1, items.get(i), i));
                if (i < items.size() - 1) cartList.add(Box.createVerticalStrut(10));
            }
        }
        cartList.revalidate();
        cartList.repaint();

        sumSub.setText(MONEY.format(cart.subtotal()));
        sumTax.setText(MONEY.format(cart.tax()));
        sumTotal.setText(MONEY.format(cart.total()));
        cartCount.setText("[ " + cart.size() + " / " + Cart.MAX_ITEMS + " ]");
        boolean has = !cart.isEmpty();
        checkoutBtn.setEnabled(has);
        emptyBtn.setEnabled(has);
        if (cartPill != null) cartPill.update(cart.size(), cart.total());
    }

    private void renderEmptyDetail() {
        view = View.EMPTY;
        setDetail(emptyState("No product selected",
                "Search the catalogue or browse all items."), false);
    }

    private void renderResults() {
        JPanel p = column();
        p.add(headerRow("RESULTS", lastResults.size() + " ITEM" + (lastResults.size() == 1 ? "" : "S"),
                "Search results"));
        p.add(Box.createVerticalStrut(16));
        if (lastResults.isEmpty()) {
            p.add(emptyState("Nothing matched", "Try fewer words, or 'Browse all'."));
        } else {
            for (int i = 0; i < lastResults.size(); i++) {
                final Product prod = lastResults.get(i);
                p.add(new ResultRow(prod, () -> selectProduct(prod)));
                if (i < lastResults.size() - 1) p.add(Box.createVerticalStrut(8));
            }
        }
        setDetail(p, true);
    }

    private void renderDetail(Product prod) {
        JPanel p = column();

        RoundButton back = new RoundButton("← Results", RoundButton.GHOST);
        back.setCompact(true);
        back.addActionListener(e -> { view = View.RESULTS; renderResults(); });
        JPanel backWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backWrap.setOpaque(false);
        backWrap.setAlignmentX(LEFT_ALIGNMENT);
        backWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        backWrap.add(back);
        p.add(backWrap);
        p.add(Box.createVerticalStrut(10));

        detailPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        detailPills.setOpaque(false);
        detailPills.setAlignmentX(LEFT_ALIGNMENT);
        detailPills.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.add(detailPills);
        p.add(Box.createVerticalStrut(14));

        JLabel name = new JLabel(clip(prod.description(), 44));
        name.setFont(tracked(serif(Font.BOLD, 22), -0.015f));
        name.setForeground(Theme.text());
        name.setAlignmentX(LEFT_ALIGNMENT);
        p.add(name);
        p.add(Box.createVerticalStrut(6));
        JLabel unit = new JLabel(MONEY.format(prod.price()) + " each");
        unit.setFont(font(Font.PLAIN, 13));
        unit.setForeground(Theme.muted());
        unit.setAlignmentX(LEFT_ALIGNMENT);
        p.add(unit);
        p.add(Box.createVerticalStrut(16));
        p.add(new Divider());
        p.add(Box.createVerticalStrut(14));

        p.add(fieldLabel("QUANTITY"));
        p.add(Box.createVerticalStrut(8));
        qtyField = new RoundField("1");
        qtyField.setText("1");
        qtyField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshDetailPreview(); }
            public void removeUpdate(DocumentEvent e) { refreshDetailPreview(); }
            public void changedUpdate(DocumentEvent e) { refreshDetailPreview(); }
        });
        qtyField.addActionListener(e -> addToCart());
        p.add(qtyField);
        p.add(Box.createVerticalStrut(16));

        JPanel lineRow = new JPanel(new BorderLayout());
        lineRow.setOpaque(false);
        lineRow.setAlignmentX(LEFT_ALIGNMENT);
        lineRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        JLabel ll = new JLabel("LINE SUBTOTAL");
        ll.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.1f));
        ll.setForeground(Theme.muted());
        detailLineVal = new JLabel();
        detailLineVal.setFont(tracked(serif(Font.BOLD, 28), -0.02f));
        detailLineVal.setForeground(Theme.text());
        lineRow.add(ll, BorderLayout.WEST);
        lineRow.add(detailLineVal, BorderLayout.EAST);
        p.add(lineRow);

        detailSaved = new JLabel(" ");
        detailSaved.setFont(font(Font.BOLD, 12));
        detailSaved.setForeground(Theme.success());
        detailSaved.setAlignmentX(LEFT_ALIGNMENT);
        p.add(Box.createVerticalStrut(4));
        p.add(detailSaved);
        p.add(Box.createVerticalStrut(18));

        RoundButton add = new RoundButton("Add to cart →", RoundButton.LIME);
        add.setFullWidth(true);
        add.addActionListener(e -> addToCart());
        if (!prod.isAvailable()) add.setEnabled(false);
        p.add(add);

        setDetail(p, true);
        refreshDetailPreview();
    }

    private void refreshDetailPreview() {
        if (detailProduct == null || detailPills == null) return;
        int qty;
        try { qty = Integer.parseInt(qtyField.getText().trim()); } catch (Exception e) { qty = 0; }
        double disc = qty > 0 ? Pricing.discountFor(qty) : 0;
        double line = detailProduct.price() * Math.max(qty, 0) * (1 - disc);
        double saved = detailProduct.price() * Math.max(qty, 0) - line;

        detailPills.removeAll();
        detailPills.add(new Pill("#" + detailProduct.id(), Pill.SOLID_INK));
        detailPills.add(new Pill((detailProduct.isAvailable() ? "IN STOCK · " : "OUT · ")
                + detailProduct.quantityAvailable(), Pill.OUTLINE));
        if (disc > 0) detailPills.add(new Pill("SAVE " + (int) Math.round(disc * 100) + "%", Pill.SOLID_LIME));
        detailPills.revalidate();
        detailPills.repaint();

        detailLineVal.setText(qty > 0 ? MONEY.format(line) : "—");
        detailSaved.setText(saved > 0 ? "You save " + MONEY.format(saved) + " with bulk pricing" : " ");
    }

    private void renderResultsBackedReceiptControls(JPanel p, final Order order, boolean fromHistory) {
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        RoundButton exportBtn = new RoundButton("Export PNG", RoundButton.GHOST);
        exportBtn.addActionListener(e -> exportReceiptPng(order));
        RoundButton printBtn = new RoundButton("Print", RoundButton.GHOST);
        printBtn.addActionListener(e -> printReceipt(order));
        actions.add(exportBtn);
        actions.add(printBtn);
        p.add(actions);
        p.add(Box.createVerticalStrut(10));

        RoundButton primary;
        if (fromHistory) {
            primary = new RoundButton("← Back to history", RoundButton.INK);
            primary.addActionListener(e -> showHistory());
        } else {
            primary = new RoundButton("Start new order →", RoundButton.LIME);
            primary.addActionListener(e -> renderEmptyDetail());
        }
        primary.setFullWidth(true);
        p.add(primary);
    }

    private void renderReceipt(Order order, boolean fromHistory) {
        JPanel p = column();
        p.add(eyebrow(fromHistory ? "RECEIPT / ARCHIVED" : "03 / RECEIPT"));
        p.add(Box.createVerticalStrut(10));

        JPanel headRow = new JPanel();
        headRow.setOpaque(false);
        headRow.setLayout(new BoxLayout(headRow, BoxLayout.X_AXIS));
        headRow.setAlignmentX(LEFT_ALIGNMENT);
        headRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        headRow.add(wrapCenter(new CheckGlyph()));
        headRow.add(Box.createHorizontalStrut(12));
        JLabel h = new JLabel(fromHistory ? "Past order" : "Order confirmed");
        h.setFont(tracked(serif(Font.BOLD, 24), -0.025f));
        h.setForeground(Theme.text());
        headRow.add(wrapCenter(h));
        headRow.add(Box.createHorizontalGlue());
        p.add(headRow);
        p.add(Box.createVerticalStrut(8));

        JLabel meta = new JLabel("TXN " + order.transactionId() + "  ·  " + order.timestamp());
        meta.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.06f));
        meta.setForeground(Theme.muted());
        meta.setAlignmentX(LEFT_ALIGNMENT);
        p.add(meta);
        p.add(Box.createVerticalStrut(18));

        p.add(buildReceiptSlip(order));

        if (order.saved() > 0) {
            p.add(Box.createVerticalStrut(12));
            JLabel sv = new JLabel("You saved " + MONEY.format(order.saved()) + " with bulk pricing");
            sv.setFont(font(Font.BOLD, 12));
            sv.setForeground(Theme.success());
            sv.setAlignmentX(LEFT_ALIGNMENT);
            p.add(sv);
        }
        p.add(Box.createVerticalStrut(10));
        JLabel note = new JLabel("SAVED TO ORDER HISTORY · APPENDED TO TRANSACTIONS.CSV");
        note.setFont(tracked(mono(Font.PLAIN, 9), 0.1f));
        note.setForeground(Theme.faint());
        note.setAlignmentX(LEFT_ALIGNMENT);
        p.add(note);
        p.add(Box.createVerticalStrut(18));

        renderResultsBackedReceiptControls(p, order, fromHistory);
        setDetail(p, true);
    }

    private JComponent buildReceiptSlip(Order order) {
        ReceiptSlip slip = new ReceiptSlip();
        JLabel store = new JLabel("NILE DOT COM · PREMIUM E-STORE");
        store.setFont(tracked(mono(Font.PLAIN, 9.5f), 0.14f));
        store.setForeground(Theme.muted());
        store.setAlignmentX(LEFT_ALIGNMENT);
        slip.add(store);
        slip.add(Box.createVerticalStrut(12));
        slip.add(new Divider());
        slip.add(Box.createVerticalStrut(12));
        List<CartItem> lines = order.lines();
        for (int i = 0; i < lines.size(); i++) {
            slip.add(receiptItemRow(lines.get(i)));
            if (i < lines.size() - 1) slip.add(Box.createVerticalStrut(10));
        }
        slip.add(Box.createVerticalStrut(14));
        slip.add(new Divider());
        slip.add(Box.createVerticalStrut(12));
        slip.add(receiptTotal("Subtotal", MONEY.format(order.subtotal()), false));
        slip.add(Box.createVerticalStrut(7));
        slip.add(receiptTotal("Sales tax (6%)", MONEY.format(order.tax()), false));
        slip.add(Box.createVerticalStrut(12));
        slip.add(new HardRule());
        slip.add(Box.createVerticalStrut(12));
        slip.add(receiptTotal("Total paid", MONEY.format(order.total()), true));
        return slip;
    }

    private JComponent receiptItemRow(CartItem it) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setAlignmentX(LEFT_ALIGNMENT);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(it.quantity() + " × " + clip(it.description(), 28));
        name.setFont(font(Font.BOLD, 13.5f));
        name.setForeground(Theme.text());
        name.setAlignmentX(LEFT_ALIGNMENT);
        String m = MONEY.format(it.unitPrice()) + " each"
                + (it.discountRate() > 0 ? "   ·   −" + (int) Math.round(it.discountRate() * 100) + "%" : "");
        JLabel mlab = new JLabel(m);
        mlab.setFont(tracked(mono(Font.PLAIN, 10), 0.04f));
        mlab.setForeground(Theme.muted());
        mlab.setAlignmentX(LEFT_ALIGNMENT);
        mid.add(name);
        mid.add(Box.createVerticalStrut(3));
        mid.add(mlab);
        r.add(mid, BorderLayout.CENTER);
        JLabel tot = new JLabel(MONEY.format(it.lineTotal()));
        tot.setFont(tracked(mono(Font.BOLD, 13.5f), 0.02f));
        tot.setForeground(Theme.text());
        r.add(wrapCenter(tot), BorderLayout.EAST);
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
            l.setFont(tracked(serif(Font.BOLD, 19), -0.01f)); l.setForeground(Theme.text());
            v.setFont(tracked(serif(Font.BOLD, 24), -0.02f)); v.setForeground(Theme.text());
        } else {
            l.setFont(font(Font.PLAIN, 12.5f)); l.setForeground(Theme.muted());
            v.setFont(tracked(mono(Font.BOLD, 12.5f), 0.02f)); v.setForeground(Theme.text());
        }
        r.add(l, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private void renderHistory() {
        JPanel p = column();
        List<Order> all = orders.findAll();
        p.add(headerRow("HISTORY", all.size() + " ORDER" + (all.size() == 1 ? "" : "S"), "Order history"));
        p.add(Box.createVerticalStrut(8));
        RoundButton back = new RoundButton("← Back to store", RoundButton.GHOST);
        back.setCompact(true);
        back.addActionListener(e -> renderEmptyDetail());
        JPanel bw = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bw.setOpaque(false);
        bw.setAlignmentX(LEFT_ALIGNMENT);
        bw.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        bw.add(back);
        p.add(bw);
        p.add(Box.createVerticalStrut(12));
        if (all.isEmpty()) {
            p.add(emptyState("No orders yet", "Completed checkouts will appear here."));
        } else {
            for (int i = 0; i < all.size(); i++) {
                final Order o = all.get(i);
                p.add(new HistoryRow(o, () -> { receiptOrder = o; view = View.RECEIPT; renderReceipt(o, true); }));
                if (i < all.size() - 1) p.add(Box.createVerticalStrut(8));
            }
        }
        setDetail(p, true);
    }

    /* ===================== RECEIPT EXPORT / PRINT ===================== */
    private BufferedImage receiptImage(Order order) {
        JPanel canvas = new JPanel();
        canvas.setLayout(new BoxLayout(canvas, BoxLayout.Y_AXIS));
        canvas.setBackground(Theme.canvas());
        canvas.setBorder(new EmptyBorder(22, 22, 22, 22));
        JLabel title = new JLabel("Nile dot com — receipt");
        title.setFont(tracked(serif(Font.BOLD, 20), -0.02f));
        title.setForeground(Theme.text());
        title.setAlignmentX(LEFT_ALIGNMENT);
        canvas.add(title);
        canvas.add(Box.createVerticalStrut(4));
        JLabel meta = new JLabel("TXN " + order.transactionId() + "  ·  " + order.timestamp());
        meta.setFont(tracked(mono(Font.PLAIN, 10), 0.06f));
        meta.setForeground(Theme.muted());
        meta.setAlignmentX(LEFT_ALIGNMENT);
        canvas.add(meta);
        canvas.add(Box.createVerticalStrut(14));
        canvas.add(buildReceiptSlip(order));

        JWindow w = new JWindow();
        w.setContentPane(canvas);
        w.pack();
        int width = Math.max(440, canvas.getPreferredSize().width);
        canvas.setSize(new Dimension(width, canvas.getPreferredSize().height));
        w.setSize(canvas.getSize());
        w.validate();
        Dimension d = canvas.getSize();
        BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Theme.canvas());
        g.fillRect(0, 0, d.width, d.height);
        canvas.paint(g);
        g.dispose();
        w.dispose();
        return img;
    }

    private void exportReceiptPng(Order order) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("receipt-" + order.transactionId() + ".png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            ImageIO.write(receiptImage(order), "png", fc.getSelectedFile());
            toast("Receipt saved.", Toast.SUCCESS);
        } catch (IOException ex) {
            toast("Could not save receipt.", Toast.ERROR);
        }
    }

    private void printReceipt(final Order order) {
        final BufferedImage img = receiptImage(order);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Nile dot com receipt " + order.transactionId());
        job.setPrintable(new Printable() {
            public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
                if (page > 0) return NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                double scale = Math.min(pf.getImageableWidth() / img.getWidth(),
                        pf.getImageableHeight() / img.getHeight());
                if (scale > 1) scale = 1;
                g2.scale(scale, scale);
                g2.drawImage(img, 0, 0, null);
                return PAGE_EXISTS;
            }
        });
        if (job.printDialog()) {
            try { job.print(); toast("Sent to printer.", Toast.SUCCESS); }
            catch (PrinterException ex) { toast("Print failed.", Toast.ERROR); }
        }
    }

    /* ====================== SHARED VIEW HELPERS ====================== */
    private void setDetail(JComponent content, boolean scroll) {
        detailHost.removeAll();
        if (scroll) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(content, BorderLayout.NORTH);
            detailHost.add(scrollOf(holder), BorderLayout.CENTER);
        } else {
            detailHost.add(content, BorderLayout.CENTER);
        }
        detailHost.revalidate();
        detailHost.repaint();
    }

    private JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JComponent headerRow(String eyebrowText, String right, String title) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setAlignmentX(LEFT_ALIGNMENT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        top.add(eyebrow(eyebrowText), BorderLayout.WEST);
        JLabel r = new JLabel(right);
        r.setFont(tracked(mono(Font.PLAIN, 10), 0.1f));
        r.setForeground(Theme.faint());
        top.add(r, BorderLayout.EAST);
        wrap.add(top);
        wrap.add(Box.createVerticalStrut(8));
        wrap.add(heading(title));
        return wrap;
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
                g2.setColor(Theme.border());
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawRect(0, 0, 53, 53);
                g2.setColor(Theme.accent());
                g2.fillRect(0, 0, 14, 14);
                g2.setColor(Theme.border());
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
        t.setForeground(Theme.text());
        t.setAlignmentX(CENTER_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(6));
        JLabel s = new JLabel(sub);
        s.setFont(font(Font.PLAIN, 12.5f));
        s.setForeground(Theme.muted());
        s.setAlignmentX(CENTER_ALIGNMENT);
        p.add(s);
        return p;
    }

    private JLabel eyebrow(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.12f));
        l.setForeground(Theme.muted());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private JLabel heading(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(serif(Font.BOLD, 24), -0.025f));
        l.setForeground(Theme.text());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private JLabel fieldLabel(String s) {
        JLabel l = new JLabel(s);
        l.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.1f));
        l.setForeground(Theme.textSoft());
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void toast(String msg, int type) { Toast.show(this, msg, type); }

    private static JPanel wrapCenter(JComponent comp) {
        JPanel w = new JPanel(new GridBagLayout());
        w.setOpaque(false);
        w.add(comp);
        return w;
    }

    private JScrollPane scrollOf(JComponent inner) {
        JScrollPane scroll = new JScrollPane(inner,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    /* ====================== CUSTOM COMPONENTS ====================== */

    static class Block extends JPanel {
        Block(int pad) {
            setOpaque(false);
            setBorder(new EmptyBorder(pad, pad, pad, pad));
            setAlignmentX(LEFT_ALIGNMENT);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(Theme.surface());
            g2.fillRect(0, 0, w, h);
            g2.setColor(Theme.border());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(0, 0, w - 1, h - 1);
            super.paintComponent(g);
        }
    }

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
            int tooth = 12, baseY = h - 9;
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
            g2.setColor(Theme.fieldBg());
            g2.fill(path);
            g2.setColor(Theme.border());
            g2.setStroke(new BasicStroke(1.4f));
            g2.draw(path);
            super.paintComponent(g);
        }
    }

    /** Clickable catalogue search-result row. */
    class ResultRow extends JPanel {
        private boolean hover;
        ResultRow(final Product p, final Runnable onClick) {
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
            JPanel mid = new JPanel();
            mid.setOpaque(false);
            mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(clip(p.description(), 34));
            name.setFont(font(Font.BOLD, 13.5f));
            name.setForeground(Theme.text());
            name.setAlignmentX(LEFT_ALIGNMENT);
            JLabel meta = new JLabel("#" + p.id() + "   ·   "
                    + (p.isAvailable() ? p.quantityAvailable() + " in stock" : "out of stock"));
            meta.setFont(tracked(mono(Font.PLAIN, 10), 0.04f));
            meta.setForeground(p.isAvailable() ? Theme.muted() : Theme.danger());
            meta.setAlignmentX(LEFT_ALIGNMENT);
            mid.add(name);
            mid.add(Box.createVerticalStrut(4));
            mid.add(meta);
            add(mid, BorderLayout.CENTER);
            JLabel price = new JLabel(MONEY.format(p.price()));
            price.setFont(tracked(mono(Font.BOLD, 14), 0.02f));
            price.setForeground(Theme.text());
            add(wrapCenter(price), BorderLayout.EAST);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(hover ? Theme.accent() : Theme.fieldBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(hover ? Theme.border() : Theme.hairline());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    /** Clickable past-order row. */
    class HistoryRow extends JPanel {
        private boolean hover;
        HistoryRow(final Order o, final Runnable onClick) {
            setOpaque(false);
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
            JPanel mid = new JPanel();
            mid.setOpaque(false);
            mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
            int n = o.lines().size();
            JLabel name = new JLabel(o.timestamp());
            name.setFont(font(Font.BOLD, 13));
            name.setForeground(Theme.text());
            name.setAlignmentX(LEFT_ALIGNMENT);
            JLabel meta = new JLabel("TXN " + o.transactionId() + "   ·   " + n + " item" + (n == 1 ? "" : "s"));
            meta.setFont(tracked(mono(Font.PLAIN, 10), 0.04f));
            meta.setForeground(Theme.muted());
            meta.setAlignmentX(LEFT_ALIGNMENT);
            mid.add(name);
            mid.add(Box.createVerticalStrut(4));
            mid.add(meta);
            add(mid, BorderLayout.CENTER);
            JLabel total = new JLabel(MONEY.format(o.total()));
            total.setFont(tracked(mono(Font.BOLD, 14), 0.02f));
            total.setForeground(Theme.text());
            add(wrapCenter(total), BorderLayout.EAST);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(hover ? Theme.accent() : Theme.fieldBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(hover ? Theme.border() : Theme.hairline());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

    /** Small ghost text link for the header (light text on the ink bar). */
    static class HeaderLink extends JComponent {
        private final String text;
        private boolean hover;
        HeaderLink(String text, final Runnable onClick) {
            this.text = text.toUpperCase();
            setFont(tracked(mono(Font.PLAIN, 10.5f), 0.1f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { onClick.run(); }
            });
        }
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + 26, 34);
        }
        public Dimension getMaximumSize() { return getPreferredSize(); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight();
            if (hover) {
                g2.setColor(Theme.accent());
                g2.fillRoundRect(0, 0, w, h, h, h);
            } else {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            }
            g2.setColor(hover ? Theme.onAccent() : Theme.onInk());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (w - fm.stringWidth(text)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    static class CheckGlyph extends JComponent {
        public Dimension getPreferredSize() { return new Dimension(30, 30); }
        public Dimension getMaximumSize() { return new Dimension(30, 30); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(Theme.accent());
            g2.fillRect(0, 0, 30, 30);
            g2.setColor(Theme.onAccent());
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(8, 16, 13, 21);
            g2.drawLine(13, 21, 23, 9);
        }
    }

    static class RoundButton extends JButton {
        static final int LIME = 0, INK = 1, GHOST = 2;
        private final int variant;
        private boolean hover, press, fullWidth, compact;
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
        void setCompact(boolean c) {
            compact = c;
            setBorder(new EmptyBorder(8, 14, 8, 14));
            setFont(tracked(font(Font.BOLD, 11f), 0.04f));
        }
        public Dimension getMaximumSize() {
            return fullWidth ? new Dimension(Integer.MAX_VALUE, 50)
                    : (compact ? getPreferredSize() : super.getMaximumSize());
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            int w = getWidth(), h = getHeight(), arc = h;
            boolean on = isEnabled();
            Color fill, txt, line = null;
            if (!on) {
                fill = Theme.hairline(); txt = Theme.faint();
            } else if (variant == GHOST) {
                if (hover) { fill = Theme.inkBlock(); txt = Theme.onInk(); }
                else { fill = null; txt = Theme.text(); line = Theme.border(); }
            } else if (variant == INK) {
                if (hover) { fill = Theme.accent(); txt = Theme.onAccent(); }
                else { fill = Theme.inkBlock(); txt = Theme.onInk(); }
            } else {
                if (hover) { fill = Theme.inkBlock(); txt = Theme.accent(); }
                else { fill = Theme.accent(); txt = Theme.onAccent(); }
            }
            int dy = (on && press) ? 1 : 0;
            if (fill != null) { g2.setColor(fill); g2.fillRoundRect(0, dy, w, h - 1, arc, arc); }
            if (line != null) { g2.setColor(line); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, dy, w - 1, h - 2, arc, arc); }
            g2.setColor(txt);
            String t = getText().toUpperCase();
            g2.setFont(tracked(font(Font.BOLD, compact ? 11f : 12.5f), 0.06f));
            FontMetrics fm = g2.getFontMetrics();
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

    static class RoundField extends JTextField {
        private final String placeholder;
        RoundField(String placeholder) {
            this.placeholder = placeholder;
            setFont(font(Font.PLAIN, 14));
            setForeground(Theme.text());
            setOpaque(false);
            setBorder(new EmptyBorder(12, 14, 12, 14));
            setCaretColor(Theme.text());
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
            g2.setColor(Theme.fieldBg());
            g2.fillRect(0, 0, w, h);
            boolean focus = isFocusOwner();
            if (focus) { g2.setColor(Theme.accent()); g2.fillRect(0, 0, 4, h); }
            g2.setColor(focus ? Theme.border() : Theme.muted());
            g2.setStroke(new BasicStroke(focus ? 1.8f : 1.2f));
            g2.drawRect(0, 0, w - 1, h - 1);
            super.paintComponent(g);
            if (getText().isEmpty() && !focus) {
                g2.setColor(Theme.faint());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                Insets in = getInsets();
                g2.drawString(placeholder, in.left, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
        }
    }

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
            if (style == SOLID_INK) { bg = Theme.inkBlock(); fg = Theme.onInk(); }
            else if (style == SOLID_LIME) { bg = Theme.accent(); fg = Theme.onAccent(); }
            else { bg = null; fg = Theme.text(); br = Theme.muted(); }
            if (bg != null) { g2.setColor(bg); g2.fillRoundRect(0, 0, w, h, h, h); }
            if (br != null) { g2.setColor(br); g2.drawRoundRect(0, 0, w - 1, h - 1, h, h); }
            g2.setColor(fg);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 12, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    class CartRow extends JPanel {
        CartRow(int index, final CartItem it, final int modelIndex) {
            setOpaque(false);
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(13, 14, 13, 12));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
            final int idx = index;
            JComponent badge = new JComponent() {
                public Dimension getPreferredSize() { return new Dimension(32, 32); }
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = aa(g);
                    g2.setColor(Theme.inkBlock());
                    g2.fillRect(0, 0, 32, 32);
                    g2.setColor(Theme.accent());
                    g2.setFont(tracked(mono(Font.BOLD, 12), 0.04f));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = (idx < 10 ? "0" : "") + idx;
                    g2.drawString(s, (32 - fm.stringWidth(s)) / 2, (32 - fm.getHeight()) / 2 + fm.getAscent());
                }
            };
            add(wrapCenter(badge), BorderLayout.WEST);

            JPanel mid = new JPanel();
            mid.setOpaque(false);
            mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(clip(it.description(), 32));
            name.setFont(font(Font.BOLD, 14));
            name.setForeground(Theme.text());
            name.setAlignmentX(LEFT_ALIGNMENT);
            String sub = it.quantity() + " × " + MONEY.format(it.unitPrice())
                    + (it.discountRate() > 0 ? "   ·   −" + (int) Math.round(it.discountRate() * 100) + "%" : "");
            JLabel meta = new JLabel(sub);
            meta.setFont(tracked(mono(Font.PLAIN, 10.5f), 0.04f));
            meta.setForeground(Theme.muted());
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
            total.setForeground(Theme.text());
            right.add(total);
            right.add(Box.createHorizontalStrut(12));
            right.add(new RemoveButton(() -> removeAt(modelIndex)));
            add(wrapCenter(right), BorderLayout.EAST);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(Theme.fieldBg());
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.hairline());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

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
            g2.setColor(hover ? Theme.danger() : Theme.fieldBg());
            g2.fillRect(0, 0, 27, 27);
            g2.setColor(hover ? Theme.danger() : Theme.muted());
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRect(0, 0, 27, 27);
            g2.setColor(hover ? Color.WHITE : Theme.muted());
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(9, 9, 18, 18);
            g2.drawLine(18, 9, 9, 18);
        }
    }

    static class LogoMark extends JComponent {
        public Dimension getPreferredSize() { return new Dimension(48, 48); }
        public Dimension getMaximumSize() { return new Dimension(48, 48); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = aa(g);
            g2.setColor(Theme.accent());
            g2.fillRect(0, 0, 48, 48);
            g2.setColor(Theme.onAccent());
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            g2.drawLine(15, 34, 15, 14);
            g2.drawLine(15, 14, 33, 34);
            g2.drawLine(33, 34, 33, 14);
        }
    }

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
            g2.setColor(Theme.accent());
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(Theme.onAccent());
            g2.fillOval(7, (h - 30) / 2, 30, 30);
            g2.setColor(Theme.accent());
            g2.setFont(tracked(mono(Font.BOLD, 13), 0.02f));
            FontMetrics fb = g2.getFontMetrics();
            String c = String.valueOf(count);
            g2.drawString(c, 7 + (30 - fb.stringWidth(c)) / 2, (h - fb.getHeight()) / 2 + fb.getAscent());
            g2.setColor(new Color(0x2A, 0x33, 0x06));
            g2.setFont(tracked(mono(Font.PLAIN, 9), 0.12f));
            g2.drawString(count == 1 ? "ITEM" : "ITEMS", 46, 18);
            g2.setColor(Theme.onAccent());
            g2.setFont(font(Font.BOLD, 15));
            g2.drawString(MONEY.format(total), 46, 35);
        }
    }

    static class Divider extends JComponent {
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 1); }
        public Dimension getPreferredSize() { return new Dimension(10, 1); }
        protected void paintComponent(Graphics g) { g.setColor(Theme.hairline()); g.fillRect(0, 0, getWidth(), 1); }
    }
    static class HardRule extends JComponent {
        public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, 2); }
        public Dimension getPreferredSize() { return new Dimension(10, 2); }
        protected void paintComponent(Graphics g) { g.setColor(Theme.border()); g.fillRect(0, 0, getWidth(), 2); }
    }

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
            g2.setColor(Theme.inkBlock());
            g2.fillRect(0, 0, w - 4, h - 4);
            Color mark = type == ERROR ? Theme.danger()
                    : type == WARN ? new Color(0xFF, 0xC8, 0x3D) : Theme.accent();
            g2.setColor(mark);
            g2.fillRect(0, 0, 6, h - 4);
            g2.fillRect(20, h / 2 - 7, 9, 9);
            g2.setColor(Theme.onInk());
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
            protected void configureScrollBarColors() { thumbColor = Theme.border(); trackColor = Theme.canvas(); }
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
                g2.setColor(Theme.border());
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
        java.util.Map<java.awt.font.TextAttribute, Object> a = new java.util.HashMap<>();
        a.put(java.awt.font.TextAttribute.TRACKING, Float.valueOf(tracking));
        return base.deriveFont(a);
    }
    static String pickFont(String[] prefs) {
        java.util.Set<String> have = new java.util.HashSet<>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            have.add(f);
        for (String p : prefs) if (have.contains(p)) return p;
        return prefs[prefs.length - 1];
    }
    static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
