package com.faceRecogntion.FaceRecognition;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.*;

public class Capture {

    private OpenCVFrameGrabber camera;
    private OpenCVFrameConverter.ToMat converter;
    private CascadeClassifier faceDetector;

    private JFrame window;
    private JLabel cameraLabel;
    private JButton btnTakePhoto, btnCancel, btnStart;
    private JTextField nameField, idField;

    private String photosPathStr;
    private String name;
    private String personId;
    private int sample = 1;

    private volatile boolean running = false;
    private boolean nameIdSaved = false;

    public Capture() throws Exception {
        String basePath = System.getProperty("user.dir");
        photosPathStr = basePath + "/src/main/resources/photos";
        Path p = Paths.get(photosPathStr);
        if (!Files.exists(p)) Files.createDirectories(p);

        faceDetector = new CascadeClassifier(basePath + "/src/main/resources/haarcascade_frontalface_alt.xml");
        converter = new OpenCVFrameConverter.ToMat();
        camera = new OpenCVFrameGrabber(0);

        createCameraWindow();
    }

    private void createCameraWindow() {
        window = new JFrame("Face Capture");
        window.setSize(700, 600);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ---------- Camera Display ----------
        cameraLabel = new JLabel();
        cameraLabel.setHorizontalAlignment(JLabel.CENTER);
        window.add(cameraLabel, BorderLayout.CENTER);

        // ---------- Input Panel ----------
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        inputPanel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Unique ID:"), gbc);
        idField = new JTextField(15);
        gbc.gridx = 1;
        inputPanel.add(idField, gbc);

        // ---------- Buttons ----------
        JPanel buttonPanel = new JPanel();
        btnStart = new JButton("Start Capture");
        btnTakePhoto = new JButton("Take Photo");
        btnTakePhoto.setEnabled(false);
        btnCancel = new JButton("Cancel");

        buttonPanel.add(btnStart);
        buttonPanel.add(btnTakePhoto);
        buttonPanel.add(btnCancel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        window.add(bottomPanel, BorderLayout.SOUTH);
        window.setVisible(true);

        // ---------- Button Actions ----------
        btnStart.addActionListener(e -> startCamera());
        btnTakePhoto.addActionListener(e -> takePicture());
        btnCancel.addActionListener(e -> closeCamera());
    }

    private void startCamera() {
        name = nameField.getText().trim();
        personId = idField.getText().trim();

        if(name.isEmpty() || personId.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(window,
                    "No Name/ID entered. Do you want to cancel?", 
                    "Confirm Cancel", JOptionPane.YES_NO_OPTION);
            if(confirm == JOptionPane.YES_OPTION) {
                closeCamera();
            }
            return;
        }

        btnStart.setEnabled(false);
        btnTakePhoto.setEnabled(true);
        running = true;

        // Start camera thread
        new Thread(() -> {
            try {
                camera.start();
                while(running) {

                    org.bytedeco.javacv.Frame frame = camera.grab();   // FIXED

                    if(frame != null) {
                        Image img = Java2DFrameUtils.toBufferedImage(frame);
                        cameraLabel.setIcon(new ImageIcon(img));
                    }
                }
            } catch(Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void takePicture() {
        try {
            org.bytedeco.javacv.Frame frame = camera.grab();    // FIXED

            if(frame == null) { 
                JOptionPane.showMessageDialog(window, "Capture failed!"); 
                return; 
            }

            Mat img = converter.convert(frame);
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(img, gray, opencv_imgproc.COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(gray, faces);

            if(faces.size() == 0) {
                JOptionPane.showMessageDialog(window, "No face detected!");
                return;
            }

            Rect r = faces.get(0);
            Mat face = new Mat(gray, r);
            opencv_imgproc.resize(face, face, new Size(160, 160));

            String fileName = photosPathStr + "/" + name + "." + personId + "." + sample + ".jpg";
            boolean saved = opencv_imgcodecs.imwrite(fileName, face);

            if(saved) {
                JOptionPane.showMessageDialog(window, "Photo " + sample + " saved!");
                sample++;

                // Save Name/ID to CSV only once
                if(!nameIdSaved) {
                    saveNameAndId();
                    nameIdSaved = true;
                }
            }

            face.close();
            gray.close();
        } catch(Exception ex) { ex.printStackTrace(); }
    }

    private void saveNameAndId() {
        try {
            Path csvPath = Paths.get(System.getProperty("user.dir") + "/src/main/resources/namedata.csv");
            if(Files.exists(csvPath)) {
                boolean exists = Files.lines(csvPath).anyMatch(line -> line.startsWith(personId + ","));
                if(exists) return; // skip duplicates
            }
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(csvPath.toFile(), true))) {
                bw.append(personId).append(",").append(name).append("\n");
            }
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void closeCamera() {
        running = false;
        try { camera.stop(); } catch(Exception e) { e.printStackTrace(); }
        window.dispose();
    }
}
