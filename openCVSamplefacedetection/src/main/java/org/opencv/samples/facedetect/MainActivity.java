package org.opencv.samples.facedetect;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * 主界面
 * Created by Luke on 2017/8/21.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int OPEN_CAMERA = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button register = (Button) findViewById(R.id.register);
        Button verify = (Button) findViewById(R.id.verify);
        Button viewData = (Button) findViewById(R.id.view_data);

        register.setOnClickListener(this);
        verify.setOnClickListener(this);
        viewData.setOnClickListener(this);

        initDatabase();
    }

    // 初始化数据库
    private void initDatabase() {
        DatabaseHelper helper = new DatabaseHelper(this);
        if (helper.query().size() == 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.face_default);
            String path = helper.saveBitmapToLocal(bitmap);
            UserInfo user = new UserInfo("默认用户", "男", 18, path);
            helper.insert(user);
        }
        helper.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register:
                PermissionHelper.with(this)
                        .requestPermission(Manifest.permission.CAMERA)
                        .requestCode(OPEN_CAMERA)
                        .setListener(new PermissionHelper.RequestListener() {
                            @Override
                            public void onGranted() {
                                Intent intent = new Intent(MainActivity.this,
                                        FdActivity.class);
                                intent.putExtra("flag", FdActivity.FLAG_REGISTER);
                                startActivityForResult(intent,
                                        FdActivity.FLAG_REGISTER);
                            }

                            @Override
                            public void onDenied() {
                                Toast.makeText(MainActivity.this, "权限拒绝",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .request();
                break;
            case R.id.verify:
                PermissionHelper.with(this)
                        .requestPermission(Manifest.permission.CAMERA)
                        .requestCode(OPEN_CAMERA)
                        .setListener(new PermissionHelper.RequestListener() {
                            @Override
                            public void onGranted() {
                                Intent intent = new Intent(MainActivity.this,
                                        FdActivity.class);
                                intent.putExtra("flag", FdActivity.FLAG_VERIFY);
                                startActivityForResult(intent,
                                        FdActivity.FLAG_VERIFY);
                            }

                            @Override
                            public void onDenied() {
                                Toast.makeText(MainActivity.this, "权限拒绝",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .request();
                break;
            case R.id.view_data:
                Intent intent = new Intent(this, ViewDataActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FdActivity.FLAG_REGISTER:
                if (resultCode == RESULT_OK)
                    Toast.makeText(this, "已注册过", Toast.LENGTH_LONG).show();
                break;
            case FdActivity.FLAG_VERIFY:
                if (resultCode == RESULT_OK) {
                    int index = data.getIntExtra("USER_ID", -1);
                    DatabaseHelper helper = new DatabaseHelper(this);
                    UserInfo user = helper.query().get(index);
                    helper.close();
                    Toast.makeText(this, "验证通过", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "UserInfo: " + user.toString());
                } else {
                    Toast.makeText(this, "验证失败", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        PermissionHelper.requestPermissionResult(requestCode, grantResults);
    }
}
