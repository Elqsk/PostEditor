package com.example.posteditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.request.RequestOptions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.ImageEngine;
import com.zhihu.matisse.engine.impl.GlideEngine;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    EditText contentView;

    SpannableStringBuilder stringBuilder;

    int width;
    int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        contentView = findViewById(R.id.main_edit_text);

        width = (int) getResources().getDisplayMetrics().xdpi;
        height = (int) getResources().getDisplayMetrics().ydpi;

        Log.d("kkang" , "Main Activity / onCreate(...) / int width: " + width);
        Log.d("kkang" , "Main Activity / onCreate(...) / int height: " + height);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_mission1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                Log.d("kkang" , "Main Activity / onOptionsItemSelected(...) / PERMISSION_GRANTED");

                // 포스팅할 이미지를 원하는 개수 만큼 선택한다.
                Matisse.from(this)
                        .choose(MimeType.of(MimeType.JPEG))
                        .countable(true)
                        .maxSelectable(9)
                        .spanCount(3)
                        .imageEngine(new MyGlideEngine())
                        .forResult(100);
                // Matisse.imageEngine(...)에 new GlideEngine()을 넣으면 에러가 난다. ImageEngine을
                // 상속 받은 새 이미지 엔진을 별도로 만들어서 사용했다.

                Log.d("kkang" , "Main Activity / onOptionsItemSelected(...) / Matisse");
            } else {
                ActivityCompat.requestPermissions(this, new String[] {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 100);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {
            Log.d("kkang" , "Main Activity / onActivityResult(...) / RESULT_OK");

            // 선택한 이미지들을 차레로 로드하기 위해 리스트에 담는다.
            List<Uri> imageUris = Matisse.obtainResult(data);
            // 포스트 내용의 본문은 전부 텍스트로만 되어 있다. 정확히는 일반 텍스트와 이미지 URI이 길게
            // 이어 붙여져 있다. ImageSpan을 이용해서 특정 텍스트를 이미지로 바꿔서 보여주기 때문에
            // 사용자의 눈에는 이미지와 텍스트가 혼합되어 있는 것 처럼 보인다. 지금은(예제에서는) 저장
            // 기능을 제공하지 않기 때문에 여기서의 URI는 "이 자리는 이미지가 출력되는 곳이다"라고 표시하는
            // 역할만 한다.
            for (Uri uri : imageUris) {
                new ImageLoaderThread(uri).start();
            }
        }
    }

    class ImageLoaderThread extends Thread {

        Uri uri;

        public ImageLoaderThread(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void run() {
            super.run();

            Bitmap bitmap = null;
            try {
                // asBitmap()을 load(...) 보다 앞에 설정해야 에러가 안 난다.
                bitmap = Glide.with(MainActivity.this)
                        .asBitmap()
                        .load(uri)
                        .override(width, height)
                        .into(width, height)
                        .get();
            } catch (ExecutionException e) {
                e.printStackTrace();
                Log.e("kkang", "Main Activity / DownloadThread / ExecutionException e: " + e);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e("kkang", "Main Activity / DownloadThread / InterruptedException e: " + e);
            }
            Message message = new Message();
            message.what = 10;

            Bundle bundle = new Bundle();
            bundle.putParcelable("bitmap", bitmap);
            bundle.putParcelable("uri", uri);

            message.setData(bundle);

            mHandler.sendMessage(message);
        }
    }

    Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            if (msg.what == 10) {
                Uri uri = msg.getData().getParcelable("uri");
                Bitmap bitmap = msg.getData().getParcelable("bitmap");

                Log.d("kkang" , "Main Activity / handler / Uri uri: " + uri);
                Log.d("kkang" , "Main Activity / handler / Bitmap bitmap: " + bitmap);

                // 포스트 내용의 본문은 전부 텍스트로만 되어 있다. 정확히는 일반 텍스트와 이미지 URI이 길게
                // 이어 붙여져 있다. ImageSpan을 이용해서 텍스트를 이미지로 바꿔서 보여주기 때문에
                // 사용자의 눈에는 이미지와 텍스트가 혼합되어 있는 것 처럼 보인다.
                stringBuilder = new SpannableStringBuilder(contentView.getText());
                // 로드할 이미지(URI)를 기존의 텍스트에 이어 붙인다.
                stringBuilder.insert(contentView.getSelectionStart(), "\n [[" + uri + "]] \n");

                Log.d("kkang", "Main Activity / handler / stringBuilder.toString(): " + stringBuilder.toString());

                // 텍스트의 어디 부터 어디 까지를 이미지로 바꿀지 결정한다.
                int start = stringBuilder.toString().indexOf("[[" + uri + "]]");
                int end = start + new String("[[" + uri + "]]").length();
                // 출력할 이미지(비트맵)을 준비한다.
                ImageSpan imageSpan =
                        new ImageSpan(MainActivity.this, bitmap, ImageSpan.ALIGN_BASELINE);
                // 텍스트를 이미지로 바꾼다.
                stringBuilder.setSpan(imageSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                // 일반 텍스트와 이미지로 변환된 텍스트가 함께 보여진다.
                contentView.setText(stringBuilder);

                // 커서를 이미지의 뒤에 가져다 놓는다.
                contentView.setSelection(end + 1);
                contentView.requestFocus();

                InputMethodManager imm =
                        (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                contentView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        imm.showSoftInput(contentView, 0);
                    }
                }, 30);
            }
        }
    };

    // Matisse.imageEngine(...)에 new GlideEngine()을 하면 에러가 난다. ImageEngine을 상속 받은 새
    // 이미지 엔진을 별도로 만들어서 사용했다.
    public class MyGlideEngine implements ImageEngine {
        @Override
        public void loadThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri) {
            RequestOptions requestOptions = new RequestOptions()
                    .override(resize, resize)
                    .centerCrop()
                    .placeholder(placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .apply(requestOptions)
                    .into(imageView);
        }

        @Override
        public void loadGifThumbnail(Context context, int resize, Drawable placeholder, ImageView imageView, Uri uri) {
            RequestOptions requestOptions = new RequestOptions()
                    .override(resize, resize)
                    .centerCrop()
                    .placeholder(placeholder);
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .apply(requestOptions)
                    .into(imageView);
        }

        @Override
        public void loadImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
            RequestOptions requestOptions = new RequestOptions()
                    .override(resizeX, resizeY)
                    .priority(Priority.HIGH)
                    .centerCrop();
            Glide.with(context)
                    .load(uri)
                    .apply(requestOptions)
                    .into(imageView);
        }

        @Override
        public void loadGifImage(Context context, int resizeX, int resizeY, ImageView imageView, Uri uri) {
            RequestOptions requestOptions = new RequestOptions()
                    .override(resizeX, resizeY)
                    .priority(Priority.HIGH)
                    .centerCrop();
            Glide.with(context)
                    .asGif()
                    .load(uri)
                    .apply(requestOptions)
                    .into(imageView);
        }

        @Override
        public boolean supportAnimatedGif() {
            return false;
        }
    }
}