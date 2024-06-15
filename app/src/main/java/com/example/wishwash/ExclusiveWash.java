package com.example.wishwash;

import android.content.Intent;
import android.os.Bundle;
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

public class ExclusiveWash extends AppCompatActivity {

    private TextView totalPriceTextView;
    private int totalHarga = 0;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference ordersReference;

    private static final String TAG = "ExclusiveWash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exclusive_wash);

        totalPriceTextView = findViewById(R.id.total_price);
        Button continueButton = findViewById(R.id.continue_button);
        ImageView backButton = findViewById(R.id.back_exlusive);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://wish-wash-7288a-default-rtdb.asia-southeast1.firebasedatabase.app");
        ordersReference = database.getReference("orders");

        Log.d(TAG, "Firebase initialized and ordersReference set.");

        setupItem(R.id.item1, 15000);
        setupItem(R.id.item2, 35000);
        setupItem(R.id.item3, 10000);
        setupItem(R.id.item4, 20000);
        setupItem(R.id.item5, 40000);

        continueButton.setOnClickListener(v -> {
            recordOrder();
            Intent intent = new Intent(ExclusiveWash.this, PaymentActivity.class);
            intent.putExtra("total_price", totalHarga);
            intent.putExtra("package_type", "Paket Exclusive");
            startActivity(intent);
            Log.d(TAG, "Continue button clicked. Order recorded and moving to PaymentActivity.");
        });

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(ExclusiveWash.this, HomaPage.class);
            startActivity(intent);
            finish();
            Log.d(TAG, "Back button clicked. Moving to HomePage.");
        });
    }

    private void setupItem(int itemId, int itemPrice) {
        View itemView = findViewById(itemId);
        EditText quantityTextView = itemView.findViewById(R.id.quantity);
        Button addButton = itemView.findViewById(R.id.btn_plus);
        Button subtractButton = itemView.findViewById(R.id.btn_minus);

        addButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(quantityTextView.getText().toString());
            quantity++;
            quantityTextView.setText(String.valueOf(quantity));
            updateTotalPrice(itemPrice);
            Log.d(TAG, "Added one item. Quantity: " + quantity + ", Total: " + totalHarga);
        });

        subtractButton.setOnClickListener(v -> {
            int quantity = Integer.parseInt(quantityTextView.getText().toString());
            if (quantity > 0) {
                quantity--;
                quantityTextView.setText(String.valueOf(quantity));
                updateTotalPrice(-itemPrice);
                Log.d(TAG, "Subtracted one item. Quantity: " + quantity + ", Total: " + totalHarga);
            }
        });
    }

    private void updateTotalPrice(int amount) {
        totalHarga += amount;
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
        Order order = new Order(orderId, totalHarga, System.currentTimeMillis(), "Paket Exclusive");

        userOrderRef.setValue(order)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Order recorded successfully.");
                    Toast.makeText(ExclusiveWash.this, "Order recorded successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to record order: " + e.getMessage());
                    Toast.makeText(ExclusiveWash.this, "Failed to record order.", Toast.LENGTH_SHORT).show();
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
