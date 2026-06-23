package com.nilecom;

import com.nilecom.domain.Product;
import com.nilecom.persistence.CsvInventoryImporter;
import com.nilecom.persistence.CsvTransactionLog;
import com.nilecom.persistence.InventoryRepository;
import com.nilecom.persistence.OrderRepository;
import com.nilecom.persistence.SqliteDatabase;
import com.nilecom.persistence.SqliteInventoryRepository;
import com.nilecom.persistence.SqliteOrderRepository;
import com.nilecom.ui.NileDotCom;

import javax.swing.SwingUtilities;
import java.io.File;
import java.sql.Connection;
import java.util.List;

/** Composition root: boots the embedded SQLite store, seeds it, launches the UI. */
public final class App {

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SqliteDatabase db = new SqliteDatabase(databasePath());
        final Connection connection = db.open();
        db.initSchema(connection);
        List<Product> seed = CsvInventoryImporter.fromClasspath();
        db.seedIfEmpty(connection, seed);

        final InventoryRepository inventory = new SqliteInventoryRepository(connection);
        final OrderRepository orders = new SqliteOrderRepository(connection);
        final CsvTransactionLog txLog = new CsvTransactionLog("transactions.csv");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { connection.close(); } catch (Exception ignore) {}
        }));

        SwingUtilities.invokeLater(() ->
                new NileDotCom(inventory, orders, txLog).setVisible(true));
    }

    private static String databasePath() {
        File dir = new File(System.getProperty("user.home"), ".nilecom");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "store.db").getAbsolutePath();
    }

    private App() {}
}
