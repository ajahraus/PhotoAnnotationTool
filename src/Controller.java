import DataModel.AnnotationItem;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Controller {

    @FXML
    Text outputText;
    @FXML
    Label outputDirectoryLabel;
    @FXML
    ImageView selectedItemImageView;
    @FXML
    TextArea userMessagesTextArea;
    @FXML
    Label annotationFileLabel;
    @FXML
    private Stage primaryStage;
    @FXML
    private ListView<AnnotationItem> annotationListView;
    @FXML
    Label currentProcessLabel;
    @FXML
    ProgressBar currentProcessProgressBar;
    @FXML
    CheckBox rotateImagesOnImport;

    private Path outputFileLocation;
    private Path annotationFileLocation;
    private List<AnnotationItem> annotationItems;
    private Image phImage;
    private HashMap<AnnotationItem, Image> previewImageMap;
    private Image rightLogo;
    private Image leftLogo;
    private AnnotationItem currentlySelectedItem;
    private boolean rotateImages;

    public void initialize() {
        annotationItems = new ArrayList<>();

        phImage = new Image("placeholder-image.jpg");

        previewImageMap = new HashMap<>();

        rightLogo = new Image("ATC_Logo_Resized.jpg");
        leftLogo = new Image("LLC_Logo_Resized.jpg");
        currentlySelectedItem = null;
        outputFileLocation = null;

        updateRotateImages();


        /*
        annotationFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\X-69\\X-69_Annotate3.txt");
        annotationFileLabel.setText("Loaded annotation file: " + annotationFileLocation.toString());
        new Thread(new LoadAnnotationFile()).start();

        annotationFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\X-69\\X-69_Annotate3.txt");
        annotationFileLabel.setText(annotationFileLocation.toString());

        outputFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\X-69\\Annotated2");
        outputDirectoryLabel.setText("Output file location: " + outputFileLocation.toString());

        new Thread(new LoadAnnotationFile()).start();

        */
        selectedItemImageView.setImage(phImage);
        selectedItemImageView.preserveRatioProperty().setValue(true);
        selectedItemImageView.setFitHeight(600);
        selectedItemImageView.setCache(true);
    }

    @FXML
    private void selectOutputFileLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if (selectedDirectory == null && outputFileLocation == null) {
            userMessagesTextArea.appendText("No directory selected.\n");
            return;
        } else if (selectedDirectory != null) {
            outputFileLocation = selectedDirectory.toPath();
        }
        userMessagesTextArea.appendText("Output file location: " + outputFileLocation.toString() + "\n");
        outputDirectoryLabel.setText("Output file location: " + outputFileLocation.toString());
    }

    private void selectOutputFileLocationAndExportPhotos() {
        selectOutputFileLocation();

        if (outputFileLocation != null)
            new Thread(new ExportAllPhotos()).start();

    }

    @FXML
    public void selectAnnotationFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Annotation File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);


        if (selectedFile != null) {
            annotationFileLocation = selectedFile.toPath();

            annotationFileLabel.setText("Loaded annotation file: " + annotationFileLocation.toString());
            new Thread(new LoadAnnotationFile()).start();
        }
    }

    public void updateRotateImages() {
        rotateImages = rotateImagesOnImport.isSelected();
    }

    private class LoadAnnotationFile extends ProcessingThread {
        @Override
        public void run() {
            BufferedReader br;
            try {
                processName = "Loading Annotation File";
                br = Files.newBufferedReader(annotationFileLocation);
                String input;
                annotationItems = new ArrayList<>();
                updateProcessAndWriteToMessageArea();
                Platform.runLater(() -> annotationListView.getItems().setAll());

                try {
                    while ((input = br.readLine()) != null) {

                        String[] itemPieces = input.split("\t");

                        if (itemPieces.length != 7) {
                            userMessagesTextArea.appendText("Warning: Line " + input + " does not have the required number of splits. Either a data " +
                                    "type is missing or there are too many tabs in this line.\n");
                        } else if (!Files.exists(Paths.get(itemPieces[0]))) {
                            userMessagesTextArea.appendText("Warning: File " + itemPieces[0] + " does not exit. Check that the annotation file contains the correct filepath.\n");
                        } else {
                            AnnotationItem item = new AnnotationItem(itemPieces[0], itemPieces[1], itemPieces[2], itemPieces[3],
                                    itemPieces[4], itemPieces[5], itemPieces[6]);
                            item.removeUnderscoresFromCoordinateSystem();
                            annotationItems.add(item);
                        }
                    }
                } finally {
                    br.close();


                    Platform.runLater(() -> annotationListView.getItems().setAll(annotationItems));
                    Platform.runLater(() -> annotationListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE));
                    Platform.runLater(() -> userMessagesTextArea.appendText("Loading annotation file... Complete!\n"));
                    clearAndUpdateProcess();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @FXML
    public void selectRightHandLogo() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image file for right logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null)
            rightLogo = new Image(selectedFile.getPath());
    }

    @FXML
    public void selectLeftHandLogo() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image file for left logo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null)
            leftLogo = new Image(selectedFile.getPath());
    }

    @FXML
    private void updateSelectedItem() {
        Object o = annotationListView.getSelectionModel().getSelectedItem();
        if (o.getClass() == AnnotationItem.class) {
            currentlySelectedItem = annotationListView.getSelectionModel().getSelectedItem();
            updateImageView();
        }
    }

    private void updateImageView() {
        if (currentlySelectedItem != null) {
            if (previewImageMap.containsKey(currentlySelectedItem) && currentlySelectedItem.imageRotated == rotateImages)
                Platform.runLater(() -> selectedItemImageView.setImage(previewImageMap.get(currentlySelectedItem)));
            else {
                loadPreviewImage(currentlySelectedItem);
                Platform.runLater(() -> selectedItemImageView.setImage(phImage));
            }
        }
    }

    private void loadPreviewImage(AnnotationItem item) {
        LoadPreviewImage loadPreviewImage = new LoadPreviewImage(item);
        new Thread(loadPreviewImage).start();
    }

    private class LoadPreviewImage extends ProcessingThread {

        AnnotationItem item;

        private LoadPreviewImage(AnnotationItem item) {
            this.item = item;
        }

        @Override
        public void run() {
            previewImageMap.put(item, createPreviewImageFromItem(item));
            updateImageView();
            clearAndUpdateProcess();
        }

    }

    @FXML
    private void preloadAllPreviewImages() {
        Runnable preloadPreviewMap = new PreloadAllPreviewImages();

        new Thread(preloadPreviewMap).start();
    }

    private class PreloadAllPreviewImages extends ProcessingThread {
        @Override
        public void run() {
            Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images...\n"));
            for (int i = 0; i < annotationItems.size(); i++) {
                processName = "Pre-loading Preview Images. Number " + (i + 1) + "/" + annotationItems.size();
                processProgress = (double) i / annotationItems.size();
                updateProcess();
                AnnotationItem item = annotationItems.get(i);
                if (!previewImageMap.containsKey(item) || item.imageRotated != rotateImages)
                    previewImageMap.put(item, createPreviewImageFromItem(item));

                updateImageView();
            }
            Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images... Complete!\n"));
            clearAndUpdateProcess();
        }

    }

    private void updateCurrentProcess(String progressName, double processProgress) {
        Platform.runLater(() -> currentProcessLabel.setText(progressName));
        Platform.runLater(() -> currentProcessProgressBar.setProgress(processProgress));
    }

    @FXML
    private void exportAllPhotos() {
        if (annotationFileLocation == null || !Files.exists(annotationFileLocation)) {
            Platform.runLater(() -> userMessagesTextArea.appendText("Export failed: Annotation file not loaded\n"));
        } else {
            Runnable exportAllPhotos = new ExportAllPhotos();

            new Thread(exportAllPhotos).start();
        }
    }

    public class ExportAllPhotos extends ProcessingThread {
        @Override
        public void run() {
            processName = "Exporting photos";
            processProgress = 0;
            updateProcessAndWriteToMessageArea();
            if (outputFileLocation == null) {
                Platform.runLater(() -> {
                    userMessagesTextArea.appendText("Select export directory before exporting photos\n");
                    selectOutputFileLocationAndExportPhotos();
                });
                clearAndUpdateProcess();
                return;
            }

            for (int i = 0; i < annotationItems.size(); i++) {
                processName = "Exporting photos. Number " + (i + 1) + "/" + annotationItems.size();
                processProgress = (double) i / (double) annotationItems.size();
                updateProcess();
                AnnotationItem item = annotationItems.get(i);
                if (outputFileLocation != item.getFilepath().getParent()) {
                    Image image = createImageFromItem(item);
                    BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);

                    if (bImage.getColorModel().hasAlpha())
                        bImage = dropAlphaChannel(bImage);

                    Path newPath = outputFileLocation.resolve(item.getFilepath().getFileName());

                    try {
                        ImageIO.write(bImage, "jpg", newPath.toFile());
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }


                } else {
                    Platform.runLater(() -> userMessagesTextArea.appendText("Warning: Photo " + item.getFilepath().getFileName() +
                            " loaded from the output directory. Skipping, so as to not save over original.\n"));
                }
            }
            Platform.runLater(() -> userMessagesTextArea.appendText("Exporting photos... Complete!"));
            clearAndUpdateProcess();
        }
    }


    private Image createImageFromItem(AnnotationItem item) {
        BufferedImage inputImage;
        try {
            inputImage = ImageIO.read(item.getFilepath().toFile());
        } catch (IOException e) {
            System.err.println(e.getMessage() + " File in question: " + item.toString());
            e.printStackTrace();
            return phImage;
        }

        item.imageRotated = rotateImages;

        if (rotateImages) {
            BufferedImage rotatedInputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), inputImage.getType());
            int h = inputImage.getHeight();
            int w = inputImage.getWidth();
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    rotatedInputImage.setRGB(w - i - 1, h - j - 1, inputImage.getRGB(i, j));
                }
            }
            inputImage = rotatedInputImage;
        }
        int logoHeight = 344;

        BufferedImage tmp = new BufferedImage(inputImage.getWidth(), inputImage.getHeight() + 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = tmp.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, inputImage.getHeight(), tmp.getWidth(), 800);
        graphics.drawImage(inputImage, 0, 0, null);

        java.awt.Image leftLogoTemp = SwingFXUtils.fromFXImage(leftLogo, null).getScaledInstance(
                logoHeight * (int) (leftLogo.getWidth() / leftLogo.getHeight()), logoHeight, java.awt.Image.SCALE_SMOOTH);
        graphics.drawImage(leftLogoTemp, 0, inputImage.getHeight(), null);

        java.awt.Image rightLogoTemp = SwingFXUtils.fromFXImage(rightLogo, null).getScaledInstance(
                logoHeight * (int) (rightLogo.getWidth() / rightLogo.getHeight()), logoHeight, java.awt.Image.SCALE_SMOOTH);
        graphics.drawImage(rightLogoTemp, inputImage.getWidth() - rightLogoTemp.getWidth(null), inputImage.getHeight(), null);

        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 80));
        FontMetrics metrics = graphics.getFontMetrics(graphics.getFont());

        // This nonsense is used to actually put the text where I want it to be on the image. The drawString location is
        // always placed at the top right corner, so if you want the text to be centered, you have to calculate width of
        // your text yourself and subtract it from the location it's being printed at.

        // Centered on the top row
        String line1 = "Line: " + item.getCircuitName() + "        " + "Structure: " + item.getStructureName();
        graphics.drawString(line1, tmp.getWidth() / 2 - (metrics.stringWidth(line1) / 2), inputImage.getHeight() + 200);

        // Centered on the middle row
        String line2 = "E: " + item.getEasting() + "        " + "N: " + item.getNorthing();
        graphics.drawString(line2, tmp.getWidth() / 2 - (metrics.stringWidth(line2) / 2), inputImage.getHeight() + 400);

        // Centered on the bottom row
        graphics.drawString(item.getCoordinateSystem(), tmp.getWidth() / 2 - (metrics.stringWidth(item.getCoordinateSystem()) / 2), inputImage.getHeight() + 600);

        // Aligned to the right edge, side height at the bottom row
        graphics.drawString(item.getDateString(), tmp.getWidth() - (metrics.stringWidth(item.getDateString()) + 100), inputImage.getHeight() + 600);

        return SwingFXUtils.toFXImage(tmp, null);

    }

    private Image createPreviewImageFromItem(AnnotationItem item) {
        Image image = createImageFromItem(item);
        java.awt.Image tmp = SwingFXUtils.fromFXImage(image, null).getScaledInstance((int) (image.getWidth() * (600 / image.getHeight())), 600, java.awt.Image.SCALE_SMOOTH);

        BufferedImage dimg = new BufferedImage((int) (image.getWidth() * (600 / image.getHeight())), 600, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = dimg.createGraphics();
        graphics.drawImage(tmp, 0, 0, null);
        graphics.dispose();

        return SwingFXUtils.toFXImage(dimg, null);
    }

    private BufferedImage dropAlphaChannel(BufferedImage src) {
        BufferedImage convertedImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImg.getGraphics().drawImage(src, 0, 0, null);

        return convertedImg;
    }

    private abstract class ProcessingThread implements Runnable {
        String processName;
        double processProgress;

        void updateProcess() {
            Platform.runLater(() -> updateCurrentProcess(processName, processProgress));
        }

        void updateProcessAndWriteToMessageArea() {
            Platform.runLater(() -> updateCurrentProcess(processName, processProgress));
            Platform.runLater(() -> userMessagesTextArea.appendText(processName + "\n"));
        }

        void clearAndUpdateProcess() {
            processName = "None";
            processProgress = 0;
            updateProcess();
        }

    }

}
