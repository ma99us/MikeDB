package org.maggus.mikedb.data;


import lombok.Data;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;

@Data
public class FileItemStream {
    private final FileItem fileItem;
    private final InputStream fileInputStream;

    public FileItemStream(FormDataContentDisposition fileDetail, InputStream fileInputStream) {
        this(new FileItem(fileDetail), fileInputStream);
    }

    public FileItemStream(FileItem fileItem, InputStream fileInputStream) {
        this.fileItem = fileItem;
        this.fileInputStream = fileInputStream;
    }
}
