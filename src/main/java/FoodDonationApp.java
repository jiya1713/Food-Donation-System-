import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

/**
 * Food Donation Record Management System
 * Student : Ms. Jiya Agnihotri | Reg No: PIET24CR035
 * Tech    : Java Swing, JDBC, MySQL
 * Features: Donor details, food type & quantity records, distribution tracking
 * SDG     : SDG 2 (Zero Hunger) & SDG 12 (Responsible Consumption)
 */
public class FoodDonationApp extends JFrame {

    // ====== DATABASE SETTINGS (apna MySQL password yahan likho) ======
    static final String DB_URL  = "jdbc:mysql://localhost:3306/food_donation_db"
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata";
    static final String DB_USER = "root";
    static final String DB_PASS = "root";   // <-- CHANGE THIS
    // ==================================================================

    private static final String[] DONOR_TYPES =
            {"Individual", "Restaurant", "Hotel", "Wedding / Event", "Organization", "Other"};
    private static final String[] FOOD_TYPES =
            {"Cooked Meals", "Grains / Rice / Wheat", "Vegetables", "Fruits",
                    "Packaged Food", "Bakery Items", "Dairy / Beverages", "Other"};
    private static final String[] UNITS = {"kg", "plates", "packets", "litres"};

    private static Connection getConn() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ----- Donors tab -----
    private DefaultTableModel donorModel;
    private JTable donorTable;
    private JTextField donorSearch, dName, dPhone, dEmail, dAddress;
    private JComboBox<String> dType;

    // ----- Donations tab -----
    private DefaultTableModel donationModel;
    private JTable donationTable;
    private JTextField donationSearch, qtyField, donationDateField, expiryField;
    private JComboBox<String> donorCombo, foodCombo, unitCombo;

    // ----- Distribution tab -----
    private DefaultTableModel availModel, distModel;
    private JTable availTable;
    private JTextField beneficiaryField, locationField, distQtyField, distDateField;

    // ----- Summary tab -----
    private JLabel lblDonors, lblDonations, lblDistributions, lblBeneficiaries;
    private DefaultTableModel summaryModel;

    public FoodDonationApp() {
        setTitle("Food Donation Record Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1020, 660);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Donors", buildDonorPanel());
        tabs.addTab("Food Donations", buildDonationPanel());
        tabs.addTab("Distribution", buildDistributionPanel());
        tabs.addTab("Summary", buildSummaryPanel());
        tabs.addChangeListener(e -> refreshTab(tabs.getSelectedIndex()));
        add(tabs);
        refreshTab(0);
    }

    private void refreshTab(int index) {
        switch (index) {
            case 0: loadDonors(); break;
            case 1: refreshDonorCombo(); loadDonations(); break;
            case 2: loadAvailable(); loadDistributions(); break;
            case 3: loadSummary(); break;
        }
    }

    private static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    // =====================================================
    //  DONORS
    // =====================================================
    private JPanel buildDonorPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search donor (name / phone / type / address):"));
        donorSearch = new JTextField(20);
        JButton bSearch = new JButton("Search");
        JButton bAll = new JButton("Show All");
        top.add(donorSearch); top.add(bSearch); top.add(bAll);
        p.add(top, BorderLayout.NORTH);

