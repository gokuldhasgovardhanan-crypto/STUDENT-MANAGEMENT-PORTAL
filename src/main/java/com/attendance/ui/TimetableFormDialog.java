package com.attendance.ui;

import com.attendance.model.Period;
import com.attendance.model.Subject;
import com.attendance.model.Teacher;
import com.attendance.model.TimetableEntry;
import com.attendance.service.SubjectService;
import com.attendance.service.TeacherService;
import com.attendance.service.TimetableService;
import com.attendance.ui.components.ModernButton;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog for scheduling class timetable periods.
 */
public class TimetableFormDialog extends JDialog {

    private final TimetableService timetableService;
    private final SubjectService subjectService;
    private final TeacherService teacherService;
    private final TimetableEntry existingEntry;
    private boolean saved = false;

    private JComboBox<String> cmbDay;
    private JComboBox<PeriodItem> cmbPeriod;
    private JComboBox<SubjectItem> cmbSubject;
    private JComboBox<TeacherItem> cmbTeacher;
    private JComboBox<String> cmbDept;
    private JComboBox<Integer> cmbYear;
    private JComboBox<String> cmbSection;
    private JTextField txtRoom;

    public TimetableFormDialog(Window owner, TimetableEntry entry, String defaultDept, int defaultYear, String defaultSec) {
        super(owner, entry == null ? "Add Timetable Slot" : "Edit Timetable Slot", ModalityType.APPLICATION_MODAL);
        this.timetableService = new TimetableService();
        this.subjectService = new SubjectService();
        this.teacherService = new TeacherService();
        this.existingEntry = entry;

        setSize(500, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 14));
        header.setBackground(new Color(168, 28, 28)); // KIT Crimson Red
        JLabel lblTitle = new JLabel(entry == null ? "📅  Schedule Timetable Period" : "✏️  Edit Timetable Period");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        form.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        cmbDay = new JComboBox<>(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"});
        cmbPeriod = new JComboBox<>();
        cmbSubject = new JComboBox<>();
        cmbTeacher = new JComboBox<>();
        cmbDept = new JComboBox<>(new String[]{"CSE", "IT", "AI&DS", "ECE", "EEE", "MECH"});
        cmbYear = new JComboBox<>(new Integer[]{1, 2, 3, 4});
        cmbSection = new JComboBox<>(new String[]{"A", "B"});
        txtRoom = new JTextField("Room 101");

        if (defaultDept != null) cmbDept.setSelectedItem(defaultDept);
        if (defaultYear > 0) cmbYear.setSelectedItem(defaultYear);
        if (defaultSec != null) cmbSection.setSelectedItem(defaultSec);

        loadPeriods();
        loadTeachers();
        loadSubjects();

        cmbDept.addActionListener(e -> loadSubjects());
        cmbYear.addActionListener(e -> loadSubjects());

        addFormField(form, gbc, 0, "Day of the Week:", cmbDay);
        addFormField(form, gbc, 1, "Period:", cmbPeriod);
        addFormField(form, gbc, 2, "Department:", cmbDept);
        addFormField(form, gbc, 3, "Year / Section:", createYearSecPanel());
        addFormField(form, gbc, 4, "Subject:", cmbSubject);
        addFormField(form, gbc, 5, "Faculty in Charge:", cmbTeacher);
        addFormField(form, gbc, 6, "Classroom / Lab:", txtRoom);

        if (existingEntry != null) {
            populateData();
        }

        add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)));

        ModernButton btnCancel = new ModernButton("Cancel", ModernButton.ButtonType.SECONDARY);
        ModernButton btnSave = new ModernButton(existingEntry == null ? "Save Slot" : "Update Slot", ModernButton.ButtonType.PRIMARY);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> handleSave());

        footer.add(btnCancel);
        footer.add(btnSave);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createYearSecPanel() {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setOpaque(false);
        p.add(cmbYear);
        p.add(cmbSection);
        return p;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(comp, gbc);
    }

    private void loadPeriods() {
        try {
            List<Period> periods = timetableService.getAllPeriods();
            cmbPeriod.removeAllItems();
            for (Period p : periods) {
                cmbPeriod.addItem(new PeriodItem(p.getPeriodId(), p.getPeriodNumber() + ": " + p.getPeriodName() + " (" + p.getStartTime() + " - " + p.getEndTime() + ")"));
            }
        } catch (Exception ignored) {}
    }

    private void loadTeachers() {
        cmbTeacher.removeAllItems();
        cmbTeacher.addItem(new TeacherItem(null, "-- Unassigned --"));
        List<Teacher> teachers = teacherService.getAllTeachers();
        for (Teacher t : teachers) {
            cmbTeacher.addItem(new TeacherItem(t.getTeacherId(), t.getTeacherName() + " (" + t.getDepartment() + ")"));
        }
    }

    private void loadSubjects() {
        cmbSubject.removeAllItems();
        String dept = (String) cmbDept.getSelectedItem();
        Integer year = (Integer) cmbYear.getSelectedItem();
        List<Subject> subjects = subjectService.getSubjectsByDeptAndYear(dept, year != null ? year : 1);
        for (Subject s : subjects) {
            cmbSubject.addItem(new SubjectItem(s.getSubjectId(), s.getSubjectCode() + " - " + s.getSubjectName()));
        }
    }

    private void populateData() {
        cmbDay.setSelectedItem(existingEntry.getDayOfWeek());
        cmbDept.setSelectedItem(existingEntry.getDepartment());
        cmbYear.setSelectedItem(existingEntry.getYearOfStudy());
        cmbSection.setSelectedItem(existingEntry.getSection());
        txtRoom.setText(existingEntry.getRoom());

        loadSubjects();

        for (int i = 0; i < cmbPeriod.getItemCount(); i++) {
            if (cmbPeriod.getItemAt(i).id == existingEntry.getPeriodId()) {
                cmbPeriod.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < cmbSubject.getItemCount(); i++) {
            if (cmbSubject.getItemAt(i).id == existingEntry.getSubjectId()) {
                cmbSubject.setSelectedIndex(i);
                break;
            }
        }

        if (existingEntry.getTeacherId() != null) {
            for (int i = 0; i < cmbTeacher.getItemCount(); i++) {
                TeacherItem item = cmbTeacher.getItemAt(i);
                if (item.id != null && item.id.equals(existingEntry.getTeacherId())) {
                    cmbTeacher.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void handleSave() {
        PeriodItem selectedPeriod = (PeriodItem) cmbPeriod.getSelectedItem();
        SubjectItem selectedSubject = (SubjectItem) cmbSubject.getSelectedItem();

        if (selectedPeriod == null) {
            JOptionPane.showMessageDialog(this, "Please select a period.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedSubject == null) {
            JOptionPane.showMessageDialog(this, "Please select a subject.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TimetableEntry t = existingEntry != null ? existingEntry : new TimetableEntry();
        t.setDayOfWeek((String) cmbDay.getSelectedItem());
        t.setPeriodId(selectedPeriod.id);
        t.setSubjectId(selectedSubject.id);
        t.setDepartment((String) cmbDept.getSelectedItem());
        t.setYearOfStudy((Integer) cmbYear.getSelectedItem());
        t.setSection((String) cmbSection.getSelectedItem());
        t.setRoom(txtRoom.getText().trim());

        TeacherItem selectedTeacher = (TeacherItem) cmbTeacher.getSelectedItem();
        t.setTeacherId(selectedTeacher != null ? selectedTeacher.id : null);

        try {
            if (existingEntry == null) {
                timetableService.createEntry(t);
            } else {
                timetableService.updateEntry(t);
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save timetable slot: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    private static class PeriodItem {
        final int id;
        final String label;

        PeriodItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class SubjectItem {
        final int id;
        final String label;

        SubjectItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class TeacherItem {
        final Integer id;
        final String label;

        TeacherItem(Integer id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
