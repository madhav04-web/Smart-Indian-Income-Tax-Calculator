
package com.smarttax.ui;

import com.smarttax.dao.*;
import com.smarttax.model.*;
import com.smarttax.service.*;
import com.smarttax.util.InputValidator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.awt.Desktop;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public final class DashboardFrame extends JFrame {

    // Existing backend services
    private final TaxpayerDAO people = new TaxpayerDAO();
    private final CalculationDAO calculations = new CalculationDAO();
    private final TaxCalculationService engine = new TaxCalculationService();
    private final ReportService reports = new ReportService();

    // Theme colors
    private static final Color NAVY = new Color(20, 35, 65);
    private static final Color BLUE = new Color(55, 105, 220);
    private static final Color BACKGROUND = new Color(245, 247, 251);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT = new Color(35, 45, 65);
    private static final Color MUTED = new Color(115, 125, 145);
    private static final Color BORDER = new Color(225, 230, 238);
    private static final Color GREEN = new Color(28, 155, 105);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final DefaultTableModel taxpayerModel =
            new DefaultTableModel(
                    new Object[]{"ID", "Name", "PAN", "Age", "Email",
                            "Mobile", "Status", "FY", "Tax Year"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

    private final JTable taxpayerTable = new JTable(taxpayerModel);
    private final JTextField taxpayerSearch = new JTextField();

    private final JTextField nameField = new JTextField();
    private final JTextField panField = new JTextField();
    private final JTextField ageField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField mobileField = new JTextField();

    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[]{"Resident", "Non-resident",
                    "Not ordinarily resident"});

    private final JTextField[] moneyFields = new JTextField[16];

    private final JComboBox<TaxpayerChoice> taxpayerSelector = new JComboBox<>();
    private final JLabel selectedTaxpayerInfo = new JLabel("Choose a taxpayer to load their details.");
    private final JLabel dashboardIncome = new JLabel("₹0");
    private final JLabel dashboardTax = new JLabel("₹0");
    private final JLabel dashboardTakeHome = new JLabel("₹0");
    private final DonutChart salaryChart = new DonutChart();
    private final JTextField yearField = new JTextField("2026-27");

    private final JComboBox<String> residentBox =
            new JComboBox<>(new String[]{"Yes", "No"});

    private final JTextArea resultArea = new JTextArea(10, 60);

    private Taxpayer selected;
    private TaxCalculationService.Comparison last;
    private TaxInput lastInput;

    private JLabel taxpayerCount;
    private JLabel calculationCount;
    private JLabel averageTax;

    public DashboardFrame() {
        super("Smart Indian Income Tax Calculator");

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 900);
        setMinimumSize(new Dimension(1100, 750));
        setLocationRelativeTo(null);

        getContentPane().setBackground(BACKGROUND);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);

        root.add(createSidebar(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(BACKGROUND);

        right.add(createHeader(), BorderLayout.NORTH);

        content.setBackground(BACKGROUND);
        content.add(createDashboard(), "dashboard");
        content.add(createCalculator(), "calculator");
        content.add(createTaxpayers(), "taxpayers");
        content.add(createHistory(), "history");

        right.add(content, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);

        setContentPane(root);

        styleTable(taxpayerTable);
        refreshTaxpayers();
        refreshDashboard();
    }

    // ---------------------------------------------------------
    // SIDEBAR
    // ---------------------------------------------------------

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBackground(NAVY);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(25, 15, 20, 15));

        JLabel logo = new JLabel("  SMART TAX");
        logo.setFont(new Font("SansSerif", Font.BOLD, 23));
        logo.setForeground(WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("  INCOME TAX MANAGEMENT");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
        subtitle.setForeground(new Color(170, 185, 210));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(5));
        sidebar.add(subtitle);
        sidebar.add(Box.createVerticalStrut(45));

        sidebar.add(createNavButton("Dashboard", "dashboard"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Tax Calculator", "calculator"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Taxpayer Records", "taxpayers"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createNavButton("Calculation History", "history"));

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel(
                "<html><div style='width:175px'>"
                        + "Smart Indian Income Tax Calculator<br><br>"
                        + "Financial Year 2026-27"
                        + "</div></html>");
        footer.setForeground(new Color(170, 185, 210));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));

        sidebar.add(footer);

        return sidebar;
    }

    private JButton createNavButton(String title, String page) {
        JButton button = new JButton(title);

        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setBackground(NAVY);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(14, 15, 14, 10));
        button.setMaximumSize(new Dimension(220, 48));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> {
            cardLayout.show(content, page);

            if (page.equals("taxpayers")) {
                refreshTaxpayers();
            }

            if (page.equals("dashboard")) {
                refreshDashboard();
            }
        });

        return button;
    }

    // ---------------------------------------------------------
    // HEADER
    // ---------------------------------------------------------

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(WHITE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(18, 30, 18, 30)));

        JLabel title = new JLabel("Smart Indian Income Tax Calculator");
        title.setFont(new Font("SansSerif", Font.BOLD, 19));
        title.setForeground(TEXT);

        JLabel year = new JLabel("FINANCIAL YEAR 2026-27");
        year.setFont(new Font("SansSerif", Font.BOLD, 11));
        year.setForeground(BLUE);

        header.add(title, BorderLayout.WEST);
        header.add(year, BorderLayout.EAST);

        return header;
    }

    // ---------------------------------------------------------
    // DASHBOARD
    // ---------------------------------------------------------

    private JPanel createDashboard() {
        JPanel panel = pagePanel();
        panel.setLayout(new BorderLayout(20, 22));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Dashboard Overview");
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(TEXT);

        JLabel description = new JLabel(
                "Monitor taxpayers, tax calculations and financial summaries.");
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(MUTED);

        top.add(heading);
        top.add(Box.createVerticalStrut(7));
        top.add(description);

        panel.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(15, 20));
        center.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 3, 18, 18));
        cards.setOpaque(false);

        taxpayerCount = addMetricCard(
                cards, "REGISTERED TAXPAYERS", "0", "People registered");

        calculationCount = addMetricCard(
                cards, "COMPLETED CALCULATIONS", "0", "Saved calculations");

        averageTax = addMetricCard(
                cards, "AVERAGE LOWER TAX", "₹0", "Average recommended tax");

        center.add(cards, BorderLayout.NORTH);

        JPanel analytics = new JPanel(new GridLayout(1, 2, 18, 18));
        analytics.setOpaque(false);
        JPanel salaryCard = new JPanel(new BorderLayout(8, 8));
        salaryCard.setBackground(WHITE);
        salaryCard.setBorder(cardBorder());
        JLabel salaryTitle = new JLabel("Income & tax distribution");
        salaryTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        salaryTitle.setForeground(TEXT);
        salaryCard.add(salaryTitle, BorderLayout.NORTH);
        salaryCard.add(salaryChart, BorderLayout.CENTER);
        JPanel legend = new JPanel(new GridLayout(3, 1, 4, 4));
        legend.setOpaque(false);
        legend.add(legendLine(new Color(20, 155, 112), "Take-home", dashboardTakeHome));
        legend.add(legendLine(BLUE, "Tax payable", dashboardTax));
        legend.add(legendLine(new Color(235, 91, 91), "Other / deductions", dashboardIncome));
        salaryCard.add(legend, BorderLayout.SOUTH);

        JPanel summary = new JPanel(new GridLayout(3, 1, 0, 12));
        summary.setOpaque(false);
        summary.add(summaryCard("TOTAL GROSS INCOME", dashboardIncome, BLUE));
        summary.add(summaryCard("ESTIMATED TAX PAYABLE", dashboardTax, new Color(205, 60, 65)));
        summary.add(summaryCard("ESTIMATED TAKE-HOME", dashboardTakeHome, GREEN));
        analytics.add(salaryCard);
        analytics.add(summary);
        center.add(analytics, BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);

        JLabel note = new JLabel(
                "Note: Tax calculations are estimates and do not replace professional tax advice.");
        note.setForeground(MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JButton refresh = secondaryButton("Refresh Dashboard");
        refresh.addActionListener(e -> refreshDashboard());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(note, BorderLayout.WEST);
        bottom.add(refresh, BorderLayout.EAST);

        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel addMetricCard(JPanel parent, String title,
                                 String value, String description) {
        JPanel card = new JPanel();
        card.setBackground(WHITE);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(cardBorder());

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 11));
        heading.setForeground(MUTED);

        JLabel number = new JLabel(value);
        number.setFont(new Font("SansSerif", Font.BOLD, 28));
        number.setForeground(BLUE);

        JLabel sub = new JLabel(description);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
        sub.setForeground(MUTED);

        card.add(heading);
        card.add(Box.createVerticalStrut(17));
        card.add(number);
        card.add(Box.createVerticalStrut(8));
        card.add(sub);

        parent.add(card);

        return number;
    }

    private void refreshDashboard() {
        guard(() -> {
            DashboardStats stats = calculations.dashboardStats();

            taxpayerCount.setText(
                    Long.toString(stats.taxpayerCount()));

            calculationCount.setText(
                    Long.toString(stats.calculationCount()));

            averageTax.setText(
                    "₹" + stats.averageRecommendedTax()
                            .setScale(0, RoundingMode.HALF_UP));
        });
    }

    // ---------------------------------------------------------
    // TAX CALCULATOR
    // ---------------------------------------------------------

    private JPanel createCalculator() {
        JPanel panel = pagePanel();
        panel.setLayout(new BorderLayout(15, 15));

        JLabel heading = new JLabel("Income Tax Calculator");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(TEXT);

        JLabel description = new JLabel(
                "Enter income and deduction details to compare both tax regimes.");
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(MUTED);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.add(heading);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(description);

        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(0, 2, 15, 12));
        fields.setBackground(WHITE);
        fields.setBorder(cardBorder());

        taxpayerSelector.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setText(value == null ? "Search / select taxpayer" : value.toString());
                return this;
            }
        });
        taxpayerSelector.setEditable(true);
        taxpayerSelector.setMaximumRowCount(12);
        taxpayerSelector.addActionListener(e -> {
            Object value = taxpayerSelector.getSelectedItem();
            if (value instanceof TaxpayerChoice choice) loadCalculatorTaxpayer(choice.person());
        });
        addField(fields, "Select Taxpayer (ID / Name / PAN)", taxpayerSelector);
        addField(fields, "Financial Year", yearField);
        addField(fields, "Selected taxpayer", selectedTaxpayerInfo);
        yearField.setEditable(false);

        String[] labels = {
                "Salary",
                "House Property",
                "Business / Profession",
                "Capital Gains",
                "Other Income",
                "Section 80C",
                "Section 80D",
                "Section 80G",
                "Section 80E",
                "Section 80TTA / 80TTB",
                "Section 80CCD(1B)",
                "Other Deductions",
                "HRA Exemption",
                "Standard Deduction"
        };

        String[] defaults = {
                "0", "0", "0", "0", "0",
                "0", "0", "0", "0", "0",
                "0", "0", "0", "0"
        };

        for (int i = 0; i < labels.length; i++) {
            moneyFields[i + 2] = new JTextField(defaults[i]);
            styleInput(moneyFields[i + 2]);
            addField(fields, labels[i], moneyFields[i + 2]);
        }

        addField(fields, "Resident Individual?", residentBox);

        JScrollPane scroll = new JScrollPane(fields);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultArea.setForeground(TEXT);
        resultArea.setBackground(WHITE);
        resultArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        resultArea.setText("Your tax comparison results will appear here.");

        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setPreferredSize(new Dimension(100, 190));
        resultScroll.setBorder(cardBorder());

        JButton calculateButton = primaryButton("Calculate Tax");
        JButton saveButton = secondaryButton("Save Calculation");
        JButton pdfButton = secondaryButton("Generate PDF");
        JButton csvButton = secondaryButton("Export CSV");

        calculateButton.addActionListener(e -> guard(() -> {
            if (selected == null) {
                throw new IllegalArgumentException(
                        "Please select a taxpayer in Taxpayer Records first.");
            }

            TaxInput input = readInput();
            last = engine.compare(input, yearField.getText().trim());
            lastInput = input;

            showComparison(resultArea, last);
            updateResultDashboard(input, last);
        }));

        saveButton.addActionListener(e -> guard(() -> {
            if (last == null || selected == null || lastInput == null) {
                throw new IllegalArgumentException(
                        "Calculate tax before saving.");
            }

            calculations.save(
                    selected.id(),
                    yearField.getText().trim(),
                    lastInput,
                    last);

            JOptionPane.showMessageDialog(
                    this, "Calculation saved successfully!");
            refreshDashboard();
        }));

        pdfButton.addActionListener(e -> guard(() -> {
            if (last == null || selected == null || lastInput == null) {
                throw new IllegalArgumentException("Calculate tax before generating the PDF.");
            }
            Path generated = reports.pdf(selected, lastInput, last);
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Tax Report PDF");
            chooser.setSelectedFile(new java.io.File("Tax-Report-" + selected.pan() + ".pdf"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path destination = chooser.getSelectedFile().toPath();
                if (!destination.toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) destination = Path.of(destination + ".pdf");
                Files.copy(generated, destination, StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "PDF saved successfully:\n" + destination.toAbsolutePath());
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(destination.toFile());
            }
        }));

        csvButton.addActionListener(e -> guard(() -> {
            if (last == null || selected == null || lastInput == null) throw new IllegalArgumentException("Calculate tax before exporting CSV.");
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Export Complete Tax Calculation CSV");
            chooser.setSelectedFile(new java.io.File("Tax-Calculation-" + selected.pan() + ".csv"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path file = chooser.getSelectedFile().toPath();
                if (!file.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) file = Path.of(file + ".csv");
                StringBuilder data = new StringBuilder("SECTION,FIELD,VALUE\r\n");
                csvAdd(data,"Taxpayer","ID",String.valueOf(selected.id()));
                csvAdd(data,"Taxpayer","Name",selected.fullName());
                csvAdd(data,"Taxpayer","PAN",selected.pan());
                csvAdd(data,"Taxpayer","Age",String.valueOf(selected.age()));
                csvAdd(data,"Taxpayer","Financial Year",yearField.getText());
                csvAdd(data,"Income","Salary",lastInput.salary());
                csvAdd(data,"Income","House Property",lastInput.houseProperty());
                csvAdd(data,"Income","Business / Profession",lastInput.business());
                csvAdd(data,"Income","Capital Gains",lastInput.capitalGains());
                csvAdd(data,"Income","Other Income",lastInput.otherIncome());
                csvAdd(data,"Deductions","Section 80C",lastInput.section80c());
                csvAdd(data,"Deductions","Section 80D",lastInput.section80d());
                csvAdd(data,"Deductions","Section 80G",lastInput.section80g());
                csvAdd(data,"Deductions","Section 80E",lastInput.section80e());
                csvAdd(data,"Deductions","Section 80TTA / 80TTB",lastInput.section80tta());
                csvAdd(data,"Deductions","Section 80CCD(1B)",lastInput.section80ccd1b());
                csvAdd(data,"Deductions","Other Deductions",lastInput.otherDeductions());
                csvAdd(data,"Deductions","HRA Exemption",lastInput.hraExemption());
                csvAdd(data,"Deductions","Standard Deduction",lastInput.standardDeduction());
                addRegimeCsv(data,last.oldRegime()); addRegimeCsv(data,last.newRegime());
                csvAdd(data,"Recommendation","Recommended Regime",last.recommendedRegime());
                csvAdd(data,"Recommendation","Estimated Tax Difference",last.savings());
                Files.writeString(file, "\uFEFF" + data, java.nio.charset.StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Complete CSV exported:\n" + file.toAbsolutePath());
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.toFile());
            }
        }));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actions.setOpaque(false);
        actions.add(calculateButton);
        actions.add(saveButton);
        actions.add(pdfButton);
        actions.add(csvButton);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(resultScroll, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);

        panel.add(center, BorderLayout.CENTER);

        return panel;
    }

    private void addField(JPanel panel, String label, JComponent field) {
        JLabel text = new JLabel(label);
        text.setFont(new Font("SansSerif", Font.BOLD, 12));
        text.setForeground(TEXT);

        if (field instanceof JTextField input) {
            styleInput(input);
        }

        panel.add(text);
        panel.add(field);
    }

    private TaxInput readInput() {
        BigDecimal[] values = new BigDecimal[14];

        for (int i = 0; i < 14; i++) {
            values[i] = InputValidator.amount(
                    moneyFields[i + 2].getText());
        }

        return new TaxInput(
                selected.age(),
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7],
                values[8], values[9], values[10], values[11],
                values[12], values[13],
                residentBox.getSelectedIndex() == 0
        );
    }

    private void showComparison(
            JTextArea output,
            TaxCalculationService.Comparison comparison) {

        TaxResult old = comparison.oldRegime();
        TaxResult newer = comparison.newRegime();

        output.setText(
                "              TAX REGIME COMPARISON\n"
                        + "------------------------------------------------------------\n"
                        + String.format("%-25s %15s %15s%n",
                        "DETAIL", "OLD REGIME", "NEW REGIME")
                        + "------------------------------------------------------------\n"
                        + row("Gross Income", old.grossIncome(), newer.grossIncome())
                        + row("Deductions", old.deductions(), newer.deductions())
                        + row("Taxable Income", old.taxableIncome(), newer.taxableIncome())
                        + row("Tax Before Rebate", old.taxBeforeRebate(), newer.taxBeforeRebate())
                        + row("Rebate", old.rebate(), newer.rebate())
                        + row("Cess", old.cess(), newer.cess())
                        + "------------------------------------------------------------\n"
                        + row("FINAL TAX PAYABLE", old.taxPayable(), newer.taxPayable())
                        + "\nRecommended Regime: " + comparison.recommendedRegime()
                        + "\nEstimated Difference: ₹" + comparison.savings()
                        + "\n\nNote: This is an estimate, not professional tax advice."
        );
    }

    private String row(String label, BigDecimal old, BigDecimal newer) {
        return String.format("%-25s %15s %15s%n",
                label, "₹" + old.toPlainString(),
                "₹" + newer.toPlainString());
    }

    private String csvRow(TaxResult result) {
        return String.join(",",
                result.regime(),
                result.grossIncome().toPlainString(),
                result.deductions().toPlainString(),
                result.taxableIncome().toPlainString(),
                result.taxBeforeRebate().toPlainString(),
                result.rebate().toPlainString(),
                result.cess().toPlainString(),
                result.taxPayable().toPlainString(),
                result.effectiveRate().toPlainString());
    }

    private void loadCalculatorTaxpayer(Taxpayer person) {
        selected = person;
        yearField.setText(person.financialYear() == null || person.financialYear().isBlank() ? "2026-27" : person.financialYear());
        selectedTaxpayerInfo.setText(person.fullName() + " • " + person.pan() + " • ID " + person.id());
        residentBox.setSelectedItem("Resident".equalsIgnoreCase(person.residentialStatus()) ? "Yes" : "No");
        last = null; lastInput = null;
        resultArea.setText("Taxpayer loaded. Enter income and deduction details, then calculate.");
        for (int i=2;i<moneyFields.length;i++) if (moneyFields[i]!=null) moneyFields[i].setText("0");
    }

    private JPanel summaryCard(String title, JLabel value, Color accent) {
        JPanel card=new JPanel(new BorderLayout(8,8)); card.setBackground(WHITE); card.setBorder(cardBorder());
        JLabel heading=new JLabel(title); heading.setFont(new Font("SansSerif",Font.BOLD,11)); heading.setForeground(MUTED);
        value.setFont(new Font("SansSerif",Font.BOLD,25)); value.setForeground(accent);
        card.add(heading,BorderLayout.NORTH); card.add(value,BorderLayout.CENTER); return card;
    }

    private JPanel legendLine(Color color,String title,JLabel amount) {
        JPanel p=new JPanel(new BorderLayout(8,0)); p.setOpaque(false);
        JLabel dot=new JLabel("●  "+title); dot.setForeground(color); dot.setFont(new Font("SansSerif",Font.BOLD,12));
        amount.setFont(new Font("SansSerif",Font.BOLD,12)); amount.setForeground(TEXT);
        p.add(dot,BorderLayout.WEST); p.add(amount,BorderLayout.EAST); return p;
    }

    private void updateResultDashboard(TaxInput input, TaxCalculationService.Comparison c) {
        BigDecimal gross=input.gross(); BigDecimal tax=c.recommendedRegime().toUpperCase(Locale.ROOT).contains("OLD")?c.oldRegime().taxPayable():c.newRegime().taxPayable();
        BigDecimal takeHome=gross.subtract(tax).max(BigDecimal.ZERO);
        dashboardIncome.setText(money(gross)); dashboardTax.setText(money(tax)); dashboardTakeHome.setText(money(takeHome));
        salaryChart.setValues(takeHome.doubleValue(),tax.doubleValue(),gross.subtract(takeHome).subtract(tax).max(BigDecimal.ZERO).doubleValue());
    }

    private String money(BigDecimal value) { return "₹"+value.setScale(0,RoundingMode.HALF_UP).toPlainString(); }
    private void csvAdd(StringBuilder b,String section,String field,Object value) { b.append(csvEscape(section)).append(',').append(csvEscape(field)).append(',').append(csvEscape(String.valueOf(value))).append("\r\n"); }
    private String csvEscape(String v) { return "\""+v.replace("\"","\"\"")+"\""; }
    private void addRegimeCsv(StringBuilder b,TaxResult r) {
        csvAdd(b,r.regime()+" Regime","Gross Income",r.grossIncome()); csvAdd(b,r.regime()+" Regime","Eligible Deductions",r.deductions());
        csvAdd(b,r.regime()+" Regime","Taxable Income",r.taxableIncome()); csvAdd(b,r.regime()+" Regime","Tax Before Rebate",r.taxBeforeRebate());
        csvAdd(b,r.regime()+" Regime","Rebate",r.rebate()); csvAdd(b,r.regime()+" Regime","Surcharge",r.surcharge());
        csvAdd(b,r.regime()+" Regime","Cess",r.cess()); csvAdd(b,r.regime()+" Regime","Tax Payable",r.taxPayable()); csvAdd(b,r.regime()+" Regime","Effective Rate (%)",r.effectiveRate());
    }

    private record TaxpayerChoice(Taxpayer person) { @Override public String toString() { return person.id()+" | "+person.fullName()+" | "+person.pan(); } }

    private static final class DonutChart extends JPanel {
        private double take=0,tax=0,other=0;
        DonutChart(){setOpaque(false);setPreferredSize(new Dimension(260,210));}
        void setValues(double a,double b,double c){take=Math.max(0,a);tax=Math.max(0,b);other=Math.max(0,c);repaint();}
        @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int size=Math.min(getWidth()-30,getHeight()-15);int x=(getWidth()-size)/2,y=(getHeight()-size)/2;double total=take+tax+other;if(total<=0){g.setColor(new Color(235,239,246));g.fillOval(x,y,size,size);}else{double start=90;Color[] colors={new Color(20,155,112),new Color(48,91,190),new Color(225,76,76)};double[] vals={take,tax,other};for(int i=0;i<3;i++){double extent=360*vals[i]/total;g.setColor(colors[i]);g.fill(new Arc2D.Double(x,y,size,size,start,-extent,Arc2D.PIE));start-=extent;}}g.setColor(WHITE);int hole=(int)(size*.53);g.fillOval(x+(size-hole)/2,y+(size-hole)/2,hole,hole);g.setColor(TEXT);g.setFont(new Font("SansSerif",Font.BOLD,13));String label="SALARY MIX";FontMetrics fm=g.getFontMetrics();g.drawString(label,(getWidth()-fm.stringWidth(label))/2,getHeight()/2+4);g.dispose();}
    }

    // ---------------------------------------------------------
    // TAXPAYER RECORDS
    // ---------------------------------------------------------

    private JPanel createTaxpayers() {
        JPanel panel = pagePanel();
        panel.setLayout(new BorderLayout(12, 12));

        JLabel heading = new JLabel("Taxpayer Management");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(TEXT);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);

        top.add(heading, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        searchPanel.setOpaque(false);

        taxpayerSearch.setPreferredSize(new Dimension(260, 35));
        styleInput(taxpayerSearch);

        JButton searchButton = primaryButton("Search");
        JButton allButton = secondaryButton("Show All");

        searchButton.addActionListener(e -> refreshTaxpayers());

        allButton.addActionListener(e -> {
            taxpayerSearch.setText("");
            refreshTaxpayers();
        });

        searchPanel.add(new JLabel("Search by name, PAN or ID:"));
        searchPanel.add(taxpayerSearch);
        searchPanel.add(searchButton);
        searchPanel.add(allButton);

        top.add(searchPanel, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        styleTable(taxpayerTable);
        taxpayerTable.setAutoCreateRowSorter(true);
        taxpayerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        taxpayerTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()
                    && taxpayerTable.getSelectedRow() >= 0) {
                loadSelectedTaxpayer();
            }
        });

        JScrollPane tableScroll = new JScrollPane(taxpayerTable);
        tableScroll.setBorder(cardBorder());

        JPanel form = new JPanel(new GridLayout(3, 4, 12, 10));
        form.setBackground(WHITE);
        form.setBorder(cardBorder());

        addField(form, "Full Name", nameField);
        addField(form, "PAN", panField);
        addField(form, "Age", ageField);
        addField(form, "Email", emailField);
        addField(form, "Mobile", mobileField);
        addField(form, "Residential Status", statusBox);

        JButton save = primaryButton("Save / Update");
        JButton delete = secondaryButton("Delete Selected");
        JButton clear = secondaryButton("Clear Form");

        save.addActionListener(e -> guard(this::saveTaxpayer));

        delete.addActionListener(e -> guard(() -> {
            if (selected == null) {
                throw new IllegalArgumentException(
                        "Select a taxpayer first.");
            }

            int answer = JOptionPane.showConfirmDialog(
                    this,
                    "Delete " + selected.fullName() + "?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);

            if (answer == JOptionPane.YES_OPTION) {
                people.delete(selected.id());
                selected = null;
                clearTaxpayerForm();
                refreshTaxpayers();
                refreshDashboard();
            }
        }));

        clear.addActionListener(e -> clearTaxpayerForm());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(clear);
        buttons.add(delete);
        buttons.add(save);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);
        bottom.add(form, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void loadSelectedTaxpayer() {
        try {
            int viewRow = taxpayerTable.getSelectedRow();

            if (viewRow < 0) return;

            int row = taxpayerTable.convertRowIndexToModel(viewRow);
            long id = ((Number) taxpayerModel.getValueAt(row, 0)).longValue();

            selected = people.find(String.valueOf(id))
                    .stream()
                    .filter(t -> t.id() == id)
                    .findFirst()
                    .orElse(null);

            if (selected != null) {
                nameField.setText(selected.fullName());
                panField.setText(selected.pan());
                ageField.setText(String.valueOf(selected.age()));
                emailField.setText(selected.email());
                mobileField.setText(selected.mobile());
                statusBox.setSelectedItem(selected.residentialStatus());

                selectedTaxpayerInfo.setText(selected.fullName() + " • " + selected.pan() + " • ID " + selected.id());
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void saveTaxpayer() throws Exception {
        if (nameField.getText().isBlank()
                || panField.getText().isBlank()
                || ageField.getText().isBlank()
                || emailField.getText().isBlank()
                || mobileField.getText().isBlank()) {
            throw new IllegalArgumentException(
                    "Please complete all taxpayer fields.");
        }

        if (!InputValidator.pan(panField.getText())) {
            throw new IllegalArgumentException(
                    "PAN must follow ABCDE1234F format.");
        }

        if (!InputValidator.email(emailField.getText())) {
            throw new IllegalArgumentException(
                    "Enter a valid email address.");
        }

        if (!InputValidator.mobile(mobileField.getText())) {
            throw new IllegalArgumentException(
                    "Mobile number must contain 10 digits.");
        }

        int age = Integer.parseInt(ageField.getText());

        if (age < 1 || age > 120) {
            throw new IllegalArgumentException(
                    "Age must be between 1 and 120.");
        }

        Taxpayer taxpayer = new Taxpayer(
                selected == null ? null : selected.id(),
                nameField.getText().trim(),
                panField.getText().trim().toUpperCase(),
                age,
                emailField.getText().trim(),
                mobileField.getText().trim(),
                (String) statusBox.getSelectedItem(),
                "2026-27",
                "2026-27"
        );

        people.save(taxpayer);

        selected = null;
        clearTaxpayerForm();
        refreshTaxpayers();
        refreshDashboard();

        JOptionPane.showMessageDialog(
                this, "Taxpayer saved successfully!");
    }

    private void clearTaxpayerForm() {
        selected = null;

        nameField.setText("");
        panField.setText("");
        ageField.setText("");
        emailField.setText("");
        mobileField.setText("");

        selectedTaxpayerInfo.setText("Choose a taxpayer to load their details.");
        taxpayerSelector.setSelectedItem(null);
        taxpayerTable.clearSelection();
    }

    private void refreshTaxpayers() {
        try {
            taxpayerModel.setRowCount(0);

            List<Taxpayer> taxpayers =
                    people.find(taxpayerSearch.getText());

            Object current = taxpayerSelector.getSelectedItem();
            taxpayerSelector.removeAllItems();
            for (Taxpayer taxpayer : people.find("")) taxpayerSelector.addItem(new TaxpayerChoice(taxpayer));
            taxpayerSelector.setSelectedIndex(-1);
            if (current instanceof TaxpayerChoice oldChoice) {
                for (int i=0;i<taxpayerSelector.getItemCount();i++) if (taxpayerSelector.getItemAt(i).person().id().equals(oldChoice.person().id())) taxpayerSelector.setSelectedIndex(i);
            }
            for (Taxpayer taxpayer : taxpayers) {
                taxpayerModel.addRow(new Object[]{
                        taxpayer.id(),
                        taxpayer.fullName(),
                        taxpayer.pan(),
                        taxpayer.age(),
                        taxpayer.email(),
                        taxpayer.mobile(),
                        taxpayer.residentialStatus(),
                        taxpayer.financialYear(),
                        taxpayer.taxYear()
                });
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ---------------------------------------------------------
    // CALCULATION HISTORY
    // ---------------------------------------------------------

    private JPanel createHistory() {
        JPanel panel = pagePanel();
        panel.setLayout(new BorderLayout(15, 15));

        JLabel heading = new JLabel("Calculation History");
        heading.setFont(new Font("SansSerif", Font.BOLD, 26));
        heading.setForeground(TEXT);

        JTextField panFilter = new JTextField(14);
        JTextField yearFilter = new JTextField("2026-27", 9);

        styleInput(panFilter);
        styleInput(yearFilter);

        JButton search = primaryButton("Search History");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filters.setOpaque(false);
        filters.add(new JLabel("PAN:"));
        filters.add(panFilter);
        filters.add(new JLabel("Financial Year:"));
        filters.add(yearFilter);
        filters.add(search);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"ID", "Taxpayer", "PAN", "Year",
                        "Gross Income", "Deductions", "Old Tax",
                        "New Tax", "Recommended", "Calculated At"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable historyTable = new JTable(model);
        styleTable(historyTable);
        historyTable.setAutoCreateRowSorter(true);

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(cardBorder());

        panel.add(scroll, BorderLayout.CENTER);

        Runnable loadHistory = () -> guard(() -> {
            model.setRowCount(0);

            for (CalculationHistory h :
                    calculations.history(
                            panFilter.getText(),
                            yearFilter.getText())) {

                model.addRow(new Object[]{
                        h.id(),
                        h.taxpayerName(),
                        h.pan(),
                        h.financialYear(),
                        h.grossIncome(),
                        h.eligibleDeductions(),
                        h.oldTax(),
                        h.newTax(),
                        h.recommendation(),
                        h.calculatedAt()
                });
            }
        });

        search.addActionListener(e -> loadHistory.run());

        return panel;
    }

    // ---------------------------------------------------------
    // DESIGN HELPERS
    // ---------------------------------------------------------

    private JPanel pagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND);
        panel.setBorder(new EmptyBorder(25, 28, 25, 28));
        return panel;
    }

    private Border cardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(18, 18, 18, 18));
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BLUE);
        button.setForeground(WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(235, 241, 252));
        button.setForeground(BLUE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 18, 11, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(TEXT);
        field.setBackground(WHITE);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("SansSerif", Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setBackground(WHITE);
        table.setRowHeight(30);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(225, 235, 255));
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(
                new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(
                new Color(237, 241, 248));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setPreferredSize(
                new Dimension(100, 36));
    }

    // ---------------------------------------------------------
    // ERROR HANDLING
    // ---------------------------------------------------------

    private void guard(Throwing action) {
        try {
            action.run();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage() == null ? ex.toString() : ex.getMessage(),
                "Unable to complete action",
                JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface Throwing {
        void run() throws Exception;
    }
}
