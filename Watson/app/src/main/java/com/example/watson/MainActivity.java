package com.example.watson;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.speech_to_text.v1.websocket.BaseRecognizeCallback;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    TextView textVoice,textPath;
    String text;
    Button btnSave, btnUpload,btnPath;
    Uri uri;
   String path;
    String FilePath;
    private static final String FILE_NAME = "exmple.txt";
    SpeechToText speechToText;
    String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission access", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "permission faild", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (String premission : permissions) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, premission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
            }
        }
            textVoice = findViewById(R.id.text_voice);
            btnSave = findViewById(R.id.save);
            textPath = findViewById(R.id.text_path);
            btnPath = findViewById(R.id.path);
            btnPath.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent uploadFile = new Intent(Intent.ACTION_GET_CONTENT);
                    uploadFile.setType("*/*");
                    uploadFile = Intent.createChooser(uploadFile, "Choose a file");
                    startActivityForResult(uploadFile,1);
                    textPath.setText(FilePath);

                }
            });

            IamAuthenticator authenticator = new IamAuthenticator("19pXTKHtHKoVXac3FZkZjTIjgsPWPriWqIO8jox-I9Jz");
            speechToText = new SpeechToText(authenticator);
            speechToText.setServiceUrl("https://api.eu-gb.speech-to-text.watson.cloud.ibm.com/instances/bd9beda8-0fb7-4cfb-bfca-6270b8c07cf6");
            btnUpload = findViewById(R.id.uploud);
            btnUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {

                        RecognizeOptions recognizeOptions = new RecognizeOptions.Builder()
                                .audio(new FileInputStream(FilePath))
                                .contentType(HttpMediaType.AUDIO_MP3)
                                .model("en-US_BroadbandModel")
                                .keywords(Arrays.asList("colorado", "tornado", "tornadoes"))
                                .keywordsThreshold((float) 0.5)
                                .interimResults(true)
                                .inactivityTimeout(2000)
                                .maxAlternatives(3)
                                .build();

                        BaseRecognizeCallback baseRecognizeCallback =
                                new BaseRecognizeCallback() {

                                    @Override
                                    public void onTranscription(SpeechRecognitionResults speechRecognitionResults) {
                                        if (speechRecognitionResults.getResults() != null && !speechRecognitionResults.getResults().isEmpty()) {
                                            text = speechRecognitionResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                                            Handler handler = new Handler(Looper.getMainLooper()) {
                                                @Override
                                                public void handleMessage(Message msg) {
                                                    // Any UI task, example
                                                    textVoice.setText(text);
                                                    String time = new SimpleDateFormat("yyyyMMdd_HH", Locale.getDefault()).format(System.currentTimeMillis());
                                                 String texts = textVoice.getText().toString();
                                                    try {
                                                        File path = Environment.getExternalStorageDirectory();
                                                        File dir = new File(path + "/My Files/");
                                                        dir.mkdir();
                                                        String fileName = "MyFile_"+time+".txt";
                                                        File file = new File(dir,fileName);
                                                        FileOutputStream fw = new FileOutputStream(file.getAbsoluteFile());
                                                        DataOutputStream data = new DataOutputStream(fw);
                                                       data.writeUTF(texts);
                                                       data.flush();
                                                        Toast.makeText(MainActivity.this, fileName+" is saved to \n"+dir, Toast.LENGTH_LONG).show();

                                                    } catch (IOException e) {
                                                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            };
                                            handler.sendEmptyMessage(1);
                                        }
                                    }

                                    @Override
                                    public void onDisconnected() {
                                    }

                                };

                        speechToText.recognizeUsingWebSocket(recognizeOptions,
                                baseRecognizeCallback);

                    } catch (FileNotFoundException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });


        }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
            if (resultCode == RESULT_OK) {
                uri = data.getData();
                String[] filePathColumn = { MediaStore.Audio.AudioColumns.DATA};
                Cursor cursor = getContentResolver().query(uri,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
               FilePath  = cursor.getString(columnIndex);
                cursor.close();
            }
                break;

            }
        }
    }

