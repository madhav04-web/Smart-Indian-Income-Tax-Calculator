package com.smarttax;

import com.formdev.flatlaf.FlatLightLaf;
import com.smarttax.ui.DashboardFrame;
import javax.swing.*;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 10);
            new DashboardFrame().setVisible(true);
        });
    }
}
