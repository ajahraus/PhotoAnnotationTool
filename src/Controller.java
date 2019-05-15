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

    public void initialize() {
        annotationItems = new ArrayList<>();

        phImage = new Image("placeholder-image.jpg");

        previewImageMap = new HashMap<>();

        rightLogo = new Image("ATC_Logo_Resized.jpg");
        leftLogo = new Image("LLC_Logo_Resized.jpg");

        if (true) {
            annotationFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\W3103\\W3103_Annotate.txt");
            annotationFileLabel.setText(annotationFileLocation.toString());

            outputFileLocation = Paths.get("Q:\\19-387\\TowerPhotos\\W3103\\Annotated2");
            outputFileLocationLabel.setText("Output file location: " + outputFileLocation.toString());

            Runnable task = () -> {
                loadInBackgroundThread();
            };

            new Thread(task).start();
        }

        selectedItemImageView.setImage(phImage);
        selectedItemImageView.preserveRatioProperty().setValue(true);
        selectedItemImageView.setFitWidth(800);
        selectedItemImageView.setCache(true);
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

        if (selectedFile != null)
            annotationFileLocation = selectedFile.toPath();

        Runnable task = () -> {
            loadInBackgroundThread();
        };

        if (annotationFileLocation != null) {
            annotationFileLabel.setText(annotationFileLocation.toString());
            new Thread(task).start();
        }
    }

    @FXML
    private void loadAnnotationFile() throws IOException {

        BufferedReader br = Files.newBufferedReader(annotationFileLocation);
        String input;

        Platform.runLater(() -> userMessagesTextArea.appendText("Loading annotation file...\n"));

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
        }

        Platform.runLater(() -> annotationListView.getItems().setAll(annotationItems));
        Platform.runLater(() -> annotationListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE));
        Platform.runLater(() -> userMessagesTextArea.appendText("Loading annotation file... Complete!\n"));

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
    public void updateSelectedItemImagePreview() {
        AnnotationItem item = (AnnotationItem) annotationListView.getSelectionModel().getSelectedItem();

        Platform.runLater(() -> selectedAnnotationItemLabel.setText(item.toString()));

        if (previewImageMap.containsKey(item)) {
            Image image = previewImageMap.get(item);
            Platform.runLater(() -> selectedItemImageView.setImage(image));
        } else {
            Platform.runLater(() -> selectedItemImageView.setImage(phImage));
            Platform.runLater(() -> userMessagesTextArea.appendText("Image not yet loaded, please wait.\n"));
        }
    }

    @FXML
    private void loadInBackgroundThread() {
        try {
            loadAnnotationFile();
            loadPreviewMap();
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateCurrentProcess(String progressName, double processProgress) {
        Platform.runLater(() -> currentProcessLabel.setText(progressName));
        Platform.runLater(() -> currentProcessProgressBar.setProgress(processProgress));
    }

    public void exportAllPhotos() {
        Runnable exportTask = () -> {
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

                    final double progress = (double) i / (double) annotationItems.size();
                    Platform.runLater(() -> updateCurrentProcess("Exporting images", progress));
                } else {
                    Platform.runLater(() -> userMessagesTextArea.appendText("Warning: Photo " + item.getFilepath().getFileName() +
                            " loaded from the output directory. Skipping, so as to not save over original.\n"));
                }
            }
            Platform.runLater(() -> updateCurrentProcess("None", 0));
        };
        new Thread(exportTask).start();

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

        BufferedImage tmp = new BufferedImage(inputImage.getWidth(), inputImage.getHeight() + 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = tmp.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, inputImage.getHeight(), tmp.getWidth(), 800);
        graphics.drawImage(inputImage, 0, 0, null);

        graphics.drawImage(SwingFXUtils.fromFXImage(leftLogo, null), 0, inputImage.getHeight(), null);
        graphics.drawImage(SwingFXUtils.fromFXImage(rightLogo, null), (int) (inputImage.getWidth() - rightLogo.getWidth()), inputImage.getHeight(), null);

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
        java.awt.Image tmp = SwingFXUtils.fromFXImage(image, null).getScaledInstance((int) (image.getWidth() * (800 / image.getHeight())), 800, java.awt.Image.SCALE_SMOOTH);

        BufferedImage dimg = new BufferedImage((int) (image.getWidth() * (800 / image.getHeight())), 800, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = dimg.createGraphics();
        graphics.drawImage(tmp, 0, 0, null);
        graphics.dispose();

        return SwingFXUtils.toFXImage(dimg, null);
    }

    private void loadPreviewMap() {
        Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images...\n"));
        for (int i = 0; i < annotationItems.size(); i++) {
            AnnotationItem item = annotationItems.get(i);
            previewImageMap.put(item, createPreviewImageUsingSwingUtilFromItem(item));
            final double progress = (double) i / (double) annotationItems.size();
            Platform.runLater(() -> updateCurrentProcess("Loading preview images", progress));
        }
        Platform.runLater(() -> userMessagesTextArea.appendText("Loading preview images... Complete!\n"));
        Platform.runLater(() -> updateCurrentProcess("None", 0));
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

    private BufferedImage dropAlphaChannel(BufferedImage src) {
        BufferedImage convertedImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        convertedImg.getGraphics().drawImage(src, 0, 0, null);

        return convertedImg;
    }
}
