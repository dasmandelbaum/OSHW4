package os.hw3;

import java.util.ArrayList;

/**
 * Represents a directory within a FAT32 file system.
 * Author: Jeffrey Hagler, David Mandelbaum
 */
public class Directory
{
    /*
         Info needed for this file system
     */
    String name;
    Directory parentDirectory;
    boolean containsFiles;//see if it holds files
    ArrayList<Directory> files;
    /*
        Stats to print
     */
    int size;
    int nextClusterNumber;//change to first cluster number
    ArrayList<Integer> clusters;
    String attributes;
    /*
        Field for reading
     */
    byte[] text;

    public Directory()
    {
        files = new ArrayList<Directory>();
        clusters = new ArrayList<Integer>();
    }


}