        donorModel = readOnlyModel("ID", "Name", "Type", "Phone", "Email", "Address");
        donorTable = new JTable(donorModel);
        donorTable.setRowHeight(24);
        donorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(donorTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dName = new JTextField(12); dType = new JComboBox<>(DONOR_TYPES);
        dPhone = new JTextField(9); dEmail = new JTextField(12); dAddress = new JTextField(16);
        f1.add(new JLabel("Name")); f1.add(dName);
        f1.add(new JLabel("Type")); f1.add(dType);
        f1.add(new JLabel("Phone")); f1.add(dPhone);
        f1.add(new JLabel("Email")); f1.add(dEmail);
        f1.add(new JLabel("Address")); f1.add(dAddress);
        JPanel f2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bAdd = new JButton("Add Donor");
        JButton bUpd = new JButton("Update Selected");
        JButton bDel = new JButton("Delete Selected");
        JButton bClr = new JButton("Clear");
        f2.add(bAdd); f2.add(bUpd); f2.add(bDel); f2.add(bClr);
        form.add(f1); form.add(f2);
        p.add(form, BorderLayout.SOUTH);

        bSearch.addActionListener(e -> loadDonors());
        donorSearch.addActionListener(e -> loadDonors());
        bAll.addActionListener(e -> { donorSearch.setText(""); loadDonors(); });
        bAdd.addActionListener(e -> addDonor());
        bUpd.addActionListener(e -> updateDonor());
        bDel.addActionListener(e -> deleteDonor());
        bClr.addActionListener(e -> clearDonorForm());
        donorTable.getSelectionModel().addListSelectionListener(e -> {
            int r = donorTable.getSelectedRow();
            if (r >= 0 && !e.getValueIsAdjusting()) {
                dName.setText(str(donorModel.getValueAt(r, 1)));
                dType.setSelectedItem(str(donorModel.getValueAt(r, 2)));
                dPhone.setText(str(donorModel.getValueAt(r, 3)));
                dEmail.setText(str(donorModel.getValueAt(r, 4)));
                dAddress.setText(str(donorModel.getValueAt(r, 5)));
            }
        });
        return p;
    }

    private void loadDonors() {
        String k = "%" + donorSearch.getText().trim() + "%";
        fill(donorModel,
                "SELECT donor_id, name, donor_type, phone, email, address FROM donors "
                        + "WHERE name LIKE ? OR phone LIKE ? OR donor_type LIKE ? OR address LIKE ? ORDER BY name",
                k, k, k, k);
    }

