package com.visionvault.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class VisionVaultGUI {
    
    private static final String API_BASE = "http://localhost:8080/api";
    
    private JFrame frame;
    private JLabel photoCountLabel;
    private JLabel faceStatusLabel;
    private JLabel indexProgressLabel;
    private JButton uploadButton;
    private JButton indexButton;
    private JButton scanButton;
    private JPanel resultsGrid;
    
    private int uploadedCount = 0;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VisionVaultGUI().buildWindow());
    }
    
    void buildWindow() {
        frame = new JFrame("VisionVault");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout(10, 10));
        
        // Top: app name
        JLabel title = new JLabel("VisionVault");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        JLabel subtitle = new JLabel("Find yourself in your photos");
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        header.add(title);
        header.add(subtitle);
        
        // Left: buttons and status
        uploadButton = new JButton("Upload Photos");
        uploadButton.setPreferredSize(new Dimension(150, 35));
        
        indexButton = new JButton("Start Indexing");
        indexButton.setPreferredSize(new Dimension(150, 35));
        indexButton.setEnabled(false);
        
        scanButton = new JButton("Scan My Face");
        scanButton.setPreferredSize(new Dimension(150, 35));
        scanButton.setEnabled(false);
        
        photoCountLabel = new JLabel("Photos uploaded: 0");
        faceStatusLabel = new JLabel("Indexing: Not started");
        indexProgressLabel = new JLabel("Progress: -");
        
        uploadButton.addActionListener(e -> uploadPhotos());
        indexButton.addActionListener(e -> startIndexing());
        scanButton.addActionListener(e -> scanFace());
        
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        side.add(uploadButton);
        side.add(Box.createVerticalStrut(10));
        side.add(indexButton);
        side.add(Box.createVerticalStrut(10));
        side.add(scanButton);
        side.add(Box.createVerticalStrut(20));
        side.add(photoCountLabel);
        side.add(Box.createVerticalStrut(5));
        side.add(faceStatusLabel);
        side.add(Box.createVerticalStrut(5));
        side.add(indexProgressLabel);
        
        // Center: results grid
        resultsGrid = new JPanel(new GridLayout(3, 4, 10, 10));
        resultsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (int i = 1; i <= 12; i++) {
            JLabel cell = new JLabel("Photo " + i, SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setBackground(new Color(230, 230, 230));
            cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            resultsGrid.add(cell);
        }
        
        JScrollPane resultsScrollPane = new JScrollPane(resultsGrid);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("Matching Photos"));
        
        // Put it together
        frame.add(header, BorderLayout.NORTH);
        frame.add(side, BorderLayout.WEST);
        frame.add(resultsScrollPane, BorderLayout.CENTER);
        
        // Check server connection on startup
        executor.execute(this::checkServerConnection);
        
        frame.setVisible(true);
    }
    
    private void checkServerConnection() {
        try {
            URL url = new URL(API_BASE + "/photos/count");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            
            if (conn.getResponseCode() == 200) {
                SwingUtilities.invokeLater(() -> {
                    photoCountLabel.setText("Server: Connected");
                    updatePhotoCount();
                });
            } else {
                SwingUtilities.invokeLater(() -> 
                    photoCountLabel.setText("Server: Error " + conn.getResponseCode()));
            }
            conn.disconnect();
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                photoCountLabel.setText("Server: Not running");
                JOptionPane.showMessageDialog(frame, 
                    "Cannot connect to backend server at " + API_BASE + "\n" +
                    "Please start the Spring Boot application first:\n" +
                    "mvn spring-boot:run",
                    "Connection Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    private void uploadPhotos() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", 
            "jpg", "jpeg", "png", "gif", "bmp"));
        
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;
        
        File[] files = chooser.getSelectedFiles();
        if (files.length == 0) return;
        
        uploadButton.setEnabled(false);
        photoCountLabel.setText("Uploading " + files.length + " photos...");
        
        executor.execute(() -> {
            try {
                String response = uploadFiles(files);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, 
                        "Upload complete!\n" + response,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    uploadButton.setEnabled(true);
                    updatePhotoCount();
                    indexButton.setEnabled(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "Upload failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    uploadButton.setEnabled(true);
                });
            }
        });
    }
    
    private String uploadFiles(File[] files) throws IOException {
        String boundary = "----Boundary" + System.currentTimeMillis();
        URL url = new URL(API_BASE + "/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {
            
            for (File file : files) {
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"files\"; filename=\"")
                      .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: ").append(getMimeType(file.getName())).append("\r\n\r\n");
                writer.flush();
                
                Files.copy(file.toPath(), os);
                os.flush();
                
                writer.append("\r\n");
            }
            
            writer.append("--").append(boundary).append("--\r\n");
        }
        
        String response = readResponse(conn);
        conn.disconnect();
        return response;
    }
    
    private void updatePhotoCount() {
        executor.execute(() -> {
            try {
                URL url = new URL(API_BASE + "/photos/count");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                if (conn.getResponseCode() == 200) {
                    String count = readResponse(conn);
                    SwingUtilities.invokeLater(() -> {
                        photoCountLabel.setText("Photos uploaded: " + count);
                        uploadedCount = Integer.parseInt(count.trim());
                        scanButton.setEnabled(uploadedCount > 0);
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void startIndexing() {
        indexButton.setEnabled(false);
        faceStatusLabel.setText("Indexing: Starting...");
        
        executor.execute(() -> {
            try {
                URL url = new URL(API_BASE + "/index/start");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                String response = readResponse(conn);
                conn.disconnect();
                
                SwingUtilities.invokeLater(() -> 
                    faceStatusLabel.setText("Indexing: In progress"));
                
                pollIndexProgress();
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "Indexing failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    indexButton.setEnabled(true);
                    faceStatusLabel.setText("Indexing: Failed");
                });
            }
        });
    }
    
    private void pollIndexProgress() {
        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            try {
                URL url = new URL(API_BASE + "/index/progress");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                
                String progress = readResponse(conn);
                conn.disconnect();
                
                SwingUtilities.invokeLater(() -> 
                    indexProgressLabel.setText("Progress: " + progress));
                
                if (progress.contains("photos done") && !progress.contains("No indexing")) {
                    String[] parts = progress.split(" of ");
                    if (parts.length == 2) {
                        int done = Integer.parseInt(parts[0].trim().split(" ")[0]);
                        int total = Integer.parseInt(parts[1].trim().split(" ")[0]);
                        
                        if (done >= total && total > 0) {
                            timer.stop();
                            SwingUtilities.invokeLater(() -> {
                                faceStatusLabel.setText("Indexing: Complete");
                                indexProgressLabel.setText("Progress: Done!");
                                scanButton.setEnabled(true);
                                JOptionPane.showMessageDialog(frame,
                                    "Indexing complete!\nYou can now scan your face.",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            });
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        timer.start();
    }
    
    private void scanFace() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", 
            "jpg", "jpeg", "png"));
        
        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) return;
        
        File selfie = chooser.getSelectedFile();
        scanButton.setEnabled(false);
        faceStatusLabel.setText("Searching...");
        
        executor.execute(() -> {
            try {
                List<String> matches = searchByFace(selfie);
                SwingUtilities.invokeLater(() -> {
                    displayResults(matches);
                    scanButton.setEnabled(true);
                    faceStatusLabel.setText("Found " + matches.size() + " match(es)");
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        "Search failed: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    scanButton.setEnabled(true);
                    faceStatusLabel.setText("Search failed");
                });
            }
        });
    }
    
    private List<String> searchByFace(File selfie) throws IOException {
        String boundary = "----Boundary" + System.currentTimeMillis();
        URL url = new URL(API_BASE + "/search");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        
        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {
            
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"")
                  .append(selfie.getName()).append("\"\r\n");
            writer.append("Content-Type: ").append(getMimeType(selfie.getName())).append("\r\n\r\n");
            writer.flush();
            
            Files.copy(selfie.toPath(), os);
            os.flush();
            
            writer.append("\r\n--").append(boundary).append("--\r\n");
        }
        
        String response = readResponse(conn);
        conn.disconnect();
        
        List<String> photoUrls = new ArrayList<>();
        if (response.contains("imageUrl")) {
            String[] items = response.split("\\{");
            for (String item : items) {
                if (item.contains("imageUrl")) {
                    String[] parts = item.split("\"");
                    for (int i = 0; i < parts.length; i++) {
                        if ("imageUrl".equals(parts[i]) && i + 2 < parts.length) {
                            photoUrls.add(parts[i + 2]);
                        }
                    }
                }
            }
        }
        
        return photoUrls;
    }
    
    private void displayResults(List<String> photoUrls) {
        resultsGrid.removeAll();
        
        if (photoUrls.isEmpty()) {
            JLabel emptyLabel = new JLabel("No matching photos found", SwingConstants.CENTER);
            emptyLabel.setOpaque(true);
            emptyLabel.setBackground(new Color(255, 230, 230));
            resultsGrid.setLayout(new GridLayout(1, 1));
            resultsGrid.add(emptyLabel);
            resultsGrid.revalidate();
            resultsGrid.repaint();
            return;
        }
        
        resultsGrid.setLayout(new GridLayout(0, 4, 10, 10));
        
        for (String photoUrl : photoUrls) {
            executor.execute(() -> {
                try {
                    BufferedImage img = loadThumbnail(photoUrl);
                    SwingUtilities.invokeLater(() -> {
                        JLabel label = new JLabel(new ImageIcon(
                            img.getScaledInstance(150, 150, Image.SCALE_SMOOTH)));
                        label.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        resultsGrid.add(label);
                        resultsGrid.revalidate();
                        resultsGrid.repaint();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    private BufferedImage loadThumbnail(String path) throws IOException {
        URL url = new URL(API_BASE.replace("/api", "") + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        BufferedImage img = ImageIO.read(conn.getInputStream());
        conn.disconnect();
        return img;
    }
    
    private String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
    
    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg";
    }
}
