package com.example.wishwash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryPage extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private DatabaseReference ordersReference;
    private LinearLayout historyContainer;

    private static final String TAG = "HistoryPage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_page);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://wish-wash-7288a-default-rtdb.asia-southeast1.firebasedatabase.app");
        ordersReference = database.getReference("orders");
        historyContainer = findViewById(R.id.history_container);

        Log.d(TAG, "Firebase initialized and ordersReference set.");

        ImageView backButton = findViewById(R.id.back_history);
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryPage.this, HomaPage.class);
            startActivity(intent);
            finish();
            Log.d(TAG, "Back button clicked. Moving to HomePage.");
        });

        displayOrderHistory();
    }

    private void displayOrderHistory() {
        if (firebaseAuth.getCurrentUser() == null) {
            Log.e(TAG, "User is not authenticated.");
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = firebaseAuth.getCurrentUser().getUid();
        Log.d(TAG, "User ID: " + userId);

        ordersReference.child(userId).orderByChild("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "Orders found for user.");
                    for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                        displayOrder(orderSnapshot);
                    }
                } else {
                    Log.d(TAG, "No orders found for user.");
                    Toast.makeText(HistoryPage.this, "No orders found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to retrieve orders: " + databaseError.getMessage());
                Toast.makeText(HistoryPage.this, "Failed to retrieve orders.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayOrder(DataSnapshot orderSnapshot) {
        if (orderSnapshot.hasChild("totalHarga") && orderSnapshot.hasChild("timestamp")) {
            int totalHarga = orderSnapshot.child("totalHarga").getValue(Integer.class);
            long timestamp = orderSnapshot.child("timestamp").getValue(Long.class);
            String packageType = orderSnapshot.child("packageType").getValue(String.class);
            String orderId = orderSnapshot.child("orderId").getValue(String.class);

            String date = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestamp));

            LayoutInflater inflater = LayoutInflater.from(this);
            View orderView = inflater.inflate(R.layout.order_item_layout, historyContainer, false);

            TextView orderIdTextView = orderView.findViewById(R.id.order_id);
            TextView orderTotalPriceTextView = orderView.findViewById(R.id.order_total_price);
            TextView orderDateTextView = orderView.findViewById(R.id.order_date);
            TextView orderPackageTypeTextView = orderView.findViewById(R.id.order_package_type);

            orderIdTextView.setText("Order ID: " + orderId);
            orderTotalPriceTextView.setText("Total: Rp" + totalHarga);
            orderDateTextView.setText("Tanggal: " + date);
            orderPackageTypeTextView.setText("Paket: " + packageType);

            historyContainer.addView(orderView);
            Log.d(TAG, "Order displayed: Total Rp" + totalHarga + ", Tanggal " + date + ", Paket " + packageType + ", Order ID: " + orderId);
        } else {
            Log.e(TAG, "Order structure invalid: " + orderSnapshot.toString());
        }
    }
}
