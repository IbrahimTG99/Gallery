//firebase part 2
package com.example.gallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;


public class Login extends AppCompatActivity {

    boolean login = true;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.signup_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.signup_menu) {
            login = false;
            TextView log_txt = findViewById(R.id.log_txt);
            log_txt.setText("Signup");
            Button log_btn = findViewById(R.id.log_btn);
            log_btn.setText("Signup");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText email = findViewById(R.id.email_edt);
        EditText pass = findViewById(R.id.pass_edt);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String TAG = "****************";

        Button log = findViewById(R.id.log_btn);
        log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCredentials(email.getText().toString(), pass.getText().toString(), db);
            }
        });
    }

    private void checkCredentials(String email, String pass, FirebaseFirestore db) {
        String TAG = "*****************";

        if (!login)
        {
            Map<String, Object> user = new HashMap<>();
            user.put("mail", email);
            user.put("pass", pass);

            // Add a new document with a generated ID
            db.collection("users")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            Intent intent = new Intent();
                            intent.putExtra("mail", email);
                            setResult(RESULT_OK, intent);
                            Toast.makeText(getApplicationContext(), "Signup Successful", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        }
        else
        {
            final String f_mail = email;
            final String f_pass = pass;

            db.collection("users")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                boolean found = false;
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Map<String, Object> data = document.getData();
                                    if (data.get("mail").toString().equals(f_mail) && data.get("pass").toString().equals(f_pass)) {
                                        found = true;
                                        Intent intent = new Intent();
                                        intent.putExtra("mail", data.get("mail").toString());
                                        setResult(RESULT_OK, intent);
                                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                    Log.d(TAG, document.getId() + " => " + document.getData());
                                }
                                if (!found) {
                                    Toast.makeText(getApplicationContext(), "Invalid email or password", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.w(TAG, "Error getting documents.", task.getException());
                            }
                        }
                    });
        }
    }
}
//        // Create a Cloud Storage reference from the app
//        StorageReference storageRef = storage.getReference();
//
//        // Create a reference to "mountains.jpg"
//        StorageReference mountainsRef = storageRef.child("mountains.jpg");
//
//        // Create a reference to 'images/mountains.jpg'
//        StorageReference mountainImagesRef = storageRef.child("images/mountains.jpg");
//
//        // While the file names are the same, the references point to different files
//        mountainsRef.getName().equals(mountainImagesRef.getName());    // true
//        mountainsRef.getPath().equals(mountainImagesRef.getPath());    // false
//
//        // Create a reference with an initial file path and name
//        StorageReference pathReference = storageRef.child("images/stars.jpg");
//
//        // Create a reference to a file from a Cloud Storage URI
//        StorageReference gsReference = storage.getReferenceFromUrl("gs://bucket/images/stars.jpg");
//
//        // Create a reference from an HTTPS URL
//        // Note that in the URL, characters are URL escaped!
//        StorageReference httpsReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/b/bucket/o/images%20stars.jpg");

    // UploadImage method
//    private void uploadImage ()
//    {
//        if (filePath != null)
//        {
//            // Code for showing progressDialog while uploading
//            ProgressDialog progressDialog = new ProgressDialog(this);
//            progressDialog.setTitle("Uploading...");
//            progressDialog.show();
//
//            // Defining the child of storageReference
//            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
//
//            // adding listeners on upload
//            // or failure of image
//            ref.putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//
//                    // Image uploaded successfully
//                    // Dismiss dialog
//                    progressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(), "Image Uploaded!!", Toast.LENGTH_SHORT).show();
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//
//                    // Error, Image not uploaded
//                    progressDialog.dismiss();
//                    Toast.makeText(getApplicationContext(), "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//
//            // Progress Listener for loading
//            // percentage on the dialog box
//            @Override
//            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
//                progressDialog.setMessage("Uploaded " + (int) progress + "%");
//                }
//            });
//        }
//    }


//        StorageReference listRef = FirebaseStorage.getInstance().getReference().child("images");
//        listRef.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
//            @Override
//            public void onSuccess(ListResult listResult) {
//                for(StorageReference file:listResult.getItems()){
//                    file.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                        @Override
//                        public void onSuccess(Uri uri) {
//                            imagelist.add(uri.toString());
//                            Log.e("Itemvalue",uri.toString());
//                        }
//                    }).addOnSuccessListener(new OnSuccessListener<Uri>() {
//                        @Override
//                        public void onSuccess(Uri uri) {
//                            recyclerView.setAdapter(adapter);
//                            progressBar.setVisibility(View.GONE);
//                        }
//                    });
//                }
//            }
//        });
//
//        imagelist = new ArrayList<>();
//        recyclerView = findViewById(R.id.recyclerview);
//        adapter = new ImageAdapter(imagelist,this);
//        recyclerView.setLayoutManager(new LinearLayoutManager(null));
//        progressBar = findViewById(R.id.progress);
//        progressBar.setVisibility(View.VISIBLE);

//    }
//}