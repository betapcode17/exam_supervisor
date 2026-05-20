package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import network.TCPServer;


public class ServerGUI {
  private JFrame frame;
    private JTextField portField;
    private JButton connectButton;
    private JTextArea logArea;
    private TCPServer server;
    private boolean isServerRunning = false;

    public ServerGUI() {
        initGUI();
    }

    private void initGUI() {
        frame = new JFrame("Exam Supervisor Assignment System - Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Color palette and fonts (keeps layout unchanged)
        final Color COLOR_BG = new Color(245, 247, 250);
        final Color COLOR_PANEL = new Color(255, 255, 255);
        final Color COLOR_PRIMARY = new Color(34, 90, 190);
        final Color COLOR_ACCENT = new Color(72, 133, 237);
        final Color COLOR_TEXT = new Color(33, 37, 41);
        final Color COLOR_MUTED = new Color(120, 125, 130);
        final Color COLOR_BORDER = new Color(220, 225, 230);

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);
        Font mono = new Font("Consolas", Font.PLAIN, 12);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(COLOR_BG);

        // Connection Panel
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        connPanel.setBackground(COLOR_PANEL);
        TitledBorder connBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Kết nối Server");
        connBorder.setTitleColor(COLOR_PRIMARY.darker());
        connPanel.setBorder(connBorder);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(labelFont);
        portLabel.setForeground(COLOR_TEXT);
        connPanel.add(portLabel);

        portField = new JTextField("8888", 10);
        portField.setFont(labelFont);
        portField.setBackground(new Color(250, 251, 252));
        portField.setForeground(COLOR_TEXT);
        portField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true), BorderFactory.createEmptyBorder(4,6,4,6)));
        connPanel.add(portField);

        connectButton = new JButton("Khởi động Server");
        styleButton(connectButton, COLOR_PRIMARY, COLOR_ACCENT, COLOR_PANEL, COLOR_TEXT);
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleServer();
            }
        });
        connPanel.add(connectButton);

        // Log Panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(COLOR_PANEL);
        TitledBorder logBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(COLOR_BORDER), "Log xử lý");
        logBorder.setTitleColor(COLOR_PRIMARY.darker());
        logPanel.setBorder(logBorder);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(mono);
        logArea.setBackground(new Color(250,250,252));
        logArea.setForeground(COLOR_TEXT);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        logPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(connPanel, BorderLayout.NORTH);
        mainPanel.add(logPanel, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Redirect System.out and System.err to log area
        redirectOutput();
    }

    private void toggleServer() {
        if (!isServerRunning) {
            startServer();
        } else {
            stopServer();
        }
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            portField.setEditable(false);
            
            logArea.append("=== Khởi động Server ===\n");
            logArea.append("Port: " + port + "\n");
            
            server = new TCPServer(port);
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        server.start();
                    } catch (Exception e) {
                        logArea.append("Lỗi: " + e.getMessage() + "\n");
                        e.printStackTrace();
                        isServerRunning = false;
                        connectButton.setText("Khởi động Server");
                        portField.setEditable(true);
                    }
                }
            }).start();
            
            isServerRunning = true;
            connectButton.setText("Dừng Server");
            logArea.append("Server đã khởi động thành công!\n");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Port không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
        isServerRunning = false;
        connectButton.setText("Khởi động Server");
        portField.setEditable(true);
        logArea.append("Server đã dừng\n");
    }

    private void redirectOutput() {
        PrintStream ps = new PrintStream(new java.io.OutputStream() {
            @Override
            public void write(byte[] b, int off, int len) {
                String s = new String(b, off, len);
                SwingUtilities.invokeLater(() -> logArea.append(s));
            }

            @Override
            public void write(int b) {
                write(new byte[]{(byte) b}, 0, 1);
            }
        });
        System.setOut(ps);
        System.setErr(ps);
    }

    // Apply consistent modern styling to buttons
    private void styleButton(JButton btn, Color primary, Color accent, Color bg, Color text) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(primary);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary.darker()), BorderFactory.createEmptyBorder(6,12,6,12)));
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(accent);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(primary);
            }
        });
    }

    // Rounded border utility (used if needed for future enhancement)
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        public RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200,200,200));
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }

}
