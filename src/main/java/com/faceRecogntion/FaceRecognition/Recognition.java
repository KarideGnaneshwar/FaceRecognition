package com.faceRecogntion.FaceRecognition;

import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.bytedeco.javacv.*;

public class Recognition {

    private static String getValue(HashMap<String, String> data, int selection) {
        return data.getOrDefault(Integer.toString(selection), "Unknown");
    }

    public void recogWithLiveFeed() {
        new Thread(() -> {
            OpenCVFrameGrabber camera = null;
            CanvasFrame canvas = null;

            try {
                camera = new OpenCVFrameGrabber(0);
                camera.start();

                OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

                // Load Haar Cascade
                File cascadeFile = new File("src/main/resources/haarcascade_frontalface_alt.xml");
                if (!cascadeFile.exists()) throw new Exception("Haar cascade missing!");
                CascadeClassifier faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());

                // Load LBPH model
                File modelFile = new File("src/main/resources/classifierLBPH.yml");
                if (!modelFile.exists() || modelFile.length() == 0)
                    throw new Exception("LBPH Model missing! Train dataset first.");

                FaceRecognizer recognizer = createLBPHFaceRecognizer();
                recognizer.load(modelFile.getAbsolutePath());
                recognizer.setThreshold(80.0); // threshold for unknown

                // Load CSV mapping
                Path csvPath = Path.of("src/main/resources/namedata.csv");
                HashMap<String, String> data = new HashMap<>();
                if (Files.exists(csvPath)) {
                    Files.lines(csvPath).forEach(line -> {
                        if (line.trim().isEmpty()) return;
                        String[] parts = line.split(",");
                        if (parts.length < 2) return;
                        data.put(parts[0], parts[1]);
                    });
                }

                canvas = new CanvasFrame("Recognition - Press ESC to exit", CanvasFrame.getDefaultGamma() / camera.getGamma());
                canvas.setDefaultCloseOperation(CanvasFrame.EXIT_ON_CLOSE);
                final CanvasFrame canvasFinal = canvas;

                boolean[] popupShown = {false}; // popup flag

                Frame frame;
                while ((frame = camera.grab()) != null && canvasFinal.isVisible()) {
                    Mat colorImage = converter.convert(frame);
                    Mat grayImage = new Mat();
                    opencv_imgproc.cvtColor(colorImage, grayImage, opencv_imgproc.COLOR_BGRA2GRAY);

                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(grayImage, faces, 1.1, 3, 0, new Size(150, 150), new Size(500, 500));

                    for (int i = 0; i < faces.size(); i++) {
                        Rect face = faces.get(i);
                        Mat faceMat = new Mat(grayImage, face);
                        opencv_imgproc.resize(faceMat, faceMat, new Size(160, 160));

                        IntPointer label = new IntPointer(1);
                        DoublePointer confidence = new DoublePointer(1);
                        recognizer.predict(faceMat, label, confidence);
                        int predictedId = label.get(0);

                        String name = predictedId == -1 ? "Unknown" : getValue(data, predictedId);

                        // Draw rectangle + overlay text continuously
                        opencv_imgproc.rectangle(colorImage, face, new Scalar(0, 255, 0, 0), 2, 0, 0);
                        opencv_imgproc.putText(colorImage,
                                name + " (" + predictedId + ")",
                                new Point(face.tl().x(), face.tl().y() - 10),
                                opencv_core.FONT_HERSHEY_PLAIN, 1.5, new Scalar(0, 255, 0, 0));

                        // Show popup & DB insert only once
                        if (!popupShown[0] && !name.equals("Unknown") && predictedId != -1) {
                            String finalName = name;
                            int finalPredictedId = predictedId;
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(null,
                                        "Recognized!\nName: " + finalName + "\nID: " + finalPredictedId,
                                        "Face Recognition", JOptionPane.INFORMATION_MESSAGE);

                                // Insert attendance
                                DBHelper.insertAttendance(finalPredictedId, finalName);

                                // Close live feed
                                if (canvasFinal != null) canvasFinal.dispose();
                            });
                            popupShown[0] = true;
                        }
                    }

                    if (canvasFinal.isVisible())
                        canvasFinal.showImage(converter.convert(colorImage));
                }

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Recognition Failed!\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                try { if (camera != null) camera.stop(); } catch (FrameGrabber.Exception ignored) {}
                if (canvas != null && canvas.isVisible()) canvas.dispose();
            }
        }).start();
    }
}
