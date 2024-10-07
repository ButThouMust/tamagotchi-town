
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class LZSSRecompress {

    private static final int MIN_MATCH_SIZE = 0x3;
    private static final int MAX_DISTANCE_BACK = 0x1000;
    private static final int MAX_MATCH_SIZE = 0xF + MIN_MATCH_SIZE;
    private static final int NO_MATCH = -1;

    private static final int GET_PREV_MATCH = 0;
    private static final int LITERAL_BYTE = 1;
    private static final int NUM_FLAGS_IN_FLAG_BYTE = 8;

    private static FileOutputStream outputFile;
    private static BufferedWriter logFile;
    private static byte[] inputData;

    private static boolean DEBUG;

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private class LZTag {
        int ptrToTag;
        int matchIndex;
        int length;
        boolean isMatch;

        public LZTag(int ptrToTag, int matchIndex, int length, boolean isMatch) {
            this.ptrToTag = ptrToTag;
            this.matchIndex = matchIndex;
            this.length = length;
            this.isMatch = isMatch;
        }

        public LZTag(LZTag other) {
            ptrToTag = other.ptrToTag;
            matchIndex = other.matchIndex;
            length = other.length;
            isMatch = other.isMatch;
        }

        public int getDistance() {
            return ptrToTag - matchIndex;
        }

        public String toString() {
            if (isMatch) {
                String format = "%4X | L=0x%4X, match @ 0x%4X";
                return String.format(format, ptrToTag, length, matchIndex);
            }
            else {
                String format = "%4X | Lit  [%02X]";
                return String.format(format, ptrToTag, inputData[ptrToTag]);
            }
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static String removeFileExtension(String filename) {
        int periodIndex = filename.lastIndexOf('.');
        return periodIndex == -1 ? filename : filename.substring(0, periodIndex);
    }

    private static void getInputDataAsArray(String inputFilename) throws IOException {
        // get file's size to know the exact size for the array
        File f = new File(inputFilename);
        long fileLength = f.length();
        inputData = new byte[(int) fileLength];

        FileInputStream inputDataStream = new FileInputStream(inputFilename);
        inputDataStream.read(inputData);
        inputDataStream.close();
    }

    private static void printTagsList(ArrayList<LZTag> tags) throws IOException {
        String tableHeader =    " Tag # | Pos  | Description\n";
        String tableSeparator = "-------+------+---------------------------\n";
        logFile.write(tableHeader);
        logFile.write(tableSeparator);

        int tagNum = 0;
        for (LZTag tag : tags) {
            logFile.write(String.format(" %4X  | %s\n", tagNum, tag.toString()));

            // separate every group of 8 tags
            tagNum++;
            if ((tagNum & 0x7) == 0)
                logFile.write(tableSeparator);
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    // note: idea for using substrings to look for matches is from this repo:
    // https://github.com/KhaledAshrafH/LZ-77
    private static ArrayList<LZTag> getLZSSTags() {
        // "convert" the raw byte array into a String
        String inputDataString = "";
        for (byte b : inputData) {
            inputDataString += (char) b;
        }

        ArrayList<LZTag> tags = new ArrayList<>();

        // initialize the search buffer with the first byte of data
        int currPos = 0;
        String searchBuffer = "";
        String stringToSearch = "";

        int indexOfLastMatchFragment = NO_MATCH;

        while (currPos < inputData.length) {
            // search in a range that begins at data start or the max distance
            // back, and ends just before the current position
            int startOfLookback = Math.max(currPos - MAX_DISTANCE_BACK, 0);
            searchBuffer = inputDataString.substring(startOfLookback, currPos);

            stringToSearch += inputDataString.charAt(currPos);
            int matchIndex = searchBuffer.indexOf(stringToSearch);

            boolean atEndOfData = currPos == inputData.length - 1;
            int length = stringToSearch.length();
            if (matchIndex == NO_MATCH) {
                // the most recently added character prevented a match; how much
                // of the search string DID match? let L be the match length
                int matchSize = length - 1;
                int tagPtr = currPos;
                int size = 0;
                int tagIndex = NO_MATCH;
                boolean isMatch = false;

                switch (matchSize) {
                    // L = 0: most recent byte is new (1 literal); consume it
                    case 0:
                        tagPtr = currPos;
                        size = 1;
                        isMatch = false;
                        tagIndex = NO_MATCH;
                        currPos++;
                    break;

                    // L = 1: encode the matched byte as a literal, and do not
                    // consume the unmatched byte
                    case 1:
                        tagPtr = currPos - 1;
                        size = 1;
                        isMatch = false;
                        tagIndex = NO_MATCH;
                    break;

                    // L = 2: encode the first matched byte as a literal, but
                    // the second matched byte may be the start of a match
                    case 2:
                        tagPtr = currPos - 2;
                        size = 1;
                        isMatch = false;
                        tagIndex = NO_MATCH;

                        // set to look for a match starting @ 2nd matched byte
                        currPos--;
                    break;

                    // L > 2: handle the match; don't consume unmatched byte
                    default:
                        tagPtr = currPos - matchSize;
                        size = matchSize;
                        isMatch = true;
                        tagIndex = indexOfLastMatchFragment;
                    break;
                }
                LZTag tag = new LZSSRecompress().new LZTag(tagPtr, tagIndex, size, isMatch);
                tags.add(tag);

                stringToSearch = "";
            }

            // got a match that is both not at end of data and not the max
            // allowed size for a match? see if match continues
            else if (!atEndOfData && length < MAX_MATCH_SIZE) {
                currPos++;
            }

            // otherwise (at end of data, or max size reached), must add the
            // current match and reset the sequence to search for
            else {
                int tagPtr = currPos - length + 1;
                LZTag tag = new LZSSRecompress().new LZTag(tagPtr, matchIndex + startOfLookback, length, true);
                tags.add(tag);

                stringToSearch = "";
                currPos++;
            }

            indexOfLastMatchFragment = matchIndex + startOfLookback;
        }

        if (DEBUG) {
            System.out.println();
        }
        return tags;
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    private static int getFlagByteForGroup(ArrayList<LZTag> tags, int tagGroupNum) {
        int startOfTagGroup = tagGroupNum * NUM_FLAGS_IN_FLAG_BYTE;
        int endOfTagGroup = startOfTagGroup + NUM_FLAGS_IN_FLAG_BYTE - 1;
        int flagByte = 0;

        for (int tagNum = startOfTagGroup; tagNum <= endOfTagGroup && tagNum < tags.size(); tagNum++) {
            LZTag tag = tags.get(tagNum);
            int bitFlag = tag.isMatch ? GET_PREV_MATCH : LITERAL_BYTE;
            flagByte |= bitFlag << (tagNum & 0x7);
        }

        return flagByte;
    }

    private static int getBytesThatEncodeMatch(LZTag tag) {
        int encodedLength = tag.length - MIN_MATCH_SIZE;
        int distanceBack = tag.ptrToTag - tag.matchIndex;
        int encodedDistance = distanceBack - 1;

        int outputByte0 = encodedDistance & 0xFF;
        int outputByte1 = encodedLength & 0xF;
        outputByte1 |= (encodedDistance & 0xF00) >> 4;

        return outputByte0 | (outputByte1 << 8);
    }

    // fill in this implementation according to the particular game you are working
    // on; this particular one is for Tamagotchi Town
    private static void generateCompressedFile(ArrayList<LZTag> tags) throws IOException {
        // first, need to write the size of the uncompressed data
        int dataSize = inputData.length;
        outputFile.write(dataSize & 0xFF);
        outputFile.write((dataSize >> 8) & 0xFF);

        // Tamagotchi Town's LZSS decompressor uses bytes each containing 8 bit
        // flags about the next 8 tags: [flags tag0 tag1 tag2 ... tag7]
        // these bytes need to be generated for each group of 8 tags
        int numFlagBytes = 1 + (tags.size() / NUM_FLAGS_IN_FLAG_BYTE);

        int currPos = 0;
        for (int tagGroup = 0; tagGroup < numFlagBytes; tagGroup++) {
            // get the flag byte, and write it to the compressed file
            outputFile.write(getFlagByteForGroup(tags, tagGroup));

            // get bounds for which tags fall under the current group
            int startOfTagGroup = tagGroup * NUM_FLAGS_IN_FLAG_BYTE;
            int endOfTagGroup = startOfTagGroup + NUM_FLAGS_IN_FLAG_BYTE - 1;

            for (int tagNum = startOfTagGroup; tagNum <= endOfTagGroup && tagNum < tags.size(); tagNum++) {
                LZTag tag = tags.get(tagNum);

                // if tag is for literal byte, simply write the data byte
                if (!tag.isMatch) {
                    int dataByte = inputData[tag.ptrToTag];
                    outputFile.write(dataByte);
                }
                // otherwise, write 2 bytes that encode the size and distance
                // back for the match
                else {
                    int encodedMatch = getBytesThatEncodeMatch(tag);
                    outputFile.write(encodedMatch & 0xFF);
                    outputFile.write((encodedMatch >> 8) & 0xFF);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    public static void main(String args[]) throws IOException {
        if (args.length == 0) {
            System.out.println("Sample usage: java LZSSRecompress data1.bin [data2.bin data3.bin ...]");
            return;
        }

        final String FILE_PREFIX = "recompressed ";
        for (String inputFile : args) {
            // allow using a wildcard like: java LZSSRecompress *.bin
            // however, do not compress any binary files that themselves are
            // the result of compressing a binary file with this program
            if (inputFile.startsWith(FILE_PREFIX)) {
                continue;
            }

            getInputDataAsArray(inputFile);
            if (inputData.length == 0) {
                System.out.println("You cannot compress an empty file.");
                continue;
            }

            ArrayList<LZTag> tags = getLZSSTags();

            String logFilename = "LOG recompress '" + removeFileExtension(inputFile) + "'.txt";
            logFile = new BufferedWriter(new FileWriter(logFilename));
            try {
                printTagsList(tags);
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
                return;
            }
            logFile.flush();
            logFile.close();

            String outputFilename = FILE_PREFIX + inputFile;
            outputFile = new FileOutputStream(outputFilename);
            generateCompressedFile(tags);
            outputFile.close();
        }
    }
}
