import DataModel.AnnotationItem;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.*;
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
    Label outputFileLocationLabel;
    @FXML
    Label selectedAnnotationItemLabel;
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

    private Path outputFileLocation;
    private Path annotationFileLocation;
    private List<AnnotationItem> annotationItems;
    private Image phImage;
    private HashMap<AnnotationItem, Image> previewImageMap;
    private Image rightLogo;
    private Image leftLogo;
    private AnnotationItem currentlySelectedItem;

    public void initialize() {
        annotationItems = new ArrayList<>();

        phImage = new Image("placeholder-image.jpg");

        previewImageMap = new HashMap<>();

        rightLogo = new Image("ATC_Logo_Resized.jpg");
        leftLogo = new Image("LLC_Logo_Resized.jpg");
        currentlySelectedItem = null;

        if (false) {
            annotationFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\W3103\\W3103_Annotate.txt");
            annotationFileLabel.setText(annotationFileLocation.toString());

            outputFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\W3103\\Annotated2");
            outputFileLocationLabel.setText("Output file location: " + outputFileLocation.toString());

            Runnable task = new LoadAnnotationFile();

            new Thread(task).start();

        }

        selectedItemImageView.setImage(phImage);
        selectedItemImageView.preserveRatioProperty().setValue(true);
        selectedItemImageView.setFitHeight(600);
        selectedItemImageView.setCache(true);
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

            annotationFileLabel.setText(annotationFileLocation.toString());
            new Thread(new LoadAnnotationFile()).start();
        }
    }

    @FXML
    private void selectOutputFileLocation() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        Path selectedDirectory = directoryChooser.showDialog(primaryStage).toPath();

        if (selectedDirectory == null) {
            System.err.println("No directory selected");
        } else {
            outputFileLocation = selectedDirectory;
            outputFileLocationLabel.setText("Output file location: " + outputFileLocation.toString());
        }
    }

    @FXML
    private void updateSelectedItem() {
        currentlySelectedItem = annotationListView.getSelectionModel().getSelectedItem();
        updateImageView();
    }

    @FXML
    public void updateSelectedItemImagePreview() {
        Platform.runLater(() -> selectedAnnotationItemLabel.setText(currentlySelectedItem.toStringComplete()));
        if (previewImageMap.containsKey(currentlySelectedItem))
            selectedItemImageView.setImage(previewImageMap.get(currentlySelectedItem));
        else {
            updateImageView();
            Platform.runLater(() -> selectedItemImageView.setImage(phImage));
        }
    }

    private void loadPreviewImage(AnnotationItem item) {
        LoadPreviewImage loadPreviewImage = new LoadPreviewImage(item);
        new Thread(loadPreviewImage).start();
    }

    private class LoadPreviewImage extends ProcessingThread {
        AnnotationItem item;

        public LoadPreviewImage(AnnotationItem item) {
            this.item = item;
        }

        @Override
        void setProcessName() {
            processName = "Loading preview image " + item.toString();
        }

        @Override
        public void run() {
            updateProcess();
            previewImageMap.put(item, createPreviewImageUsingSwingUtilFromItem(item));
            updateImageView();

            processName = "None";
            processProgress = 0;
            updateProcess();
        }

    }

    @FXML
    private void preloadPreviewMap() {
        Runnable preloadPreviewMap = new PreloadPreviewMap();

        new Thread(preloadPreviewMap).start();
    }

    private void updateCurrentProcess(String progressName, double processProgress) {
        Platform.runLater(() -> currentProcessLabel.setText(progressName));
        Platform.runLater(() -> currentProcessProgressBar.setProgress(processProgress));
    }

    @FXML
    private void exportAllPhotos() {
        Runnable exportAllPhotos = new ExportAllPhotos();

        new Thread(exportAllPhotos).start();
    }

    private Image createImageUsingSwingUtilFromItem(AnnotationItem item) {
        BufferedImage inputImage = null;
        try {
            inputImage = ImageIO.read(item.getFilepath().toFile());
        } catch (IOException e) {
            System.err.println(e.getMessage() + " File in question: " + item.toString());
            e.printStackTrace();
            return phImage;
        }

        Image rightLogo = new Image("ATC_Logo_Resized.jpg");
        Image leftLogo = new Image("LLC_Logo_Resized.jpg");
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
        graphics.setFont(new Font("Serif", Font.BOLD, 80));
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

    private Image createPreviewImageUsingSwingUtilFromItem(AnnotationItem item) {
        Image image = createImageUsingSwingUtilFromItem(item);
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

    private Image createImageFromItem(AnnotationItem item) {
        Image inputImage = new Image(item.getFilepath().toUri().toString());

        if (inputImage == null)
            return phImage;

        Canvas canvas = new Canvas(inputImage.getWidth(), inputImage.getHeight() + 800);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.drawImage(inputImage, 0, 0);
        gc.drawImage(leftLogo, 0, inputImage.getHeight());
        gc.drawImage(rightLogo, inputImage.getWidth() - rightLogo.getWidth(), inputImage.getHeight());

        return canvas.snapshot(null, null);
    }

    private Image createPreviewImageFromItem(AnnotationItem item) {
        Image image = createImageFromItem(item);

        Canvas canvas = new Canvas(800 / image.getHeight() * image.getWidth(), 800);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.drawImage(image, 0, 0, 800 / image.getHeight() * image.getWidth(), 800);

        return canvas.snapshot(null, null);
    }

    private abstract class ProcessingThread implements Runnable {
        String processName;
        double processProgress;

        abstract void setProcessName();

        void updateProcess() {
            Platform.runLater(() -> updateCurrentProcess(processName, processProgress));
        }
    }

    public class ExportAllPhotos extends ProcessingThread {

        @Override
        void setProcessName() {
            processName = "Exporting photos";
        }

        @Override
        public void run() {
            Platform.runLater(() -> userMessagesTextArea.appendText("Exporting images..."));
            for (int i = 0; i < annotationItems.size(); i++) {
                AnnotationItem item = annotationItems.get(i);
                if (outputFileLocation != item.getFilepath().getParent()) {
                    Image image = createImageUsingSwingUtilFromItem(item);
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

                    processProgress = (double) i / (double) annotationItems.size();
                    updateProcess();
                } else {
                    Platform.runLater(() -> userMessagesTextArea.appendText("Warning: Photo " + item.getFilepath().getFileName() +
                            " loaded from the output directory. Skipping, so as to not save over original.\n"));
                }
            }
            Platform.runLater(() -> userMessagesTextArea.appendText("Exporting images... Complete!"));
            processName = "None";
            processProgress = 0;
            updateProcess();
        }

    }

    private class PreloadPreviewMap extends ProcessingThread {
        @Override
        void setProcessName() {
            processName = "Pre-loading Preview Images";
        }

        @Override
        public void run() {
            Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images...\n"));
            for (int i = 0; i < annotationItems.size(); i++) {
                final double progress = (double) i / (double) annotationItems.size();
                updateProcess();
                AnnotationItem item = annotationItems.get(i);
                previewImageMap.putIfAbsent(item, createPreviewImageUsingSwingUtilFromItem(item));
                updateImageView();
                // yield();
            }
            Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images... Complete!\n"));
            processName = "None";
            processProgress = 0;
            updateProcess();
        }
    }

    private class LoadAnnotationFile extends ProcessingThread {
        @Override
        void setProcessName() {
            processName = "Loading Annotation File";
        }

        @Override
        public void run() {
            BufferedReader br;
            try {
                br = Files.newBufferedReader(annotationFileLocation);
                String input;
                annotationItems = new ArrayList<>();
                Platform.runLater(() -> userMessagesTextArea.appendText("Loading annotation file...\n"));

                ArrayList<String> temp = new ArrayList<String>() {{
                    add("Annotation List Loading...");
                }};
                Platform.runLater(() -> annotationListView.getItems().setAll());

                try {
                    while ((input = br.readLine()) != null) {

                        String[] itemPieces = input.split("\t");

                        if (itemPieces.length != 7) {
                            userMessagesTextArea.appendText("Warning: Line " + input + " does not have the required number of splits. Either a data " +
                                    "type is missing or there are too many tabs in this line.\n");
                        } else if (Files.exists(Paths.get(itemPieces[0])) == false) {
                            userMessagesTextArea.appendText("Warning: File " + itemPieces[0] + " does not exit. Check that the annotation file contains the correct filepath.\n");
                        } else {
                            AnnotationItem item = new AnnotationItem(itemPieces[0], itemPieces[1], itemPieces[2], itemPieces[3],
                                    itemPieces[4], itemPieces[5], itemPieces[6]);
                            annotationItems.add(item);
                        }
                    }
                } finally {
                    if (br != null) {
                        br.close();
                    }

                    Platform.runLater(() -> annotationListView.getItems().setAll(annotationItems));
                    Platform.runLater(() -> annotationListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE));
                    Platform.runLater(() -> userMessagesTextArea.appendText("Loading annotation file... Complete!\n"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateImageView() {
        if (currentlySelectedItem != null) {
            if (previewImageMap.containsKey(currentlySelectedItem))
                Platform.runLater(() -> selectedItemImageView.setImage(previewImageMap.get(currentlySelectedItem)));
            else {
                loadPreviewImage(currentlySelectedItem);
                Platform.runLater(() -> selectedItemImageView.setImage(phImage));
            }
        }
    }
}
