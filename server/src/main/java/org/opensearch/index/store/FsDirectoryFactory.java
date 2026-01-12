/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.index.store;

import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.BufferedChecksum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FileSwitchDirectory;
import org.apache.lucene.store.FilterDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Setting.Property;
import org.opensearch.common.util.io.IOUtils;
import org.opensearch.index.IndexModule;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.shard.ShardPath;
import org.opensearch.plugins.IndexStorePlugin;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Factory for a filesystem directory
 *
 * @opensearch.internal
 */
public class FsDirectoryFactory implements IndexStorePlugin.DirectoryFactory {

    public static final Setting<LockFactory> INDEX_LOCK_FACTOR_SETTING = new Setting<>("index.store.fs.fs_lock", "native", (s) -> {
        switch (s) {
            case "native":
                return NativeFSLockFactory.INSTANCE;
            case "simple":
                return SimpleFSLockFactory.INSTANCE;
            default:
                throw new IllegalArgumentException("unrecognized [index.store.fs.fs_lock] \"" + s + "\": must be native or simple");
        } // can we set on both - node and index level, some nodes might be running on NFS so they might need simple rather than native
    }, Property.IndexScope, Property.NodeScope);

    @Override
    public Directory newDirectory(IndexSettings indexSettings, ShardPath path) throws IOException {
        final Path location = path.resolveIndex();
        final LockFactory lockFactory = indexSettings.getValue(INDEX_LOCK_FACTOR_SETTING);
        Files.createDirectories(location);
        return newFSDirectory(location, lockFactory, indexSettings);
    }

    public Directory newFSDirectory(Path location, LockFactory lockFactory, IndexSettings indexSettings) throws IOException {
        final String storeType = indexSettings.getSettings()
            .get(IndexModule.INDEX_STORE_TYPE_SETTING.getKey(), IndexModule.Type.FS.getSettingsKey());
        IndexModule.Type type;
        if (IndexModule.Type.FS.match(storeType)) {
            type = IndexModule.defaultStoreType(IndexModule.NODE_STORE_ALLOW_MMAP.get(indexSettings.getNodeSettings()));
        } else {
            type = IndexModule.Type.fromSettingsKey(storeType);
        }
        Set<String> preLoadExtensions = new HashSet<>(indexSettings.getValue(IndexModule.INDEX_STORE_PRE_LOAD_SETTING));
        switch (type) {
            case HYBRIDFS:
                // Use Lucene defaults
//                final FSDirectory primaryDirectory = FSDirectory.open(location, lockFactory);
//                final Set<String> nioExtensions = new HashSet<>(indexSettings.getValue(IndexModule.INDEX_STORE_HYBRID_NIO_EXTENSIONS));
//                if (primaryDirectory instanceof MMapDirectory mMapDirectory) {
//                    return new HybridDirectory(lockFactory, setPreload(mMapDirectory, preLoadExtensions), nioExtensions);
//                } else {
//                    return primaryDirectory;
//                }
            case MMAPFS:
//                return setPreload(new MMapDirectory(location, lockFactory), preLoadExtensions);
            // simplefs was removed in Lucene 9; support for enum is maintained for bwc
            case SIMPLEFS:
            case NIOFS:
                return new DirectIODirectoryWrapper(new NIOFSDirectory(location, lockFactory));
            default:
                throw new AssertionError("unexpected built-in store type [" + type + "]");
        }
    }

    public static MMapDirectory setPreload(MMapDirectory mMapDirectory, Set<String> preLoadExtensions) throws IOException {
        if (preLoadExtensions.isEmpty() == false) {
            mMapDirectory.setPreload(createPreloadPredicate(preLoadExtensions));
        }
        return mMapDirectory;
    }

    /**
     * Returns true iff the directory is a hybrid fs directory
     */
    public static boolean isHybridFs(Directory directory) {
        Directory unwrap = FilterDirectory.unwrap(directory);
        return unwrap instanceof HybridDirectory;
    }

    static BiPredicate<String, IOContext> createPreloadPredicate(Set<String> preLoadExtensions) {
        if (preLoadExtensions.contains("*")) {
            return MMapDirectory.ALL_FILES;
        } else {
            return (s, f) -> {
                int dotIndex = s.lastIndexOf('.');
                if (dotIndex > 0) {
                    return preLoadExtensions.contains(s.substring(dotIndex + 1));
                }
                return false;
            };
        }
    }

