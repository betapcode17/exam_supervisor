package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import network.TCPClient;
import util.ExcelReader;

public class ClientGUI extends JFrame {
    // Accessible color palette (high contrast)
    private static final Color COLOR_BG = new Color(250, 250, 250);
    private static final Color COLOR_PANEL = new Color(245, 245, 245);
    private static final Color COLOR_PRIMARY = new Color(0, 102, 204); // primary buttons / headers
    private static final Color COLOR_ACCENT = new Color(0, 153, 51);  // success / accent
    private static final Color COLOR_ERROR = new Color(220, 53, 69);   // error / danger
    private static final Color COLOR_TEXT = new Color(33, 33, 33);     // primary text (dark)
    private static final Color COLOR_MUTED = new Color(117, 117, 117);
    private static final Color COLOR_BORDER = new Color(200, 200, 200);
    private JTextField serverUrlField;
    private JTextField serverPortField;
    private JButton connectButton;
    private JLabel connectionStatusLabel;
    
    private JTextField filePathField;
    private JButton selectFileButton;
    private JButton sendFileButton;
    
    private JTextField numberOfRoomsField;
    private JTextField numberOfInvigilatorsField;
    private JButton generateButton;
    private JButton exportButton;
    
    private JTextArea resultArea;
    private JProgressBar progressBar;
    private JTabbedPane tabbedPane;
    // make these available to other methods so we can populate them
    private JTable inputSheet0Table;
    private JTable inputSheet1Table;
    private JTable resultSheet0Table;
    private JTable resultSheet1Table;
    // Pagination state for result tables (each sheet in workbook = one page)
    private java.util.List<java.util.List<String>> result0Pages = new java.util.ArrayList<>();
    private int result0PageIndex = 0;
    private JButton prevResult0Btn;
    private JButton nextResult0Btn;
    private JLabel result0PageLabel;

    private java.util.List<java.util.List<String>> result1Pages = new java.util.ArrayList<>();
    private int result1PageIndex = 0;
    private JButton prevResult1Btn;
    private JButton nextResult1Btn;
    private JLabel result1PageLabel;
    
    private String selectedFilePath;
    private TCPClient tcpClient;
    private boolean isConnected = false;
    private boolean isFileUploaded = false;
    private int currentShift = 0;  // Auto-increment shift
    private final Consumer<String> uiLog = message -> SwingUtilities.invokeLater(() -> {
        resultArea.append(message.endsWith("\n") ? message : message + "\n");
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    });

