package com.example.wishwash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity {

    private TextView countdownTextView;
    private ImageView qrCodeImageView;
    private Button scanButton;
    private int totalHarga;
    private String packageType;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference ordersReference;

    private static final String TAG = "PaymentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        countdownTextView = findViewById(R.id.countdown);
        qrCodeImageView = findViewById(R.id.qr_code);
        scanButton = findViewById(R.id.scan_button);
        ImageView backButton = findViewById(R.id.back_payment);
        totalHarga = getIntent().getIntExtra("total_price", 0);
        packageType = getIntent().getStringExtra("package_type");

        firebaseAuth = FirebaseAuth.getInstance();
        ordersReference = FirebaseDatabase.getInstance().getReference("orders");

        startCountdown();
        generateQRCode(String.valueOf(totalHarga));

        scanButton.setOnClickListener(v -> handleQrCodeScanResult("success"));

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(PaymentActivity.this, HomaPage.class);
            startActivity(intent);
            finish(); // Optional: Call finish if you don't want to keep this activity in the back stack.
        });
    }

    private void startCountdown() {
        new CountDownTimer(300000, 1000) {
            public void onTick(long millisUntilFinished) {
                countdownTextView.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            public void onFinish() {
                Toast.makeText(PaymentActivity.this, "Time's up! Please try again.", Toast.LENGTH_LONG).show();
                finish();
            }
        }.start();
    }

    private void generateQRCode(String text) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            Bitmap bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void handleQrCodeScanResult(String qrCodeData) {
        if ("success".equals(qrCodeData)) {
            Toast.makeText(this, "Payment successful: " + qrCodeData, Toast.LENGTH_LONG).show();
            recordOrder();
        } else {
            Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void recordOrder() {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String orderId = UUID.randomUUID().toString();

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("totalHarga", totalHarga);
        orderData.put("status", "in_process");
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("packageType", packageType);

        ordersReference.child(userId).child(orderId).setValue(orderData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Order recorded successfully");
                startOrderCompletionTimer(userId, orderId);
            } else {
                Toast.makeText(PaymentActivity.this, "Failed to record order", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to record order");
            }
        });
    }

    private void startOrderCompletionTimer(String userId, String orderId) {
        new CountDownTimer(120000, 1000) { // 2 minutes timer
            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                ordersReference.child(userId).child(orderId).child("status").setValue("completed").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Order status updated to completed");
                        navigateToHistoryPage();
                    } else {
                        Toast.makeText(PaymentActivity.this, "Failed to update order status", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Failed to update order status");
                    }
                });
            }
        }.start();
    }

    private void navigateToHistoryPage() {
        Intent intent = new Intent(PaymentActivity.this, HistoryPage.class);
        startActivity(intent);
        Log.d(TAG, "Navigating to HistoryPage");
        finish();
    }
}
