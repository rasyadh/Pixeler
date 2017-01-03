package rasyadhabdulaziz.pixeler;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Bitmap bitmap;
    private boolean fabStatus = false;
    private String filename;
    private float scale = 1f;

    private static final int SELECT_PICTURE = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private FloatingActionButton btnSelectImage;
    private AppCompatImageView imgView;
    private ImageView starterImage;
    private TextView starterText, imgName;
    private ScaleGestureDetector scaleGesture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find the views...
        btnSelectImage = (FloatingActionButton) findViewById(R.id.btnSelectImage);
        imgView = (AppCompatImageView) findViewById(R.id.imgView);
        starterImage = (ImageView) findViewById(R.id.starterImage);
        starterText = (TextView) findViewById(R.id.starterText);
        imgName = (TextView) findViewById(R.id.imgName);
        scaleGesture = new ScaleGestureDetector(this, new ScaleListener());

        // On Click Floating Button
        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fabStatus) {
                    processImage();
                    btnSelectImage.hide();
                } else if (!fabStatus) {
                    btnSelectImage.setRippleColor(Color.parseColor("#013F8A"));
                    openImageChooser();
                    btnSelectImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_photo_filter));
                    fabStatus = true;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear) {
            clearImageProcess();
        } else if (id == R.id.action_save) {
            if (bitmap != null) {
                verifyStoragePermissions(this);
                saveImage(bitmap);
            } else {
                Toast.makeText(this, "Open image first", Toast.LENGTH_SHORT).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    /* Choose an image from Gallery */
    private void openImageChooser() {
        starterImage.setVisibility(View.GONE);
        starterText.setVisibility(View.GONE);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    /* Intent Result and Set Image to ImageView*/
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();

            try {
                // Get Image from Uri
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                getNameImage(selectedImageUri);
                imgView.setImageBitmap(bitmap);
                logger();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                Toast.makeText(this, "Ooops it's a bug", Toast.LENGTH_SHORT).show();
                clearImageProcess();
            }
        }
    }

    /* Get Image Name from Uri */
    private void getNameImage(Uri uri) {
        filename = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (filename == null) {
            filename = uri.getPath();
            int cut = filename.lastIndexOf('/');
            if (cut != -1) {
                filename = filename.substring(cut + 1);
            }
        }

        imgName.setText("File Image : " + filename);
    }

    /* Log Value for Image*/
    private void logger() {
        Log.d("bitmap width : ", String.valueOf(bitmap.getWidth()));
        Log.d("bitmap height : ", String.valueOf(bitmap.getHeight()));
        Log.d("Bitmap Value :", String.valueOf(bitmap));
    }

    /* Process Bitmap Image to Array*/
    private void processImage() {
        int[][] srcColorValues, destColorValues;

        try {
            srcColorValues = new int[bitmap.getWidth()][bitmap.getHeight()];
            destColorValues = new int[bitmap.getWidth()][bitmap.getHeight()];

            int[] pixel = new int[bitmap.getWidth() * bitmap.getHeight()];
            int indeks = 0;
            bitmap.getPixels(pixel, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

            for (int i = 0; i < bitmap.getWidth(); i++) {
                for (int j = 0; j < bitmap.getHeight(); j++) {
                    srcColorValues[i][j] = pixel[indeks];
                    indeks++;
                }
            }

            for (int a = 0; a < bitmap.getWidth(); a++) {
                for (int b = 0; b < bitmap.getHeight(); b++) {
                    destColorValues[a][b] = srcColorValues[Math.abs(a / 10) * 10][Math.abs(b / 10) * 10];
                }
            }

            int[] pixelAfter = new int[destColorValues.length * destColorValues[0].length];
            int indeksAfter = 0;

            for (int x = 0; x < destColorValues.length; x++) {
                for (int y = 0; y < destColorValues[0].length; y++) {
                    pixelAfter[indeksAfter] = destColorValues[x][y];
                    indeksAfter++;
                }
            }
            bitmap = Bitmap.createBitmap(pixelAfter, destColorValues.length, destColorValues[0].length, Bitmap.Config.ARGB_8888);
            imgView.setImageBitmap(bitmap);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            Toast.makeText(this, "Ooops it's a bug,\nCannot process image", Toast.LENGTH_SHORT).show();
        }
    }

    /* Process Image (way number 2) */
    public void processImageWay2() {
        int[] imgValue, imgProcessValue;
        imgValue = new int[bitmap.getWidth() * bitmap.getHeight()];
        imgProcessValue = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(imgValue, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < imgValue.length; i++) {
            imgProcessValue[i] = imgValue[Math.abs(i / (imgValue.length / 20) * (imgValue.length / 20))];
        }

        bitmap = Bitmap.createBitmap(imgProcessValue, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        imgView.setImageBitmap(bitmap);
    }

    /* Save Image */
    private void saveImage(Bitmap imageBitmap) {
        File myDir = new File("/sdcard/Pictures/Pixeler");
        myDir.mkdirs();
        String fileName = filename.substring(0, filename.lastIndexOf(".")) + "-Pixeler" + ".jpg";

        File file = new File(myDir, fileName);
        if (file.exists())
            file.delete();

        try {
            FileOutputStream out = new FileOutputStream(file);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(this, "Image saved to : " + myDir, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Reset All Process*/
    private void clearImageProcess() {
        Toast.makeText(this, "Image cleared", Toast.LENGTH_SHORT).show();
        fabStatus = false;
        btnSelectImage.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_photo));
        btnSelectImage.show();
        imgName.setText("");
        imgView.setImageBitmap(null);
        bitmap = null;
        starterImage.setVisibility(View.VISIBLE);
        starterText.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        scaleGesture.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            scale *= scaleGestureDetector.getScaleFactor();
            imgView.setScaleX(scale);
            imgView.setScaleY(scale);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }


        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            scale = 1;
            imgView.setScaleX(scale);
            imgView.setScaleY(scale);
        }
    }

    private static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }
}