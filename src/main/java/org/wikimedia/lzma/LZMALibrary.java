package org.wikimedia.lzma;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LZMALibrary {

    public static final int BUFSIZE = 8192;

    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary)Native.loadLibrary("lzma", CLibrary.class);
        
        int lzma_stream_decoder(LZMAStream strm, long memlimit, int flags);
        void lzma_end(LZMAStream strm);
        int lzma_code(LZMAStream strm, int action);
        int lzma_easy_encoder(LZMAStream strm, int preset, int check);
    }
    
    /** analog to lzma_stream */
    public static class LZMAStream extends Structure {
        public Pointer next_in = new Memory(BUFSIZE);
        public NativeLong avail_in;
        public long total_in;
        public Pointer next_out = new Memory(BUFSIZE);
        public NativeLong avail_out;
        public long total_out;
        public Pointer allocator;
        public Pointer internal;
        public Pointer reserved_ptr1;
        public Pointer reserved_ptr2;
        public Pointer reserved_ptr3;
        public Pointer reserved_ptr4;
        public long reserved_int1;
        public long reserved_int2;
        public NativeLong reserved_int3;
        public NativeLong reserved_int4;
        public int reserved_enum1;
        public int reserved_enum2;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] {
                    "next_in",
                    "avail_in",
                    "total_in",
                    "next_out",
                    "avail_out",
                    "total_out",
                    "allocator",
                    "internal",
                    "reserved_ptr1",
                    "reserved_ptr2",
                    "reserved_ptr3",
                    "reserved_ptr4",
                    "reserved_int1",
                    "reserved_int2",
                    "reserved_int3",
                    "reserved_int4",
                    "reserved_enum1",
                    "reserved_enum2", });
        }
    }

    @SuppressWarnings("serial")
    public static class size_t extends IntegerType {
        public size_t() {
            this(0);
        }

        public size_t(long value) {
            super(Native.SIZE_T_SIZE, value, true);
        }
    }

    // TODO: Standardize these; Potential for reuse here.

    /** analog to lzma_check (see: /usr/include/lzma/check.h) */
    public static enum LZMACheck {
        NONE(0), CRC32(1), CRC64(4), SHA256(10);

        private final int code;

        private LZMACheck(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    /** lzma_return analog (see: /usr/include/lzma/base.h) */
    public static enum LZMAReturn {
        OK(0, "operation completed successfully"),
        STREAM_END(1, "end of stream was reached"),
        NO_CHECK(2, "input stream has no integrity check"),
        UNSUPPORTED_CHECK(3, "cannot calculate the integrity check"),
        GET_CHECK(4, "integrity check type is now available"),
        MEM_ERROR(5, "cannot allocate memory"),
        MEMLIMIT_ERROR(6, "memory usage limit was reached"),
        FORMAT_ERROR(7, "file format not recognized"),
        OPTIONS_ERROR(8, "invalid or unsupported options"),
        DATA_ERROR(9, "data is corrupt"),
        BUF_ERROR(10, "no progress is possible"),
        PROG_ERROR(11, "programming error");

        private static final Map<Integer, LZMAReturn> index = Maps.newHashMap();

        static {
            for (LZMAReturn e : EnumSet.allOf(LZMAReturn.class)) {
                index.put(e.getCode(), e);
            }
        }

        private final int code;
        private final String msg;

        private LZMAReturn(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return this.code;
        }

        public String getMessage() {
            return this.msg;
        }

        public static LZMAReturn fromCode(int code) {
            return index.get(code);
        }
    }

    /** lzma_action analog (see: /usr/include/lzma/base.h) */
    public static enum LZMAAction {
        RUN(0), SYNC_FLUSH(1), FULL_FLUSH(2), FINISH(3);

        private static final Map<Integer, LZMAAction> index = Maps.newHashMap();

        static {
            for (LZMAAction e : EnumSet.allOf(LZMAAction.class)) {
                index.put(e.getCode(), e);
            }
        }

        private final int code;

        private LZMAAction(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        public static LZMAAction fromCode(int code) {
            return index.get(code);
        }
    }

    /** see /usr/include/lzma/container.h */
    public static class DecoderFlags {
        public static final int LZMA_TELL_NO_CHECK = 0x01;
        public static final int LZMA_TELL_UNSUPPORTED_CHECK = 0x02;
        public static final int LZMA_TELL_ANY_CHECK = 0x04;
        public static final int LZMA_CONCATENATED = 0x08;
    }
}
