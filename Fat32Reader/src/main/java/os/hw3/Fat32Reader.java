package os.hw3;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/************************************************************
 *   Name of program: Fat32Reader
 *   Authors: Jeffrey Hagler, David Mandelbaum
 *   Description: a program that supports file system commands (from specs)
 *   See readme for more
 **********************************************************/
public class Fat32Reader {

    private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private Directory fs;
    FileHandler fh;
    private String header;
    Boot boot;
    private String volumeName;
    int currentLocation;
    private int[] firstThreeFreecClusters;
    private int numFreeClusters;
    private int fat1Address;
    private int fat2Address;

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Fat32Reader() throws IOException
    {
        LogManager.getLogManager().reset();//https://stackoverflow.com/a/3363747
        LOGGER.setLevel(Level.INFO);
        fh = new FileHandler("fat32.log");
        LOGGER.addHandler(fh);
        setHeader("/");
        fs = new Directory();
        currentLocation = 0;
        firstThreeFreecClusters = new int[3];
        numFreeClusters = 0;
    }

    /**
     * Main method. Begins with getting info, then parsing root, then taking commands
     * @param args - the Fat32 File image
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        Fat32Reader fr = new Fat32Reader();

        /* Parse args and open our image file */
        File file = new File(args[0]);
        //System.out.println("File exists: " + file.exists());//TEST
        //System.out.println("Fat32 file path: " + file.getAbsolutePath());//TEST
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        /* Parse boot sector and get information */
        fr.boot = new Boot(raf, fr);
        fr.fat1Address = fr.getAddress(fr.getFATSecNum(1));
        fr.fat2Address = (((fr.getAddress(fr.boot.getRootDirAddress()) - fr.fat1Address) / 2) + fr.fat1Address);//added late
        /* Get root directory address */
        int startOfRootDirectory = fr.getAddress(fr.boot.getRootDirAddress());
        //System.out.println("rootDirAddress is 0x" + Integer.toHexString(fr.boot.getRootDirAddress()) + ", " + fr.boot.getRootDirAddress());//TEST
        //System.out.println("Skipping to " + (startOfRootDirectory));//TEST
        fr.currentLocation = startOfRootDirectory;
        raf.seek(fr.currentLocation);

        //start with root
        Directory dir = new Directory();
        fr.parseCluster(raf, dir);//just for the root


        fr.fs = dir;//set to current directory
        fr.fs.parentDirectory = null;
        int n = fr.boot.getBPB_RootClus();//fr.fs.nextClusterNumber;

        //System.out.println("Root clus: " + n);//TEST
        fr.fs.clusters = fr.getClusters(raf, fr.getFATSecNum(n), fr.getFATEntOffset(n), fr.fs.nextClusterNumber);
        raf.seek(startOfRootDirectory + 32);
        fr.parseDirectories(raf,fr.fs);

        fr.fs.nextClusterNumber = fr.boot.getBPB_RootClus();//added late
        fr.fs.clusters.clear();
        fr.fs.clusters.add(fr.boot.getBPB_RootClus());//added late

        /*Get free cluster indices and count*/

        //go to BPB_FSInfo location - FSINFO sector
        fr.currentLocation = fr.getAddress(fr.boot.getBPB_FSInfo());

        //get number of free clusters
        byte[] FSI_Free_Count = new byte[4];
        fr.numFreeClusters = Integer.parseInt(fr.getValue(raf, FSI_Free_Count, 488, 4), 16);
        //System.out.println(fr.numFreeClusters);

        //get first three free cluster numbers
        byte[] FSI_Nxt_Free = new byte[4];
        int firstFree = Integer.parseInt(fr.getValue(raf, FSI_Nxt_Free, 492, 4), 16);
       // System.out.println("this is the first free cluster: " + firstFree);
        fr.firstThreeFreecClusters[0] = firstFree;
        int secondFree = fr.getNextFreeCluster(raf, firstFree);
       // System.out.println("this is the second free cluster: " + secondFree);
        fr.firstThreeFreecClusters[1] = secondFree;
        int thirdFree = fr.getNextFreeCluster(raf, secondFree);
        fr.firstThreeFreecClusters[2] = thirdFree;
        //System.out.println(fr.firstThreeFreecClusters[0]);
        //System.out.println(fr.firstThreeFreecClusters[1]);
        //System.out.println(fr.firstThreeFreecClusters[2]);

