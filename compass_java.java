// compass_java.java — компас с калибровкой на Java (Swing)

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;
import java.io.*;
import java.nio.file.*;

public class CompassJava extends JFrame {
    private static final String CONFIG_FILE = "compass_config.json";
    private double angle = 0;
    private double offset = 0;
    private boolean calibrated = false;
    private boolean simulating = false;
    private Timer timer;
    private CompassPanel compassPanel;
    private JLabel infoLabel, statusLabel;
    private Random rand = new Random();

    public CompassJava() {
        setTitle("🧭 CompassCalibrator — Java");
        setSize(600, 680);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadConfig();

        compassPanel = new CompassPanel();
        add(compassPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton readBtn = new JButton("Прочитать");
        JButton calibrateBtn = new JButton("Калибровка");
        JButton resetBtn = new JButton("Сброс");
        JButton simBtn = new JButton("Симуляция");
        JButton manualBtn = new JButton("Ввести азимут");
        controlPanel.add(readBtn);
        controlPanel.add(calibrateBtn);
        controlPanel.add(resetBtn);
        controlPanel.add(simBtn);
        controlPanel.add(manualBtn);
        add(controlPanel, BorderLayout.NORTH);

        infoLabel = new JLabel("Азимут: 0° (С)", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        add(infoLabel, BorderLayout.CENTER);

        statusLabel = new JLabel("Готов", SwingConstants.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        readBtn.addActionListener(e -> readAngle());
        calibrateBtn.addActionListener(e -> calibrate());
        resetBtn.addActionListener(e -> reset());
        simBtn.addActionListener(e -> toggleSimulation());
        manualBtn.addActionListener(e -> manualInput());

        timer = new Timer(50, e -> simulate());
        updateDisplay();
    }

    private void readAngle() {
        if (!simulating) {
            angle = (angle + rand.nextDouble() * 10 - 5) % 360;
            if (angle < 0) angle += 360;
        }
        updateDisplay();
    }

    private void calibrate() {
        statusLabel.setText("Калибровка... (имитация)");
        double sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += rand.nextDouble() * 360;
        }
        double avg = sum / 10;
        offset = (0 - avg) % 360;
        if (offset < 0) offset += 360;
        calibrated = true;
        statusLabel.setText(String.format("Калибровка завершена. Смещение: %.2f°", offset));
        saveConfig();
        updateDisplay();
    }

    private void reset() {
        offset = 0;
        calibrated = false;
        angle = 0;
        statusLabel.setText("Сброшено");
        updateDisplay();
    }

    private void toggleSimulation() {
        simulating = !simulating;
        if (simulating) {
            timer.start();
            statusLabel.setText("Симуляция включена");
        } else {
            timer.stop();
            statusLabel.setText("Симуляция выключена");
        }
    }

    private void simulate() {
        angle = (angle + 0.5) % 360;
        updateDisplay();
    }

    private void manualInput() {
        String input = JOptionPane.showInputDialog(this, "Введите азимут (0-360):", angle);
        if (input != null) {
            try {
                double val = Double.parseDouble(input) % 360;
                if (val < 0) val += 360;
                angle = val;
                updateDisplay();
                statusLabel.setText(String.format("Азимут установлен: %.1f°", angle));
            } catch (NumberFormatException ex) {}
        }
    }

    private void updateDisplay() {
        double displayAngle = (angle + offset) % 360;
        if (displayAngle < 0) displayAngle += 360;
        compassPanel.setAngle(displayAngle);
        String dir = directionName(displayAngle);
        infoLabel.setText(String.format("Азимут: %.1f° (%s)", displayAngle, dir));
        statusLabel.setText(String.format("Текущий азимут: %.1f°", displayAngle));
        compassPanel.repaint();
    }

    private String directionName(double deg) {
        String[] dirs = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
        int idx = (int)((deg + 22.5) / 45) % 8;
        return dirs[idx];
    }

    private void loadConfig() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            // упрощённый парсинг (не используем JSON, для демонстрации)
            if (content.contains("offset")) {
                // упрощённо
            }
        } catch (IOException e) {}
    }

    private void saveConfig() {
        // упрощённо сохраняем
        try (PrintWriter pw = new PrintWriter(CONFIG_FILE)) {
            pw.println("{\"offset\":" + offset + ",\"calibrated\":" + calibrated + "}");
        } catch (IOException e) {}
    }

    class CompassPanel extends JPanel {
        private double angle = 0;
        private int size = 400;

        public void setAngle(double deg) {
            angle = deg;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int cx = w/2;
            int cy = h/2;
            int r = Math.min(w, h)/2 - 20;
            // Рисуем круг
            g2.drawOval(cx-r, cy-r, 2*r, 2*r);
            // Деления
            for (int deg = 0; deg < 360; deg += 30) {
                double rad = Math.toRadians(deg);
                int x1 = (int)(cx + (r-20) * Math.sin(rad));
                int y1 = (int)(cy - (r-20) * Math.cos(rad));
                int x2 = (int)(cx + r * Math.sin(rad));
                int y2 = (int)(cy - r * Math.cos(rad));
                g2.drawLine(x1, y1, x2, y2);
                if (deg % 90 == 0) {
                    String label;
                    if (deg == 0) label = "N";
                    else if (deg == 90) label = "E";
                    else if (deg == 180) label = "S";
                    else label = "W";
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    int lx = (int)(cx + (r-40) * Math.sin(rad));
                    int ly = (int)(cy - (r-40) * Math.cos(rad));
                    g2.drawString(label, lx-8, ly+5);
                }
            }
            // Стрелка
            double rad = Math.toRadians(angle);
            int len = 150;
            int x1 = (int)(cx + 20 * Math.sin(rad));
            int y1 = (int)(cy - 20 * Math.cos(rad));
            int x2 = (int)(cx + len * Math.sin(rad));
            int y2 = (int)(cy - len * Math.cos(rad));
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(6));
            g2.drawLine(x1, y1, x2, y2);
            // Южный конец
            int x3 = (int)(cx - 20 * Math.sin(rad));
            int y3 = (int)(cy + 20 * Math.cos(rad));
            int x4 = (int)(cx - 0.6 * len * Math.sin(rad));
            int y4 = (int)(cy + 0.6 * len * Math.cos(rad));
            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(x3, y3, x4, y4);
            // Наконечник
            g2.setColor(Color.RED);
            Polygon head = new Polygon();
            head.addPoint(x2, y2);
            double a1 = rad - 0.4;
            double a2 = rad + 0.4;
            head.addPoint((int)(x2 - 15*Math.cos(a1) + 15*Math.sin(a1)),
                          (int)(y2 - 15*Math.sin(a1) - 15*Math.cos(a1)));
            head.addPoint((int)(x2 - 15*Math.cos(a2) - 15*Math.sin(a2)),
                          (int)(y2 - 15*Math.sin(a2) + 15*Math.cos(a2)));
            g2.fillPolygon(head);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            new CompassJava().setVisible(true);
        });
    }
}
