import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleIDM {

    // private static final int MAX_THREADS = 3;
    private static final String DOWNLOAD_FOLDER = "downloads/";
    private static final List<DownloadTask> downloadTasks = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            System.out.println("Enter your choice:");

            int choice = scanner.nextInt();
            scanner.nextLine(); // consume the newline

            switch (choice) {
                case 1:
                    addDownload(scanner);
                    break;
                case 2:
                    stopAllDownloads();
                    scanner.close();
                    System.out.println("Exiting the program.");
                    System.exit(0);
                    break;
                case 3:
                    resumeDownload(scanner);
                    break;
                case 4:
                    stopDownload(scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\nMenu:");
        System.out.println("1. Add Download");
        System.out.println("2. Exit");
        System.out.println("3. Resume Download");
        System.out.println("4. Stop Download");
    }

    private static void addDownload(Scanner scanner) {
        System.out.println("Enter file URL:");
        String userInput = scanner.nextLine().trim();

        DownloadTask downloadTask = new DownloadTask(userInput);// creating the object of a list downloadTask
        downloadTasks.add(downloadTask);// adding a task
        downloadTask.start();//using seperate thread for a specific download task
    }

    private static void stopDownload(Scanner scanner) {
        System.out.println("Enter file URL to stop download:");
        String userInput = scanner.nextLine().trim();

        for (DownloadTask downloadTask : downloadTasks) {
            if (downloadTask.getFileURL().equals(userInput)) { //matching currently downloading file with user entered file
                downloadTask.interrupt(); // Interrupt the file to stop from downloading
                System.out.println("Download stopped: " + userInput);
                return;
            }
        }

        System.out.println("Download not found for URL: " + userInput);
    }

    private static void stopAllDownloads() {
        for (DownloadTask downloadTask : downloadTasks) {
            downloadTask.interrupt();// Interrupting all the downloadings
        }
        downloadTasks.clear();// realsing all the data form the list
    }

    private static void resumeDownload(Scanner scanner) {
        System.out.println("Enter file URL to resume download:");
        String userInput = scanner.nextLine().trim();

        DownloadTask downloadTask = new DownloadTask(userInput);
        downloadTasks.add(downloadTask);
        downloadTask.start();
    }

    private static class DownloadTask extends Thread {// Defining user define list datatype.
        private final String fileURL;

        public DownloadTask(String fileURL) {
            this.fileURL = fileURL;
        }

        public String getFileURL() {
            return fileURL;
        }

        @Override
        public void run() {//overriding method of a thread class 
            try {
                downloadFile(fileURL);
            } catch (InterruptedException e) {
                System.out.println("Download interrupted: " + fileURL);
            } catch (IOException e) {
                System.err.println("Error downloading file from " + fileURL + ": " + e.getMessage());
            } finally {
                downloadTasks.remove(this); // releasing downloaded task from the queue handel through arraylist
            }
        }

        private void downloadFile(String fileURL) throws InterruptedException, IOException {
            URL url = new URL(fileURL);//Creating object of the URL to prefom request on targeted url

            //Setting up connection between url and user now we can request from url through connection
            //refrence variable
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            //fenching name of the file from its link
            String fileName = fileURL.substring(fileURL.lastIndexOf('/') + 1);

            File categoryFolder = new File(DOWNLOAD_FOLDER);
            categoryFolder.mkdirs();

            /*if categoryFolder is set to "downloads/" and fileName is set to "example.txt", 
            then outputFile represents the file path "downloads/example.txt". This file path 
            is used in the program to save the downloaded content to the local file system. */

            File outputFile = new File(categoryFolder, fileName);

            // Storing no. of bites of file downloaded till now
            long downloadedBytes = 0;

            //Checking if file exist, if so then is it partially exists then download the
            //remaining part of the file by taking length of the partially downloaded file
            if (outputFile.exists()) {
                downloadedBytes = outputFile.length();
                connection.setRequestProperty("Range", "bytes=" + downloadedBytes + "-");
            }
            /*Reads file located in remote server and write in a outputFile rw stands for read & write */
            try (InputStream inputStream = connection.getInputStream();
                 RandomAccessFile outputFileStream = new RandomAccessFile(outputFile, "rw")) {

                    /*The seek method is called on the RandomAccessFile object. 
                    It moves the file pointer to a specified position in the file.  */
                outputFileStream.seek(downloadedBytes);

                byte[] buffer = new byte[1024]; //Storing actual data temperary with each iteration
                int bytesRead; // Stroring size of data read with each iteration

                while ((bytesRead = inputStream.read(buffer)) != -1) {// reading data till inputStream.read returns -1
                    if (isInterrupted()) {
                        System.out.println("Download interrupted: " + fileName);
                        throw new InterruptedException("Download interrupted");//If interuption by user or by server
                    }

                    outputFileStream.write(buffer, 0, bytesRead);//writing data into the file buffer store data bytesRead store size of data
                    downloadedBytes += bytesRead;
                }

                System.out.println("Download complete: " + fileName);
            } finally {
                connection.disconnect();
            }
        }
    }
}


