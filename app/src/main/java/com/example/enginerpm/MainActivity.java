package com.example.enginerpm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import java.io.BufferedReader;
import java.io.FileReader;
import org.jtransforms.fft.DoubleFFT_1D; // Add this library for FFT
import android.widget.LinearLayout;
import android.view.LayoutInflater;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MIC_PERMISSION = 1;
    private static final String FILE_NAME = "engine_rpm_records.txt";

    private TextView tvCurrentRPM;
    private TextView tvRecordDetails;
    private ScrollView scrollViewRecordDetails;
    private Button btnStart, btnStop, btnDelete, btnBack;
    private RecyclerView rvRecords;
    private RecordAdapter recordAdapter;
    private Handler handler;
    private Runnable rpmTask;
    private boolean isMeasuring = false;
    private List<Integer> measuredRPMs;

    private AudioRecord audioRecord;

    private short[] audioBuffer; // Buffer for audio data
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 2048; // Adjust if necessary
    private static final double NOISE_THRESHOLD = 0.02;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the correct layout
        setContentView(R.layout.activity_main);

        // Initialize UI components
        tvCurrentRPM = findViewById(R.id.tv_current_rpm);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);
        rvRecords = findViewById(R.id.rv_records);

        // Set up RecyclerView
        rvRecords.setLayoutManager(new LinearLayoutManager(this));
        recordAdapter = new RecordAdapter(new ArrayList<>(), recordName -> displayRecordDetails(recordName));
        rvRecords.setAdapter(recordAdapter);

        // Initialize other components
        handler = new Handler();
        measuredRPMs = new ArrayList<>();

        // Set up listeners
        setupListeners();

        // Check microphone permissions
        checkMicrophonePermission();

        // Load records from file
        loadRecordsFromFile();
    }



    private void checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Microphone permission not granted.");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC_PERMISSION);
        }
    }


    private void setupRecyclerView() {
        rvRecords = findViewById(R.id.rv_records);
        rvRecords.setLayoutManager(new LinearLayoutManager(this));

        // Ensure adapter is created and attached
        recordAdapter = new RecordAdapter(new ArrayList<>(), recordName -> displayRecordDetails(recordName));
        rvRecords.setAdapter(recordAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.e("Permission", "Microphone permission not granted");
                finish(); // Close the app if permission is not granted
            }
        }
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> startMeasurement());
        btnStop.setOnClickListener(v -> stopMeasurement());
        btnDelete.setOnClickListener(v -> deleteSelectedRecords());
        btnBack.setOnClickListener(v -> showMainScreen());
    }

    private void startMeasurement() {
        if (isMeasuring) {
            Log.d("MainActivity", "Measurement already running. Ignoring start request.");
            return;
        }

        isMeasuring = true;
        measuredRPMs.clear();

        // Determine minimum buffer size
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("AudioRecord", "Invalid buffer size");
            return;
        }

        // Initialize AudioRecord
        audioBuffer = new short[bufferSize];
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed");
            return;
        }

        audioRecord.startRecording();
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            Log.e("AudioRecord", "Failed to start recording");
            return;
        }

        Log.d("MainActivity", "Measurement started successfully.");

        // Create a new Runnable for measurement
        rpmTask = new Runnable() {
            @Override
            public void run() {
                int read = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);
                if (read <= 0) {
                    Log.e("AudioRecord", "No audio data read");
                    return;
                }

                int rpm = measureRPM();
                measuredRPMs.add(rpm);
                tvCurrentRPM.setText(String.format(Locale.getDefault(), "Current RPM: %d", rpm));
                handler.postDelayed(this, 1000);
            }
        };

        // Start the measurement task
        handler.post(rpmTask);
    }
    private void stopMeasurement() {
        if (!isMeasuring) return;

        isMeasuring = false;
        handler.removeCallbacks(rpmTask);

        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        // Save the measured values to a single file
        saveMeasuredValuesToFile();

        Log.d("MainActivity", "Measurement stopped and resources released.");
    }
    private int measureRPM() {
        int read = audioRecord.read(audioBuffer, 0, BUFFER_SIZE);
        if (read <= 0) {
            Log.e("AudioRecord", "No audio data read");
            return 0;
        }

        // Calculate the dominant frequency
        int dominantFrequency = calculateDominantFrequency(audioBuffer, SAMPLE_RATE);
        if (dominantFrequency <= 0) {
            Log.w("Frequency", "Invalid dominant frequency: " + dominantFrequency);
            return 0;
        }

        // Correct for the fifth harmonic
        int fundamentalFrequency = dominantFrequency / 5;

        // Filter out unrealistic frequencies after correction
        if (fundamentalFrequency < 100 || fundamentalFrequency > 700) {
            Log.w("Frequency", "Filtered out unrealistic fundamental frequency: " + fundamentalFrequency);
            return 0;
        }

        // Convert frequency to RPM
        int rpm = fundamentalFrequency * 60;

        Log.d("Frequency", "Dominant Frequency: " + dominantFrequency + " Hz");
        Log.d("Frequency", "Corrected Fundamental Frequency: " + fundamentalFrequency + " Hz");
        Log.d("RPM", "Calculated RPM: " + rpm);

        return rpm;
    }


    private int calculateDominantFrequency(short[] buffer, int sampleRate) {
        int fftSize = buffer.length;
        double[] fftInput = new double[fftSize * 2]; // Real and imaginary parts

        // Normalize the input buffer
        for (int i = 0; i < buffer.length; i++) {
            fftInput[i * 2] = buffer[i] / 32768.0; // Scale to [-1, 1]
            fftInput[i * 2 + 1] = 0.0; // Imaginary part
        }

        // Perform FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(fftSize);
        fft.complexForward(fftInput);

        // Analyze the FFT output
        double maxMagnitude = 0.0;
        int dominantIndex = 0;

        for (int i = 1; i < fftSize / 2; i++) { // Skip DC component (index 0)
            double real = fftInput[i * 2];
            double imaginary = fftInput[i * 2 + 1];
            double magnitude = Math.sqrt(real * real + imaginary * imaginary);

            // Skip low-magnitude noise
            if (magnitude < NOISE_THRESHOLD) {
                continue;
            }

            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
                dominantIndex = i;
            }
        }

        // Convert index to frequency
        int dominantFrequency = (dominantIndex * sampleRate) / fftSize;

        // Log the dominant frequency
        Log.d("FFT", "Dominant Frequency: " + dominantFrequency + " Hz");
        return dominantFrequency;
    }

    private void saveMeasuredValuesToFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        try (FileWriter writer = new FileWriter(new File(getFilesDir(), FILE_NAME), true)) {
            writer.write("Record: " + timestamp + "\n");
            for (int rpm : measuredRPMs) {
                writer.write(rpm + "\n");
            }
            writer.write("\n"); // Add a blank line to separate records

            recordAdapter.addRecord(timestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedRecords() {
        ArrayList<String> selectedRecords = recordAdapter.getSelectedRecords();
        for (String record : selectedRecords) {
            File file = new File(getFilesDir(), record + ".txt");
            if (file.exists()) {
                file.delete();
            }
        }
        recordAdapter.removeRecords(selectedRecords);
    }

    private void displayRecordDetails(String recordName) {
        // Inflate the record detail layout
        View detailView = LayoutInflater.from(this).inflate(R.layout.record_detail_layout, null);

        // Ensure the root layout is a LinearLayout
        LinearLayout linearLayout = (LinearLayout) detailView;

        // Find views in the inflated layout
        TextView tvRecordName = linearLayout.findViewById(R.id.tv_record_name);
        TextView tvRecordDetails = linearLayout.findViewById(R.id.tv_record_details);

        // Populate the record details
        tvRecordName.setText(recordName);
        tvRecordDetails.setText(getRecordDetails(recordName));

        // Replace the current content with the record detail view
        setContentView(linearLayout);

        // Add a back button in the detail view
        Button btnBack = new Button(this);
        btnBack.setText("Back");
        btnBack.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        btnBack.setOnClickListener(v -> {
            Log.d("MainActivity", "Back button pressed. Returning to main view.");
            showMainScreen();
        });


        // Add the back button programmatically
        linearLayout.addView(btnBack);
    }

    // Helper method to get record details from the file
    private String getRecordDetails(String recordName) {
        File file = new File(getFilesDir(), "engine_rpm_records.txt");
        if (!file.exists()) {
            return "No details available.";
        }

        StringBuilder recordDetails = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean recordFound = false;
            while ((line = reader.readLine()) != null) {
                if (line.equals("Record: " + recordName)) {
                    recordFound = true;
                } else if (recordFound && line.startsWith("Record:")) {
                    break;
                }
                if (recordFound) {
                    recordDetails.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading record details.";
        }
        return recordDetails.toString();
    }

    private void showMainScreen() {
        // Set the main content view
        setContentView(R.layout.activity_main);

        // Reinitialize UI components
        tvCurrentRPM = findViewById(R.id.tv_current_rpm);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);
        rvRecords = findViewById(R.id.rv_records);

        // Reattach the RecyclerView adapter
        if (rvRecords.getAdapter() == null) {
            rvRecords.setLayoutManager(new LinearLayoutManager(this));
            rvRecords.setAdapter(recordAdapter);
        }

        // Restore button visibility
        tvCurrentRPM.setVisibility(View.VISIBLE);
        rvRecords.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.VISIBLE);
        btnDelete.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.GONE);

        // Reset measurement state
        isMeasuring = false;

        // Reinitialize listeners
        setupListeners();
        Log.d("MainActivity", "Navigating back to main view.");
        Log.d("MainActivity", "Main screen restored with components and listeners reinitialized.");
    }

    private void loadRecordsFromFile() {
        File file = new File(getFilesDir(), FILE_NAME);
        if (!file.exists()) {
            Log.d("LoadRecords", "No record file found.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            List<String> records = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Record: ")) {
                    String recordName = line.substring(8); // Extract record name after "Record: "
                    records.add(recordName);
                }
            }
            // Add records to the existing adapter
            recordAdapter.addAll(records);
        } catch (Exception e) {
            Log.e("LoadRecords", "Error reading records from file", e);
        }
    }
}