    private void addDonor() {
        if (!validDonor()) return;
        String sql = "INSERT INTO donors (name, donor_type, phone, email, address) VALUES (?,?,?,?,?)";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dName.getText().trim());
            ps.setString(2, (String) dType.getSelectedItem());
            ps.setString(3, dPhone.getText().trim());
            ps.setString(4, dEmail.getText().trim());
            ps.setString(5, dAddress.getText().trim());
            ps.executeUpdate();
            msg("Donor added successfully.");
            clearDonorForm();
            loadDonors();
        } catch (SQLException ex) { showError(ex); }
    }

    private void updateDonor() {
        int r = donorTable.getSelectedRow();
        if (r < 0) { msg("Select a donor from the table first."); return; }
        if (!validDonor()) return;
        String sql = "UPDATE donors SET name=?, donor_type=?, phone=?, email=?, address=? WHERE donor_id=?";
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, dName.getText().trim());
            ps.setString(2, (String) dType.getSelectedItem());
            ps.setString(3, dPhone.getText().trim());
            ps.setString(4, dEmail.getText().trim());
            ps.setString(5, dAddress.getText().trim());
            ps.setInt(6, Integer.parseInt(str(donorModel.getValueAt(r, 0))));
            ps.executeUpdate();
            msg("Donor updated.");
            loadDonors();
        } catch (SQLException ex) { showError(ex); }
    }

    private void deleteDonor() {
        int r = donorTable.getSelectedRow();
        if (r < 0) { msg("Select a donor from the table first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this donor?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM donors WHERE donor_id=?")) {
            ps.setInt(1, Integer.parseInt(str(donorModel.getValueAt(r, 0))));
            ps.executeUpdate();
            clearDonorForm();
            loadDonors();
        } catch (SQLIntegrityConstraintViolationException ex) {
            msg("This donor has donation records, so cannot be deleted.");
        } catch (SQLException ex) { showError(ex); }
    }

    private boolean validDonor() {
        if (dName.getText().trim().isEmpty()) { msg("Donor name is required."); return false; }
        if (!dPhone.getText().trim().matches("\\d{10}")) {
            msg("Phone number must be exactly 10 digits."); return false;
        }
        return true;
    }

    private void clearDonorForm() {
        dName.setText(""); dPhone.setText(""); dEmail.setText(""); dAddress.setText("");
        dType.setSelectedIndex(0);
        donorTable.clearSelection();
    }

    // =====================================================
    //  DONATIONS
    // =====================================================
    private JPanel buildDonationPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search (donor / food type):"));
        donationSearch = new JTextField(20);
        JButton bSearch = new JButton("Search");
        JButton bAll = new JButton("Show All");
        top.add(donationSearch); top.add(bSearch); top.add(bAll);
        p.add(top, BorderLayout.NORTH);

        donationModel = readOnlyModel("Donation ID", "Donor", "Food Type", "Quantity",
                "Unit", "Remaining", "Donation Date", "Expiry Date");
        donationTable = new JTable(donationModel);
        donationTable.setRowHeight(24);
        donationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(donationTable), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel f1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        donorCombo = new JComboBox<>();
        donorCombo.setPrototypeDisplayValue("0000 - Long Donor Name Here");
        foodCombo = new JComboBox<>(FOOD_TYPES);
        qtyField = new JTextField(5);
        unitCombo = new JComboBox<>(UNITS);
        donationDateField = new JTextField(LocalDate.now().toString(), 8);
        expiryField = new JTextField(8);
        f1.add(new JLabel("Donor")); f1.add(donorCombo);
        f1.add(new JLabel("Food")); f1.add(foodCombo);
        f1.add(new JLabel("Qty")); f1.add(qtyField); f1.add(unitCombo);
        f1.add(new JLabel("Date")); f1.add(donationDateField);
        f1.add(new JLabel("Expiry (optional)")); f1.add(expiryField);
        JPanel f2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton bAdd = new JButton("Record Donation");
        JButton bDel = new JButton("Delete Selected");
        f2.add(bAdd); f2.add(bDel);
        f2.add(new JLabel("   (Date format: yyyy-MM-dd, e.g. 2026-11-20)"));
        form.add(f1); form.add(f2);
        p.add(form, BorderLayout.SOUTH);

        bSearch.addActionListener(e -> loadDonations());
        donationSearch.addActionListener(e -> loadDonations());
        bAll.addActionListener(e -> { donationSearch.setText(""); loadDonations(); });
        bAdd.addActionListener(e -> addDonation());
        bDel.addActionListener(e -> deleteDonation());
        return p;
    }

    private void refreshDonorCombo() {
        donorCombo.removeAllItems();
        try (Connection c = getConn(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT donor_id, name FROM donors ORDER BY name")) {
            while (rs.next()) donorCombo.addItem(rs.getInt(1) + " - " + rs.getString(2));
        } catch (SQLException ex) { showError(ex); }
    }

    private void loadDonations() {
        String k = "%" + donationSearch.getText().trim() + "%";
        fill(donationModel,
                "SELECT d.donation_id, o.name, d.food_type, d.quantity, d.unit, d.remaining_quantity, "
                        + "d.donation_date, d.expiry_date FROM donations d JOIN donors o ON o.donor_id = d.donor_id "
                        + "WHERE o.name LIKE ? OR d.food_type LIKE ? ORDER BY d.donation_id DESC", k, k);
    }

    private void addDonation() {
        if (donorCombo.getSelectedItem() == null) { msg("Add a donor first (Donors tab)."); return; }
        try {
            int donorId = Integer.parseInt(((String) donorCombo.getSelectedItem()).split(" - ")[0]);
            double qty = Double.parseDouble(qtyField.getText().trim());
            if (qty <= 0) { msg("Quantity must be greater than 0."); return; }
            Date donationDate = Date.valueOf(donationDateField.getText().trim());
            String exp = expiryField.getText().trim();

            String sql = "INSERT INTO donations (donor_id, food_type, quantity, unit, remaining_quantity, "
                    + "donation_date, expiry_date) VALUES (?,?,?,?,?,?,?)";
            try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, donorId);
                ps.setString(2, (String) foodCombo.getSelectedItem());
                ps.setDouble(3, qty);
                ps.setString(4, (String) unitCombo.getSelectedItem());
                ps.setDouble(5, qty);
                ps.setDate(6, donationDate);
                if (exp.isEmpty()) ps.setNull(7, Types.DATE); else ps.setDate(7, Date.valueOf(exp));
                ps.executeUpdate();
            }
            msg("Donation recorded successfully.");
            qtyField.setText(""); expiryField.setText("");
            loadDonations();
        } catch (NumberFormatException ex) {
            msg("Enter a valid quantity (number).");
        } catch (IllegalArgumentException ex) {
            msg("Date must be in yyyy-MM-dd format (e.g. 2026-11-20).");
        } catch (SQLException ex) { showError(ex); }
    }

    private void deleteDonation() {
        int r = donationTable.getSelectedRow();
        if (r < 0) { msg("Select a donation from the table first."); return; }
        if (JOptionPane.showConfirmDialog(this, "Delete this donation record?", "Confirm",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try (Connection c = getConn();
             PreparedStatement ps = c.prepareStatement("DELETE FROM donations WHERE donation_id=?")) {
            ps.setInt(1, Integer.parseInt(str(donationModel.getValueAt(r, 0))));
            ps.executeUpdate();
            loadDonations();
        } catch (SQLIntegrityConstraintViolationException ex) {
            msg("This donation already has distribution records, so cannot be deleted.");
        } catch (SQLException ex) { showError(ex); }
    }

    // =====================================================
    //  DISTRIBUTION
    // =====================================================
    private JPanel buildDistributionPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel centre = new JPanel(new GridLayout(2, 1, 5, 8));
        availModel = readOnlyModel("Donation ID", "Food Type", "Donor", "Remaining", "Unit", "Expiry Date");
        availTable = new JTable(availModel);
        availTable.setRowHeight(22);
        availTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane sp1 = new JScrollPane(availTable);
        sp1.setBorder(BorderFactory.createTitledBorder("Food available for distribution (select one)"));

        distModel = readOnlyModel("Dist ID", "Donation ID", "Food Type", "Beneficiary",
                "Location", "Quantity", "Unit", "Date");
        JTable distTable = new JTable(distModel);
        distTable.setRowHeight(22);
        JScrollPane sp2 = new JScrollPane(distTable);
        sp2.setBorder(BorderFactory.createTitledBorder("Distribution history"));
        centre.add(sp1); centre.add(sp2);
        p.add(centre, BorderLayout.CENTER);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        beneficiaryField = new JTextField(14);
        locationField = new JTextField(12);
        distQtyField = new JTextField(5);
        distDateField = new JTextField(LocalDate.now().toString(), 8);
        JButton bDist = new JButton("Distribute");
        form.add(new JLabel("Beneficiary / NGO")); form.add(beneficiaryField);
        form.add(new JLabel("Location")); form.add(locationField);
        form.add(new JLabel("Qty")); form.add(distQtyField);
        form.add(new JLabel("Date")); form.add(distDateField);
        form.add(bDist);
        p.add(form, BorderLayout.SOUTH);

        bDist.addActionListener(e -> distribute());
        return p;
    }

    private void loadAvailable() {
        fill(availModel,
                "SELECT d.donation_id, d.food_type, o.name, d.remaining_quantity, d.unit, d.expiry_date "
                        + "FROM donations d JOIN donors o ON o.donor_id = d.donor_id "
                        + "WHERE d.remaining_quantity > 0 ORDER BY d.expiry_date IS NULL, d.expiry_date");
    }

    private void loadDistributions() {
        fill(distModel,
                "SELECT x.dist_id, x.donation_id, d.food_type, x.beneficiary, x.location, "
                        + "x.quantity, d.unit, x.dist_date FROM distributions x "
                        + "JOIN donations d ON d.donation_id = x.donation_id ORDER BY x.dist_id DESC");
    }

    /** Reduces remaining stock and saves the distribution record in one transaction. */
    private void distribute() {
        int r = availTable.getSelectedRow();
        if (r < 0) { msg("Select a donation from the available list."); return; }
        String who = beneficiaryField.getText().trim();
        if (who.isEmpty()) { msg("Enter beneficiary / NGO name."); return; }

        Connection c = null;
        try {
            int donationId = Integer.parseInt(str(availModel.getValueAt(r, 0)));
            double qty = Double.parseDouble(distQtyField.getText().trim());
            if (qty <= 0) { msg("Quantity must be greater than 0."); return; }
            Date date = Date.valueOf(distDateField.getText().trim());

            c = getConn();
            c.setAutoCommit(false);
            try (PreparedStatement upd = c.prepareStatement(
                    "UPDATE donations SET remaining_quantity = remaining_quantity - ? "
                            + "WHERE donation_id = ? AND remaining_quantity >= ?");
                 PreparedStatement ins = c.prepareStatement(
                         "INSERT INTO distributions (donation_id, beneficiary, location, quantity, dist_date) "
                                 + "VALUES (?,?,?,?,?)")) {
                upd.setDouble(1, qty); upd.setInt(2, donationId); upd.setDouble(3, qty);
                if (upd.executeUpdate() == 0) {
                    c.rollback();
                    msg("Not enough quantity remaining in this donation.");
                    return;
                }
                ins.setInt(1, donationId); ins.setString(2, who);
                ins.setString(3, locationField.getText().trim());
                ins.setDouble(4, qty); ins.setDate(5, date);
                ins.executeUpdate();
            }
            c.commit();
            msg("Distribution recorded. Remaining stock updated.");
            beneficiaryField.setText(""); locationField.setText(""); distQtyField.setText("");
            loadAvailable();
            loadDistributions();
        } catch (NumberFormatException ex) {
            msg("Enter a valid quantity (number).");
        } catch (IllegalArgumentException ex) {
            msg("Date must be in yyyy-MM-dd format (e.g. 2026-11-20).");
        } catch (SQLException ex) {
            try { if (c != null) c.rollback(); } catch (SQLException ignored) { }
            showError(ex);
        } finally {
            try { if (c != null) c.close(); } catch (SQLException ignored) { }
        }
    }

    // =====================================================
    //  SUMMARY
    // =====================================================
    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 10));
        lblDonors = statLabel(); lblDonations = statLabel();
        lblDistributions = statLabel(); lblBeneficiaries = statLabel();
        stats.add(statBox("Total Donors", lblDonors));
        stats.add(statBox("Donations Received", lblDonations));
        stats.add(statBox("Distributions Done", lblDistributions));
        stats.add(statBox("Beneficiaries Served", lblBeneficiaries));
        p.add(stats, BorderLayout.NORTH);

        summaryModel = readOnlyModel("Food Type", "Unit", "Total Donated", "Distributed", "Remaining");
        JTable t = new JTable(summaryModel);
        t.setRowHeight(24);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createTitledBorder("Food-wise donation and distribution tracking"));
        p.add(sp, BorderLayout.CENTER);

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> loadSummary());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(refresh);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JLabel statLabel() {
        JLabel l = new JLabel("0", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 28));
        return l;
    }

    private JPanel statBox(String title, JLabel value) {
        JPanel b = new JPanel(new BorderLayout());
        b.setBorder(BorderFactory.createTitledBorder(title));
        b.add(value, BorderLayout.CENTER);
        return b;
    }

    private void loadSummary() {
        lblDonors.setText(String.valueOf(scalar("SELECT COUNT(*) FROM donors")));
        lblDonations.setText(String.valueOf(scalar("SELECT COUNT(*) FROM donations")));
        lblDistributions.setText(String.valueOf(scalar("SELECT COUNT(*) FROM distributions")));
        lblBeneficiaries.setText(String.valueOf(
                scalar("SELECT COUNT(DISTINCT beneficiary) FROM distributions")));
        fill(summaryModel,
                "SELECT food_type, unit, SUM(quantity), SUM(quantity - remaining_quantity), "
                        + "SUM(remaining_quantity) FROM donations GROUP BY food_type, unit ORDER BY food_type");
    }

    private long scalar(String sql) {
        try (Connection c = getConn(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException ex) { showError(ex); return 0; }
    }

    // =====================================================
    //  HELPERS
    // =====================================================
    /** Runs a SELECT and puts all rows into the table model. */
    private void fill(DefaultTableModel m, String sql, Object... params) {
        m.setRowCount(0);
        try (Connection c = getConn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ResultSet rs = ps.executeQuery();
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[n];
                for (int i = 0; i < n; i++) row[i] = rs.getObject(i + 1);
                m.addRow(row);
            }
        } catch (SQLException ex) { showError(ex); }
    }

    private static String str(Object o) { return o == null ? "" : o.toString(); }

    private void msg(String m) { JOptionPane.showMessageDialog(this, m); }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                "Database error:\n" + ex.getMessage()
                        + "\n\nCheck MySQL is running, DB_PASS is correct and food_donation_db.sql was executed.",
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FoodDonationApp().setVisible(true));
    }
}
