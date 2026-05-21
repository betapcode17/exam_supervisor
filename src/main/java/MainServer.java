import javax.swing.SwingUtilities;

import view.ServerGUI;


public class MainServer {
  
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ServerGUI::new);
    }
}
