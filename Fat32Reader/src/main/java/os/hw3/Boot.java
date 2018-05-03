package os.hw3;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class contains important fields from the boot sector of the FAT32 file system.
 * Author: Jeffrey Hagler, David Mandelbaum
 */
class Boot
{
    //boot fields
    private int BPB_BytesPerSec;
    private int BPB_SecPerClus;
    private int BPB_RsvdSecCnt;
    private int BPB_NumFATS;
    private int BPB_FATSz32;



    private int BPB_FSInfo;
    private int rootDirAddress;
    private int BPB_RootClus;

    /**
     * Set the BPB data fields
     * @param raf
     * @throws IOException
     */
    Boot(RandomAccessFile raf, Fat32Reader fr) throws IOException
    {
        BPB_BytesPerSec = setValue(raf, 11, 2, 11);//11->13
        BPB_SecPerClus = setValue(raf, 13, 1, 0);//13->14
        BPB_RsvdSecCnt = setValue(raf, 14, 2, 0);//14->16
        BPB_NumFATS = setValue(raf, 16, 1, 0);//16->17
        BPB_FATSz32 = setValue(raf, 36, 4, 36);//36->40
        BPB_RootClus = setValue(raf, 44, 4, 44);
        BPB_FSInfo = setValue(raf, 48, 2, 0);
        rootDirAddress = (getBPB_NumFATS() * getBPB_FATSz32()) + getBPB_RsvdSecCnt();
        fr.currentLocation = 40;
    }

    /**
     * given a file stream, length of the field, and amount to skip (can be zero), return the integer value of the field
     * @param raf
     * @param offset
     * @param length
     * @param seek
     * @return
     * @throws IOException
     */
    private int setValue(RandomAccessFile raf, int offset, int length, int seek) throws IOException
    {
        byte[] buffer = new byte[length];
        //System.out.println("Buffer size: " + buffer.length);//TEST
        if(seek != 0)
        {
            raf.seek(seek);
        }
        raf.read(buffer, 0, length);
        String temp = "";
        //turn into hex string
        for(int i = buffer.length - 1; i >= 0; i--)
        {
            byte b = buffer[i];
            //System.out.printf("0x%02X\n", b);//https://stackoverflow.com/a/1748044//TEST
            temp += String.format("%02X", b);
        }
        //System.out.println("0x" + temp);//TEST
        int value = Integer.parseInt(temp, 16);
        //System.out.println("integer: " + value);//TEST
        //String value2 = Integer.toHexString(value);
        //value2 = "0x" + value2;
        //System.out.println("hex string: " + value2);//TEST
        return value;
    }

    public int getRootDirAddress() {
        return rootDirAddress;
    }

    public int getBPB_FATSz32() {
        return BPB_FATSz32;
    }

    public int getBPB_NumFATS() {
        return BPB_NumFATS;
    }

    public int getBPB_RsvdSecCnt() {
        return BPB_RsvdSecCnt;
    }

    public int getBPB_SecPerClus() {
        return BPB_SecPerClus;
    }

    public int getBPB_BytesPerSec() {
        return BPB_BytesPerSec;
    }

    public int getBPB_RootClus() {
        return BPB_RootClus;
    }

    public int getBPB_FSInfo() {
        return BPB_FSInfo;
    }
}
