package com.example.issuetracker;

import com.example.issuetracker.User; // <- THIS IS REQUIRED

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import java.util.HashSet;
import java.util.Set;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;





public class AdminDashboard extends AppCompatActivity {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private Set<String> updatingEmails = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        rvUsers = findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvUsers.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadUsersFromFirestore();
    }

    private void setBoldLabel(TextView textView, String label, String value) {
        SpannableString spannable = new SpannableString(label + " " + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
    }

    //change the size of the button

    private Button createButton(String text) {
        Button btn = new Button(AdminDashboard.this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(12); // smaller text
        btn.setPadding(8, 0, 8, 0); // smaller padding
//        btn.setMinHeight(0);
//        btn.setMinimumHeight(0);

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor("#1565C0")); // background color
        shape.setCornerRadius(25); // rounded corners
        btn.setBackground(shape);

        return btn;
    }


    // ------------------- Firestore Listener -------------------
    private void loadUsersFromFirestore() {
        db.collection("users")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(AdminDashboard.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value != null) {
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                switch (dc.getType()) {
                                    case ADDED:
                                        User newUser = new User(
                                                dc.getDocument().getString("fullName"),
                                                dc.getDocument().getString("email"),
                                                dc.getDocument().getString("dept"),
                                                dc.getDocument().getString("section"),
                                                dc.getDocument().getString("role"),
                                                dc.getDocument().getString("status")

                                        );
                                        userList.add(newUser);
                                        adapter.notifyItemInserted(userList.size() - 1);
                                        break;

                                    case REMOVED:
                                        String removedEmail = dc.getDocument().getString("email");
                                        for (int i = 0; i < userList.size(); i++) {
                                            if (userList.get(i).email.equals(removedEmail)) {
                                                userList.remove(i);
                                                adapter.notifyItemRemoved(i);
                                                break;
                                            }
                                        }
                                        break;

                                    case MODIFIED:
                                        String modifiedEmail = dc.getDocument().getString("email");
                                        if (updatingEmails.contains(modifiedEmail)) {
                                            // Skip updating the list temporarily
                                            break;
                                        }

                                        for (int i = 0; i < userList.size(); i++) {
                                            if (userList.get(i).email.equals(modifiedEmail)) {
                                                userList.set(i, new User(
                                                        dc.getDocument().getString("fullName"),
                                                        modifiedEmail,
                                                        dc.getDocument().getString("dept"),
                                                        dc.getDocument().getString("section"),
                                                        dc.getDocument().getString("role"),   // <-- add role
                                                        dc.getDocument().getString("status")
                                                ));
                                                adapter.notifyItemChanged(i);
                                                // break;
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    }
                });
    }

    // ------------------- RecyclerView Adapter -------------------
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private final List<User> users;

        UserAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            CardView cardView = new CardView(AdminDashboard.this);
            cardView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            cardView.setRadius(20);
            cardView.setCardElevation(8);
            cardView.setUseCompatPadding(true);
            cardView.setContentPadding(20, 20, 20, 20);
            cardView.setCardBackgroundColor(Color.WHITE); // <-- Set CardView background to white

            LinearLayout layout = new LinearLayout(AdminDashboard.this);
            layout.setOrientation(LinearLayout.VERTICAL);

            TextView tvName = createTextView(18, "#000000");
            TextView tvEmail = createTextView(14, "#000000");
            TextView tvDept = createTextView(14, "#000000");
            TextView tvSection = createTextView(14, "#000000");
            TextView tvRole = createTextView(14, "#000000");
            TextView tvStatus = createTextView(14, "#000000"); // red color for pending

            layout.addView(tvName);
            layout.addView(tvEmail);
            layout.addView(tvDept);
            layout.addView(tvSection);
            layout.addView(tvRole);   // <-- add role viewonBindViewHolder
            layout.addView(tvStatus);

            LinearLayout buttonLayout = new LinearLayout(AdminDashboard.this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setWeightSum(3);
            buttonLayout.setPadding(0, 0, 0, 0);

////
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
//            params.setMarginEnd(4);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dpToPx(35), 1 // width = 0 (weight), height = 36dp
            );
            params.setMarginEnd(4);

            Button btnEdit = createButton("Edit");
            Button btnChange = createButton("Change");
            Button btnDelete = createButton("Delete");

            btnEdit.setLayoutParams(params);
            btnChange.setLayoutParams(params);
            btnDelete.setLayoutParams(params);

            buttonLayout.addView(btnEdit);
            buttonLayout.addView(btnChange);
            buttonLayout.addView(btnDelete);

            layout.addView(buttonLayout);
            cardView.addView(layout);

            return new UserViewHolder(cardView, tvName, tvEmail, tvDept, tvSection, tvRole, tvStatus, btnEdit, btnChange, btnDelete);
        }

        private int dpToPx(int dp) {
            float scale = getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            User user = users.get(position);

            holder.tvName.setText(user.fullName);
            holder.tvName.setTypeface(null, Typeface.BOLD); // makes text bold

            // add bottom margin programmatically
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.tvName.getLayoutParams();
            params.bottomMargin = 20; // in pixels
            holder.tvName.setLayoutParams(params);

            //holder.tvEmail.setText("Email: " + user.email);
            //holder.tvDept.setText("Department: " + user.dept);
            //holder.tvSection.setText("Section: " + user.section);
            //holder.tvRole.setText("Role: " + user.role);   // <-- show role
            setBoldLabel(holder.tvEmail, "Email:             ", user.email);
            setBoldLabel(holder.tvDept, "Department:  ", user.dept);
            setBoldLabel(holder.tvSection, "Section:          ", user.section);
            setBoldLabel(holder.tvRole, "Role:               ", user.role);
//            setBoldLabel(holder.tvStatus, "Status:           ", user.status);

            //MAKING THE COLOR RED AND GREEN
            String statusLabel = "Status: ";
            holder.tvStatus.setText(statusLabel + user.status);
            holder.tvStatus.setTypeface(null, Typeface.BOLD);

            if ("pending".equalsIgnoreCase(user.status)) {
                holder.tvStatus.setTextColor(Color.RED);
            } else if ("active".equalsIgnoreCase(user.status)) {
                holder.tvStatus.setTextColor(Color.parseColor("#208A25"));
            } else {
                holder.tvStatus.setTextColor(Color.BLACK);
            }



//            holder.btnEdit.setOnClickListener(v ->
//                    Toast.makeText(AdminDashboard.this, "Edit " + user.fullName, Toast.LENGTH_SHORT).show());

            holder.btnEdit.setOnClickListener(v -> {
                showEditDialog(user, position);
            });
//
//            holder.btnChange.setOnClickListener(v ->
//                    Toast.makeText(AdminDashboard.this, "Change " + user.fullName, Toast.LENGTH_SHORT).show());

//
            // ✅ Add this section for Approve/Active button
            if (user.status.equals("pending")) {
                holder.btnChange.setText("Approve");
                holder.btnChange.setVisibility(View.VISIBLE);

                holder.btnChange.setEnabled(true); // make sure it is clickable
                holder.btnChange.setOnClickListener(v -> {
                    db.collection("users").whereEqualTo("email", user.email)
                            .get().addOnSuccessListener(query -> {
                                for (var doc : query.getDocuments()) {
                                    doc.getReference().update("status", "active");
                                }
                                Toast.makeText(AdminDashboard.this, user.fullName + " approved", Toast.LENGTH_SHORT).show();
                            });
                });
            } else {
                //holder.btnChange.setText(user.status != null ? user.status : "Active");
                holder.btnChange.setText("Active");
                holder.btnChange.setEnabled(false);
            }

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AdminDashboard.this)
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to delete " + user.fullName + "?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            db.collection("users")
                                    .whereEqualTo("email", user.email.trim())
                                    .get()
                                    .addOnSuccessListener(query -> {
                                        if (query.isEmpty()) {
                                            Toast.makeText(AdminDashboard.this, "No user found with email " + user.email, Toast.LENGTH_SHORT).show();
                                        } else {
                                            for (var doc : query.getDocuments()) {
                                                doc.getReference().delete()
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(AdminDashboard.this, "User deleted", Toast.LENGTH_SHORT).show();

                                                            // ✅ Also delete issues linked to this user
                                                            db.collection("issues").whereEqualTo("email", user.email.trim())
                                                                    .get()
                                                                    .addOnSuccessListener(issueQuery -> {
                                                                        for (var issueDoc : issueQuery.getDocuments()) {
                                                                            issueDoc.getReference().delete();
                                                                        }
                                                                    });
                                                        })
                                                        .addOnFailureListener(e ->
                                                                Toast.makeText(AdminDashboard.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                            }
                                        }
                                    })

                                    .addOnFailureListener(e ->
                                            Toast.makeText(AdminDashboard.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });


        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvDept, tvSection, tvRole, tvStatus;
            Button btnEdit, btnChange, btnDelete;

            public UserViewHolder(View itemView, TextView tvName, TextView tvEmail, TextView tvDept, TextView tvSection, TextView tvRole, TextView tvStatus,
                                  Button btnEdit, Button btnChange, Button btnDelete) {
                super(itemView);
                this.tvName = tvName;
                this.tvEmail = tvEmail;
                this.tvDept = tvDept;
                this.tvSection = tvSection;
                this.tvRole = tvRole; // <-- add role
                this.tvStatus = tvStatus;
                this.btnEdit = btnEdit;
                this.btnChange = btnChange;
                this.btnDelete = btnDelete;
            }
        }

        private TextView createTextView(int sizeSp, String colorHex) {
            TextView tv = new TextView(AdminDashboard.this);
            tv.setTextSize(sizeSp);
            tv.setTextColor(Color.parseColor(colorHex));
            return tv;
        }
