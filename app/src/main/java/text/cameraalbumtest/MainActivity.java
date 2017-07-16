package text.cameraalbumtest;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKE_PHOTO = 1;
    private static final int CHOOSE_PHOTO=2;
    private Button mBtnTakePhoto,mBtnChoosephoto;
    private ImageView mImvPicture;
    private Uri mImvUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnTakePhoto= (Button) findViewById(R.id.take_photo);
        mBtnChoosephoto= (Button) findViewById(R.id.choose_photo);
        mImvPicture= (ImageView) findViewById(R.id.show_photo);
        mBtnTakePhoto.setOnClickListener(this);
        mBtnChoosephoto.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.take_photo:
                File outputImage=new File(getExternalCacheDir(),"output.jpg");
                try{
                    if (outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }if (Build.VERSION.SDK_INT>=24){
                mImvUri= FileProvider.getUriForFile(MainActivity.this,"text.cameraalbumtest.fileprovider",outputImage);
                }else{
                mImvUri=Uri.fromFile(outputImage);
                }
                Intent intent1=new Intent("android.media.action.IMAGE_CAPTURE");
                intent1.putExtra(MediaStore.EXTRA_OUTPUT,mImvUri);
                startActivityForResult(intent1,TAKE_PHOTO);
                break;
            case R.id.choose_photo:
                Intent intent2=new Intent("android.intent.action.GET_CONTENT");
                intent2.setType("image/*");
                startActivityForResult(intent2,CHOOSE_PHOTO);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Log.d("TAG", "onActivityResult: 显示照片");
                        Bitmap bitmap= BitmapFactory.decodeStream(getContentResolver().openInputStream(mImvUri));
                        mImvPicture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }else
                    Log.d("TAG", "onActivityResult:显示失败 ");
                break;
            case CHOOSE_PHOTO:
                if (resultCode==RESULT_OK){
                    if (Build.VERSION.SDK_INT>=19){
                        //4.4是以上系统使用这个
                        handleImageOnkiKat(data);
                    }else {
                        handleImageBeforeOnkiKat(data);
                    }
                }
        }
    }

    private void handleImageBeforeOnkiKat(Intent data) {
        Uri uri=data.getData();
        String imagePath=getImagePath(uri,null);
        displayImage(imagePath);
    }

    private void displayImage(String imagePath) {
        if (imagePath!=null){
            Bitmap bitmap=BitmapFactory.decodeFile(imagePath);
            mImvPicture.setImageBitmap(bitmap);
        }else {
            Toast.makeText(this,"打开图片失败",Toast.LENGTH_LONG).show();
        }
    }

    private String getImagePath(Uri uri, String selection) {
        String path=null;
        //通过Uri和selection来获取真实图片路径
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if (cursor!=null){
            if (cursor.moveToNext()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    @TargetApi(19)
    private void handleImageOnkiKat(Intent data) {
        String imagepath=null;
        Uri uri=data.getData();
        if (DocumentsContract.isDocumentUri(this,uri)){
            String docId=DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id=docId.split(":")[1];
                String selection=MediaStore.Images.Media._ID+"="+id;
                imagepath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contenuri= ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagepath=getImagePath(contenuri,null);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())){
            //如果是content类型的Uri,则通过普通方式
            imagepath=getImagePath(uri,null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())){
            //如果File类型的Uri，直接获取图片路径
            imagepath=uri.getPath();
        }
        displayImage(imagepath);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length<0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"加权限",Toast.LENGTH_LONG).show();
            }
            break;
        }
    }
}
