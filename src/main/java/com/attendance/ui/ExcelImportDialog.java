package com.attendance.ui;

import com.attendance.model.Student;
import com.attendance.service.ExcelImportService;
import com.attendance.service.ExcelImportService.ImportAnalysisResult;
import com.attendance.ui.components.ModernButton;
import com.attendance.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;

/**
 * Dialog for importing students in bulk from Excel (.xlsx) files with pre-import validation.
 */
public class ExcelImportDialog extends JDialog {

    private final ExcelImportService importService;
    private File selectedFile;
    private ImportAnalysisResult analysis;
    private boolean imported = false;

    private JLabel lblFilePath;
    private JLabel lblStats;
    private DefaultTableModel previewModel;
    private JTextArea txtIssues;
    private ModernButton btnCommit;

    public ExcelImportDialog(Window owner) {
        super(owner, "Bulk Import Students from Excel (.xlsx)", ModalityType.APPLICATION_MODAL);
        this.importService = new ExcelImportService();

        setSize(780, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel("📊  Bulk Import Students from Excel");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // File Selection Row
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fileRow.setOpaque(false);
        ModernButton btnBrowse = new ModernButton("Choose Excel File...", ModernButton.ButtonType.SECONDARY);
        lblFilePath = new JLabel("No file selected (Expected columns: RegNo, Name, Gender, Dept, Year, Sec, Email, Phone...)");
        lblFilePath.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFilePath.setForeground(new Color(100, 116, 139));
        btnBrowse.addActionListener(e -> onChooseFile());
        fileRow.add(btnBrowse);
        fileRow.add(lblFilePath);
        body.add(fileRow, BorderLayout.NORTH);

        // Middle Split: Preview Table & Issues Log
        String[] cols = {"Reg No", "Student Name", "Gender", "Dept", "Year", "Sec", "Email", "Phone"};
        previewModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable previewTable = new ModernTable(previewModel);
        JScrollPane tableScroll = new JScrollPane(previewTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Validated Students Preview"));

        txtIssues = new JTextArea(5, 20);
        txtIssues.setEditable(false);
        txtIssues.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtIssues.setForeground(new Color(185, 28, 28));
        JScrollPane issuesScroll = new JScrollPane(txtIssues);
        issuesScroll.setBorder(BorderFactory.createTitledBorder("Validation & Duplicate Warnings"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, issuesScroll);
        split.setResizeWeight(0.7);
        body.add(split, BorderLayout.CENTER);

        // Stats Footer
        lblStats = new JLabel("Select an Excel file to analyze.");
        lblStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        body.add(lblStats, BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnCancel = new ModernButton("Close", ModernButton.ButtonType.SECONDARY);
        btnCommit = new ModernButton("Import Valid Records", ModernButton.ButtonType.PRIMARY);
        btnCommit.setEnabled(false);

        btnCancel.addActionListener(e -> dispose());
        btnCommit.addActionListener(e -> onCommit());

        footer.add(btnCancel);
        footer.add(btnCommit);
        add(footer, BorderLayout.SOUTH);
    }

    private void onChooseFile() {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            lblFilePath.setText(selectedFile.getName());
            analyzeFile();
        }
    }

    private void analyzeFile() {
        if (selectedFile == null) return;
        try {
            this.analysis = importService.analyzeExcelFile(selectedFile);
            previewModel.setRowCount(0);
            txtIssues.setText("");

            for (Student s : analysis.validStudents) {
                previewModel.addRow(new Object[]{
                        s.getRegisterNo(),
                        s.getStudentName(),
                        s.getGender(),
                        s.getDepartment(),
                        s.getYearOfStudy(),
                        s.getSection(),
                        s.getEmail(),
                        s.getPhoneNumber()
                });
            }

            for (String issue : analysis.issues) {
                txtIssues.append(issue + "\n");
            }

            lblStats.setText(String.format("Found %d rows: %d valid for import, %d duplicate (will skip), %d invalid format.",
                    analysis.totalRows, analysis.validCount, analysis.duplicateCount, analysis.invalidCount));

            btnCommit.setEnabled(analysis.validCount > 0);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to analyze Excel file: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCommit() {
        if (analysis == null || analysis.validStudents.isEmpty()) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Import " + analysis.validStudents.size() + " valid student records into the database?",
                "Confirm Bulk Import",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int count = importService.commitValidStudents(analysis.validStudents);
                imported = true;
                JOptionPane.showMessageDialog(this,
                        "Successfully imported " + count + " student records into the database!",
                        "Import Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Import error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isImported() {
        return imported;
    }
}
