package org.dcache.chimera.nfs;

import com.google.common.base.Splitter;
import java.io.IOException;
import org.dcache.chimera.nfs.vfs.Inode;
import org.dcache.chimera.nfs.vfs.Stat;
import org.dcache.chimera.nfs.vfs.VirtualFileSystem;

public class ExportPathCreator {

    private ExportFile exportFile;
    public VirtualFileSystem vfs;

    public void setVfs(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    public void setExportFile(ExportFile exportFile) {
        this.exportFile = exportFile;
    }

    public void init()  throws IOException {
        Inode root = vfs.getRootInode();
        for (FsExport export : exportFile.getExports()) {
            String path = export.getPath();
            Splitter splitter = Splitter.on('/').omitEmptyStrings();
            Inode inode = root;
            for (String s : splitter.split(path)) {

                Inode child;
                try {
                    child = vfs.lookup(inode, s);
                } catch(ChimeraNFSException e) {
                    if (e.getStatus() == nfsstat.NFSERR_NOENT)
                        child = vfs.create(inode, Stat.Type.DIRECTORY, s, 0, 0, 0777);
                }
            }
        }
    }
}
