package com.example.gallery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    boolean Vault_open = false;
    String Username = "";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.backup_menu:
                if (Username.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please login before backup", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Backup Data")
                            .setMessage("Do you want to backup all pictures?")
                            .setCancelable(false)
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    backupData();
                                }
                            })
                            .show();
                }
                return true;

            case R.id.restore_menu:
                if (Username.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please login before restoring", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Restore Data")
                            .setMessage("Do you want to restore backed up pictures?")
                            .setCancelable(false)
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    RestoreData();
                                    Toast.makeText(getApplicationContext(), "Restore Data", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .show();
                }
                return true;
            case R.id.log_menu:
                if (Username.equals("")) {
                    Intent i = new Intent(MainActivity.this, Login.class);
                    startActivityForResult(i, 999);
                } else {
                    Toast.makeText(getApplicationContext(), "You are already logged in", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.hidden_menu:
                if (!Vault_open)
                {
                    Vault_open = true;
                    showImages(false, getHiddenImages());
                }
                else
                {
                    Vault_open = false;
                    showImages(false, getAllImagesByFolder());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE},
                    101);
        }

        showImages(false, getAllImagesByFolder());

        ImageButton sort_btn = findViewById(R.id.sort_btn);
        sort_btn.setOnClickListener(new View.OnClickListener() {

            boolean ascending = false;

            @Override
            public void onClick(View v){
                ascending = !ascending;
                showImages(ascending, getAllImagesByFolder());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Vault_open = false;
        showImages(false, getAllImagesByFolder());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 999) {
            if (resultCode == RESULT_OK) {
                Username = data.getStringExtra("mail");
            }
        }
    }

    void showImages(boolean ascending, ArrayList<Picture> pictures)
    {
        LinearLayout linearLayout = findViewById(R.id.gallery_ll);
        linearLayout.removeAllViews();

//        ArrayList<Picture> pictures = getAllImagesByFolder();

        if (ascending)
        {
            Collections.reverse(pictures);
        }

        String date = "";

        int i = 0;
        int sameRow = 0;

        while (i < pictures.size())
        {
            TableRow tr;
            if (!date.equals(pictures.get(i).getDate()))
            {
                tr = new TableRow(getApplicationContext());
                tr.setPadding(0, 15, 0, 15);
                TextView txt = new TextView(getApplicationContext());
                txt.setText(pictures.get(i).getDate());
                date = pictures.get(i).getDate();
                tr.addView(txt);
                linearLayout.addView(tr);
                txt.setTextColor(Color.parseColor("#49454F"));
            }

            tr = new TableRow(getApplicationContext());
            tr.setPadding(0, 15, 0, 15);

            while (i < pictures.size() && date.equals(pictures.get(i).getDate()) && sameRow != 3)
            {
                date = pictures.get(i).getDate();
                ImageButton img = new ImageButton(getApplicationContext());
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(pictures.get(i).getPath(), bmOptions);
                bitmap = Bitmap.createScaledBitmap(bitmap, 300, 400, true);
                img.setImageBitmap(bitmap);
                img.setBackgroundColor(Color.TRANSPARENT);

                int space = (int)(Resources.getSystem().getDisplayMetrics().widthPixels / 3.2f);
                tr.addView(img, space, space);

                final String f_path = pictures.get(i).getPath();
                final String f_name = pictures.get(i).getName();
                final boolean f_hidden = Vault_open;
                img.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(MainActivity.this, viewImage.class);
                        i.putExtra("path", f_path);
                        i.putExtra("name", f_name);
                        i.putExtra("hidden", f_hidden);
                        startActivity(i);
                    }
                });

                i++;
                sameRow++;
            }
            linearLayout.addView(tr);
            sameRow = 0;
        }
    }


    public ArrayList<Picture> getAllImagesByFolder()
    {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + '/';

        ArrayList<Picture> images = new ArrayList<>();
        Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN };
        Cursor cursor = this.getContentResolver().query(uri, projection, MediaStore.Images.Media.DATA + " like ? ", new String[] {"%"+path+"%"}, null);

        try
        {
            cursor.moveToFirst();
            do
            {
                Picture pic = new Picture();

                pic.setName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)));
                pic.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)));
                pic.setSize(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)));
                pic.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)));

                images.add(pic);
            }
            while(cursor.moveToNext());

            cursor.close();
            ArrayList<Picture> reSelection = new ArrayList<>();

            for (int i = images.size()-1; i > -1; i--)
            {
                reSelection.add(images.get(i));
            }
            images = reSelection;

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return images;
    }

    public ArrayList<Picture> getHiddenImages()
    {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("hidden_images", Context.MODE_PRIVATE);

        File[] files = directory.listFiles();
        ArrayList<Picture> images = new ArrayList<>();

        for (File file : files)
        {
                Picture p = new Picture();
                p.name = file.getName();
                p.path = file.getPath();
                p.date = file.lastModified();
                p.size = file.length()+"";

                images.add(p);
        }
        return images;
    }

    private void backupData()
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> pics = new HashMap<>();
        ArrayList<Picture> pictures = getAllImagesByFolder();

        pics.put("mail", Username);
        pics.put("size", pictures.size());

        Toast.makeText(getApplicationContext(), "Backup in progress", Toast.LENGTH_LONG).show();

