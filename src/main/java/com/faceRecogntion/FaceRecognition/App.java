package com.faceRecogntion.FaceRecognition;

import javax.swing.*;
import java.awt.*;

public class App extends JFrame {

    private JButton CaptureButton;
    private JButton RecognizeButton;
    private JButton TrainButton;
    private JTextArea titleArea;

    public App() {
        initComponents();
        setTitle("Face Recognition System");
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Resize listener to scale fonts
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                resizeFonts();
            }
        });
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        // ---------- TITLE ----------
        titleArea = new JTextArea("Face Recognition System");
        titleArea.setEditable(false);
        titleArea.setOpaque(false);
        titleArea.setFont(new Font("Arial Black", Font.BOLD, 48));
        titleArea.setForeground(new Color(30, 144, 255));
        titleArea.setHighlighter(null);
        titleArea.setFocusable(false);
        titleArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleArea.setAlignmentY(Component.CENTER_ALIGNMENT);

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 50, 0); // space below title
        add(titleArea, gbc);

        // ---------- BUTTONS ----------
        CaptureButton = new JButton("Start Camera & Enter Details");
        RecognizeButton = new JButton("Recognize Faces");
        TrainButton = new JButton("Train Dataset");

        JButton[] buttons = {CaptureButton, RecognizeButton, TrainButton};

        gbc.insets = new Insets(10, 0, 10, 0); // spacing between buttons
        for (int i = 0; i < buttons.length; i++) {
            gbc.gridy = i + 1; // below title
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5; // buttons expand horizontally
            add(buttons[i], gbc);
        }

        // ---------- BUTTON ACTIONS ----------
        CaptureButton.addActionListener(evt -> {
            try { new Capture(); } catch (Exception e) { e.printStackTrace(); }
        });

        RecognizeButton.addActionListener(evt -> new Thread(() -> new Recognition().recogWithLiveFeed()).start());

        TrainButton.addActionListener(evt -> {
            JLabel label = new JLabel("Training in progress, please wait...");
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            final JDialog progressDialog = new JDialog(this, "Training Faces", true);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.getContentPane().add(label);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(this);

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    new YaleTraining().yaleTraining();
                    return null;
                }

                @Override
                protected void done() {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(App.this, "Training Completed Successfully!");
                }
            };
            worker.execute();
            progressDialog.setVisible(true);
        });
    }

    // ---------- RESIZE FONTS ----------
    private void resizeFonts() {
        int frameWidth = getWidth();
        int frameHeight = getHeight();

        int titleFontSize = Math.min(frameWidth / 15, frameHeight / 10);
        titleArea.setFont(new Font("Arial Black", Font.BOLD, titleFontSize));

        int btnFontSize = Math.min(frameWidth / 25, frameHeight / 20);
        for (Component c : getContentPane().getComponents()) {
            if (c instanceof JButton) {
                ((JButton) c).setFont(new Font("Tahoma", Font.BOLD, btnFontSize));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().setVisible(true));
    }
}
