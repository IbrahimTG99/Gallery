package com.example.gallery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    LinearLayout linearLayout;
    boolean Vault_open = false;

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
                Intent intent = new Intent(MainActivity.this, BackupImages.class);
                startActivity(intent);
//                Toast.makeText(getApplicationContext(), "Backup selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.restore_menu:
                Toast.makeText(getApplicationContext(), "Restore selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.log_menu:
                Toast.makeText(getApplicationContext(), "Login/Logout selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.hidden_menu:
                Toast.makeText(this, Vault_open+"", Toast.LENGTH_SHORT).show();
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

    void showImages(boolean ascending, ArrayList<Picture> pictures)
    {
        linearLayout = findViewById(R.id.gallery_ll);
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