# OSHW4

## Jeffrey Hagler and David Mandelbaum - (5/14/18)
## A listing of all files/directories in your submission and a brief description of each
  * Fat32Reader class - main command center for parsing file and user-run navigation
  * Boot class - used for info command
  * Directory class - represents directly (tree node-like)
  * Log file prints errors in longer format than will appear on the screen

## Commands that work
 1. info
 2. volume 
 3. quit
 4. stat
 5. cd
 6. read
 7. ls
 8. freelist
 9. delete
 10. newfile

##	Instructions for running program
 1. Open the zip.
 2. Navigate into Fat32Reader folder
 3. Run the following command (with maven) from the Fat32Reader folder:
```
    mvn clean install
```
 4. Run the following command to boot up the shell:
 
```
    java -cp .:./target/Fat32Reader-1.0-SNAPSHOT.jar os.hw3.Fat32Reader "fat32.img" 
```     
 5. Start inputting commands and navigate, read, delete, add files and quit 
    to your heart's content.
 
##	Notes
 1. Assumptions:
        a. Assuming file name before period must be 8 or fewer letters
        b. Ignoring long names in directory entries
        c. First open clusters from fs info sector is correct until shown otherwise 
 2. Will assume "/" is part of file name if typed with command, not relating to navigation

##	Challenges encountered along the way
 1. Balancing efficiency in navigation versus storage - we took an OO approach, which 
    simplified code and navigation efficiency, sacrificing on space.
 2. Because we did not know that HW4 would build on HW3, there are some repetitions in code
    and not all methods are as optimized as they could be.

##	Sources you used to help you write your program
 1. logging help from: http://www.vogella.com/tutorials/Logging/article.html
 2. hex to decimal help from: //https://stackoverflow.com/a/26738067
 3. http://www.cs.uni.edu/~diesburg/courses/cop4610_fall10/week11/week11.pdf
 4. https://www.pjrc.com/tech/8051/ide/fat32.html
 