        /* Main loop.  You probably want to create a helper function for each command besides quit. */
        Scanner s = new Scanner(System.in);
        String input;
        String[] inputParts;
        while(true)
        {
            System.out.print(fr.getHeader() + "]");//print prompt
            input = s.nextLine().toLowerCase();
            inputParts = input.split(" ");
            String command = inputParts[0];
            /* Start comparing input */
            if(inputParts.length == 2)
            {
                String fName = inputParts[1];
                if (command.equalsIgnoreCase("cd"))
                {
                    fr.cd(fName, raf);
                }
                else if (command.equalsIgnoreCase("ls"))
                {
                    fr.ls(fName);
                }
                else if(command.equalsIgnoreCase("stat"))
                {
                    fr.stat(fName, raf);
                }
                else if(command.equalsIgnoreCase("delete"))
                {
                    fr.delete(fName, raf);
                }
                
                else
                {
                    System.out.println("Unrecognized command.");
                }
            }
            
            else if(inputParts.length == 3 && command.equalsIgnoreCase("newfile")) 
            {
            		System.out.println("newFile!!!");
            		fr.newFile(inputParts[1], inputParts[2], raf);
            }
        
            else if(inputParts.length == 4 && command.equalsIgnoreCase("read"))
            {
                fr.read(raf, inputParts[1], inputParts[2], inputParts[3]);
            }
            else if(inputParts.length == 1)
            {
                if (command.equalsIgnoreCase("info"))
                {
                    fr.printInfo();
                }
                else if(command.equalsIgnoreCase("volume"))
                {
                    fr.volume();
                }
                else if (command.equalsIgnoreCase("quit"))
                {
                    break;
                }
                else if(command.equalsIgnoreCase("freelist"))
                {
                    fr.getFreelist();
                }
                else
                {
                    System.out.println("Unrecognized command.");
                }
            }
            else
                {
                System.out.println("Unrecognized command.");
            }
        }


        /* Close the file */
        raf.close();