/*
        private Button createButton(String text) {
            Button btn = new Button(AdminDashboard.this);
            btn.setText(text);
            btn.setTextColor(Color.WHITE);
            btn.setTextSize(12); // smaller text
            btn.setPadding(16, 8, 16, 8); // smaller padding
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(25); // rounded corners
            btn.setBackgroundColor(Color.parseColor("#1565C0"));
            return btn;
        }
        */
    }

    // method to show the Edit dialog
    private void showEditDialog(User user, int position) {
//        // Create a dialog
//        AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
//        builder.setTitle("Edit User");
//
//        // Create a vertical layout for input fields
//        LinearLayout layout = new LinearLayout(this);
//        layout.setOrientation(LinearLayout.VERTICAL);
//        layout.setPadding(50, 40, 50, 10);
//
//        // Create EditTexts for each field
//        android.widget.EditText etName = new android.widget.EditText(this);
//        etName.setHint("Full Name");
//        etName.setText(user.fullName);
//        layout.addView(etName);
//
//        android.widget.EditText etEmail = new android.widget.EditText(this);
//        etEmail.setHint("Email");
//        etEmail.setText(user.email);
//        etEmail.setEnabled(false); // disable email if you want it immutable
//        layout.addView(etEmail);
//
//
//
//        // Create a layout param with margin
//        LinearLayout.LayoutParams paramsWithMargin = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        paramsWithMargin.setMargins(0, 20, 0, 0); // left, top, right, bottom (20px top margin)
//
//        // Department Spinner
//        Spinner spDept = new Spinner(this);
//        spDept.setLayoutParams(paramsWithMargin);
//        String[] deptArray = getResources().getStringArray(R.array.dept);
//        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deptArray);
//        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spDept.setAdapter(deptAdapter);
//        int deptPosition = deptAdapter.getPosition(user.dept);
//        if (deptPosition >= 0) spDept.setSelection(deptPosition);
//        layout.addView(spDept);
//
//// Section Spinner
//        Spinner spSection = new Spinner(this);
//        spSection.setLayoutParams(paramsWithMargin);
//        String[] sectionArray = getResources().getStringArray(R.array.section);
//        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sectionArray);
//        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spSection.setAdapter(sectionAdapter);
//        int sectionPosition = sectionAdapter.getPosition(user.section);
//        if (sectionPosition >= 0) spSection.setSelection(sectionPosition);
//        layout.addView(spSection);
//
//// Role Spinner
//        Spinner spRole = new Spinner(this);
//        spRole.setLayoutParams(paramsWithMargin);
//        String[] roleArray = getResources().getStringArray(R.array.roles);
//        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleArray);
//        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spRole.setAdapter(roleAdapter);
//        int rolePosition = roleAdapter.getPosition(user.role);
//        if (rolePosition >= 0) spRole.setSelection(rolePosition);
//        layout.addView(spRole);




        //here the change starts
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Edit User");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etEmail = dialogView.findViewById(R.id.etEmail);
        Spinner spDept = dialogView.findViewById(R.id.spDept);
        Spinner spSection = dialogView.findViewById(R.id.spSection);
        Spinner spRole = dialogView.findViewById(R.id.spRole);
        TextView tvError = dialogView.findViewById(R.id.tvError);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);

        // Fill fields
        etName.setText(user.fullName);
        etEmail.setText(user.email);
        etEmail.setEnabled(false);
        etName.setEnabled(false);

        // Setup spinners
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.deptAdmin));
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDept.setAdapter(deptAdapter);
        spDept.setSelection(deptAdapter.getPosition(user.dept));

        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.sectionAdmin));
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSection.setAdapter(sectionAdapter);
        spSection.setSelection(sectionAdapter.getPosition(user.section));


        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, getResources().getStringArray(R.array.rolesAdmin));
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(roleAdapter);
        spRole.setSelection(roleAdapter.getPosition(user.role));

        View titleView = getLayoutInflater().inflate(R.layout.dialog_title, null);
        builder.setCustomTitle(titleView);


        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle Save button
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newDept = spDept.getSelectedItem().toString();
            String newSection = spSection.getSelectedItem().toString();
            String newRole = spRole.getSelectedItem().toString();

            if (newName.isEmpty()) {
                tvError.setText("Name cannot be empty!");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
       // builder.setView(layout);


        // Add buttons
//        builder.setPositiveButton("Save", (dialog, which) -> {
//            // Get updated values
//            String newName = etName.getText().toString().trim();
////            String newDept = etDept.getText().toString().trim();
////            String newSection = etSection.getText().toString().trim();
////            String newRole = etRole.getText().toString().trim();
//            String newDept = spDept.getSelectedItem().toString();
//            String newSection = spSection.getSelectedItem().toString();
//            String newRole = spRole.getSelectedItem().toString();


            updatingEmails.add(user.email); // <-- mark as updating

            // ✅ Update Firestore users collection
            db.collection("users").whereEqualTo("email", user.email)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (var doc : query.getDocuments()) {
                            doc.getReference().update(
                                    "fullName", newName,
                                    "dept", newDept,
                                    "section", newSection,
                                    "role", newRole
                            ).addOnFailureListener(e ->
                                    Toast.makeText(AdminDashboard.this, "Failed to update user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }

                        // ✅ Also update issues collection
                        db.collection("issues").whereEqualTo("email", user.email)
                                .get()
                                .addOnSuccessListener(issueQuery -> {
                                    for (var issueDoc : issueQuery.getDocuments()) {
                                        issueDoc.getReference().update(
                                                "fullName", newName,
                                                "dept", newDept,
                                                "section", newSection,
                                                "role", newRole
                                        ).addOnFailureListener(e ->
                                                Toast.makeText(AdminDashboard.this, "Failed to update issue: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(AdminDashboard.this, "Failed to query issues: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );

                        // Update RecyclerView locally
//                        userList.set(position, new User(newName, user.email, newDept, newSection, newRole));
//                        adapter.notifyItemChanged(position);

                        updatingEmails.remove(user.email);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(AdminDashboard.this, "Failed to query users: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

            dialog.dismiss();
        });

        // Handle Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

       //builder.create().show();
    }
}


