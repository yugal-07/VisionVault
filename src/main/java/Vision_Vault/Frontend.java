import javax.swing.*;
import java.awt.*;

public class Frontend {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(Frontend::buildWindow);
  }

  static void buildWindow() {
    JFrame frame = new JFrame("VisionVault");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(900, 650);
    frame.setLayout(new BorderLayout(10, 10));

    // ----- Top: app name -----
    JLabel title = new JLabel("VisionVault");
    title.setFont(new Font("SansSerif", Font.BOLD, 28));
    JLabel subtitle = new JLabel("Find yourself in your photos");

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
    header.add(title);
    header.add(subtitle);

    // ----- Left: buttons and status -----
    JButton uploadButton = new JButton("Upload Photos");
    JButton scanButton = new JButton("Scan My Face");
    JLabel photoCount = new JLabel("Photos uploaded: 0");
    JLabel faceStatus = new JLabel("Face scanned: No");

    uploadButton.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Photo upload: coming soon"));
    scanButton.addActionListener(e -> JOptionPane.showMessageDialog(frame, "Face scan: coming soon"));

    JPanel side = new JPanel();
    side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
    side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    side.add(uploadButton);
    side.add(Box.createVerticalStrut(10));
    side.add(scanButton);
    side.add(Box.createVerticalStrut(20));
    side.add(photoCount);
    side.add(Box.createVerticalStrut(5));
    side.add(faceStatus);

    // ----- Center: empty results grid -----
    JPanel grid = new JPanel(new GridLayout(3, 4, 10, 10));
    for (int i = 1; i <= 12; i++) {
      JLabel cell = new JLabel("Photo " + i, SwingConstants.CENTER);
      cell.setOpaque(true);
      cell.setBackground(new Color(230, 230, 230));
      cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
      grid.add(cell);
    }

    JScrollPane results = new JScrollPane(grid);
    results.setBorder(BorderFactory.createTitledBorder("Matching Photos"));

    // ----- Put it together -----
    frame.add(header, BorderLayout.NORTH);
    frame.add(side, BorderLayout.WEST);
    frame.add(results, BorderLayout.CENTER);
    frame.setVisible(true);
  }
}