    /**
     * A hybrid directory implementation
     *
     * @opensearch.internal
     */
    static final class HybridDirectory extends NIOFSDirectory {
        private final MMapDirectory delegate;
        private final Set<String> nioExtensions;

        HybridDirectory(LockFactory lockFactory, MMapDirectory delegate, Set<String> nioExtensions) throws IOException {
            super(delegate.getDirectory(), lockFactory);
            this.delegate = delegate;
            this.nioExtensions = nioExtensions;
        }

        @Override
        public IndexInput openInput(String name, IOContext context) throws IOException {
            if (useDelegate(name)) {
                // we need to do these checks on the outer directory since the inner doesn't know about pending deletes
                ensureOpen();
                ensureCanRead(name);
                // we only use the mmap to open inputs. Everything else is managed by the NIOFSDirectory otherwise
                // we might run into trouble with files that are pendingDelete in one directory but still
                // listed in listAll() from the other. We on the other hand don't want to list files from both dirs
                // and intersect for perf reasons.
                return delegate.openInput(name, context);
            } else {
                return super.openInput(name, context);
            }
        }

        boolean useDelegate(String name) {
            final String extension = FileSwitchDirectory.getExtension(name);
            return nioExtensions.contains(extension) == false;
        }

        @Override
        public void close() throws IOException {
            IOUtils.close(super::close, delegate);
        }

        MMapDirectory getDelegate() {
            return delegate;
        }
    }

    /**
     * DirectIO directory implementation
     * for all operation types e,g; SEARCH, INDEX, MERGE etc
     * @opensearch.internal
     */
    static final class DirectIODirectoryWrapper extends FilterDirectory {
        /**
         * Default buffer size before writing to disk (16 KB); larger means less IO load but more RAM and
         * direct buffer storage space consumed during merging.
         */
        public static final int DEFAULT_MERGE_BUFFER_SIZE = 16 * 1024;

        /** Default min expected merge size before direct IO is used (10 MB): */
        public static final long DEFAULT_MIN_BYTES_DIRECT = 10 * 1024;

        private final int blockSize, mergeBufferSize;
        private final long minBytesDirect;

        volatile boolean isOpen = true;

        /**
         * Reference to {@code com.sun.nio.file.ExtendedOpenOption.DIRECT} by reflective class and enum
         * lookup. There are two reasons for using this instead of directly referencing
         * ExtendedOpenOption.DIRECT:
         *
         * <ol>
         *   <li>ExtendedOpenOption.DIRECT is OpenJDK's internal proprietary API. This API causes
         *       un-suppressible(?) warning to be emitted when compiling with --release flag and value N,
         *       where N is smaller than the version of javac used for compilation. For details, please
         *       refer to https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8259039.
         *   <li>It is possible that Lucene is run using JDK that does not support
         *       ExtendedOpenOption.DIRECT. In such a case, dynamic lookup allows us to bail out with
         *       UnsupportedOperationException with meaningful error message.
         * </ol>
         *
         * <p>This reference is {@code null}, if the JDK does not support direct I/O.
         */
        static final OpenOption ExtendedOpenOption_DIRECT; // visible for test

        static {
            OpenOption option;
            try {
                final Class<? extends OpenOption> clazz =
                    Class.forName("com.sun.nio.file.ExtendedOpenOption").asSubclass(OpenOption.class);
                option =
                    Arrays.stream(clazz.getEnumConstants())
                        .filter(e -> e.toString().equalsIgnoreCase("DIRECT"))
                        .findFirst()
                        .orElse(null);
            } catch (
                @SuppressWarnings("unused")
                Exception e) {
                option = null;
            }
            ExtendedOpenOption_DIRECT = option;
        }

        /**
         * Create a new DirectIODirectory for the named location.
         *
         * @param delegate Directory for non-merges, also used as reference to file system path.
         * @param mergeBufferSize Size of buffer to use for merging.
         * @param minBytesDirect Merges, or files to be opened for reading, smaller than this will not use
         *     direct IO. See {@link #DEFAULT_MIN_BYTES_DIRECT} and {@link #useDirectIO}.
         * @throws IOException If there is a low-level I/O error
         */
        public DirectIODirectoryWrapper(FSDirectory delegate, int mergeBufferSize, long minBytesDirect)
            throws IOException {
            super(delegate);
            this.blockSize = Math.toIntExact(Files.getFileStore(delegate.getDirectory()).getBlockSize());
            this.mergeBufferSize = mergeBufferSize;
            this.minBytesDirect = minBytesDirect;
        }

