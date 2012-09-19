/*
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this program (see the file COPYING.LIB for more
 * details); if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.dcache.chimera.nfs.vfs;

import org.dcache.utils.Bytes;

/**
 * NFS file handle on wire representation format v1.
 *
 * <pre>
 *   byte fh_version;      // file handle format version number; version 1 description
 *   byte[3] fh_magic      // 0xcaffee 
 *   uint32 fh_generation; // server boot time or 0 for permanent handles 
 *   byte export_index;    // index into export table
 *   byte fh_type          // 1 if pseudo fs
 *   byte fh_olen;         // length of opaque data
 *   byte[] fh_opaque;     // FS specific opaque data <= 117
 * </pre>
*/
public class FileHandle {

    private final static int MIN_LEN = 11;
    private final static int VERSION = 1;
    private final static int MAGIC = 0xCAFFEE;
    private final static byte[] EMPTY_FH = new byte[0];

    private final int version;
    private final int magic;
    private final int generation;
    private final int export_idx;
    private final int type;
    private final byte[] fs_opaque;

    public FileHandle(int generation, int auth_flavior, int type, byte[] fs_opaque) {
        this.version = VERSION;
        this.magic = MAGIC;
        this.generation = generation;
        this.export_idx = auth_flavior;
        this.type = type;
        this.fs_opaque = fs_opaque;
    }

    
    public FileHandle(byte[] bytes) {
        if (bytes.length < MIN_LEN) {
            throw new IllegalArgumentException("too short");
        }

        int magic_version = Bytes.getInt(bytes, 0);
        version = (magic_version & 0xFF000000) >>> 24;
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }

        magic = magic_version & 0x00FFFFFF;
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Bad magic number");
        }

        generation = Bytes.getInt(bytes, 4);
        export_idx = (int) bytes[8];
        type = (int) bytes[9];
        int olen = (int) bytes[10];
        fs_opaque = new byte[olen];
        System.arraycopy(bytes, 11, fs_opaque, 0, olen);
    }

    public int getVersion() {
        return version;
    }

    public int getMagic() {
        return magic;
    }

    public int getGeneration() {
        return generation;
    }

    public int getExportIdx() {
        return export_idx;
    }

    public int getType() {
        return type;
    }

    public byte[] getFsOpaque() {
        return fs_opaque;
    }

    public byte[] bytes() {
        int len = fs_opaque.length + MIN_LEN;
        byte[] bytes = new byte[len];

        Bytes.putInt(bytes, 0, version << 24 | magic);
        Bytes.putInt(bytes, 4, generation);
        bytes[8] = (byte) export_idx;
        bytes[9] = (byte) type;
        bytes[10] = (byte) fs_opaque.length;
        System.arraycopy(fs_opaque, 0, bytes, 11, fs_opaque.length);
        return bytes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(byte b: bytes()) {
            int i = b&0xFF;
            if ( i < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(i));
        }
        return sb.toString();        
    }
    
    public static class FileHandleBuilder {
        private int version = VERSION;
        private int magic = MAGIC;        
        private int generation = 0;
        private int export_idx = 0;
        private int type = 0;
        private byte[] fs_opaque = EMPTY_FH;

        public FileHandleBuilder setGeneration(int generation) {
            this.generation = generation;
            return this;
        }

        public FileHandleBuilder setExportIdx(int export_idx) {
            this.export_idx = export_idx;
            return this;
        }

        public FileHandleBuilder setType(int type) {
            this.type = type;
            return this;
        }

        public FileHandleBuilder setFsOpaque(byte[] fs_opaque) {
            this.fs_opaque = fs_opaque;
            return this;
        }


        /**
         * A shortcut with defaults
         * @param opaque
         * @return 
         */
        public FileHandle build(byte[] opaque) {
            return new FileHandle(generation, export_idx, type, opaque);
        }

        public FileHandle build() {
            return build(fs_opaque);
        }
    }
}
