package developmentalVersion;

import java.io.File:
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.swing.JOptionPane;

public class FileReaderTest {
    private static void readInventoryFile() {
        File inputFile = new File("inventory.csv"); 
        FileReader inputFileReader = null;
        BufferedReader inputBufferedReader = null;
        String inventoryLine;

        try {
            
        }
    }

    public static void main(String[] args) {
        String filename = "inventory.txt"; // Change this to your file path
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "File not found: " + filename, "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error reading file: " + filename, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}