        /**
         * Create a new DirectIODirectory for the named location.
         *
         * @param delegate Directory for non-merges, also used as reference to file system path.
         * @throws IOException If there is a low-level I/O error
         */
        public DirectIODirectoryWrapper(FSDirectory delegate) throws IOException {
            this(delegate, DEFAULT_MERGE_BUFFER_SIZE, DEFAULT_MIN_BYTES_DIRECT);
        }

        /**
         * @return the underlying file system directory
         */
        public Path getDirectory() {
            return ((FSDirectory) in).getDirectory();
        }

        @Override
        protected void ensureOpen() throws AlreadyClosedException {
            if (!isOpen) {
                throw new AlreadyClosedException("this Directory is closed");
            }
        }

        /**
         * Determines if direct IO should be used for a file. By default this tests if it is a merge
         * context and if the merge or file length extends the minimum size (see {@link
         * #DEFAULT_MIN_BYTES_DIRECT}). Subclasses may override method to enforce direct IO for specific
         * file types.
         *
         * @param name file name (unused by default implementation)
         * @param context information about merge size
         * @param fileLength if available, gives the file length. Will be empty when requesting an {@link
         *     IndexOutput}.
         * @return {@code true} if direct IO should be used; {@code false} if input/output should be
         *     requested from delegate directory.
         */
        protected boolean useDirectIO(String name, IOContext context, OptionalLong fileLength) {
            return true;
        }

        @Override
        public IndexInput openInput(String name, IOContext context) throws IOException {
            ensureOpen();
            if (useDirectIO(name, context, OptionalLong.of(fileLength(name)))) {
                return new DirectIODirectoryWrapper.DirectIOIndexInput(getDirectory().resolve(name), blockSize, mergeBufferSize);
            } else {
                return in.openInput(name, context);
            }
        }

        @Override
        public IndexOutput createOutput(String name, IOContext context) throws IOException {
            ensureOpen();
            if (useDirectIO(name, context, OptionalLong.empty())) {
                return new DirectIODirectoryWrapper.DirectIOIndexOutput(
                    getDirectory().resolve(name), name, blockSize, mergeBufferSize);
            } else {
                return in.createOutput(name, context);
            }
        }

        @Override
        public void close() throws IOException {
            isOpen = false;
            super.close();
        }

        private static OpenOption getDirectOpenOption() {
            if (ExtendedOpenOption_DIRECT == null) {
                throw new UnsupportedOperationException(
                    "com.sun.nio.file.ExtendedOpenOption.DIRECT is not available in the current JDK version.");
            }
            return ExtendedOpenOption_DIRECT;
        }

        private static final class DirectIOIndexOutput extends IndexOutput {
            private final ByteBuffer buffer;
            private final FileChannel channel;
            private final Checksum digest;

            private long filePos;
            private boolean isOpen;

            /**
             * Creates a new instance of DirectIOIndexOutput for writing index output with direct IO
             * bypassing OS buffer
             *
             * @throws UnsupportedOperationException if the JDK does not support Direct I/O
             * @throws IOException if the operating system or filesystem does not support support Direct I/O
             *     or a sufficient equivalent.
             */
            public DirectIOIndexOutput(Path path, String name, int blockSize, int bufferSize)
                throws IOException {
                super("DirectIOIndexOutput(path=\"" + path.toString() + "\")", name);

                channel =
                    FileChannel.open(
                        path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, getDirectOpenOption());
                buffer = ByteBuffer.allocateDirect(bufferSize + blockSize - 1).alignedSlice(blockSize);
                digest = new BufferedChecksum(new CRC32());

                isOpen = true;
            }

            @Override
            public void writeByte(byte b) throws IOException {
                buffer.put(b);
                digest.update(b);
                if (!buffer.hasRemaining()) {
                    dump();
                }
            }

            @Override
            public void writeBytes(byte[] src, int offset, int len) throws IOException {
                int toWrite = len;
                while (true) {
                    final int left = buffer.remaining();
                    if (left <= toWrite) {
                        buffer.put(src, offset, left);
                        digest.update(src, offset, left);
                        toWrite -= left;
                        offset += left;
                        dump();
                    } else {
                        buffer.put(src, offset, toWrite);
                        digest.update(src, offset, toWrite);
                        break;
                    }
                }
            }

