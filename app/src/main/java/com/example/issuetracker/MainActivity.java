package com.example.issuetracker;
import com.example.issuetracker.Login;
import com.example.issuetracker.Registration;
import com.example.issuetracker.R;

import android.os.Bundle;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import com.example.issuetracker.databinding.ActivityMainBinding;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.view.GestureDetector;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.MotionEvent;
import android.os.Bundle;
import android.text.Html;           // For Html.fromHtml
import android.widget.Button;
import android.widget.TextView;    // For TextView
import androidx.appcompat.app.AppCompatActivity; // If using AppCompatActivity




public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private GestureDetector gestureDetector;

    Button StartButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        /*binding for the arrow
        binding.arrow.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Login.class);
            startActivity(intent);
        });
         */

        StartButton = findViewById(R.id.StartButton);

        StartButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Login.class));
        });

        TextView tv = findViewById(R.id.textView2);
        tv.setText(Html.fromHtml(getString(R.string.welcome_text), Html.FROM_HTML_MODE_LEGACY));

        TextView textView9 = findViewById(R.id.textView9);
        textView9.setText(
                Html.fromHtml(getString(R.string.welcome_text2), Html.FROM_HTML_MODE_LEGACY)
        );


        //root layout for the swipe detection
        ConstraintLayout mainLayout = findViewById(R.id.main2);

        // Initialize gesture detector
        gestureDetector = new GestureDetector(this, new SwipeGestureDetector());

        // Attach touch listener
        mainLayout.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }


    private boolean loginLaunched = false;
    // Gesture detector class
    private class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;
        private static final int SWIPE_VELOCITY_THRESHOLD = 50;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) { // Horizontal swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) { // Swipe left
                        goToLogin(true);
                    }
                    //else { // Swipe right
                       // goToLogin(false);
                    //}
                }
            } else { // Vertical swipe
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) { // Swipe up
                        goToLogin(true);
                    }
                    //else { // Swipe down
                      //  goToLogin(false);
                   // }
                }
            }
            return true;
        }
    }

    // Navigate to Login activity with slide animation
    private void goToLogin(boolean forward) {
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
/*
        if (forward) {
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else {
            overridePendingTransition(android.R.anim.slide_out_right, android.R.anim.slide_in_left);
        }

 */
        overridePendingTransition(0, 0);
    }

        //binding.registerButton.setOnClickListener(v -> startActivity(new Intent(this, Registration.class)));
}
