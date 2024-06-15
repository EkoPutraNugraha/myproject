package com.example.wishwash;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

public class WashKilogramActivity extends AppCompatActivity {

    private TextView totalPriceTextView;
    private EditText kgInput;
    private int totalHarga = 0;
    private int quantity = 0;
    private static final int PRICE_PER_KG = 6000;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference ordersReference;

    private static final String TAG = "WashKilogramActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wash_kilogram);

        totalPriceTextView = findViewById(R.id.total_price_kilogram);
        kgInput = findViewById(R.id.kg_input);
        Button continueButton = findViewById(R.id.continue_button_kilogram);
        ImageView backButton = findViewById(R.id.back_kilogram);
        Button addButton = findViewById(R.id.btn_plus);
        Button subtractButton = findViewById(R.id.btn_minus);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://wish-wash-7288a-default-rtdb.asia-southeast1.firebasedatabase.app");
        ordersReference = database.getReference("orders");

        Log.d(TAG, "Firebase initialized and ordersReference set.");

        addButton.setOnClickListener(v -> {
            updateQuantity(true);
        });

        subtractButton.setOnClickListener(v -> {
            updateQuantity(false);
        });

        kgInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateTotalPrice();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        continueButton.setOnClickListener(v -> {
            recordOrder();
            Intent intent = new Intent(WashKilogramActivity.this, PaymentActivity.class);
            intent.putExtra("total_price", totalHarga);
            intent.putExtra("package_type", "Wash per Kilogram");
            startActivity(intent);
            Log.d(TAG, "Continue button clicked. Order recorded and moving to PaymentActivity.");
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(WashKilogramActivity.this, HomaPage.class);
            startActivity(intent);
            finish();
            Log.d(TAG, "Back button clicked. Moving to HomePage.");
        });
    }

    private void updateQuantity(boolean increment) {
        String currentText = kgInput.getText().toString();
        if (currentText.isEmpty()) {
            quantity = 0;
        } else {
            quantity = Integer.parseInt(currentText);
        }
        if (increment) {
            quantity++;
        } else {
            if (quantity > 0) {
                quantity--;
            }
        }
        kgInput.setText(String.valueOf(quantity));
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        if (kgInput.getText().toString().isEmpty()) {
            kgInput.setText("0");
        }
        quantity = Integer.parseInt(kgInput.getText().toString());
        totalHarga = quantity * PRICE_PER_KG;
        totalPriceTextView.setText("Total Harga: Rp" + totalHarga);
    }

    private void recordOrder() {
        if (firebaseAuth.getCurrentUser() == null) {
            Log.e(TAG, "User is not authenticated.");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "User ID: " + userId);

        DatabaseReference userOrderRef = ordersReference.child(userId).push();
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, totalHarga, System.currentTimeMillis(), "Wash per Kilogram");

        userOrderRef.setValue(order)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order recorded successfully.");
                    Toast.makeText(WashKilogramActivity.this, "Order recorded successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to record order: " + e.getMessage());
                    Toast.makeText(WashKilogramActivity.this, "Failed to record order.", Toast.LENGTH_SHORT).show();
                });
    }

    public static class Order {
        public String orderId;
        public int totalHarga;
        public long timestamp;
        public String packageType;

        public Order() {
        }

        public Order(String orderId, int totalHarga, long timestamp, String packageType) {
            this.orderId = orderId;
            this.totalHarga = totalHarga;
            this.timestamp = timestamp;
            this.packageType = packageType;
        }
    }
}
