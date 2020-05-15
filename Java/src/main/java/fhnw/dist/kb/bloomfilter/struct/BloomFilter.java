package fhnw.dist.kb.bloomfilter.struct;

import com.google.common.hash.Funnels;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import fhnw.dist.kb.bloomfilter.Starter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

public class BloomFilter {
    private static List<String> objects = new LinkedList<>();

    private final BitSet arr;
    private final int k;

    /**
     * Returns the optimal size of the data structure according to the formula on wikipedia and other sources.
     * @param n number of expected insertions.
     * @param p accepted false positive probability.
     * @return returns the ideal size of the data structure to hold n elements.
     */
    static int optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (int) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * Returns the optimal number of hash functions according to the formula on wikipedia and other sources.
     * @param n number of expected insertions.
     * @param m number of bits in the set.
     * @return number of hash functions.
     */
    static int optimalNumOfHashFunctions(long n, long m) {
        // (m / n) * log(2), but avoid truncation due to division!
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    /**
     * Creates a new instance of the bloom filter.
     * @param file String of the file containing the words to read.
     * @param fpp Accepted false positive probability (value between 0 and 1).
     * @return
     */
    public static BloomFilter create(String file, double fpp) {
        readStrings(file);
        int expectedInsertions = objects.size();
        int length = optimalNumOfBits(expectedInsertions, fpp);
        int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, length);
        try {
            return new BloomFilter(objects, length, numHashFunctions);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not create BloomFilter of " + length + " bits", e);
        }
    }

    /**
     * Constructor
     * @param list list of objects added. (For testing)
     * @param nrOfBits size of the bitset.
     * @param numHashFunctions number of has functions to use.
     */
    public BloomFilter(List<String> list, int nrOfBits, int numHashFunctions) {
        objects = list;
        arr = new BitSet(nrOfBits);
        k = numHashFunctions;
        for (String word : objects) {
            this.put(word);
        }
    }

    /**
     * Adds a string to the data structure.
     * @param object string to be added.
     * @return true if adding was successful, false otherwise.
     */
    public boolean put(String object) {
        long size = arr.size();
        for (int i = 0; i < k; i++) {
            HashCode hash = Hashing.murmur3_128(i).hashObject(object, Funnels.stringFunnel(StandardCharsets.UTF_8));
            int arrayPosition = (int) ((Integer.MAX_VALUE & hash.asInt()) % size);
            try {
                arr.set(arrayPosition);
            } catch (IndexOutOfBoundsException e) {
                System.err.println("COULD NOT SET BITS FOR OBJECT " + object);
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether a string is contained within the set.
     * @param candidate string that needs to be checked.
     * @return true if the string is contained, false otherwise.
     */
    public boolean contains(String candidate) {
        boolean[] positions = new boolean[k];
        for (int i = 0; i < k; i++) {
            HashCode hash = Hashing.murmur3_128((int) (Math.random() * Integer.MAX_VALUE)).hashObject(candidate, Funnels.stringFunnel(StandardCharsets.UTF_8));
            int arrayPosition = ((Integer.MAX_VALUE & hash.asInt()) % arr.size());
            positions[i] = arr.get(arrayPosition);
        }
        boolean contained = true;
        for (boolean val : positions) {
            contained &= val;
        }
        return contained;
    }

    /**
     * Test method used to test with randomly generated words
     * @param nrOfWords amount of words to test.
     */
    public void testFilter(long nrOfWords) {
        long fppCount = 0;
        for (int i = 0; i < nrOfWords; i++) {
            String testWord = generateWord(5, 12);
            if (this.contains(testWord) && !objects.contains(testWord)) fppCount++;
        }
        System.out.println(fppCount + " false positives (from a total of " + nrOfWords + " words).");
        double percentage = 100 * ((double) fppCount / nrOfWords);
        double roundOff = Math.round(percentage * 10000) / (double) 10000;
        System.out.println("Error percentage: " + roundOff + "%");
    }

    /**
     * Generates a random word with length between minLength and maxLength.
     * @param minLength minimum string length.
     * @param maxLength maximum string length.
     * @return the generated string.
     */
    private String generateWord(int minLength, int maxLength) {
        String ALPHA_NUMERIC_STRING = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder builder = new StringBuilder();
        int wordLength = minLength + (int) Math.random() * (maxLength - minLength);
        while (wordLength-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    /**
     * Reads all elements from the supplied file.
     * @param file location of the file that should be read.
     */
    public static void readStrings(String file) {
        int count = 0;
        try {
            InputStream in = Starter.class.getResourceAsStream(file);
            InputStreamReader inr = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(inr);
            String line = reader.readLine();
            while (line != null) {
                objects.add(line);
                count++;
                line = reader.readLine();
            }
        } catch (Exception e) {
            System.out.println("FILE NOT FOUND");
        }
        System.out.println(count + " words added to the set.");
    }
}