//        for (int i = 0; i < pictures.size(); i++)
        for(int i = 0;  i < 2; i++)
        {
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(pictures.get(0).getPath(), bmOptions);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] byteArray = baos.toByteArray();
            String imageB64 = Base64.encodeToString(byteArray, Base64.URL_SAFE);
            pics.put("pic"+i, imageB64);
        }
        String TAG = "*************";
        db.collection("pics")
                .add(pics)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        Toast.makeText(getApplicationContext(), "Backup successful", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                        Toast.makeText(getApplicationContext(), "Unable to backup data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void RestoreData()
    {

    }

    public class Picture {

        private String name;
        private String path;
        private String size;
        private String uri;
        private Long date;

        public Picture(){

        }

        public Picture(String pictureName, String picturePath, String pictureSize, String imageUri) {
            this.name = pictureName;
            this.path = picturePath;
            this.size = pictureSize;
            this.uri = imageUri;
        }


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Long getDateTime() {
            return date;
        }

        public String getDate() {
            Date d = new Date(this.date);
            SimpleDateFormat sd = new SimpleDateFormat("dd.MM.yyyy");
            return sd.format(d);
        }

        public void setDate(Long date) {
            this.date = date;
        }
    }

    /*------------firebase-------------------
    // views for button
    private Button btnSelect, btnUpload;

    // view for image view
    private ImageView imageView;

    // Uri indicates, where the image will be picked from
    private Uri filePath;

    // request code
    private final int PICK_IMAGE_REQUEST = 22;

    // instance for firebase storage and StorageReference
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar;
        actionBar = getSupportActionBar();
        ColorDrawable colorDrawable
                = new ColorDrawable(
                Color.parseColor("#0F9D58"));
        actionBar.setBackgroundDrawable(colorDrawable);

        // initialise views
        btnSelect = findViewById(R.id.btnChoose);
        btnUpload = findViewById(R.id.btnUpload);
        imageView = findViewById(R.id.imgView);

        // get the Firebase  storage reference
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // on pressing btnSelect SelectImage() is called
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SelectImage();
            }
        });

        // on pressing btnUpload uploadImage() is called
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                uploadImage();
            }
        });
    }

    // Select Image method
    private void SelectImage()
    {

        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(
                        intent,
                        "Select Image from here..."),
                PICK_IMAGE_REQUEST);
    }

    // Override onActivityResult method
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {

        super.onActivityResult(requestCode,
                resultCode,
                data);

        // checking request code and result code
        // if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK
        // then set image in the image view
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            // Get the Uri of data
            filePath = data.getData();
            try {

                // Setting image on image view using Bitmap
                Bitmap bitmap = MediaStore
                        .Images
                        .Media
                        .getBitmap(
                                getContentResolver(),
                                filePath);
                imageView.setImageBitmap(bitmap);
            }

            catch (IOException e) {
                // Log the exception
                e.printStackTrace();
            }
        }
    }

    // UploadImage method
    private void uploadImage()
    {
        if (filePath != null) {

            // Code for showing progressDialog while uploading
            ProgressDialog progressDialog
                    = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            // Defining the child of storageReference
            StorageReference ref
                    = storageReference
                    .child(
                            "images/"
                                    + UUID.randomUUID().toString());

            // adding listeners on upload
            // or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {

                                @Override
                                public void onSuccess(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {

                                    // Image uploaded successfully
                                    // Dismiss dialog
                                    progressDialog.dismiss();
                                    Toast
                                            .makeText(MainActivity.this,
                                                    "Image Uploaded!!",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {

                            // Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast
                                    .makeText(MainActivity.this,
                                            "Failed " + e.getMessage(),
                                            Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(
                                        UploadTask.TaskSnapshot taskSnapshot)
                                {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage(
                                            "Uploaded "
                                                    + (int)progress + "%");
                                }
                            });
        }
    }
    */
}