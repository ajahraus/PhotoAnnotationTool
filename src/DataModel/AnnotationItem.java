package DataModel;

import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class AnnotationItem {
    private Path filepath;
    private String circuitName;
    private String structureName;
    private String easting;
    private String northing;
    private String coordinateSystem;
    private String dateString; // This date string is to accommodate loading and storing the date format specifically
    // from the annotation file. Soon this program will bypass the need for an annotation file, in which case I'll be
    // able to use a Date object and allow the user to specify date format in this program.
    private DateTimeFormatter formatter;
    private ImageData imageData;

    public AnnotationItem(String filepath, String circuitName, String structureName, String easting, String northing, String coordinateSystem, String dateString) {
        this.filepath = Paths.get(filepath);
        this.circuitName = circuitName;
        this.structureName = structureName;
        this.easting = easting;
        this.northing = northing;
        this.coordinateSystem = coordinateSystem;
        this.dateString = dateString;
        this.imageData = new ImageData();
    }

    public boolean getImageRotated() {
        return this.imageData.imageRotated;
    }

    public Path getFilepath() {
        return filepath;
    }

    public String getCircuitName() {
        return circuitName;
    }

    public String getStructureName() {
        return structureName;
    }

    public String getEasting() {
        return easting;
    }

    public String getNorthing() {
        return northing;
    }

    public String getCoordinateSystem() {
        return coordinateSystem;
    }

    public String getDateString() {
        return dateString;
    }

    public void removeUnderscoresFromCoordinateSystem() {
        coordinateSystem = coordinateSystem.replace("_", " ");
    }

    public void setImageRotated(boolean rotated) {
        this.imageData.imageRotated = rotated;
    }

    @Override
    public String toString() {
        return filepath.getFileName().toString();
    }

    public String toStringComplete() {
        return filepath.toString() + "\t" +
                circuitName + "\t" +
                structureName + "\t" +
                easting + "\t" +
                northing + "\t" +
                coordinateSystem + "\t" +
                dateString;
    }

    public BufferedImage getBufferedImageFromFilepath() {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(filepath.toFile());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return bufferedImage;
    }

    static public String toString(AnnotationItem item) {
        return item.getFilepath().getFileName().toString();
    }

    public class ImageData {
        private Image inputImageDownScaled;
        private Image annotationOverlay;
        private int annotationAreaHeight = 800;
        public boolean imageRotated;
    }
}
