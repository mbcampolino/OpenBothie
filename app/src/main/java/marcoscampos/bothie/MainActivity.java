package marcoscampos.bothie;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.wonderkiln.camerakit.CameraKit;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    CameraView cameraView1, cameraView2;
    ImageView btnChange;
    ImageView btnTake;
    CircleImageView btnGalery;
    boolean invertido = false;
    LinearLayout photocrop;
    ProgressBar pb;
    Bitmap result;
    Bitmap result2;

    String[] permissions = new String[]{
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkPermission()) {
            configureCameras();
        }
    }


    private boolean checkPermission() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                configureCameras();
            } else {
                finish();
            }

            return;
        }
    }

    private void configureCameras() {
        pb = (ProgressBar) findViewById(R.id.progress);
        cameraView1 = (CameraView) findViewById(R.id.cameratop);
        cameraView2 = (CameraView) findViewById(R.id.cameraback);
        btnChange = (ImageView) findViewById(R.id.btn_change);
        btnTake = (ImageView) findViewById(R.id.btn_take_photo);
        btnGalery = (CircleImageView) findViewById(R.id.btn_galery);
        photocrop = (LinearLayout) findViewById(R.id.photocrop);
        btnChange.setOnClickListener(this);
        btnTake.setOnClickListener(this);
        btnGalery.setOnClickListener(this);
        cameraView1.start();
        cameraView2.start();

        cameraView1.setZoom(CameraKit.Constants.ZOOM_PINCH);
        cameraView2.setZoom(CameraKit.Constants.ZOOM_PINCH);
        cameraView1.setPermissions(CameraKit.Constants.PERMISSIONS_PICTURE);
        cameraView2.setPermissions(CameraKit.Constants.PERMISSIONS_PICTURE);

        cameraView1.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {
            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                result =cameraKitImage.getBitmap();
                cameraView2.captureImage();
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        cameraView2.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {
            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                result2 = cameraKitImage.getBitmap();
                saveBitMap(MainActivity.this, combineImages(result, result2));
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pb.setVisibility(View.GONE);
                        btnTake.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });
    }

    public Bitmap combineImages(Bitmap c, Bitmap s) { // can add a 3rd parameter 'String loc' if you want to save the new image - left some code to do that at the bottom
        Bitmap cs = null;
        int h = c.getHeight() + s.getHeight();
        cs = Bitmap.createBitmap(c.getWidth(), h, Bitmap.Config.ARGB_8888);
        Canvas comboImage = new Canvas(cs);
        comboImage.drawBitmap(c, 0f, 0f, null);
        comboImage.drawBitmap(s, 0f, c.getHeight(), null);

        final Bitmap finalCs = cs;
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnGalery.setImageBitmap(finalCs);
            }
        });
        return cs;
    }

    private File saveBitMap(Context context, Bitmap bitmap) {
        File pictureFileDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
        if (!pictureFileDir.exists()) {
            boolean isDirectoryCreated = pictureFileDir.mkdirs();
            if (!isDirectoryCreated)
                Log.i("ATG", "Can't create directory to save the image");
            return null;
        }
        String filename = pictureFileDir.getPath() + File.separator + System.currentTimeMillis() + ".jpg";
        File pictureFile = new File(filename);

        try {
            pictureFile.createNewFile();
            FileOutputStream oStream = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, oStream);
            oStream.flush();
            oStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("TAG", "There was an issue saving the image.");
        }
        scanGallery(context, pictureFile.getAbsolutePath());
        return pictureFile;
    }

    // used for scanning gallery
    private void scanGallery(Context cntx, String path) {
        try {
            MediaScannerConnection.scanFile(cntx, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (cameraView1 != null && cameraView2 != null) {
            cameraView1.stop();
            cameraView2.stop();
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        if (cameraView1 != null && cameraView2 != null) {
            cameraView1.start();
            cameraView2.start();
        }

        super.onResume();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_change) {
            invertido = !invertido;
            changeCameras();
        } else if (view.getId() == R.id.btn_take_photo) {
            cameraView1.captureImage();
            pb.setVisibility(View.VISIBLE);
            btnTake.setVisibility(View.GONE);
        } else if (view.getId() == R.id.btn_galery) {
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setType("image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void changeCameras() {
        if (invertido) {
            cameraView1.setFacing(CameraKit.Constants.FACING_BACK);
            cameraView2.setFacing(CameraKit.Constants.FACING_FRONT);
        } else {
            cameraView1.setFacing(CameraKit.Constants.FACING_FRONT);
            cameraView2.setFacing(CameraKit.Constants.FACING_BACK);
        }
    }
}
