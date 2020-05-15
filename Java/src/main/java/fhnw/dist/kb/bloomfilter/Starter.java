package fhnw.dist.kb.bloomfilter;


import fhnw.dist.kb.bloomfilter.struct.BloomFilter;

public class Starter {

    public static void main(String[] args) {
        // Initialize the filter
        BloomFilter filter = BloomFilter.create("/words.txt", 0.01);

        // Test against generated words
        filter.testFilter(100000);
    }
}
