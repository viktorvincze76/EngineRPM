package com.example.enginerpm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class RecordDetailActivity extends AppCompatActivity {

    private static final String EXTRA_RECORD_NAME = "record_name";
    private static final String EXTRA_RECORD_VALUES = "record_values";

    private TextView recordNameTextView;
    private ListView recordValuesListView;
    private Button backButton;

    public static void start(Context context, String recordName) {
        Intent intent = new Intent(context, RecordDetailActivity.class);
        intent.putExtra(EXTRA_RECORD_NAME, recordName);
        // Mocked data: replace with actual measured values
        ArrayList<Double> mockValues = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            mockValues.add(20000.0 + i * 1000); // Simulating RPM values
        }
        intent.putExtra(EXTRA_RECORD_VALUES, mockValues);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_detail);

        recordNameTextView = findViewById(R.id.recordNameTextView);
        recordValuesListView = findViewById(R.id.recordValuesListView);
        backButton = findViewById(R.id.backButton);

        String recordName = getIntent().getStringExtra(EXTRA_RECORD_NAME);
        ArrayList<Double> recordValues = (ArrayList<Double>) getIntent().getSerializableExtra(EXTRA_RECORD_VALUES);

        recordNameTextView.setText(recordName);

        ArrayAdapter<Double> valuesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recordValues);
        recordValuesListView.setAdapter(valuesAdapter);

        backButton.setOnClickListener(v -> finish());
    }
}
