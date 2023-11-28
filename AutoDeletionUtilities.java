import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoDeletionUtilities {

    private static final String ROOT_PATH = "\\\\fccfs02";
    private static final String TEMP_PATH = "C:\\temp"; // Change this to your temporary folder path
    private static final int DELETION_FREQUENCY_DAYS = 30;

    public static void main(String[] args) {
        scheduleMonthlyTask();
    }

    private static void scheduleMonthlyTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule a task to run the file moving and deletion process every month
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                autoMoveAndDeleteFiles();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, calculateInitialDelay(), 30, TimeUnit.DAYS);
    }

    private static long calculateInitialDelay() {
        // Calculate the initial delay until the next month
        Calendar now = Calendar.getInstance();
        int currentDayOfMonth = now.get(Calendar.DAY_OF_MONTH);

        // Calculate the number of days until the next month
        int daysUntilNextMonth = currentDayOfMonth > DELETION_FREQUENCY_DAYS
                ? (30 - currentDayOfMonth) + DELETION_FREQUENCY_DAYS
                : DELETION_FREQUENCY_DAYS - currentDayOfMonth;

        return TimeUnit.DAYS.toMillis(daysUntilNextMonth);
    }

    private static void autoMoveAndDeleteFiles() {
        // Iterate through vertical folders
        for (VerticalFolder vertical : VerticalFolder.values()) {
            File folder = new File(ROOT_PATH + "\\" + vertical.folderName);

            // Iterate through files in the folder
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Check last modified date
                    Date lastModifiedDate = new Date(file.lastModified());
                    if (isOlderThanThreshold(lastModifiedDate)) {
                        // Move the file to the temporary folder
                        moveFileToTemp(file);
                    }
                }
            }
        }

        // Schedule deletion of files in the temporary folder after 1 day
        scheduleTempFolderDeletion();
    }

    private static void moveFileToTemp(File file) {
        // Move the file to the temporary folder
        Path tempFilePath = Paths.get(TEMP_PATH, file.getName());
        try {
            Files.move(file.toPath(), tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File moved to temp folder: " + tempFilePath.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void scheduleTempFolderDeletion() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule a task to delete files from the temp folder after 1 day
        scheduler.schedule(() -> {
            deleteFilesInTempFolder();
        }, 1, TimeUnit.DAYS);
    }

    private static void deleteFilesInTempFolder() {
        File tempFolder = new File(TEMP_PATH);
        File[] files = tempFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                file.delete();
                System.out.println("File deleted from temp folder: " + file.getAbsolutePath());
            }
        }
    }

    private static boolean isOlderThanThreshold(Date lastModifiedDate) {
        // Calculate the deletion threshold
        Date deletionThreshold = calculateDeletionThreshold();

        // Compare last modified date with the threshold
        return lastModifiedDate.before(deletionThreshold);
    }

    private static Date calculateDeletionThreshold() {
        // Calculate the deletion threshold based on the current date and frequency
        return new Date(); // Placeholder - replace with actual implementation
    }

    // Enum to represent the vertical folders
    private enum VerticalFolder {
        Digital("Digital"),
        Rural("Rural"),
        UrbanLAP("Urban LAP"),
        UrbanCentral("Urban Central"),
        UrbanCV("Urban CV"),
        UrbanLAS("Urban LAS"),
        UrbanPL("Urban PL"),
        UrbanBIL("Urban BIL");

        private final String folderName;

        VerticalFolder(String folderName) {
            this.folderName = folderName;
        }
    }
}
