package org.maggus.mikedb.data;

import lombok.Data;
import org.apache.commons.io.FilenameUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.maggus.mikedb.services.PersistenceService;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Date;

@Data
public class FileItem implements Cloneable {
    private Long id;
    private String name;
    private String fileName;
    private String type;
    private String mimeType;
    private Long fileSize;
    private Date fileTimestamp;

    public FileItem() {
    }

    public FileItem(File file) {
        this.name = file.getName();
        this.fileName = file.getAbsolutePath();
        this.fileSize = file.length();
        this.fileTimestamp = new Date(file.lastModified());
        // clean up name and type (extension)
        if (this.name.endsWith(PersistenceService.DB_FILE_EXT)) {
            this.name = this.name.substring(0, this.name.length() - PersistenceService.DB_FILE_EXT.length());
        }
        this.type = FilenameUtils.getExtension(this.name);
        if (this.type != null && !this.type.isEmpty()) {
            String ext = "." + this.type;
            this.name = this.name.substring(0, this.name.length() - ext.length());
        }
    }


    public FileItem(FormDataContentDisposition fileDetail) {
        this.name = FilenameUtils.getBaseName(fileDetail.getFileName());    // not using  fileDetail.getName()
        this.fileName = fileDetail.getFileName();
        this.fileSize = fileDetail.getSize();
        this.type = FilenameUtils.getExtension(fileDetail.getFileName());   // not using fileDetail.getType();
        this.fileTimestamp = fileDetail.getModificationDate() != null ? fileDetail.getModificationDate() : fileDetail.getCreationDate();
    }

    public String guessMimeType() {
        String mimeType = getMimeType();
        if (mimeType == null) {
            try {
                java.nio.file.Path path = new File(getFileName()).toPath();
                mimeType = Files.probeContentType(path);
            } catch (Exception e) {
                // no-op
            }
        }
        if (mimeType == null) {
            File file = new File(getNameWithExtension());
            mimeType = URLConnection.guessContentTypeFromName(file.getName());
        }

        setMimeType(mimeType != null ? mimeType : "application/x-binary");

        return getMimeType();
    }

    private String getNameWithExtension() {
        String name = this.getName();
        String ext = this.getType();
        return (ext != null && !ext.isEmpty()) ? (name + "." + ext) : name;
    }

    @Override
    public FileItem clone() {
        FileItem clone = new FileItem();
        clone.setName(this.getName());
//        clone.setFileName(this.getFileName());
        clone.setType(this.getType());
        clone.setFileSize(this.getFileSize());
        clone.setFileTimestamp(this.getFileTimestamp());
        clone.setMimeType(this.getMimeType());
        return clone;
    }
}
