package dirpoll;

import java.io.*;

import com.futeh.progeny.util.DirPoll;
import com.futeh.progeny.util.DirPoll.DirPollException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * $Revision$
 * $Date$
 * $Author: Matthew Milliss &lt;mmilliss@moneyswitch.net&gt;
 */
public class DirPollTest {

    private DirPoll dirPoll;
    private static final String BASE_PATH = 
        System.getProperty ("java.io.tmpdir");
    private static final String ARCHIVE_DIR = BASE_PATH + "/archive";
    
    private File testIncomingFile;
    private boolean fileProcessed;

    @BeforeEach
    protected void setUp() throws Exception {
        dirPoll = new DirPoll();
        dirPoll.setPath(BASE_PATH);
        dirPoll.setProcessor(new DirPoll.FileProcessor() {
            public void process(File name) throws DirPollException { 
                fileProcessed = true;
            }
        });
        dirPoll.createDirs();       
        new Thread(dirPoll).start();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        dirPoll.destroy();
        File archiveDir = new File(ARCHIVE_DIR);
        File[] files = archiveDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            files[i].delete();
        } 
    }

    @Test
    public void testArchiveFile() throws IOException, InterruptedException {
        String dateFormatString = "yyyyMMddHHmmss";
        String filename = "dodgyTestFile.test";
        
        dirPoll.setShouldArchive(true);
        dirPoll.setArchiveDateFormat(dateFormatString);
        dirPoll.setShouldTimestampArchive(true);
        testIncomingFile = new File(BASE_PATH + "/request/" + filename);
        FileOutputStream fileOutputStream = new FileOutputStream(testIncomingFile);
        fileOutputStream.write(new String("test").getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
        assertTrue(testIncomingFile.exists());
        
        while(!fileProcessed) {            
            Thread.yield();
        }
        Thread.sleep(200);
        File archiveDirectory = new File(ARCHIVE_DIR);        
        assertEquals(1, archiveDirectory.listFiles().length);
        assertEquals((dateFormatString.length() + filename.length() + 1), archiveDirectory.listFiles()[0].getName().length());
    }

    @Test
    public void testDoNotArchiveFile() throws IOException, InterruptedException {
        dirPoll.setShouldArchive(false);
        testIncomingFile = new File(BASE_PATH + "/request/dodgyTestFile2.test");
        FileOutputStream fileOutputStream = new FileOutputStream(testIncomingFile);
        fileOutputStream.write(new String("test").getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
        assertTrue(testIncomingFile.exists());
        
        while(!fileProcessed) {            
            Thread.yield();
        }
        Thread.sleep(200);
        File archiveDirectory = new File(ARCHIVE_DIR);        
        assertEquals(0, archiveDirectory.listFiles().length);
    }

    @Test
    public void testArchiveNoTimestamp() throws Exception {

        String filename = "dodgyTestFile3.test";
        
        dirPoll.setShouldArchive(true);
        dirPoll.setShouldTimestampArchive(false);
        testIncomingFile = new File(BASE_PATH + "/request/" + filename);
        FileOutputStream fileOutputStream = new FileOutputStream(testIncomingFile);
        fileOutputStream.write(new String("test").getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
        assertTrue(testIncomingFile.exists());
        
        while(!fileProcessed) {            
            Thread.yield();
        }
        Thread.sleep(200);
        File archiveDirectory = new File(ARCHIVE_DIR);        
        assertEquals(1, archiveDirectory.listFiles().length);
        assertEquals(filename.length(), archiveDirectory.listFiles()[0].getName().length());
        assertEquals(filename, archiveDirectory.listFiles()[0].getName());
        
    }
    
}

