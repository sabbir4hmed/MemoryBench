package com.sabbir.memorybench;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private StorageSpeedTester speedTester;
    private LinearProgressIndicator progressBar;
    private TextView progressText;
    private MaterialButton startTestButton;
    private TextInputEditText bufferSizeInput;
    private TextInputEditText testSizeInput;
    private TextInputEditText iterationsInput;
    private AutoCompleteTextView bufferUnitSpinner;
    private AutoCompleteTextView testUnitSpinner;

    // RAM Performance TextViews
    private TextView ramFrequencyResult, ramFrequencyAvg;
    private TextView ramLatencyResult, ramLatencyAvg;
    private TextView ramBandwidthResult, ramBandwidthAvg;

    // Storage Performance TextViews
    private TextView storageSeqWriteResult, storageSeqWriteAvg;
    private TextView storageSeqReadResult, storageSeqReadAvg;
    private TextView storageRandWriteResult, storageRandWriteAvg;
    private TextView storageRandReadResult, storageRandReadAvg;

    // Score Lists
    private List<Double> ramFrequencyScores = new ArrayList<>();
    private List<Double> ramLatencyScores = new ArrayList<>();
    private List<Double> ramBandwidthScores = new ArrayList<>();
    private List<Double> storageSeqWriteScores = new ArrayList<>();
    private List<Double> storageSeqReadScores = new ArrayList<>();
    private List<Double> storageRandWriteScores = new ArrayList<>();
    private List<Double> storageRandReadScores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setupSpinners();
        speedTester = new StorageSpeedTester(this);
        checkPermissions();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        startTestButton = findViewById(R.id.startTestButton);
        bufferSizeInput = findViewById(R.id.bufferSizeInput);
        testSizeInput = findViewById(R.id.testSizeInput);
        iterationsInput = findViewById(R.id.iterationsInput);
        bufferUnitSpinner = findViewById(R.id.bufferUnitSpinner);
        testUnitSpinner = findViewById(R.id.testUnitSpinner);

        // RAM Results
        ramFrequencyResult = findViewById(R.id.ramFrequencyResult);
        ramFrequencyAvg = findViewById(R.id.ramFrequencyAvg);
        ramLatencyResult = findViewById(R.id.ramLatencyResult);
        ramLatencyAvg = findViewById(R.id.ramLatencyAvg);
        ramBandwidthResult = findViewById(R.id.ramBandwidthResult);
        ramBandwidthAvg = findViewById(R.id.ramBandwidthAvg);

        // Storage Results
        storageSeqWriteResult = findViewById(R.id.storageSeqWriteResult);
        storageSeqWriteAvg = findViewById(R.id.storageSeqWriteAvg);
        storageSeqReadResult = findViewById(R.id.storageSeqReadResult);
        storageSeqReadAvg = findViewById(R.id.storageSeqReadAvg);
        storageRandWriteResult = findViewById(R.id.storageRandWriteResult);
        storageRandWriteAvg = findViewById(R.id.storageRandWriteAvg);
        storageRandReadResult = findViewById(R.id.storageRandReadResult);
        storageRandReadAvg = findViewById(R.id.storageRandReadAvg);

        startTestButton.setOnClickListener(v -> startBenchmark());
    }

    private void setupSpinners() {
        String[] units = new String[]{"KB", "MB", "GB"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, units
        );
        bufferUnitSpinner.setAdapter(adapter);
        testUnitSpinner.setAdapter(adapter);

        bufferUnitSpinner.setText("MB", false);
        testUnitSpinner.setText("MB", false);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    100);
        }
    }

    private void startBenchmark() {
        if (!validateInputs()) return;

        int iterations = Integer.parseInt(iterationsInput.getText().toString());
        String bufferSize = bufferSizeInput.getText().toString();
        String testSize = testSizeInput.getText().toString();
        String bufferUnit = bufferUnitSpinner.getText().toString();
        String testUnit = testUnitSpinner.getText().toString();

        speedTester.setBufferSize(bufferSize, bufferUnit);
        speedTester.setTestSize(testSize, testUnit);

        clearResults();
        progressBar.setMax(iterations * 7); // 3 RAM tests + 4 Storage tests
        progressBar.setProgress(0);

        new Thread(() -> {
            for (int i = 0; i < iterations; i++) {
                runIteration(i + 1, iterations);
            }
            runOnUiThread(this::calculateAndDisplayAverages);
        }).start();
    }

    private boolean validateInputs() {
        if (bufferSizeInput.getText().toString().isEmpty() ||
                testSizeInput.getText().toString().isEmpty() ||
                iterationsInput.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void runIteration(int current, int total) {
        int progress = (current - 1) * 7;

        // RAM Tests
        StorageSpeedTester.RamTestResults ramResults = speedTester.testRamPerformance();
        updateRamResults(ramResults);
        updateProgress(progress += 3, "RAM Performance Tests");

        // Storage Tests
        double storageSeqWrite = speedTester.testStorageSequentialWrite();
        storageSeqWriteScores.add(storageSeqWrite);
        updateResult(storageSeqWriteResult, storageSeqWrite, "MB/s");
        updateProgress(++progress, "Storage Sequential Write");

        double storageSeqRead = speedTester.testStorageSequentialRead();
        storageSeqReadScores.add(storageSeqRead);
        updateResult(storageSeqReadResult, storageSeqRead, "MB/s");
        updateProgress(++progress, "Storage Sequential Read");

        double storageRandWrite = speedTester.testStorageRandomWrite();
        storageRandWriteScores.add(storageRandWrite);
        updateResult(storageRandWriteResult, storageRandWrite, "MB/s");
        updateProgress(++progress, "Storage Random Write");

        double storageRandRead = speedTester.testStorageRandomRead();
        storageRandReadScores.add(storageRandRead);
        updateResult(storageRandReadResult, storageRandRead, "MB/s");
        updateProgress(++progress, "Storage Random Read");
    }

    private void updateRamResults(StorageSpeedTester.RamTestResults results) {
        ramFrequencyScores.add(results.frequency);
        ramLatencyScores.add(results.latency);
        ramBandwidthScores.add(results.bandwidth);

        runOnUiThread(() -> {
            ramFrequencyResult.setText(String.format("%.2f MHz", results.frequency));
            ramLatencyResult.setText(String.format("%.2f ns", results.latency));
            ramBandwidthResult.setText(String.format("%.2f GB/s", results.bandwidth));
        });
    }

    private void updateResult(TextView view, double result, String unit) {
        runOnUiThread(() -> view.setText(String.format("%.2f %s", result, unit)));
    }

    private void updateProgress(int progress, String operation) {
        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            int percentage = (progress * 100) / progressBar.getMax();
            progressText.setText(String.format("Testing: %s (%d%%)", operation, percentage));
        });
    }

    private void calculateAndDisplayAverages() {
        updateAverage(ramFrequencyAvg, ramFrequencyScores, "MHz");
        updateAverage(ramLatencyAvg, ramLatencyScores, "ns");
        updateAverage(ramBandwidthAvg, ramBandwidthScores, "GB/s");
        updateAverage(storageSeqWriteAvg, storageSeqWriteScores, "MB/s");
        updateAverage(storageSeqReadAvg, storageSeqReadScores, "MB/s");
        updateAverage(storageRandWriteAvg, storageRandWriteScores, "MB/s");
        updateAverage(storageRandReadAvg, storageRandReadScores, "MB/s");

        startTestButton.setEnabled(true);
        progressText.setText("Benchmark Complete");
    }

    private void updateAverage(TextView view, List<Double> scores, String unit) {
        double average = scores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        view.setText(String.format("Avg: %.2f %s", average, unit));
    }

    private void clearResults() {
        startTestButton.setEnabled(false);

        // Clear all score lists
        ramFrequencyScores.clear();
        ramLatencyScores.clear();
        ramBandwidthScores.clear();
        storageSeqWriteScores.clear();
        storageSeqReadScores.clear();
        storageRandWriteScores.clear();
        storageRandReadScores.clear();

        // Reset all result views
        ramFrequencyResult.setText("-");
        ramLatencyResult.setText("-");
        ramBandwidthResult.setText("-");
        storageSeqWriteResult.setText("-");
        storageSeqReadResult.setText("-");
        storageRandWriteResult.setText("-");
        storageRandReadResult.setText("-");

        // Reset all average views
        ramFrequencyAvg.setText("Avg: -");
        ramLatencyAvg.setText("Avg: -");
        ramBandwidthAvg.setText("Avg: -");
        storageSeqWriteAvg.setText("Avg: -");
        storageSeqReadAvg.setText("Avg: -");
        storageRandWriteAvg.setText("Avg: -");
        storageRandReadAvg.setText("Avg: -");
    }
}