import org.junit.Assert;
import org.junit.Test;
import org.maggus.mikedb.data.FileItem;
import org.maggus.mikedb.services.PersistenceService;

import java.io.File;

public class FileItemTest {

    @Test
    public void FileItemFromFileTest() {
        File testFile = new File("./some.funky.key.png" + PersistenceService.DB_FILE_EXT);

        FileItem fileItem = new FileItem(testFile);

        Assert.assertEquals(testFile.getAbsolutePath(), fileItem.getFileName());
        Assert.assertEquals("some.funky.key", fileItem.getName());
        Assert.assertEquals("png", fileItem.getType());
        Assert.assertEquals("image/png", fileItem.guessMimeType());
    }

    @Test
    public void FileItemFromFileNopDbTest() {
        File testFile = new File("./some.funky.key.jpeg");

        FileItem fileItem = new FileItem(testFile);

        Assert.assertEquals(testFile.getAbsolutePath(), fileItem.getFileName());
        Assert.assertEquals("some.funky.key", fileItem.getName());
        Assert.assertEquals("jpeg", fileItem.getType());
        Assert.assertEquals("image/jpeg", fileItem.guessMimeType());
    }

    @Test
    public void cloneTest() {
        File testFile = new File("./some.funky-video.avi" + PersistenceService.DB_FILE_EXT);

        FileItem fileItem = new FileItem(testFile).clone();

        Assert.assertNull(fileItem.getFileName());
        Assert.assertEquals("some.funky-video", fileItem.getName());
        Assert.assertEquals("avi", fileItem.getType());
        Assert.assertEquals("application/x-troff-msvideo", fileItem.guessMimeType());
    }

}