            private void dump() throws IOException {
                final int size = buffer.position();

                // we need to rewind, as we have to write full blocks (we truncate file later):
                buffer.rewind();

                channel.write(buffer, filePos);
                filePos += size;

                buffer.clear();
            }

            @Override
            public long getFilePointer() {
                return filePos + buffer.position();
            }

            @Override
            public long getChecksum() {
                return digest.getValue();
            }

            @Override
            public void close() throws IOException {
                if (isOpen) {
                    isOpen = false;
                    try {
                        dump();
                    } finally {
                        try (FileChannel ch = channel) {
                            ch.truncate(getFilePointer());
                        }
                    }
                }
            }
        }

        private static final class DirectIOIndexInput extends IndexInput {
            private final ByteBuffer buffer;
            private final FileChannel channel;
            private final int blockSize;
            private final long offset;
            private final long length;
            private final boolean isClosable; // clones and slices are not closable
            private boolean isOpen;
            private long filePos;

            /**
             * Creates a new instance of DirectIOIndexInput for reading index input with direct IO bypassing
             * OS buffer
             *
             * @throws UnsupportedOperationException if the JDK does not support Direct I/O
             * @throws IOException if the operating system or filesystem does not support support Direct I/O
             *     or a sufficient equivalent.
             */
            public DirectIOIndexInput(Path path, int blockSize, int bufferSize) throws IOException {
                super("DirectIOIndexInput(path=\"" + path + "\")");
                this.channel = FileChannel.open(path, StandardOpenOption.READ, getDirectOpenOption());
                this.blockSize = blockSize;
                this.buffer = allocateBuffer(bufferSize, blockSize);
                this.isOpen = true;
                this.isClosable = true;
                this.length = channel.size();
                this.offset = 0L;
                this.filePos = -bufferSize;
                this.buffer.limit(0);
            }

            // for clone/slice
            private DirectIOIndexInput(
                String description, DirectIODirectoryWrapper.DirectIOIndexInput other, long offset, long length) throws IOException {
                super(description);
                Objects.checkFromIndexSize(offset, length, other.channel.size());
                final int bufferSize = other.buffer.capacity();
                this.buffer = allocateBuffer(bufferSize, other.blockSize);
                this.blockSize = other.blockSize;
                this.channel = other.channel;
                this.isOpen = true;
                this.isClosable = false;
                this.length = length;
                this.offset = offset;
                this.filePos = -bufferSize;
                buffer.limit(0);
            }

            private static ByteBuffer allocateBuffer(int bufferSize, int blockSize) {
                return ByteBuffer.allocateDirect(bufferSize + blockSize - 1)
                    .alignedSlice(blockSize)
                    .order(LITTLE_ENDIAN);
            }

            @Override
            public void close() throws IOException {
                if (isOpen && isClosable) {
                    channel.close();
                    isOpen = false;
                }
            }

            @Override
            public long getFilePointer() {
                long filePointer = filePos + buffer.position() - offset;

                // opening the input and immediately calling getFilePointer without calling readX (and thus
                // refill) first,
                // will result in negative value equal to bufferSize being returned,
                // due to the initialization method filePos = -bufferSize used in constructor.
                assert filePointer == -buffer.capacity() - offset || filePointer >= 0
                    : "filePointer should either be initial value equal to negative buffer capacity, or larger than or equal to 0";
                return Math.max(filePointer, 0);
            }

            @Override
            public void seek(long pos) throws IOException {
                if (pos != getFilePointer()) {
                    final long absolutePos = pos + offset;
                    if (absolutePos >= filePos && absolutePos <= filePos + buffer.limit()) {
                        // the new position is within the existing buffer
                        buffer.position(Math.toIntExact(absolutePos - filePos));
                    } else {
                        seekInternal(pos); // do an actual seek/read
                    }
                }
                assert pos == getFilePointer();
            }

            private void seekInternal(long pos) throws IOException {
                final long absPos = pos + offset;
                final long alignedPos = absPos - (absPos % blockSize);
                filePos = alignedPos - buffer.capacity();

                final int delta = (int) (absPos - alignedPos);
                refill(delta);
                buffer.position(delta);
            }

            @Override
            public long length() {
                return length;
            }

            @Override
            public byte readByte() throws IOException {
                if (!buffer.hasRemaining()) {
                    refill(1);
                }

                return buffer.get();
            }

            @Override
            public short readShort() throws IOException {
                if (buffer.remaining() >= Short.BYTES) {
                    return buffer.getShort();
                } else {
                    return super.readShort();
                }
            }

