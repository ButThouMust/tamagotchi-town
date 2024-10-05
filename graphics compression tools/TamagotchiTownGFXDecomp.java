import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TamagotchiTownGFXDecomp {

    private static RandomAccessFile romFile;
    private static BufferedWriter logFile;

    private static boolean debug;

    private static final int GFX_PTR_LIST = 0x40000;
    private static final int NUM_GFX_PTRS = 0x63;
    private static final int BANK_SIZE = 0x8000;

    private static final int LITERAL_BYTE = 1;
    private static final int GET_PREV_MATCH = 0;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------
    
    private static byte[] decompress(int gfxDataStartROM) throws IOException {
        romFile.seek(gfxDataStartROM);

        // the first two bytes are the size of the uncompressed data
        int size = romFile.readUnsignedByte();
        size |= (romFile.readUnsignedByte() << 8);

        // use a byte array to simulate WRAM, specifically bank $7F
        byte[] uncomp = new byte[size];

        // set up variables for how to decompress the data
        int arrPos = 0;
        int numBitsLeft = 1;
        int bitFlags = 0;

        String tableHeader =    " Pos  | Description\n";
        String tableSeparator = "------+-------------------------\n";
        if (debug) {
            logFile.write(tableHeader);
            logFile.write(tableSeparator);
        }

        String tableLine = " %4X | ";

        while (arrPos < size) {
            if (debug) {
                logFile.write(String.format(tableLine, arrPos));
            }
            String description = "";

            // when buffer is exhausted, the next byte has next set of bit flags
            numBitsLeft--;
            if (numBitsLeft == 0) {
                bitFlags = romFile.readUnsignedByte();
                numBitsLeft = 8;
            }

            // check a bit flag
            switch (bitFlags & 0x1) {
                // if flag is 1, simply read a literal byte
                case LITERAL_BYTE:
                    uncomp[arrPos] = (byte) romFile.readUnsignedByte();
                    arrPos++;
                    if (debug) {
                        description = String.format("Lit [%02X]", uncomp[arrPos - 1]);
                    }
                break;

                // if flag is 0, read two bytes that specify how far to go back
                // in WRAM (up to 0x1000) and # bytes to copy (0x3 - 0x12)
                case GET_PREV_MATCH:
                    int byte0 = romFile.readUnsignedByte();
                    int byte1 = romFile.readUnsignedByte();

                    // # bytes to go back is a 12 bit value:
                    // low byte, and top 4 bits of the high byte
                    // the 1 is from having a DEX before running the MVN
                    // so go back one more byte on top of calculated value
                    int numBytesToGoBack = 1 + (byte0 | ((byte1 >> 4) << 8));

                    // # bytes to copy is the low 4 bits of the high byte
                    // the 3 is from two INCs in the ASM, plus an inherent +1
                    // from using this as the A reg for an MVN instruction
                    // where (A = transfer_length - 1) => (length = A + 1)
                    int numBytesToCopy = (byte1 & 0xF) + 3;

                    if (debug) {
                        description = String.format("L=0x%3X, match @ 0x%4X", numBytesToCopy, arrPos - numBytesToGoBack);
                    }

                    // simulate the MVN instruction
                    for (int i = 0; i < numBytesToCopy; i++) {
                        byte byteToCopy = uncomp[arrPos - numBytesToGoBack];
                        uncomp[arrPos] = byteToCopy;
                        arrPos++;
                    }
                break;
            }

            // move to the next bit flag
            bitFlags >>= 1;

            if (debug) {
                logFile.write(description + "\n");
            }
        }
        return uncomp;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static void decompressFile(String inputFile) throws IOException {
        // given a binary file compressed in this format (e.g. dumped from the
        // ROM, or generated from my compressor), decompress the data
        debug = true;
        romFile = new RandomAccessFile(inputFile, "r");

        String logFilename = "LOG '" + inputFile + "'.txt";
        logFile = new BufferedWriter(new FileWriter(logFilename));

        // decompress starting at the beginning of the file
        String outputFilename = "decompress file '%s'.bin";
        byte gfx[] = decompress(0x0);

        FileOutputStream output = new FileOutputStream(String.format(outputFilename, inputFile));
        output.write(gfx);
        output.close();

        logFile.flush();
        logFile.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static int[] getPointersForData() throws IOException {
        int ptrList[] = new int[NUM_GFX_PTRS];
        romFile.seek(GFX_PTR_LIST);
        for (int i = 0; i < ptrList.length; i++) {
            // read a LoROM pointer from the list in the ROM
            int rawPtr = romFile.readUnsignedByte();
            rawPtr |= (romFile.readUnsignedByte() << 8);
            rawPtr |= (romFile.readUnsignedByte() << 16);

            // each pointer has an extraneous [00] byte, I guess for readability
            // for when you're looking at it in a hex editor? Who knows.
            romFile.readUnsignedByte();

            // convert LoROM pointer to file offset to use with romFile.seek()
            // ignore the top 8 nibble, like 88 -> 08, 8D -> 0D, etc.
            int bankOffset = rawPtr & 0xFFFF;
            int bankNum = (rawPtr >> 16) & 0x7F;
            ptrList[i] = (bankNum - 1) * BANK_SIZE + bankOffset;
        }

        return ptrList;
    }

    private static void decompressGfxFromROM() throws IOException {
        debug = false;
        String romName = "Tamagotch Town (Japan) (NP).sfc";
        romFile = new RandomAccessFile(romName, "r");

        String outputFolder = "gfx/";
        Files.createDirectories(Paths.get(outputFolder));
        String logFilename = "";

        int dataPtrs[] = getPointersForData();
        int i = 0;
        for (int dataPtr : dataPtrs) {
            String filename = String.format("%02d (%02X) decomp gfx @ 0x%05X", i, i, dataPtr);

            if (debug) {
                logFilename = outputFolder + "LOG " + filename + ".txt";
                logFile = new BufferedWriter(new FileWriter(logFilename));
            }

            byte gfx[] = decompress(dataPtr);

            String binaryFilename = outputFolder + filename + ".bin";
            FileOutputStream output = new FileOutputStream(String.format(binaryFilename, i, i++, dataPtr));
            output.write(gfx);
            output.close();

            if (debug) {
                logFile.flush();
                logFile.close();
            }
        }

        romFile.close();
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static void main(String args[]) throws IOException {
        // two different modes for running this program:
        // no arguments -> decompress all LZSS graphics from the Japanese ROM
        // 1+ arguments -> decompress a file that is compressed in this format
        // - intent: verify that game will decompress data correctly after
        //           running original data through the compressor
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                decompressFile(args[i]);
            }
            return;
        }
        else {
            decompressGfxFromROM();
        }
    }
}
