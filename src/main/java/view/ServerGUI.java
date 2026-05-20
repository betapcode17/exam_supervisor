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

import network.TCPServer;

public class ServerGUI {

	private static final Color BG_TOP = new Color(20, 31, 58);
	private static final Color BG_BOTTOM = new Color(34, 88, 163);
	private static final Color CARD_BG = new Color(255, 255, 255, 236);
	private static final Color CARD_BORDER = new Color(185, 199, 220);
	private static final Color TEXT_DARK = new Color(23, 33, 53);
	private static final Color MUTED_TEXT = new Color(89, 102, 126);
	private static final Color PRIMARY = new Color(34, 88, 163);
	private static final Color PRIMARY_DARK = new Color(24, 66, 122);
	private static final Color DANGER = new Color(192, 57, 43);
	private static final Color DANGER_DARK = new Color(150, 39, 28);
	private static final Color LOG_BG = new Color(17, 24, 39);
	private static final Color LOG_FG = new Color(232, 238, 246);

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
		frame.setSize(700, 560);
		frame.setLocationRelativeTo(null);
		frame.setMinimumSize(new java.awt.Dimension(660, 520));
		frame.setResizable(true);

		JPanel mainPanel = new GradientPanel(BG_TOP, BG_BOTTOM);
		mainPanel.setLayout(new BorderLayout(16, 16));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JPanel headerPanel = createHeaderPanel();
		JPanel bodyPanel = new JPanel(new BorderLayout(14, 14));
		bodyPanel.setOpaque(false);

