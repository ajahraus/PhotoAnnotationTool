package DataModel;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class AnnotationItem {
    public boolean imageRotated;
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
        this.imageData = new ImageData(this.filepath.toFile());
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
        private Metadata inputImageMetadata;
        private int annotationAreaHeight = 800;

        private ImageData(File file) {
            try {
                inputImageMetadata = ImageMetadataReader.readMetadata(file);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ImageProcessingException e) {
                e.printStackTrace();
            }

        }
    }
}
