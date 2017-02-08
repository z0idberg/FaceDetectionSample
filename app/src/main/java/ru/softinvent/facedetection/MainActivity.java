package ru.softinvent.facedetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ErrorDialogFragment.OnDialogButtonClickListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String STATE_CURRENT_PHOTO_PATH = "currentPhotoPath";
    private FaceDetector faceDetector;
    private String currentPhotoPath;

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

        // Если ранее был сохранён путьк файлу фото, восстанавливаем его
        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(STATE_CURRENT_PHOTO_PATH);
        }

        Button btn = (Button) findViewById(R.id.takeShotButton);
        btn.setOnClickListener(onTakeShotClickListener);
    }

    /**
     * При изменении конфигурации сохраняем путь к файлу фото для последующего восстановления.
     * @param outState    Состояние.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_PHOTO_PATH, currentPhotoPath);
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
            // Проверяем есть ли на устройстве приложение камеры
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                try {
                    File photoFile = createPhotoFile();
                    currentPhotoPath = photoFile.getAbsolutePath();
                    // Приложению камеры передаём URI файла, в который требуется сохранить снимок
                    // Начиная с Android 7.0 работает только URI контент-провайдера
                    Uri photoFileUri = FileProvider.getUriForFile(MainActivity.this, getString(R.string.file_provider_authority), photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoFileUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (IOException e) {
                    Log.e("FACE", "onTakeShotClickListener: файл фото не создан", e);
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ImageView pictureView = (ImageView) findViewById(R.id.pictureView);
            // Используем для загрузки изображения Picasso,
            // выделяем найденные лица в трансформации
            Picasso.with(this)
                    .load(new File(currentPhotoPath))
                    .centerInside()
                    .fit()
                    .transform(new Transformation() {
                        @Override
                        public Bitmap transform(Bitmap source) {
                            Paint myRectPaint = new Paint();
                            myRectPaint.setStrokeWidth(5);
                            myRectPaint.setColor(Color.RED);
                            myRectPaint.setStyle(Paint.Style.STROKE);

                            Bitmap tempBitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.RGB_565);
                            Canvas tempCanvas = new Canvas(tempBitmap);
                            tempCanvas.drawBitmap(source, 0, 0, null);

                            Frame frame = new Frame.Builder().setBitmap(source).build();
                            SparseArray<Face> faces = faceDetector.detect(frame);

                            for(int i=0; i<faces.size(); i++) {
                                Face thisFace = faces.valueAt(i);
                                float x1 = thisFace.getPosition().x;
                                float y1 = thisFace.getPosition().y;
                                float x2 = x1 + thisFace.getWidth();
                                float y2 = y1 + thisFace.getHeight();
                                tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                            }

                            source.recycle(); // исходный битмап обязательно нужно уничтожить
                            return tempBitmap;
                        }

                        @Override
                        public String key() {
                            return "face_detection";
                        }
                    })
                    .into(pictureView);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Создаёт файл с уникальным имененм.
     * @return Созданный файл.
     * @throws IOException В случае ошибок при создании файла.
     */
    private File createPhotoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}
