package lk.icbt.cis6003.dental.server.db.trigger;

import org.h2.api.Trigger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Database <b>trigger</b> that records every change to the money on a bill.
 *
 * <p>Money is the part of this system a clinic will be audited on, so a change
 * to {@code amount_paid} or {@code payment_status} leaves a row behind no
 * matter which route made the change. Registered by {@code V4__triggers.sql}
 * as an {@code AFTER UPDATE} trigger on {@code invoice}.</p>
 *
 * <p>It also acts as a last-line integrity check: if a row ever appears with
 * {@code amount_paid > total_amount} the trigger records a
 * {@code PAYMENT_INTEGRITY_WARNING} entry, so an over-payment introduced
 * outside the application cannot pass unnoticed. It records rather than throws,
 * for the same reason as {@link AppointmentAuditTrigger} - the
 * {@code chk_invoice_amount_paid} CHECK constraint in the schema is the part
 * that actually refuses bad data.</p>
 */
public class InvoicePaymentAuditTrigger implements Trigger {

    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO audit_log (username, action, entity_type, entity_key, details, source, occurred_at)
            VALUES (?, ?, 'INVOICE', ?, ?, 'DB_TRIGGER', ?)
            """;

    private final Map<String, Integer> columnIndex = new HashMap<>();

    @Override
    public void init(Connection conn, String schemaName, String triggerName,
                     String tableName, boolean before, int type) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                columnIndex.put(rs.getString("COLUMN_NAME").toUpperCase(),
                                rs.getInt("ORDINAL_POSITION") - 1);
            }
        }
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) {
        try {
            if (oldRow == null || newRow == null) {
                return;
            }

            String oldStatus = stringAt(oldRow, "PAYMENT_STATUS");
            String newStatus = stringAt(newRow, "PAYMENT_STATUS");
            BigDecimal oldPaid = decimalAt(oldRow, "AMOUNT_PAID");
            BigDecimal newPaid = decimalAt(newRow, "AMOUNT_PAID");

            boolean statusChanged = oldStatus == null ? newStatus != null : !oldStatus.equals(newStatus);
            boolean paidChanged = oldPaid.compareTo(newPaid) != 0;
            if (!statusChanged && !paidChanged) {
                return;
            }

            String invoiceNumber = stringAt(newRow, "INVOICE_NUMBER");
            BigDecimal total = decimalAt(newRow, "TOTAL_AMOUNT");

            String action = newPaid.compareTo(total) > 0
                    ? "PAYMENT_INTEGRITY_WARNING"
                    : "INVOICE_PAYMENT_CHANGED";

            String details = "paid: " + oldPaid + " -> " + newPaid
                    + "; status: " + oldStatus + " -> " + newStatus
                    + "; total: " + total;

            try (PreparedStatement ps = conn.prepareStatement(INSERT_AUDIT_SQL)) {
                ps.setString(1, "database");
                ps.setString(2, action);
                ps.setString(3, invoiceNumber);
                ps.setString(4, details);
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            System.err.println("[invoice] payment audit trigger failed: " + ex.getMessage());
        }
    }

    private String stringAt(Object[] row, String column) {
        Integer index = columnIndex.get(column);
        if (row == null || index == null || index >= row.length || row[index] == null) {
            return null;
        }
        return String.valueOf(row[index]);
    }

    private BigDecimal decimalAt(Object[] row, String column) {
        Integer index = columnIndex.get(column);
        if (row == null || index == null || index >= row.length || row[index] == null) {
            return BigDecimal.ZERO;
        }
        Object value = row[index];
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void close() {
        // no resources held
    }

    @Override
    public void remove() {
        // no resources held
    }
}