		JPanel connPanel = createCardPanel("Kết nối Server", new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)));
		JPanel connInner = (JPanel) ((BorderLayout) connPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		connInner.setOpaque(false);

		JLabel portLabel = new JLabel("Port:");
		portLabel.setForeground(MUTED_TEXT);
		portLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		connInner.add(portLabel);
		portField = new JTextField("8888", 10);
		styleTextField(portField);
		connInner.add(portField);

		connectButton = createModernButton("Khởi động Server", PRIMARY, PRIMARY_DARK);
		connectButton.addActionListener(e -> toggleServer());
		connInner.add(connectButton);

		JPanel logPanel = createCardPanel("Log xử lý", new JPanel(new BorderLayout()));
		JPanel logInner = (JPanel) ((BorderLayout) logPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		logInner.setOpaque(false);

		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
		logArea.setBackground(LOG_BG);
		logArea.setForeground(LOG_FG);
		logArea.setCaretColor(Color.WHITE);

		JScrollPane scrollPane = new JScrollPane(logArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		logInner.add(scrollPane, BorderLayout.CENTER);

		bodyPanel.add(connPanel, BorderLayout.NORTH);
		bodyPanel.add(logPanel, BorderLayout.CENTER);

		mainPanel.add(headerPanel, BorderLayout.NORTH);
		mainPanel.add(bodyPanel, BorderLayout.CENTER);

		frame.add(mainPanel);
		frame.setVisible(true);

		redirectOutput();
	}

	private JPanel createHeaderPanel() {
		JPanel panel = new RoundedCardPanel(new BorderLayout(10, 10), new Color(24, 66, 122), new Color(53, 102, 179));
		panel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

		JLabel title = new JLabel("Server Control Center");
		title.setForeground(Color.WHITE);
		title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 23));

		JLabel subtitle = new JLabel("Monitor connections, logs and runtime processing in real time");
		subtitle.setForeground(new Color(226, 236, 248));
		subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

		JPanel textPanel = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
		textPanel.setOpaque(false);
		textPanel.add(title);
		textPanel.add(subtitle);

		JLabel statusPill = new JLabel("LIVE LOG");
		statusPill.setOpaque(true);
		statusPill.setBackground(new Color(34, 197, 94));
		statusPill.setForeground(Color.WHITE);
		statusPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
		statusPill.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(999, new Color(21, 128, 61)),
				BorderFactory.createEmptyBorder(6, 12, 6, 12)));

		panel.add(textPanel, BorderLayout.WEST);
		panel.add(statusPill, BorderLayout.EAST);
		return panel;
	}

	private JPanel createCardPanel(String titleText, JPanel inner) {
		JPanel card = new RoundedCardPanel(new BorderLayout(10, 10), CARD_BG, Color.WHITE);
		card.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(18, CARD_BORDER),
				BorderFactory.createEmptyBorder(12, 14, 14, 14)));

		JLabel title = new JLabel(titleText);
		title.setForeground(TEXT_DARK);
		title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 15));
		title.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 0));

		JPanel wrap = new JPanel(new BorderLayout(8, 8));
		wrap.setOpaque(false);
		wrap.add(title, BorderLayout.NORTH);
		wrap.add(inner, BorderLayout.CENTER);

		card.add(wrap, BorderLayout.CENTER);
		return card;
	}

	private JButton createModernButton(String text, Color base, Color hover) {
		JButton button = new JButton(text) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color baseColor = (Color) getClientProperty("modern.base");
				Color hoverColor = (Color) getClientProperty("modern.hover");
				Color fill = getModel().isRollover() && hoverColor != null ? hoverColor : baseColor;
				if (fill == null) {
					fill = PRIMARY;
				}
				if (getModel().isPressed()) {
					fill = fill.darker();
				}
				g2.setColor(fill);
				g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		button.putClientProperty("modern.base", base);
		button.putClientProperty("modern.hover", hover);
		button.setFocusPainted(false);
		button.setBorderPainted(false);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setForeground(Color.WHITE);
		button.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
		button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
		return button;
	}

	private void updateConnectButtonTheme(boolean running) {
		if (connectButton == null) {
			return;
		}
		connectButton.putClientProperty("modern.base", running ? DANGER : PRIMARY);
		connectButton.putClientProperty("modern.hover", running ? DANGER_DARK : PRIMARY_DARK);
		connectButton.setText(running ? "Dừng Server" : "Khởi động Server");
		connectButton.repaint();
	}

	private void styleTextField(JTextField field) {
		field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		field.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(12, new Color(198, 210, 230)),
				BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		field.setBackground(Color.WHITE);
		field.setForeground(TEXT_DARK);
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

			new Thread(() -> {
				try {
					server.start();
				} catch (java.io.IOException e) {
					logArea.append("Lỗi: " + e.getMessage() + "\n");
					logArea.append("[ERROR] Server thread stopped unexpectedly\n");
					isServerRunning = false;
					updateConnectButtonTheme(false);
					portField.setEditable(true);
				}
			}).start();

			isServerRunning = true;
			updateConnectButtonTheme(true);
			logArea.append("Server đã khởi động thành công!\n");

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(frame, "Port không hợp lệ", "Lỗi", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void stopServer() {
		if (server != null) {
			server.stop();
			isServerRunning = false;
			updateConnectButtonTheme(false);
			portField.setEditable(true);
			logArea.append("Server đã dừng\n");
		}
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
				write(new byte[] { (byte) b }, 0, 1);
			}
		});
		System.setOut(ps);
		System.setErr(ps);
	}

	private static class GradientPanel extends JPanel {
		private final Color top;
		private final Color bottom;

		GradientPanel(Color top, Color bottom) {
			this.top = top;
			this.bottom = bottom;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
			g2.setPaint(gp);
			g2.fillRect(0, 0, getWidth(), getHeight());
			g2.dispose();
		}
	}

	private static class RoundedCardPanel extends JPanel {
		private final Color fill;
		private final Color border;

		RoundedCardPanel(java.awt.LayoutManager layout, Color fill, Color border) {
			super(layout);
			this.fill = fill;
			this.border = border;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(fill);
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
			g2.setColor(border);
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 26, 26);
			g2.dispose();
			super.paintComponent(g);
		}
	}

	private static class RoundedBorder extends AbstractBorder {
		private final int radius;
		private final Color color;

		RoundedBorder(int radius, Color color) {
			this.radius = radius;
			this.color = color;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(color);
			g2.draw(new RoundRectangle2D.Double(x + 1, y + 1, width - 3, height - 3, radius, radius));
			g2.dispose();
		}

		@Override
		public java.awt.Insets getBorderInsets(Component c) {
			return new java.awt.Insets(4, 8, 4, 8);
		}

		@Override
		public java.awt.Insets getBorderInsets(Component c, java.awt.Insets insets) {
			insets.set(4, 8, 4, 8);
			return insets;
		}
	}

}
