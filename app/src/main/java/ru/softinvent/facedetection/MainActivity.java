package ru.softinvent.facedetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

public class MainActivity extends AppCompatActivity implements ErrorDialogFragment.OnDialogButtonClickListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private FaceDetector faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверяем наличие Google Play Services на устройстве
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        // Если есть какая-то проблема, пытаемся её разрешить
        if (googleApiAvailability.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            // библиотека на устройстве отсутствует, отключена, либо устарела
            ErrorDialogFragment.getInstance("Сервисы Google Play недоступны. Без них работа невозможна.")
                    .show(getSupportFragmentManager(), null);
            return;
        }

        // Инициализируем детектор
        faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .build();
        // Проверяем его готовность
        if(!faceDetector.isOperational()){
            // Надо подождать...
            ErrorDialogFragment.getInstance("Компоненты детектора лиц ещё не загружены. Попробуйте позже.")
                    .show(getSupportFragmentManager(), null);
            return;
        }

        Button btn = (Button) findViewById(R.id.takeShotButton);
        btn.setOnClickListener(onTakeShotClickListener);
    }

    /**
     * Обработчик кнопки "Выход" в диалоге, сообщающем об отсутствии Google Play Services.
     * Просто завершает активити.
     */
    @Override
    public void onExitClick() {
        finish();
    }

    private View.OnClickListener onTakeShotClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            showFaces(imageBitmap);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showFaces(Bitmap bitmap) {
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.RED);
        myRectPaint.setStyle(Paint.Style.STROKE);

        Bitmap tempBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(bitmap, 0, 0, null);

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);

        for(int i=0; i<faces.size(); i++) {
            Face thisFace = faces.valueAt(i);
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            float x2 = x1 + thisFace.getWidth();
            float y2 = y1 + thisFace.getHeight();
            tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
        }
        ImageView pictureView = (ImageView) findViewById(R.id.pictureView);
        pictureView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
    }
}