            @Override
            public int readInt() throws IOException {
                if (buffer.remaining() >= Integer.BYTES) {
                    return buffer.getInt();
                } else {
                    return super.readInt();
                }
            }

            @Override
            public long readLong() throws IOException {
                if (buffer.remaining() >= Long.BYTES) {
                    return buffer.getLong();
                } else {
                    return super.readLong();
                }
            }

            private void refill(int bytesToRead) throws IOException {
                filePos += buffer.capacity();

                // BaseDirectoryTestCase#testSeekPastEOF test for consecutive read past EOF,
                // hence throwing EOFException early to maintain buffer state (position in particular)
                if (filePos > offset + length || ((offset + length) - filePos < bytesToRead)) {
                    throw new EOFException("read past EOF: " + this);
                }

                buffer.clear();
                try {
                    // read may return -1 here iff filePos == channel.size(), but that's ok as it just reaches
                    // EOF
                    // when filePos > channel.size(), an EOFException will be thrown from above
                    channel.read(buffer, filePos);
                } catch (IOException ioe) {
                    throw new IOException(ioe.getMessage() + ": " + this, ioe);
                }

                buffer.flip();
            }

            @Override
            public void readBytes(byte[] dst, int offset, int len) throws IOException {
                int toRead = len;
                while (true) {
                    final int left = buffer.remaining();
                    if (left < toRead) {
                        buffer.get(dst, offset, left);
                        toRead -= left;
                        offset += left;
                        refill(toRead);
                    } else {
                        buffer.get(dst, offset, toRead);
                        break;
                    }
                }
            }

            @Override
            public void readInts(int[] dst, int offset, int len) throws IOException {
                int remainingDst = len;
                while (remainingDst > 0) {
                    int cnt = Math.min(buffer.remaining() / Integer.BYTES, remainingDst);
                    buffer.asIntBuffer().get(dst, offset + len - remainingDst, cnt);
                    buffer.position(buffer.position() + Integer.BYTES * cnt);
                    remainingDst -= cnt;
                    if (remainingDst > 0) {
                        if (buffer.hasRemaining()) {
                            dst[offset + len - remainingDst] = readInt();
                            --remainingDst;
                        } else {
                            refill(remainingDst * Integer.BYTES);
                        }
                    }
                }
            }

            @Override
            public void readFloats(float[] dst, int offset, int len) throws IOException {
                int remainingDst = len;
                while (remainingDst > 0) {
                    int cnt = Math.min(buffer.remaining() / Float.BYTES, remainingDst);
                    buffer.asFloatBuffer().get(dst, offset + len - remainingDst, cnt);
                    buffer.position(buffer.position() + Float.BYTES * cnt);
                    remainingDst -= cnt;
                    if (remainingDst > 0) {
                        if (buffer.hasRemaining()) {
                            dst[offset + len - remainingDst] = Float.intBitsToFloat(readInt());
                            --remainingDst;
                        } else {
                            refill(remainingDst * Float.BYTES);
                        }
                    }
                }
            }

            @Override
            public void readLongs(long[] dst, int offset, int len) throws IOException {
                int remainingDst = len;
                while (remainingDst > 0) {
                    int cnt = Math.min(buffer.remaining() / Long.BYTES, remainingDst);
                    buffer.asLongBuffer().get(dst, offset + len - remainingDst, cnt);
                    buffer.position(buffer.position() + Long.BYTES * cnt);
                    remainingDst -= cnt;
                    if (remainingDst > 0) {
                        if (buffer.hasRemaining()) {
                            dst[offset + len - remainingDst] = readLong();
                            --remainingDst;
                        } else {
                            refill(remainingDst * Long.BYTES);
                        }
                    }
                }
            }

            @Override
            public DirectIODirectoryWrapper.DirectIOIndexInput clone() {
                try {
                    var clone = new DirectIODirectoryWrapper.DirectIOIndexInput("clone:" + this, this, offset, length);
                    clone.seekInternal(getFilePointer());
                    return clone;
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }

            @Override
            public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
                if ((length | offset) < 0 || length > this.length - offset) {
                    throw new IllegalArgumentException(
                        "slice() " + sliceDescription + " out of bounds: " + this);
                }
                var slice = new DirectIODirectoryWrapper.DirectIOIndexInput(sliceDescription, this, this.offset + offset, length);
                slice.seekInternal(0L);
                return slice;
            }
        }
    }
}
