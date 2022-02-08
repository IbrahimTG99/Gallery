package com.example.gallery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class viewImage extends AppCompatActivity {

    String path;
    String name;
    boolean hidden;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.info_menu:
                CreateInfoDialogue();
                return true;

            case R.id.hide_menu:
                if (!hidden)
                {
                    MoveToHiddenFolder();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Picture Already Hidden", Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        Bundle b = getIntent().getExtras();
        path = "";
        if (b != null)
        {
            path = b.getString("path");
            name = b.getString("name");
            hidden = b.getBoolean("hidden");
        }

        ImageView picture_img = findViewById(R.id.picture_img);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
        picture_img.setImageBitmap(bitmap);

        Button delete_btn = findViewById(R.id.delete_btn);
        delete_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteImage();
            }
        });

        Button share_btn = findViewById(R.id.share_btn);
        share_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(path);
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, "Share Image"));
            }
        });
    }

    private void CreateInfoDialogue()
    {
        try
        {
            ExifInterface exif = new ExifInterface(path);
            StringBuilder builder = new StringBuilder();

            builder.append("Date & Time:\t\t\t" + getExifTag(exif,ExifInterface.TAG_DATETIME) + "\n");
            builder.append("Flash:\t\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_FLASH) + "\n");
            builder.append("Focal Length:\t\t\t" + getExifTag(exif,ExifInterface.TAG_FOCAL_LENGTH) + "\n");
            builder.append("GPS Datestamp:\t\t\t" + getExifTag(exif,ExifInterface.TAG_FLASH) + "\n");
            builder.append("GPS Latitude:\t\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_LATITUDE) + "\n");
            builder.append("GPS Latitude Ref:\t\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_LATITUDE_REF) + "\n");
            builder.append("GPS Longitude:\t\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_LONGITUDE) + "\n");
            builder.append("GPS Longitude Ref:\t\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_LONGITUDE_REF) + "\n");
            builder.append("GPS Processing Method:\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_PROCESSING_METHOD) + "\n");
            builder.append("GPS Timestamp:\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_GPS_TIMESTAMP) + "\n");
            builder.append("Image Length:\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_IMAGE_LENGTH) + "\n");
            builder.append("Image Width:\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_IMAGE_WIDTH) + "\n");
            builder.append("Camera Make:\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_MAKE) + "\n");
            builder.append("Camera Model:\t\t\t\t" + getExifTag(exif,ExifInterface.TAG_MODEL) + "\n");
            builder.append("Camera Orientation:\t\t\t" + getExifTag(exif,ExifInterface.TAG_ORIENTATION) + "\n");
            builder.append("Camera White Balance:\t\t\t" + getExifTag(exif,ExifInterface.TAG_WHITE_BALANCE) + "\n");

            new AlertDialog.Builder(this)
                    .setTitle("Information")
                    .setMessage(builder)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .show();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private String getExifTag(ExifInterface exif, String tag)
    {
        String attribute = exif.getAttribute(tag);
        return ((attribute != null && !attribute.equals("")) ? attribute : "[No Info]");
    }

    private void DeleteImage()
    {
        File file = new File(path);

        final String where = MediaStore.MediaColumns.DATA + "=?";
        final String[] selectionArgs = new String[]{
                file.getAbsolutePath()
        };
        final ContentResolver contentResolver = getContentResolver();
        final Uri filesUri = MediaStore.Files.getContentUri("external");
        contentResolver.delete(filesUri, where, selectionArgs);
        if (file.exists()) {
            contentResolver.delete(filesUri, where, selectionArgs);
            file.delete();
        }
        finish();
    }

    private void MoveToHiddenFolder()
    {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("hidden_images", Context.MODE_PRIVATE);
        File internal_path = new File(directory, name);

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(internal_path);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(path, bmOptions);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        DeleteImage();
    }

//    private void ReadFromHidden()
//    {
//        try
//        {
//            File f = new File(path, name);
//            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
//            ImageView img = (ImageView)findViewById(R.id.imgPicker);
//            img.setImageBitmap(b);
//        }
//        catch (FileNotFoundException e)
//        {
//            e.printStackTrace();
//        }
//    }
}