    public ClientGUI() {
        setTitle("Exam Supervisor Assignment System - Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        applyModernTheme();

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Connection Panel
        JPanel connectionPanel = createConnectionPanel();
        
        // Input Panel
        JPanel inputPanel = createInputPanel();
        
        // Result Panel
        JPanel resultPanel = createResultPanel();
        
        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        
        // Create intermediate panel for input and result
        JPanel intermediatePanel = new JPanel(new BorderLayout(10, 10));
        intermediatePanel.add(inputPanel, BorderLayout.NORTH);
        intermediatePanel.add(resultPanel, BorderLayout.CENTER);
        
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        mainPanel.add(intermediatePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        this.add(mainPanel);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        TitledBorder tb = BorderFactory.createTitledBorder("Kết nối Server");
        tb.setTitleFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        tb.setTitleColor(COLOR_TEXT);
        panel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(COLOR_BORDER), tb));
        panel.setBackground(COLOR_BG);
        
        JLabel urlLbl = new JLabel("URL Server:");
        urlLbl.setForeground(COLOR_TEXT);
        panel.add(urlLbl);
        serverUrlField = new JTextField("localhost", 12);
        serverUrlField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(serverUrlField);
        
        JLabel portLbl = new JLabel("Port:");
        portLbl.setForeground(COLOR_TEXT);
        panel.add(portLbl);
        serverPortField = new JTextField("8888", 8);
        serverPortField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(serverPortField);
        
        connectButton = new JButton("Kết nối");
        connectButton.addActionListener(e -> connectToServer());
        styleButton(connectButton, COLOR_PRIMARY);
        panel.add(connectButton);
        
        connectionStatusLabel = new JLabel("Chưa kết nối");
        connectionStatusLabel.setForeground(COLOR_MUTED);
        connectionStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(connectionStatusLabel);
        
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        TitledBorder tb = BorderFactory.createTitledBorder("Nhập thông tin");
        tb.setTitleFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        tb.setTitleColor(COLOR_TEXT);
        panel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(COLOR_BORDER), tb));
        panel.setBackground(COLOR_BG);
        
        // File selection panel
        JPanel filePanel = new JPanel(new BorderLayout(6, 6));
        filePanel.setBackground(COLOR_BG);
        JLabel fileLbl = new JLabel("Chọn file Excel:");
        fileLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileLbl.setForeground(COLOR_TEXT);
        filePanel.add(fileLbl, BorderLayout.WEST);
        
        JPanel fileBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filePathField = new JTextField(20);
        filePathField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filePathField.setEditable(false);
        selectFileButton = new JButton("Chọn file");
        selectFileButton.addActionListener(e -> selectFile());
        selectFileButton.setEnabled(false);
        
        sendFileButton = new JButton("Gửi file");
        sendFileButton.addActionListener(e -> uploadFile());
        sendFileButton.setEnabled(false);
        styleButton(selectFileButton, COLOR_ACCENT);
        styleButton(sendFileButton, COLOR_PRIMARY);

        fileBtnPanel.add(filePathField);
        fileBtnPanel.add(selectFileButton);
        fileBtnPanel.add(sendFileButton);
        filePanel.add(fileBtnPanel, BorderLayout.CENTER);
        panel.add(filePanel);
        
        // Number of rooms
        JPanel roomsPanel = new JPanel(new BorderLayout(5, 5));
        JLabel roomsLbl = new JLabel("Số phòng cần sử dụng (n):");
        roomsLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomsLbl.setForeground(COLOR_TEXT);
        roomsPanel.add(roomsLbl, BorderLayout.WEST);
        numberOfRoomsField = new JTextField("10", 15);
        numberOfRoomsField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        numberOfRoomsField.setEnabled(false);
        roomsPanel.add(numberOfRoomsField, BorderLayout.CENTER);
        panel.add(roomsPanel);
        
        // Number of invigilators
        JPanel invigilatorPanel = new JPanel(new BorderLayout(5, 5));
        JLabel invLbl = new JLabel("Số cán bộ cần sử dụng (m):");
        invLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        invLbl.setForeground(COLOR_TEXT);
        invigilatorPanel.add(invLbl, BorderLayout.WEST);
        numberOfInvigilatorsField = new JTextField("20", 15);
        numberOfInvigilatorsField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        numberOfInvigilatorsField.setEnabled(false);
        invigilatorPanel.add(numberOfInvigilatorsField, BorderLayout.CENTER);
        panel.add(invigilatorPanel);
        
        // Progress bar
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        JLabel pLbl = new JLabel("Tiến độ:");
        pLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pLbl.setForeground(COLOR_TEXT);
        progressPanel.add(pLbl, BorderLayout.WEST);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(COLOR_PRIMARY);
        progressPanel.add(progressBar, BorderLayout.CENTER);
        panel.add(progressPanel);
        
        return panel;
    }

    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        TitledBorder tb = BorderFactory.createTitledBorder("Kết quả & Log");
        tb.setTitleFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        tb.setTitleColor(COLOR_TEXT);
        panel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(COLOR_BORDER), tb));
        panel.setBackground(COLOR_PANEL);
        
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        resultArea.setForeground(COLOR_TEXT);
        resultArea.setBackground(Color.WHITE);
        
        tabbedPane = new JTabbedPane();

        JScrollPane logScroll = new JScrollPane(resultArea);
        tabbedPane.addTab("Log", logScroll);

        // placeholder tabs for input sheets and result sheets (use tables for proper tabular view)
        inputSheet0Table = new JTable();
        inputSheet1Table = new JTable();
        resultSheet0Table = new JTable();
        resultSheet1Table = new JTable();
        Font tableFont = new Font("Segoe UI", Font.PLAIN, 13);
        inputSheet0Table.setFont(tableFont);
        inputSheet1Table.setFont(tableFont);
        resultSheet0Table.setFont(tableFont);
        resultSheet1Table.setFont(tableFont);
        inputSheet0Table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        inputSheet1Table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resultSheet0Table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        resultSheet1Table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        // header contrast
        inputSheet0Table.getTableHeader().setBackground(COLOR_PRIMARY);
        inputSheet0Table.getTableHeader().setForeground(Color.WHITE);
        inputSheet1Table.getTableHeader().setBackground(COLOR_PRIMARY);
        inputSheet1Table.getTableHeader().setForeground(Color.WHITE);
        resultSheet0Table.getTableHeader().setBackground(COLOR_PRIMARY);
        resultSheet0Table.getTableHeader().setForeground(Color.WHITE);
        resultSheet1Table.getTableHeader().setBackground(COLOR_PRIMARY);
        resultSheet1Table.getTableHeader().setForeground(Color.WHITE);
        inputSheet0Table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        inputSheet1Table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultSheet0Table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultSheet1Table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tabbedPane.addTab("Danh sách cán bộ", new JScrollPane(inputSheet0Table));
        tabbedPane.addTab("Danh sách phòng thi", new JScrollPane(inputSheet1Table));
        // Create panels for result tables with pagination controls
        JPanel result0Panel = new JPanel(new BorderLayout());
        result0Panel.setBackground(COLOR_PANEL);
        JScrollPane res0Scroll = new JScrollPane(resultSheet0Table);
        result0Panel.add(res0Scroll, BorderLayout.CENTER);
        JPanel res0Nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        prevResult0Btn = new JButton("⟨ Trước");
        nextResult0Btn = new JButton("Sau ⟩");
        styleButton(prevResult0Btn, COLOR_MUTED);
        styleButton(nextResult0Btn, COLOR_MUTED);
        result0PageLabel = new JLabel("Trang 0/0");
        prevResult0Btn.addActionListener(e -> {
            if (result0PageIndex > 0) {
                result0PageIndex--; showResult0Page();
            }
        });
        nextResult0Btn.addActionListener(e -> {
            if (result0PageIndex < result0Pages.size() - 1) {
                result0PageIndex++; showResult0Page();
            }
        });
        res0Nav.add(prevResult0Btn);
        res0Nav.add(result0PageLabel);
        res0Nav.add(nextResult0Btn);
        result0Panel.add(res0Nav, BorderLayout.SOUTH);

        JPanel result1Panel = new JPanel(new BorderLayout());
        result1Panel.setBackground(COLOR_PANEL);
        JScrollPane res1Scroll = new JScrollPane(resultSheet1Table);
        result1Panel.add(res1Scroll, BorderLayout.CENTER);
        JPanel res1Nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        prevResult1Btn = new JButton("⟨ Trước");
        nextResult1Btn = new JButton("Sau ⟩");
        styleButton(prevResult1Btn, COLOR_MUTED);
        styleButton(nextResult1Btn, COLOR_MUTED);
        result1PageLabel = new JLabel("Trang 0/0");
        prevResult1Btn.addActionListener(e -> {
            if (result1PageIndex > 0) { result1PageIndex--; showResult1Page(); }
        });
        nextResult1Btn.addActionListener(e -> {
            if (result1PageIndex < result1Pages.size() - 1) { result1PageIndex++; showResult1Page(); }
        });
        res1Nav.add(prevResult1Btn);
        res1Nav.add(result1PageLabel);
        res1Nav.add(nextResult1Btn);
        result1Panel.add(res1Nav, BorderLayout.SOUTH);

        tabbedPane.addTab("Danh sách giám thị", result0Panel);
        tabbedPane.addTab("Danh sách giám sát", result1Panel);

        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.setBackground(COLOR_BG);

        generateButton = new JButton("Phân công");
        generateButton.setEnabled(false);
        generateButton.addActionListener(e -> generateAssignment());
        styleButton(generateButton, COLOR_PRIMARY);

        exportButton = new JButton("Xuất kết quả");
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportResult());
        styleButton(exportButton, COLOR_ACCENT);

        JButton exitButton = new JButton("Thoát");
        styleButton(exitButton, COLOR_MUTED);
        exitButton.addActionListener(e -> System.exit(0));
        
        panel.add(generateButton);
        panel.add(exportButton);
        panel.add(exitButton);
        
        return panel;
    }

    // Apply a few lightweight UI defaults for a cleaner, modern look
    private void applyModernTheme() {
        Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIManager.put("Label.font", uiFont);
        UIManager.put("Label.foreground", COLOR_TEXT);
        UIManager.put("Button.font", uiFont);
        UIManager.put("TextField.font", uiFont);
        UIManager.put("TextArea.font", uiFont);
        UIManager.put("Table.font", uiFont);
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Panel.background", COLOR_BG);
    }

    private void styleButton(JButton b, Color bg) {
        b.setFocusPainted(false);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        // hover effect
        final Color base = bg;
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(base.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(base);
            }
        });
    }

    private void connectToServer() {
        if (isConnected) {
            disconnectFromServer();
            return;
        }
        
        try {
            String url = serverUrlField.getText().trim();
            int port = Integer.parseInt(serverPortField.getText().trim());
            
            if (url.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập URL Server", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            uiLog.accept("Đang kết nối tới " + url + ":" + port + "...");
            
            tcpClient = new TCPClient(url, port, uiLog);
            tcpClient.connect();
            
            isConnected = true;
            isFileUploaded = false;
            currentShift = 0;
            
            connectionStatusLabel.setText("Đã kết nối");
            connectionStatusLabel.setForeground(COLOR_ACCENT);
            connectButton.setText("Ngắt kết nối");
            
            serverUrlField.setEditable(false);
            serverPortField.setEditable(false);
            
            selectFileButton.setEnabled(true);
            sendFileButton.setEnabled(false);
            generateButton.setEnabled(false);
            
            uiLog.accept("Kết nối thành công!");
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            uiLog.accept("Lỗi kết nối: " + ex.getMessage());
        }
    }

    private void disconnectFromServer() {
        if (tcpClient != null) {
            tcpClient.disconnect();
        }
        
        isConnected = false;
        isFileUploaded = false;
        currentShift = 0;
        
        connectionStatusLabel.setText("Chưa kết nối");
        connectionStatusLabel.setForeground(COLOR_ERROR);
        connectButton.setText("Kết nối");
        
        serverUrlField.setEditable(true);
        serverPortField.setEditable(true);
        
        selectFileButton.setEnabled(false);
        sendFileButton.setEnabled(false);
        generateButton.setEnabled(false);
        
        uiLog.accept("Đã ngắt kết nối");
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            selectedFilePath = file.getAbsolutePath();
            filePathField.setText(selectedFilePath);
            sendFileButton.setEnabled(true);
            uiLog.accept("Đã chọn file: " + selectedFilePath);
            // Load first two sheets into the input tab areas for preview
            try {
                ExcelReader reader = new ExcelReader(selectedFilePath);
                List<String> s0 = reader.readSheetAsText(0);
                List<String> s1 = reader.readSheetAsText(1);
                populateTableFromLines(inputSheet0Table, s0);
                populateTableFromLines(inputSheet1Table, s1);
            } catch (Exception ex) {
                uiLog.accept("Lỗi đọc file để hiển thị preview: " + ex.getMessage());
            }
        }
    }

    private void uploadFile() {
        if (selectedFilePath == null || selectedFilePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn file Excel", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        File file = new File(selectedFilePath);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File không tồn tại: " + selectedFilePath, "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (file.length() == 0) {
            JOptionPane.showMessageDialog(this, "File rỗng, vui lòng chọn file khác", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!isConnected || tcpClient == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng kết nối tới server trước", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Disable buttons during upload
        selectFileButton.setEnabled(false);
        sendFileButton.setEnabled(false);
        generateButton.setEnabled(false);
        numberOfRoomsField.setEnabled(false);
        numberOfInvigilatorsField.setEnabled(false);
        
        new SwingWorker<Boolean, String>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    publish("Đang gửi file (" + (file.length() / 1024) + "KB)...\n");
                    long startTime = System.currentTimeMillis();
                    
                    boolean success = tcpClient.loadFile(selectedFilePath);
                    
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;
                    
                    if (success) {
                        publish("✓ Gửi file thành công! (" + duration + "ms)\n");
                        publish("✓ Dữ liệu đã được lưu vào database\n");
                        publish("✓ Bạn có thể nhập số phòng (n) và số cán bộ (m) để tiến hành phân công\n");
                        progressBar.setValue(100);
                        return true;
                    } else {
                        publish("✗ Gửi file thất bại!\n");
                        publish("✗ Vui lòng kiểm tra file và thử lại\n");
                        progressBar.setValue(0);
                        return false;
                    }
                } catch (Exception ex) {
                    publish("✗ Lỗi: " + ex.getMessage() + "\n");
                    progressBar.setValue(0);
                    return false;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    uiLog.accept(chunk.trim());
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        isFileUploaded = true;
                        numberOfRoomsField.setEnabled(true);
                        numberOfInvigilatorsField.setEnabled(true);
                        generateButton.setEnabled(true);
                            // allow re-upload: keep select/send enabled so user can send a new file
                            selectFileButton.setEnabled(true);
                            sendFileButton.setEnabled(true);
                    } else {
                        // Enable selection buttons if upload failed
                        selectFileButton.setEnabled(true);
                        sendFileButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    uiLog.accept("Lỗi khi gửi file: " + e.getMessage());
                    selectFileButton.setEnabled(true);
                    sendFileButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void generateAssignment() {
        if (!isFileUploaded) {
            JOptionPane.showMessageDialog(this, "Dữ liệu chưa được lưu vào database!\nVui lòng gửi file Excel trước", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!isConnected || tcpClient == null) {
            JOptionPane.showMessageDialog(this, "Kết nối server bị mất, vui lòng kết nối lại", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            String roomsStr = numberOfRoomsField.getText().trim();
            String invigilatorsStr = numberOfInvigilatorsField.getText().trim();
            
            if (roomsStr.isEmpty() || invigilatorsStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số phòng (n) và số cán bộ (m)", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int numberOfRooms = Integer.parseInt(roomsStr);
            int numberOfInvigilators = Integer.parseInt(invigilatorsStr);
            
            if (numberOfRooms <= 0) {
                JOptionPane.showMessageDialog(this, "Số phòng (n) phải lớn hơn 0", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (numberOfInvigilators <= 0) {
                JOptionPane.showMessageDialog(this, "Số cán bộ (m) phải lớn hơn 0", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validate: numberOfInvigilators > 2*numberOfRooms + 1
            int minRequired = 2 * numberOfRooms + 1;
            if (numberOfInvigilators <= minRequired) {
                JOptionPane.showMessageDialog(this, 
                    "Số cán bộ (m) phải lớn hơn " + minRequired + " (2n+1)\n" +
                    "Hiện tại: m=" + numberOfInvigilators + ", n=" + numberOfRooms + "\n" +
                    "Yêu cầu: m > " + minRequired, 
                    "Lỗi - Dữ liệu không hợp lệ", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            currentShift++;  // Auto-increment shift
            
            // Disable buttons during assignment
            generateButton.setEnabled(false);
            exportButton.setEnabled(false);
            numberOfRoomsField.setEnabled(false);
            numberOfInvigilatorsField.setEnabled(false);
            
            uiLog.accept("============================================================");
            uiLog.accept("PHÂN CÔNG CÁ " + currentShift);
            uiLog.accept("============================================================");
            uiLog.accept("Số phòng (n): " + numberOfRooms);
            uiLog.accept("Số cán bộ (m): " + numberOfInvigilators);
            uiLog.accept("Đang xử lý...");
            progressBar.setValue(50);
            
            new SwingWorker<Boolean, String>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        long startTime = System.currentTimeMillis();
                        
                        boolean success = tcpClient.generateAssignment(selectedFilePath, numberOfRooms,
                                                                      numberOfInvigilators, currentShift,
                                                                      msg -> publish(msg));
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        
                        if (success) {
                            publish("✓ Phân công thành công! (" + duration + "ms)");
                            publish("✓ Đã phân " + tcpClient.getLastInvigilatorCount() + " giáo viên cho bên giám thị");
                            publish("✓ Đã phân " + tcpClient.getLastSupervisorCount() + " giáo viên cho bên giám sát");
                            publish("✓ Kết quả đã được xuất tại thư mục 'output/<timestamp>'");
                            progressBar.setValue(100);
                            return true;
                        } else {
                            publish("✗ Phân công thất bại!");
                            progressBar.setValue(0);
                            currentShift--;  // Rollback shift on failure
                            return false;
                        }
                    } catch (Exception ex) {
                        publish("✗ Lỗi: " + ex.getMessage());
                        progressBar.setValue(0);
                        currentShift--;  // Rollback shift on error
                        return false;
                    }
                }
                
                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String chunk : chunks) {
                        uiLog.accept(chunk);
                    }
                }
                
                @Override
                protected void done() {
                    try {
                        if (get()) {
                            exportButton.setEnabled(true);
                            // try to load generated files into result tabs
                            try {
                                String session = tcpClient.getLastOutputSessionDir();
                                if (session != null && !session.isEmpty()) {
                                    File outDir = new File("output", session);
                                    if (outDir.exists() && outDir.isDirectory()) {
                                        File[] files = outDir.listFiles();
                                        if (files != null) {
                                                        // Clear previous pages
                                                        result0Pages.clear();
                                                        result1Pages.clear();
                                                        for (File f : files) {
                                                            String name = f.getName().toLowerCase();
                                                            try {
                                                                ExcelReader r = new ExcelReader(f.getAbsolutePath());
                                                                int sheets = r.getNumberOfSheets();
                                                                for (int si = 0; si < sheets; si++) {
                                                                    List<String> lines = r.readSheetAsText(si);
                                                                    if (name.contains("phancong") || name.contains("giamthi") || name.contains("giam-thi")) {
                                                                        result0Pages.add(lines);
                                                                    } else if (name.contains("giamsat") || name.contains("giam-sat") || name.contains("giam_sat")) {
                                                                        result1Pages.add(lines);
                                                                    } else {
                                                                        // default to giám thị if unknown
                                                                        result0Pages.add(lines);
                                                                    }
                                                                }
                                                                uiLog.accept("Nạp file kết quả: " + f.getName() + " (" + sheets + " sheet(s))");
                                                            } catch (Exception ex) {
                                                                uiLog.accept("Lỗi đọc file kết quả " + f.getName() + ": " + ex.getMessage());
                                                            }
                                                        }
                                                        // show first pages if available
                                                        result0PageIndex = 0;
                                                        result1PageIndex = 0;
                                                        showResult0Page();
                                                        showResult1Page();
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                uiLog.accept("Lỗi khi nạp file kết quả: " + ex.getMessage());
                            }
                            // allow running another assignment without re-uploading
                            generateButton.setEnabled(true);
                        } else {
                            generateButton.setEnabled(true);
                        }
                    } catch (Exception e) {
                        uiLog.accept("Lỗi khi hoàn tất phân công: " + e.getMessage());
                        generateButton.setEnabled(true);
                    } finally {
                        numberOfRoomsField.setEnabled(true);
                        numberOfInvigilatorsField.setEnabled(true);
                    }
                }
            }.execute();
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập số nguyên hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportResult() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File dir = fileChooser.getSelectedFile();
            uiLog.accept("Kết quả đã được lưu tại: " + dir.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI());
    }

    // Helper: populate a JTable from tab-separated lines returned by ExcelReader.readSheetAsText
    private void populateTableFromLines(JTable table, List<String> lines) {
        if (lines == null) {
            table.setModel(new DefaultTableModel());
            return;
        }
        int maxCols = 0;
        for (String l : lines) {
            if (l == null) continue;
            String[] parts = l.split("\t", -1);
            if (parts.length > maxCols) maxCols = parts.length;
        }
        if (maxCols == 0) {
            table.setModel(new DefaultTableModel());
            return;
        }
        // Use first row as header if it contains non-empty values
        String[] header = new String[maxCols];
        String first = lines.size() > 0 ? lines.get(0) : null;
        boolean hasHeader = false;
        if (first != null && !first.trim().isEmpty()) {
            String[] hparts = first.split("\t", -1);
            boolean anyNonEmpty = false;
            for (String s : hparts) if (s != null && !s.trim().isEmpty()) anyNonEmpty = true;
            if (anyNonEmpty) {
                hasHeader = true;
                for (int i = 0; i < maxCols; i++) header[i] = i < hparts.length ? hparts[i] : "";
            }
        }
        if (!hasHeader) {
            for (int i = 0; i < maxCols; i++) header[i] = "C" + (i+1);
        }

        DefaultTableModel model = new DefaultTableModel(header, 0);
        int startRow = hasHeader ? 1 : 0;
        for (int r = startRow; r < lines.size(); r++) {
            String line = lines.get(r);
            String[] parts = line.split("\t", -1);
            Object[] row = new Object[maxCols];
            for (int c = 0; c < maxCols; c++) row[c] = c < parts.length ? parts[c] : "";
            model.addRow(row);
        }
        table.setModel(model);
        table.setFillsViewportHeight(true);
        // adjust columns to fill available viewport width
        adjustTableToFill(table);
    }

    private void adjustTableToFill(JTable table) {
        SwingUtilities.invokeLater(() -> {
            try {
                java.awt.Component parent = table.getParent();
                int viewportWidth = 0;
                if (parent instanceof javax.swing.JViewport) {
                    viewportWidth = parent.getWidth();
                } else {
                    viewportWidth = table.getWidth();
                }
                int colCount = table.getColumnModel().getColumnCount();
                if (colCount <= 0 || viewportWidth <= 0) return;
                int each = Math.max(80, viewportWidth / colCount);
                for (int i = 0; i < colCount; i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(each);
                }
            } catch (Exception ex) {
                // ignore sizing errors
            }
        });
    }

    // Display helpers for paginated result tables
    private void showResult0Page() {
        if (result0Pages == null || result0Pages.isEmpty()) {
            populateTableFromLines(resultSheet0Table, null);
            result0PageLabel.setText("Trang 0/0");
            prevResult0Btn.setEnabled(false);
            nextResult0Btn.setEnabled(false);
            return;
        }
        if (result0PageIndex < 0) result0PageIndex = 0;
        if (result0PageIndex >= result0Pages.size()) result0PageIndex = result0Pages.size() - 1;
        List<String> page = result0Pages.get(result0PageIndex);
        populateTableFromLines(resultSheet0Table, page);
        result0PageLabel.setText("Trang " + (result0PageIndex + 1) + "/" + result0Pages.size());
        prevResult0Btn.setEnabled(result0PageIndex > 0);
        nextResult0Btn.setEnabled(result0PageIndex < result0Pages.size() - 1);
    }

    private void showResult1Page() {
        if (result1Pages == null || result1Pages.isEmpty()) {
            populateTableFromLines(resultSheet1Table, null);
            result1PageLabel.setText("Trang 0/0");
            prevResult1Btn.setEnabled(false);
            nextResult1Btn.setEnabled(false);
            return;
        }
        if (result1PageIndex < 0) result1PageIndex = 0;
        if (result1PageIndex >= result1Pages.size()) result1PageIndex = result1Pages.size() - 1;
        List<String> page = result1Pages.get(result1PageIndex);
        populateTableFromLines(resultSheet1Table, page);
        result1PageLabel.setText("Trang " + (result1PageIndex + 1) + "/" + result1Pages.size());
        prevResult1Btn.setEnabled(result1PageIndex > 0);
        nextResult1Btn.setEnabled(result1PageIndex < result1Pages.size() - 1);
    }
}