        /* Success */
    }
    
    private void newFile(String fileName, String size, RandomAccessFile raf) throws IOException 
    {
        //FIX THE ACTUAL FILE
        //go to the clusters in the data region and add the file (make sure not to add too much to the last cluster)
        //seek to first free cluster, then continue in next free if run out of room
        ArrayList<Integer> clusters = new ArrayList<Integer>();
        clusters = writeNewFileClusters(size, raf, clusters);
        writeToFat(raf, clusters);

        // FIX THE PARENT DIRECTORY
        //TODO: create a new file with the given name

        //go through the parentDirectory in the data region and look for an open 32
   		int start32 = parentDirectoryAdd(fileName, size, raf);

   		//if there is no space then you have to add a new cluster
   		if(start32 == -1)
   		{
   			//find the first available cluster
   			int freeCluster = firstThreeFreecClusters[0];
   			
   			//DIDN'T FINISH YET - go to that cluster in the data region and add new file with the given name in the first 32 bytes
            int nextAddress = getAddress(this.boot.getRootDirAddress() + freeCluster - this.boot.getBPB_RootClus());
            currentLocation = nextAddress;
   			
   			//replace it with the next free cluster
   	        int nextFree = getNextFreeCluster(raf, firstThreeFreecClusters[2]);
   	        firstThreeFreecClusters[0] = nextFree;
            Arrays.sort(firstThreeFreecClusters);
   			
   			//update numFreeClusters (subtract 1)
   			numFreeClusters--;
            
   			//update FAT (both) - this cluster will be FFFFFFFF and the previous last cluster will have the current last cluster
           // int fat1Address = getAddress(getFATSecNum(1));
   			fixFat(freeCluster, raf, fat1Address);
            //int fat2Address = (((getAddress(this.boot.getRootDirAddress()) - fat1Address) / 2) + fat1Address);
   			fixFat(freeCluster, raf, fat2Address);
   		}
   		else //if there is space in parent directory info cluster then add newFile line there
        {
            raf.seek(start32);//go back to beginning of open entry in parent
            //raf.write(newDirectoryInfoLine);
   		}
   		

   		
    }

    private ArrayList<Integer> writeNewFileClusters(String size, RandomAccessFile raf, ArrayList<Integer> clusters) throws IOException {
        int nextAddress = getFirstFreeCluster(clusters, raf);

        //replace it with the next free cluster
        int nextFree = getNextFreeCluster(raf, firstThreeFreecClusters[2]);
        firstThreeFreecClusters[0] = nextFree;
        Arrays.sort(firstThreeFreecClusters);
        //TODO:clean out free cluster

        raf.seek(nextAddress);
        currentLocation = nextAddress;
        int count = 0;
        for (int i = 0; i < Integer.parseInt(size); i += 13)
        {
            if(count + 13 > this.boot.getBPB_BytesPerSec())
            {
                raf.seek(getFirstFreeCluster(clusters, raf));
                count = 0;
            }
            System.out.println("writing at " + currentLocation);
            String toWrite = "New File.\r\n";
            for(char c : toWrite.toCharArray())
            {
                String charToWrite = Integer.toHexString(c);
                System.out.println("About to write: " + charToWrite);
                byte b = (byte) Integer.parseInt(charToWrite, 16);
                raf.write(b);
            }
            /*byte[] b = toWrite.getBytes(StandardCharsets.US_ASCII);//https://stackoverflow.com/a/5688062
            for(int j = 0; j < b.length; j++)
            {
                System.out.println("About to write: " + b[j]);
                raf.write(b[j]);
            }*/
            currentLocation += 13;
        }
        return clusters;
    }

    private void writeToFat(RandomAccessFile raf, ArrayList<Integer> clusters) throws IOException {
        //go to each cluster in each fat and set them to the next cluster (FFFFFFFF for the last cluster)
        for(int i = 0; i < clusters.size() - 1; i++)// cluster: clusters)
        {
            int cluster = clusters.get(i);
            int nextAddress = fat1Address + getFATEntOffset(cluster);
            //write address of next cluster
            raf.seek(nextAddress);
            int nextClusterInFile = getFATEntOffset(clusters.get(i + 1));
            String bytes = Integer.toHexString(nextClusterInFile);
            while(bytes.length() < 8)
            {
                bytes = "0".concat(bytes);
            }
            //System.out.println(bytes);
            for(int m = 8; m > 1; m = i - 2)
            {
                byte b = (byte) Integer.parseInt(bytes.substring(m - 2, m), 16);
                raf.write(b);
            }
        }
        int cluster = clusters.get(clusters.size() - 1);
        int nextAddress = fat1Address + getFATEntOffset(cluster);
        raf.seek(nextAddress);
        //write address of next cluster
        //change value from 00000000 to FFFFFFFF
        for(int i = 0; i < 4; i++)
        {
            byte b = (byte) 0xFF;
            raf.write(b);
        }
    }

    //update FAT (both)- this cluster will be FFFFFFFF and the previous last cluster will be the current last cluster
    private void fixFat(int freeCluster, RandomAccessFile raf, int location) throws IOException 
    {
		//previous last cluster
		int previousLastCluster = this.fs.clusters.get(this.fs.clusters.size()-1);

		//Change the next cluster number to hex and replace FFFFFFFF and add zeros is necessary 
		String bytes = Integer.toHexString(freeCluster);
        while(bytes.length() < 8)
        {
            bytes = "0".concat(bytes);
        }
        
        //Go to location of last cluster in fat and replace it with next cluster number in hex
        int clusterEntryAddress = location + getFATEntOffset(previousLastCluster);
        raf.seek(clusterEntryAddress);

        for(int i = 8; i > 1; i = i - 2)
        {
            byte b = (byte) Integer.parseInt(bytes.substring(i - 2, i), 16);            
            raf.write(b);
            //System.out.println("FSI FREE COUNT " + b);//TEST
        }
			
		
		//current last cluster
        //Go to location of the current last cluster
        raf.seek(location + getFATEntOffset(freeCluster));
        
        //change value from 00000000 to FFFFFFFF
	    for(int i = 0; i < 4; i++)
	    {
            byte b = (byte) 0xFF;
            raf.write(b);
	    }    	
    }
    
    //returns the beginning of the 32 byte space that we will be adding to
    private int parentDirectoryAdd(String fileName, String size, RandomAccessFile raf) throws IOException 
    {
        for(int cluster: this.fs.clusters)
        {
             System.out.println("Cluster number: " + cluster);
             int nextAddress = getAddress(this.boot.getRootDirAddress() + cluster - this.boot.getBPB_RootClus());
             currentLocation = nextAddress;
             System.out.println("Address: " + nextAddress);
             raf.seek(nextAddress);
             //search for file name in cluster
             for (int i = 0; i < 16; i++)//parse each 32 bit potential entry
             {
                 //System.out.println(currentLocation);
                 byte firstByte = raf.readByte();
                 System.out.println("This is my current location: " + currentLocation);
                 this.currentLocation += 1;
                 System.out.println("this is the first byte: " + firstByte);//TEST
                 if(firstByte == -27 || firstByte == 0)//found open space in parent
                 {
                     currentLocation -= 1;
                     return currentLocation;
                 }
                 else
                 {
                     raf.seek(currentLocation + 31);//try next one
                     currentLocation += 31;
                 }
             }
        }
         return -1;
     }

    public int getFirstFreeCluster(ArrayList<Integer> clusters, RandomAccessFile raf) throws IOException
    {
        int nextAddress = getAddress(this.boot.getRootDirAddress() + firstThreeFreecClusters[0] - this.boot.getBPB_RootClus());
        System.out.println("Adding cluster " + firstThreeFreecClusters[0] + " to clusters");
        clusters.add(firstThreeFreecClusters[0]);
        System.out.println(nextAddress);
        //raf.seek(nextAddress);

        //replace it with the next free cluster
        int nextFree = getNextFreeCluster(raf, firstThreeFreecClusters[2]);
        firstThreeFreecClusters[0] = nextFree;
        Arrays.sort(firstThreeFreecClusters);
        return nextAddress;
    }

    private void delete(String fName, RandomAccessFile raf) throws IOException
    {
        boolean marked = false;
        //mark file as deleted in current directory
        for(Directory dir: this.fs.files)
        {
            if (dir.name.equalsIgnoreCase(fName) && dir.containsFiles == false)//dealing with file not directory
            {
                marked = markDirectoryEntries(raf, this.fs.clusters, dir.name);
                //int fat1Address = getAddress(getFATSecNum(1));
                //System.out.println(fat1Address);//TEST
                updateFat(raf, fat1Address, dir.nextClusterNumber, 1);
                //int fat2Address = (((getAddress(this.boot.getRootDirAddress()) - fat1Address) / 2) + fat1Address);
                //System.out.println(fat2Address);//TEST
                updateFat(raf, fat2Address, dir.nextClusterNumber, 2);
                break;
            }
            else if(dir.name.equalsIgnoreCase(fName) && dir.containsFiles == true)
            {
                LOGGER.log(Level.WARNING, fName + " is directory and cannot be deleted.");
                System.out.println("Error: cannot delete directory");
                break;
            }
        }
        if(marked)//marking worked, now refresh parent object and update fsinfo sector fields
        {
            this.fs.files.clear();
            if(this.fs.parentDirectory == null)//root
            {
                int startOfRootDirectory = getAddress(this.boot.getRootDirAddress()) + 32;
                this.currentLocation = startOfRootDirectory;
                raf.seek(this.currentLocation);
            }
            parseDirectories(raf, this.fs);
            this.currentLocation = this.getAddress(this.boot.getBPB_FSInfo()) + 488;
            raf.seek(this.currentLocation);
            //fill up with numfreecluster
            String bytes = Integer.toHexString(numFreeClusters);
            while(bytes.length() < 8)
            {
                bytes = "0".concat(bytes);
            }
            //System.out.println(bytes);
            for(int i = 8; i > 1; i = i - 2)
            {
                byte b = (byte) Integer.parseInt(bytes.substring(i - 2, i), 16);
                raf.write(b);
                //System.out.println("FSI FREE COUNT " + b);//TEST
            }
            //reset first  free cluster number in fsinfo sector
            byte[] FSI_Nxt_Free = new byte[4];
            //492, 4
            bytes = Integer.toHexString(firstThreeFreecClusters[0]);
            while(bytes.length() < 8)
            {
                bytes = "0".concat(bytes);
            }
            //System.out.println(bytes);
            for(int i = 8; i > 1; i = i - 2)
            {
                byte b = (byte) Integer.parseInt(bytes.substring(i - 2, i), 16);
                raf.write(b);
                //System.out.println("FSI FREE COUNT " + b);
            }
            //TODO: RECALIBRATE?
            //firstThreeFreecClusters[1] = getNextFreeCluster(raf, firstThreeFreecClusters[0]);
            //firstThreeFreecClusters[2] = getNextFreeCluster(raf, firstThreeFreecClusters[1]);
//            /*
//            TEST
//             */
//            this.currentLocation = this.getAddress(this.boot.getBPB_FSInfo()) + 492;
//            raf.seek(this.currentLocation);
//            FSI_Nxt_Free = new byte[4];
//            int firstFree = Integer.parseInt(this.getValue(raf, FSI_Nxt_Free, 0, 4), 16);
//            System.out.println(firstFree);
        }


        //update number of free clusters and first free clusters if its earlier than current first free clusters
    }

    private boolean markDirectoryEntries(RandomAccessFile raf, ArrayList<Integer> clusters, String toDelete) throws IOException
    {
        for(int cluster: clusters)
        {
            //System.out.println(cluster);
            int nextAddress = getAddress(this.boot.getRootDirAddress() + cluster - this.boot.getBPB_RootClus());
            currentLocation = nextAddress;
            //System.out.println(nextAddress);
            raf.seek(nextAddress);
            //search for file name in cluster
            for (int i = 0; i < 16; i++)//parse each 32 bit potential entry
            {
                //System.out.println(currentLocation);
                byte[] DIR_Name = new byte[11];//short name - 0 -> 11
                raf.read(DIR_Name, 0, 11);
                this.currentLocation += 11;
                // System.out.println(DIR_Name[0]);//TEST
                String byteString = new String(DIR_Name, "UTF-8");//https://stackoverflow.com/a/18583290
                String[] splitName = byteString.split(" +");
                if(splitName.length == 2)
                {
                    byteString = splitName[0] + "." + splitName[1];
                    byteString = byteString.trim();//lowercase?
                }
                else
                {
                    byteString = byteString.trim();//lowercase?
                }
                //System.out.println("This is the directory's name: " + byteString);//TEST
                if(byteString.equalsIgnoreCase(toDelete))
                {
                    //mark it
                    raf.seek(currentLocation - 11);//go back to beginning
                    byte b = (byte) 0xE5;
                    raf.write(b);
                            /*
                                test

                            raf.seek(currentLocation - 11);//go back to beginning
                            System.out.println("Just marked location with " + raf.readByte());*/
                    return true;
                }
                else
                {
                    raf.seek(currentLocation + 21);//try next one
                    currentLocation += 21;
                }
            }
        }
        return false;
    }

    private void updateFat(RandomAccessFile raf, int location, int firstCluster, int fatNum) throws IOException
    {
        String valueString = "";
        int nextCluster = firstCluster;
        int clusterEntryAddress = 0;
        while(!valueString.equalsIgnoreCase("0FFFFFF8") && !valueString.equalsIgnoreCase("0FFFFFFF") && !valueString.equalsIgnoreCase("FFFFFFFF") && !valueString.equalsIgnoreCase("00000000"))
        {
            //System.out.println("Resetting fat for cluster " + nextCluster);
            valueString = "";
            clusterEntryAddress = location + getFATEntOffset(nextCluster);
            //System.out.println("Address of fat entry: " + Integer.toHexString(clusterEntryAddress));//TEST
            raf.seek(clusterEntryAddress);
            byte[] value = new byte[4];
            raf.read(value, 0, 4);
            for(int i = value.length - 1; i >= 0; i--)
            {
                byte b = value[i];
                //System.out.printf("0x%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
                valueString += String.format("%02X", b);
            }
            if(fatNum == 1  && nextCluster > this.boot.getBPB_RootClus()) //only update for first fat
            {
                numFreeClusters++;
                if (firstThreeFreecClusters[2] > nextCluster) {
                    firstThreeFreecClusters[2] = nextCluster;
                    Arrays.sort(firstThreeFreecClusters);
                }
            }
            //System.out.println("Value in string: " + valueString);//TEST
            if(!valueString.equalsIgnoreCase("0FFFFFF8") && !valueString.equalsIgnoreCase("0FFFFFFF") && !valueString.equalsIgnoreCase("FFFFFFFF") && !valueString.equalsIgnoreCase("00000000"))
            {
                //there is another cluster to reset
                nextCluster = Integer.parseInt(valueString, 16);
            }
            //go back to fat entry and set to free with 00000000
            raf.seek(clusterEntryAddress);
            for(int i = 0; i < 4; i++)
            {
                byte b = (byte) 0x00;
                raf.write(b);
            }

        }
    }


    /**
     * Take current free and get next free
     * @param raf
     * @param currentFree
     * @return
     */
    private int getNextFreeCluster(RandomAccessFile raf, int currentFree) throws IOException
    {
        int potentialFreeCluster = currentFree;// + 1;
        int nextClusterEntryAddress = getAddress(getFATSecNum(potentialFreeCluster)) + getFATEntOffset(potentialFreeCluster);
        int endOfFat = 532992;//TODO: change to real number
        while(nextClusterEntryAddress < endOfFat)
        {
            potentialFreeCluster = potentialFreeCluster + 1;
            nextClusterEntryAddress = getAddress(getFATSecNum(potentialFreeCluster)) + getFATEntOffset(potentialFreeCluster);
            //System.out.println("Address of fat entry: " + Integer.toHexString(nextClusterEntryAddress));//TEST
            raf.seek(nextClusterEntryAddress);
            byte[] value = new byte[4];
            String valueString = "";
            raf.read(value, 0, 4);
            for (int i = value.length - 1; i >= 0; i--) {
                byte b = value[i];
                //System.out.printf("0x%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
                valueString += String.format("%02X", b);
            }
            //System.out.println("Value in string: " + valueString);//TEST
            if (valueString.equals("00000000")) {
                return potentialFreeCluster;
            }
            else if(this.numFreeClusters <= 0)
            {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Return address of cluster
     * @param i
     * @return
     */
    private int getAddress(int i)
    {
        return i * this.boot.getBPB_BytesPerSec();
    }

    /**
     * Goes through directory to add its children (files and directories) so directory can be accessed
     * @param raf
     * @param dir
     * @throws IOException
     */
    private void parseDirectories(RandomAccessFile raf, Directory dir) throws IOException
    {
        Directory newDir;
        //parse directory
        for(int j = 0; j < this.fs.clusters.size(); j++)//for each cluster in directory
        {
            if(dir.parentDirectory != null) //not the root
            {
                int clusterAddress = this.boot.getRootDirAddress() + dir.clusters.get(j) - this.boot.getBPB_RootClus();
                clusterAddress = getAddress(clusterAddress);
                raf.seek(clusterAddress);
                this.currentLocation = clusterAddress;
                //System.out.println("Parsing in Cluster address " + Integer.toHexString(clusterAddress) + " from cluster " + dir.clusters.get(j));//TEST
            }
            //parse cluster
            for (int i = 0; i < 16; i++)//parse each 32 bit potential entry
            {
                newDir = new Directory();
                if (parseCluster(raf, newDir))
                {
                    if(!newDir.name.equalsIgnoreCase("done"))
                    {
                        newDir.parentDirectory = dir;
                        dir.files.add(newDir);
                    }
                    else //found 00 as first byte, done with directory
                    {
                        break;
                    }
                }
            }
            //https://stackoverflow.com/a/19471040 - order the directories alphabetically
            Collections.sort(dir.files, new Comparator<Directory>() {
                @Override
                public int compare(Directory lhs, Directory rhs)
                {
                    return lhs.name.compareTo(rhs.name);
                }
            });
        }
    }

    /**
     * Go through the file/directory entry within parent directory
     * @param raf
     * @param dir
     * @return
     * @throws IOException
     */
    private boolean parseCluster(RandomAccessFile raf, Directory dir) throws IOException
    {
        //System.out.println("CURRENT LOCATION BEGINNING OF PARSE: " + this.currentLocation);
        byte[] DIR_Name = new byte[11];//short name - 0 -> 11
        raf.read(DIR_Name, 0, 11);
        this.currentLocation += 11;
        //System.out.println(currentLocation);//TEST
        if(DIR_Name[0] == -27)
        {
            //done with directories
            this.currentLocation += 21;
            raf.seek(this.currentLocation);
            return false;
        }
        else if(DIR_Name[0] == 0)
        {
            dir.name = "done";
            return true;
        }
        String byteString = new String(DIR_Name, "UTF-8");//https://stackoverflow.com/a/18583290
        //System.out.println(byteString);//TEST
        String[] splitName = byteString.split(" +");
        if(splitName.length == 2)
        {
            dir.name = splitName[0] + "." + splitName[1];
            dir.name = dir.name.trim();//lowercase?
        }
        else
        {
            dir.name = byteString.trim();//lowercase?
        }
       //System.out.println("This is the directory's name: " + dir.name);//TEST
        byte[] DIR_Attr = new byte[1];//file attributes - 11 -> 12
        raf.read(DIR_Attr,0,1);
        this.currentLocation += 1;
        String temp = "";
        for(int i = DIR_Attr.length - 1; i >= 0; i--)
        {
            byte b = DIR_Attr[i];
            //System.out.printf("%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
            temp += b;//String.format("%02X", b);
        }
        //System.out.println("attribute: " + temp);//TEST
        if(!temp.equalsIgnoreCase("1") && !temp.equalsIgnoreCase("2") && !temp.equalsIgnoreCase("4") && !temp.equalsIgnoreCase("8") && !temp.equalsIgnoreCase("16") && !temp.equalsIgnoreCase("32"))
        {
            this.currentLocation += 20;
            raf.seek(this.currentLocation);
            return false;
        }
        setAttribute(dir, byteString, temp);

        byte[] DIR_FstClusHI = new byte[2];//High word of this entry’s first cluster number - 20 -> 22
        String hi = getValue(raf, DIR_FstClusHI, 8, 2);
        this.currentLocation += 10;
        //System.out.println("Hi value: " + hi);//TEST

        byte[] DIR_FstClusLO = new byte[2];//Low word of this entry’s first cluster number - 26 -> 28
        String lo = getValue(raf, DIR_FstClusLO, 4, 2);
        this.currentLocation += 6;
        //System.out.println("Lo value: " + lo);//TEST
        String clus = hi.concat(lo);
        //System.out.println("Cluster value: " + clus);//TEST
        dir.nextClusterNumber = Integer.parseInt(clus, 16);
        //System.out.println("next cluster number: 0x" + Integer.toHexString(dir.nextClusterNumber));//TEST

        byte[] DIR_FileSize = new byte[4];//32-bit DWORD holding this file’s size in bytes. - 28-32
        //System.out.println("CURRENT LOCATION END OF PARSE: " + this.currentLocation);//TEST
        temp = getValue(raf, DIR_FileSize, 0, 4);
        this.currentLocation += 4;
        //System.out.println("File size: 0x" + temp);//TEST
        dir.size = Integer.parseInt(temp, 16);
        return true;
    }

    /**
     *
     * @param dir
     * @param byteString
     * @param temp
     */
    private void setAttribute(Directory dir, String byteString, String temp)
    {
        //System.out.println(temp);
        if(temp.equalsIgnoreCase("1"))//root
        {
            dir.attributes = "ATTR_READ_ONLY";
        }
        else if(temp.equalsIgnoreCase("2"))//root
        {
            dir.attributes = "ATTR_HIDDEN";
        }
        else if(temp.equalsIgnoreCase("4"))//root
        {
            dir.attributes = "ATTR_SYSTEM";
        }
        else if(temp.equalsIgnoreCase("8"))//root - TODO: Make . and .. directories and add it?
        {
            dir.attributes = "ATTR_VOLUME_ID";
            this.volumeName = byteString.substring(0, byteString.indexOf(" "));
        }
        else if(temp.equalsIgnoreCase("16"))
        {
            dir.attributes = "ATTR_DIRECTORY";
            dir.containsFiles = true;
        }
        else if(temp.equalsIgnoreCase("32"))
        {
            dir.attributes = "ATTR_ARCHIVE";
        }
    }

    /**
     * returns string value with proper endianess
     * @param raf
     * @param buffer
     * @param skip
     * @param size
     * @return
     * @throws IOException
     */
    private String getValue(RandomAccessFile raf, byte[] buffer, int skip, int size) throws IOException {
        raf.seek(this.currentLocation + skip);  //do I even need this seek anymore?
        raf.read(buffer, 0, size);
        String temp = "";
        for(int i = buffer.length - 1; i >= 0; i--)
        {
            byte b = buffer[i];
            //System.out.printf("%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
            temp += String.format("%02X", b);
        }
        return temp;
    }

    /**
     * Prints out the following info (in both hex and base 10 - saved in fields as base 10)
     * BPB_BytesPerSec (512, 1024, 2048 or 4096 - offset 11 bytes, size 2 bytes)
     * BPB_SecPerClus (legal values are 1, 2, 4, 8, 16, 32, 64, and 128 - offset 13 bytes, size 1 bytes)
     * BPB_RsvdSecCnt (typically 32, cannot be 0 - offset 14 bytes, size 2 bytes)
     * BPB_NumFATS (standard value is 2 - offset 16 bytes, size 1 bytes)
     * BPB_FATSz32 (offset 36 bytes, size 4 bytes)
     */
    private void printInfo()
    {
        System.out.println("BPB_BytesPerSec is 0x" + Integer.toHexString(this.boot.getBPB_BytesPerSec()) + ", " + this.boot.getBPB_BytesPerSec());
        System.out.println("BPB_SecPerClus is 0x" +  Integer.toHexString(this.boot.getBPB_SecPerClus()) + ", " + this.boot.getBPB_SecPerClus());
        System.out.println("BPB_RsvdSecCnt is 0x" + Integer.toHexString(this.boot.getBPB_RsvdSecCnt()) + ", " + this.boot.getBPB_RsvdSecCnt());
        System.out.println("BPB_NumFATS is 0x" + Integer.toHexString(this.boot.getBPB_NumFATS()) + ", " + this.boot.getBPB_NumFATS());
        System.out.println("BPB_FATSz32 is 0x" + Integer.toHexString(this.boot.getBPB_FATSz32()) + ", " + this.boot.getBPB_FATSz32());
    }

    /**
     * changes the present working directory to DIR_NAME.
     * Log an error if the directory does not exist.
     * DIR_NAME may be “.” (here) and “..” (up one directory).
     * @param dName
     */
    private void cd(String dName, RandomAccessFile raf) throws IOException
    {
        if(!exists(dName))
        {
            LOGGER.log(Level.WARNING, dName + " does not exist.");
            System.out.println("Error: does not exist");
        }
        else if(dName.equalsIgnoreCase(".."))
        {
            if(this.fs.parentDirectory != null)// not in root
            {
                this.fs = this.fs.parentDirectory;
                //check if cd .. is root directory
                if (this.fs.parentDirectory == null)
                {
                    setHeader("/");
                }
                else
                {
                    //go to the "/" after the parent directory
                    int parentDirectoryIndex = getHeader().indexOf(this.fs.name) + this.fs.name.length() + 1;
                    String name = getHeader().substring(0, parentDirectoryIndex);
                    setHeader(name);
                }
            }
            else
            {
                System.out.println("Error: already in root");
            }
        }
        else if(dName.equalsIgnoreCase(".") || (dName.equalsIgnoreCase(this.fs.name)))
        {
            //stay where you are...
        }
        else //move into new directory
        {
            Directory dir = isDirectory(dName, raf);
            if(dir == null)
            {
                LOGGER.log(Level.WARNING, dName + " is not a directory.");
                System.out.println("Error: not a directory");
            }
            else if(dir.clusters.size() > 0) //already parsed
            {
                this.fs = dir;
                setHeader(getHeader() +  this.fs.name + "/");
            }

            else// if(dir != null)
            {
                this.fs = dir;//now it is current working directory
                //parse through its contents and set to current directory
                int n = this.fs.nextClusterNumber;
                this.fs.clusters = this.getClusters(raf, this.getFATSecNum(n), this.getFATEntOffset(n), this.fs.nextClusterNumber);
                this.parseDirectories(raf,this.fs);
                setHeader(getHeader() + this.fs.name + "/");
            }

        }
    }

    /**
    * lists the contents of DIR_NAME, including “.” and “..”
     * Cannot do "ls .." need to "cd .." first
    * @param dName
     */
    private void ls(String dName)
    {
        if(dName.equalsIgnoreCase(".") || (dName.equalsIgnoreCase(this.fs.name)))
        {
            //list current directory contents
            printLs(this.fs.files);
        }
        else if(dName.equalsIgnoreCase("..") && (this.fs.parentDirectory != null))
        {
            printLs(this.fs.parentDirectory.files);
        }
        else
        {
            boolean dirCheck = false;
            for(Directory dir : this.fs.files)
            {
                if(dir.name.equalsIgnoreCase(dName))
                {
                    printLs(dir.files);
                    dirCheck = true;
                    break;
                }
            }
            if(dirCheck == false)
            {
                LOGGER.log(Level.WARNING, dName + " is not a directory.");
                System.out.println("Error: not a directory");
            }
        }
    }

    private void printLs(ArrayList<Directory> currentDir)
    {
        for(Directory dir : currentDir)//TODO: change this - test?
        {
            System.out.print(dir.name + "\t");
        }
        System.out.println();
    }

    /**
     * reads from a file named FILE_NAME, starting at POSITION, and prints NUM_BYTES.
     * @param fName
     * @param position
     * @param num_bytes
     */
    private void read(RandomAccessFile raf, String fName, String position, String num_bytes) throws IOException {
        for(Directory dir: this.fs.files)
        {
            if(dir.name.equalsIgnoreCase(fName) && dir.containsFiles == false)//dealing with file not directory
            {
                //read file
                try {
                    //take file contents and print them out
                    int positionInt = Integer.parseInt(position);
                    int numbytesInt = Integer.parseInt(num_bytes);
                    int n = dir.nextClusterNumber;
                    //System.out.println("NextClusterNumber is: " + n);//TEST
                    dir.clusters = this.getClusters(raf, this.getFATSecNum(n), this.getFATEntOffset(n), n);
                    byte[] newLine;
                    if(dir.size < positionInt + numbytesInt)
                    {
                        newLine = new byte[dir.size - positionInt];
                    }
                    else
                    {
                        newLine = new byte[numbytesInt];
                    }
                    //System.out.println(newLine[newLine.length - 1]);//TEST
                    for (int i : dir.clusters)
                    {
                        int clusterAddress = this.boot.getRootDirAddress() + i - this.boot.getBPB_RootClus();
                        clusterAddress = getAddress(clusterAddress) + positionInt;
                        raf.seek(clusterAddress);
                        this.currentLocation = clusterAddress;
                       // System.out.println("Accessing cluster address " + Integer.toHexString(clusterAddress) + " from cluster " + i);//TEST
                        raf.read(newLine, 0, newLine.length);
                        this.currentLocation += newLine.length;
                        if (newLine[newLine.length - 1] != 0) {//maxed out
                            break;
                        }
                        positionInt = 0;
                    }
                    String lineString = new String(newLine, "UTF-8");
                    System.out.println(lineString);
                    if(dir.size < positionInt + numbytesInt)
                    {
                        LOGGER.log(Level.WARNING, "File " + fName + " error: attempt to read beyond EoF.");
                        System.out.println("Error: attempt to read beyond EoF");
                    }
                } catch (Exception E) {
                    LOGGER.log(Level.WARNING, "File " + fName + " error: attempt to read beyond EoF.");
                    System.out.println("Error: attempt to read beyond EoF");
                }
            }
        }
    }

    /**
     * Check to see if file exists within current directory or not
     * @param fName
     * @return
     */
    private boolean exists(String fName)
    {
        if(fName.equalsIgnoreCase("..") || this.fs.parentDirectory != null)
        {
            return true;
        }
        else
        {
            for(Directory dir: this.fs.files)
            {
                if(dir.name.equalsIgnoreCase(fName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * sees if file name is a directory or not, return it if it is and null if not
     * @param fName
     * @return
     */
    private Directory isDirectory(String fName, RandomAccessFile raf) throws IOException
    {
        //search through files in current directory to get relevant stats
        for(Directory dir: this.fs.files)
        {

            if (dir.name.equalsIgnoreCase(fName))
            {
                if(dir.containsFiles == true)
                {
                    return dir;
                }
            }
        }
        return null;
    }

    /**
     * Handle stat command
     * @param fName
     * @param raf
     * @throws IOException
     */
    private void stat(String fName, RandomAccessFile raf) throws IOException {
        //System.out.println("Retrieving stats.");//TEST
        boolean dirCheck = false;
        if(fName.equalsIgnoreCase(".") || (fName.equalsIgnoreCase(this.fs.name)))
        {
            //list current directory contents
            printStats(this.fs);
        }
        else if(fName.equalsIgnoreCase("..") && (this.fs.parentDirectory != null))
        {
            printStats(this.fs.parentDirectory);
        }
        else {
            for (Directory dir : this.fs.files) {
                if (dir.name.equalsIgnoreCase(fName)) {
                    printStats(dir);
                    dirCheck = true;
                    break;
                }
            }
            if (dirCheck == false) {
                LOGGER.log(Level.WARNING, fName + " is not a directory.");
                System.out.println("Error: not a directory");
            }
        }
    }

    /**
     * Helper method for stats
     * @param dir
     */
    private void printStats(Directory dir)
    {
        System.out.println("Size is " + dir.size);
        System.out.println("Attributes " + dir.attributes);
        System.out.println("Next cluster number is 0x" + Integer.toHexString(dir.nextClusterNumber));
    }

    /**
     * Handle volume command
     */
    private void volume()
    {
        //System.out.println("Retrieving volume.");//TEST
        System.out.println("Volume name: " + this.volumeName);
    }

    int getFATSecNum(int n)
    {
        return this.boot.getBPB_RsvdSecCnt() + ((n * 4) / this.boot.getBPB_BytesPerSec());
    }

    int getFATEntOffset(int n)
    {
        return ((n * 4));//  % this.boot.getBPB_BytesPerSec());
    }

    private ArrayList<Integer> getClusters(RandomAccessFile raf, int fatSecNum, int fatEntOffset, int firstClusterNumber) throws IOException
    {
        ArrayList<Integer> clusters = new ArrayList<Integer>();
        byte[] value = new byte[4];
        String valueString = "";
        //go to beginning of fat
        int clusterEntryAddress;
        while(!valueString.equalsIgnoreCase("0FFFFFF8") && !valueString.equalsIgnoreCase("0FFFFFFF") && !valueString.equalsIgnoreCase("FFFFFFFF") && !valueString.equalsIgnoreCase("00000000"))
        {
            valueString = "";
            //System.out.println("FatSecNum: " + fatSecNum);//TEST
            //System.out.println("FatEntryOffest: " + fatEntOffset);//TEST
            clusterEntryAddress = getAddress(fatSecNum) + fatEntOffset;
            //System.out.println("Address of fat entry: " + Integer.toHexString(clusterEntryAddress));//TEST
            raf.seek(clusterEntryAddress);//clusterEntryAddress);
            raf.read(value, 0, 4);
            for(int i = value.length - 1; i >= 0; i--)
            {
                byte b = value[i];
                //System.out.printf("0x%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
                valueString += String.format("%02X", b);
            }
            //System.out.println("Value in string: " + valueString);//TEST
            int clusterNum = Integer.parseInt(valueString, 16);
            if(!valueString.equalsIgnoreCase("0FFFFFF8") && !valueString.equalsIgnoreCase("0FFFFFFF") && !valueString.equalsIgnoreCase("FFFFFFFF") && !valueString.equalsIgnoreCase("00000000"))
            {
                //System.out.println("Cluster number: " + clusterNum);//TEST
                clusters.add(clusterNum);
            }
            else
            {
                clusters.add(0, firstClusterNumber);
            }
            fatEntOffset = getFATEntOffset(clusterNum);
        }
        //System.out.println("Cluster list size: " + clusters.size());//TEST
        return clusters;
    }

    public void getFreelist() 
    {
        System.out.println("First three free cluster indices: " + firstThreeFreecClusters[0] + ", " + firstThreeFreecClusters[1] + ", " + firstThreeFreecClusters[2]);
        System.out.println("Number of free clusters in FAT: " + numFreeClusters);
    }


}