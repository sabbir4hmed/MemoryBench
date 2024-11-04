package com.sabbir.memorybench;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class StorageSpeedTester {
    private Context context;
    private long bufferSize;
    private long testSize;
    private byte[] testData;
    private Random random;
    private static final int DEFAULT_BUFFER_SIZE = 8 * 1024 * 1024; // 8MB
    private static final int DEFAULT_TEST_SIZE = 100 * 1024 * 1024; // 100MB

    public class RamTestResults {
        public double frequency;  // MHz
        public double latency;    // nanoseconds
        public double bandwidth;  // GB/s
    }

    public StorageSpeedTester(Context context) {
        this.context = context;
        this.random = new Random();
        setDefaultSizes();
    }

    private void setDefaultSizes() {
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.testSize = DEFAULT_TEST_SIZE;
        initTestData();
    }

    public void setBufferSize(String size, String unit) {
        this.bufferSize = calculateSize(size, unit);
        initTestData();
    }

    public void setTestSize(String size, String unit) {
        this.testSize = calculateSize(size, unit);
    }

    private long calculateSize(String size, String unit) {
        long value = Long.parseLong(size);
        switch (unit.toUpperCase()) {
            case "GB":
                return value * 1024 * 1024 * 1024;
            case "MB":
                return value * 1024 * 1024;
            case "KB":
                return value * 1024;
            default:
                return value;
        }
    }

    private void initTestData() {
        testData = new byte[(int)bufferSize];
        random.nextBytes(testData);
    }

    public RamTestResults testRamPerformance() {
        RamTestResults results = new RamTestResults();

        // Test RAM Frequency
        results.frequency = testRamFrequency();

        // Test RAM Latency
        results.latency = testRamLatency();

        // Test RAM Bandwidth
        results.bandwidth = testRamBandwidth();

        return results;
    }

    private double testRamFrequency() {
        int arraySize = 64 * 1024 * 1024; // 64MB test array
        long[] testArray = new long[arraySize / 8];

        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = i;
        }

        long startTime = System.nanoTime();
        long sum = 0;

        for (int pass = 0; pass < 10; pass++) {
            for (int i = 0; i < testArray.length; i++) {
                sum += testArray[i];
                testArray[i] = sum;
            }
        }

        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        double totalOperations = testArray.length * 10 * 2;
        return (totalOperations / elapsedSeconds) / 1_000_000; // MHz
    }

    private double testRamLatency() {
        int arraySize = 16 * 1024 * 1024; // 16MB for cache miss
        int[] testArray = new int[arraySize / 4];
        int stride = 64; // Cache line size

        for (int i = 0; i < testArray.length; i++) {
            testArray[i] = (i + stride) % testArray.length;
        }

        long startTime = System.nanoTime();
        int index = 0;

        for (int i = 0; i < 1000000; i++) {
            index = testArray[index];
        }

        double elapsedNanos = System.nanoTime() - startTime;
        return elapsedNanos / 1000000.0; // Average latency in ns
    }

    private double testRamBandwidth() {
        // Use a smaller test size that works on most devices
        int arraySize = 128 * 1024 * 1024; // 128MB test array
        byte[] testArray = new byte[arraySize];

        long startTime = System.nanoTime();

        // Perform multiple passes to get accurate bandwidth measurement
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < arraySize; i += 64) {
                testArray[i] = (byte)(i & 0xFF);
            }
        }

        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        // Calculate total data transferred (reads + writes) * passes
        double totalGB = (arraySize * 2.0 * 4) / (1024.0 * 1024.0 * 1024.0);
        return totalGB / elapsedSeconds; // GB/s
    }



    public double testStorageSequentialWrite() {
        File file = new File(context.getFilesDir(), "test_file");
        long startTime = System.nanoTime();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (long i = 0; i < testSize; i += bufferSize) {
                fos.write(testData, 0, (int)Math.min(bufferSize, testSize - i));
            }
            fos.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return calculateSpeed(startTime);
    }

    public double testStorageSequentialRead() {
        File file = new File(context.getFilesDir(), "test_file");
        byte[] buffer = new byte[(int)bufferSize];
        long totalBytesRead = 0;
        long startTime = System.nanoTime();

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), (int)bufferSize)) {
            while (totalBytesRead < testSize) {
                int bytesRead = bis.read(buffer, 0, (int)Math.min(bufferSize, testSize - totalBytesRead));
                if (bytesRead == -1) break;

                // Force data processing
                for (int i = 0; i < bytesRead; i++) {
                    if (buffer[i] != 0) buffer[i]--;
                }

                totalBytesRead += bytesRead;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return calculateSpeed(startTime);
    }

    public double testStorageRandomWrite() {
        File file = new File(context.getFilesDir(), "test_file");
        long startTime = System.nanoTime();
        int iterations = (int)(testSize / bufferSize);

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            for (int i = 0; i < iterations; i++) {
                long offset = random.nextInt((int)(testSize - bufferSize));
                raf.seek(offset);
                raf.write(testData, 0, (int)bufferSize);
            }
            raf.getFD().sync();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return calculateSpeed(startTime);
    }

    public double testStorageRandomRead() {
        File file = new File(context.getFilesDir(), "test_file");
        byte[] buffer = new byte[(int)bufferSize];
        int iterations = (int)(testSize / bufferSize);
        long startTime = System.nanoTime();

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            for (int i = 0; i < iterations; i++) {
                long offset = random.nextInt((int)(testSize - bufferSize));
                raf.seek(offset);
                int bytesRead = raf.read(buffer);

                // Force data processing
                for (int j = 0; j < bytesRead; j++) {
                    if (buffer[j] != 0) buffer[j]--;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return calculateSpeed(startTime);
    }

    private double calculateSpeed(long startTime) {
        double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return (testSize / 1024.0 / 1024.0) / elapsedSeconds; // MB/s
    }
}