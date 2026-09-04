package lk.icbt.cis6003.dental.server.db.trigger;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Database <b>trigger</b> that writes an audit row for every appointment that
 * is inserted or updated.
 *
 * <p><b>Why a trigger and not just application code?</b> The application also
 * audits, through the observer on the notification pipeline. That covers the
 * happy path. A trigger covers everything else - a correction applied by a DBA
 * over a SQL console, a bulk update from a future integration, a script run at
 * 2 a.m. Anything that reaches the table is recorded, because the rule lives
 * with the data rather than with one particular client of it. Audit rows
 * written here are stamped {@code source = 'DB_TRIGGER'} so the two sources
 * stay distinguishable in the audit screen.</p>
 *
 * <p>Registered by {@code V4__triggers.sql}:</p>
 * <pre>
 *   CREATE TRIGGER trg_appointment_audit
 *       AFTER INSERT, UPDATE ON appointment
 *       FOR EACH ROW CALL '...AppointmentAuditTrigger';
 * </pre>
 *
 * <p><b>Failure policy.</b> An {@code AFTER} trigger that throws would roll the
 * booking back. Losing an audit line is bad; refusing to book a patient because
 * the audit line could not be written is worse. Every failure is therefore
 * caught and reported to {@code System.err} rather than propagated.</p>
 */
public class AppointmentAuditTrigger implements Trigger {

    private static final String INSERT_AUDIT_SQL = """
            INSERT INTO audit_log (username, action, entity_type, entity_key, details, source, occurred_at)
            VALUES (?, ?, 'APPOINTMENT', ?, ?, 'DB_TRIGGER', ?)
            """;

    /** Column name (upper case) to its zero-based index in the row arrays. */
    private final Map<String, Integer> columnIndex = new HashMap<>();

    private String tableName;

    /**
     * H2 hands the trigger the row as an {@code Object[]} ordered by physical
     * column position, so the positions are resolved once here from JDBC
     * metadata rather than being hard-coded. A later migration that adds a
     * column therefore cannot silently shift the values this trigger reads.
     */
    @Override
    public void init(Connection conn, String schemaName, String triggerName,
                     String tableName, boolean before, int type) throws SQLException {
        this.tableName = tableName;
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, schemaName, tableName, null)) {
            while (rs.next()) {
                String column = rs.getString("COLUMN_NAME");
                int ordinal = rs.getInt("ORDINAL_POSITION");
                columnIndex.put(column.toUpperCase(), ordinal - 1);
            }
        }
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) {
        try {
            if (newRow == null) {
                return;
            }

            String appointmentNumber = stringAt(newRow, "APPOINTMENT_NUMBER");
            String actor = firstNonBlank(stringAt(newRow, "UPDATED_BY"),
                                         stringAt(newRow, "CREATED_BY"),
                                         "system");

            String action;
            String details;
            if (oldRow == null) {
                action = "APPOINTMENT_CREATED";
                details = "Booked for " + stringAt(newRow, "APPOINTMENT_DATE")
                        + " " + stringAt(newRow, "APPOINTMENT_TIME")
                        + ", status " + stringAt(newRow, "STATUS");
            } else {
                action = "APPOINTMENT_UPDATED";
                details = describeChanges(oldRow, newRow);
                if (details.isEmpty()) {
                    // Nothing the clinic cares about changed - do not create noise.
                    return;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(INSERT_AUDIT_SQL)) {
                ps.setString(1, actor);
                ps.setString(2, action);
                ps.setString(3, appointmentNumber);
                ps.setString(4, truncate(details, 1000));
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        } catch (Exception ex) {
            // Never let auditing break a booking - see the failure policy above.
            System.err.println("[" + tableName + "] audit trigger failed: " + ex.getMessage());
        }
    }

    /** Reports only the fields a clinic manager would want explained. */
    private String describeChanges(Object[] oldRow, Object[] newRow) {
        StringBuilder sb = new StringBuilder();
        appendIfChanged(sb, oldRow, newRow, "STATUS", "status");
        appendIfChanged(sb, oldRow, newRow, "APPOINTMENT_DATE", "date");
        appendIfChanged(sb, oldRow, newRow, "APPOINTMENT_TIME", "time");
        appendIfChanged(sb, oldRow, newRow, "DENTIST_ID", "dentist");
        appendIfChanged(sb, oldRow, newRow, "TREATMENT_ID", "treatment");
        return sb.toString();
    }

    private void appendIfChanged(StringBuilder sb, Object[] oldRow, Object[] newRow,
                                 String column, String label) {
        String before = stringAt(oldRow, column);
        String after = stringAt(newRow, column);
        if (before == null ? after != null : !before.equals(after)) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(label).append(": ").append(before).append(" -> ").append(after);
        }
    }

    private String stringAt(Object[] row, String column) {
        if (row == null) {
            return null;
        }
        Integer index = columnIndex.get(column);
        if (index == null || index >= row.length) {
            return null;
        }
        Object value = row[index];
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "system";
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
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
