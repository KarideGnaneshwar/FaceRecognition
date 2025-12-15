package com.faceRecogntion.FaceRecognition;

import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

import javax.swing.JOptionPane;

import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.Size;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_imgproc;

public class Training {

    public void training() {
        try {
            File directory = new File("src/main/resources/photos");

            // Only JPG or PNG
            FilenameFilter imageFilter = (dir, name) ->
                    name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png");

            File[] files = directory.listFiles(imageFilter);

            if (files == null || files.length == 0) {
                JOptionPane.showMessageDialog(null,
                        "No photos found to train!",
                        "FACE RECOGNITION",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Prepare matrices for LBPH
            MatVector photos = new MatVector(files.length);
            Mat labels = new Mat(files.length, 1, CV_32SC1);
            IntBuffer labelBuffer = labels.createBuffer();

            for (int i = 0; i < files.length; i++) {
                Mat photo = opencv_imgcodecs.imread(files[i].getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (photo.empty()) continue;

                int personId;
                try {
                    // Filename: Name.ID.sample.jpg
                    String[] parts = files[i].getName().split("\\.");
                    if (parts.length < 3) throw new Exception("Filename format incorrect: " + files[i].getName());
                    personId = Integer.parseInt(parts[1]);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null,
                            "Skipping file: " + files[i].getName() + "\nReason: " + e.getMessage(),
                            "TRAINING WARNING", JOptionPane.WARNING_MESSAGE);
                    continue;
                }

                opencv_imgproc.resize(photo, photo, new Size(160, 160));

                photos.put(i, photo);
                labelBuffer.put(i, personId);
            }

            // Train LBPH
            FaceRecognizer lbph = createLBPHFaceRecognizer();
            lbph.train(photos, labels);

            // Save model
            lbph.save("src/main/resources/classifierLBPH.yml");

            JOptionPane.showMessageDialog(null,
                    "Training Complete! LBPH Model Saved.",
                    "FACE RECOGNITION",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error during training: " + e.getMessage(),
                    "ERROR", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
