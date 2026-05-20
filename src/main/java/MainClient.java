import view.ClientGUI;

public class MainClient {
    public static void main(String[] args) {
        System.out.println("=== Exam Supervisor Assignment System - Client ===");
        System.out.println("Starting client application...");
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI();
            }
        });
    }
}
