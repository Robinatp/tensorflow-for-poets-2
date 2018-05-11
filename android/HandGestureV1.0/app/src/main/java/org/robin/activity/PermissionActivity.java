package org.robin.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import org.robin.playcamera.R;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by liangcheng on 2017/9/6.
 */

public class PermissionActivity extends Activity implements EasyPermissions.PermissionCallbacks{
    private static final int RC_PERM_CODE = 123;
    private  List<String> mRequestPermissions;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_permisson);

        checkPermission();
    }

    private void checkPermission() {
        mRequestPermissions = new ArrayList<>();
        if(!hasCameraPermission()){
            mRequestPermissions.add(Manifest.permission.CAMERA);
        }
        if(!hasReadxternalStoragePermission()){
            mRequestPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }


        if(mRequestPermissions.size() > 0){
            String[] arr = mRequestPermissions.toArray(new String[mRequestPermissions.size()]);
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.permission_request_content),
                    RC_PERM_CODE,  arr);
        }else{
            gotoCameraFunnyActivity();
        }
    }
    private void gotoCameraFunnyActivity(){
        startActivity(new Intent(PermissionActivity.this, CameraActivity.class));
        finish();
    }

    private boolean hasCameraPermission() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA);
    }

    private boolean hasReadxternalStoragePermission() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private boolean hasxternalStoragePermission() {
        return EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(perms.size() == mRequestPermissions.size()){
            gotoCameraFunnyActivity();
        }else{
            finish();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}
