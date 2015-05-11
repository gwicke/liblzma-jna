package org.wikimedia.lzma;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;
import org.wikimedia.lzma.LZMALibrary.LZMAAction;
import org.wikimedia.lzma.LZMALibrary.LZMACheck;
import org.wikimedia.lzma.LZMALibrary.LZMAReturn;
import org.wikimedia.lzma.LZMALibrary.LZMAStream;

import com.google.common.io.BaseEncoding;
import com.sun.jna.NativeLong;

public class LZMALibraryTest {

    @Test
    public void test() {
        LZMAStream stream;
        ByteBuffer input, compressed;

        // Encoding ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        stream = new LZMAStream();
        assertReturnOK(LZMALibrary.lzma_easy_encoder(stream, 1, LZMACheck.CRC64.getCode()));

        // Obtain a ByteBuffer that is mapped to the pointer, and write the test
        // data to it.
        input = stream.next_in.getByteBuffer(0, LZMALibrary.BUFSIZE);
        input.put("the rain in spain falls mostly on the plains".getBytes());
        input.flip();
        int inputSize = input.remaining();

        // Buffer to copy compressed output to.
        compressed = ByteBuffer.allocate(LZMALibrary.BUFSIZE);

        stream.avail_in = new NativeLong(inputSize);
        stream.avail_out = new NativeLong(LZMALibrary.BUFSIZE);
        int totalOut = 0, writeSize, loopCount = 0;

        System.out.println("before compress loop: " + stream);

        while (true) {
            loopCount += 1;
            int code = LZMALibrary.lzma_code(stream, LZMAAction.FINISH.getCode());
            LZMAReturn ret = LZMAReturn.fromCode(code);

            // When the buffer is filled, or when the end of stream is reached
            if (stream.avail_out.intValue() == 0 || ret.equals(LZMAReturn.STREAM_END)) {
                writeSize = LZMALibrary.BUFSIZE - stream.avail_out.intValue();
                ByteBuffer nextOut = stream.next_out.getByteBuffer(0, writeSize);
                assertThat(writeSize, equalTo(nextOut.remaining()));
                totalOut += nextOut.remaining();
                compressed.put(nextOut);
            }

            // If OK, we'll loop again
            if (!ret.equals(LZMAReturn.OK)) {
                // Not OK but stream-end, exit the loop; We are done
                if (ret.equals(LZMAReturn.STREAM_END)) {
                    break;
                }
                // An error occurred.
                fail(String.format("lzma_code returned %s (%s)%n", ret, ret.getMessage()));
            }
        }

        compressed.flip();

        assertThat(stream.avail_in.intValue(), equalTo(0));
        assertThat((int) stream.total_in, equalTo(inputSize));
        assertThat(totalOut, equalTo(compressed.remaining()));
        assertThat(compressed.remaining(), is(equalTo((int) stream.total_out)));

        System.out.printf("after compress loop (%d passes): %s%n", loopCount, stream);

        // Copy compressed data into an array for debug printing.
        byte[] dst = new byte[compressed.remaining()];
        compressed.slice().get(dst, 0, compressed.remaining());
        System.out.printf("%n%d bytes of compressed data (as hex):%n", dst.length);
        System.out.println(BaseEncoding.base16().encode(dst));
        System.out.println("Should begin w/ magic header: FD 37 7A 58 5A 00 ^^^^^^");
 
        // Write compressed stream to disk for debugging.
        try (FileOutputStream f = new FileOutputStream(new File("output.xz"))) {
            f.write(dst, 0, dst.length);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        LZMALibrary.lzma_end(stream);

        // Decoding ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        // ...

    }

    private void assertReturnOK(int code) {
        assertThat(LZMAReturn.fromCode(code), equalTo(LZMAReturn.OK));
    }

}
