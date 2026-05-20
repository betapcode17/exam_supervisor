import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import network.TCPServer;

public class MainServer {
    private JFrame frame;
    private JTextField portField;
    private JButton connectButton;
    private JTextArea logArea;
    private TCPServer server;
    private boolean isServerRunning = false;

    public MainServer() {
        initGUI();
    }

    private void initGUI() {
        frame = new JFrame("Exam Supervisor Assignment System - Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Connection Panel
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        connPanel.setBorder(BorderFactory.createTitledBorder("Kết nối Server"));
        
        connPanel.add(new JLabel("Port:"));
        portField = new JTextField("8888", 10);
        connPanel.add(portField);
        
        connectButton = new JButton("Khởi động Server");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleServer();
            }
        });
        connPanel.add(connectButton);

        // Log Panel
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(BorderFactory.createTitledBorder("Log xử lý"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainServer();
            }
        });
    }
}
