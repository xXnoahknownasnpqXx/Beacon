package com.example.newbeacon;

import static android.app.Activity.RESULT_OK;
import static android.webkit.WebStorage.getInstance;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.security.Key;
import java.util.HashMap;


public class ProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase database;
    private DatabaseReference reference;

    //storage
    private StorageReference storageReference;
    //path where images of user profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Imgs/";

    private ImageView avatarIv;
    private TextView nameTv, usernameTv, interestsTv;
    FloatingActionButton fab;

    //Progress dialog
    ProgressDialog pd;

    //Permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int Storage_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //arrays of permissions to be requested
    String cameraPermissions[];
    String storagePermissions[];

    //uri of picked image
    Uri image_uri;


    //for checking profile or cover photo
    String profileOrCoverPhoto;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Users");

        //init arrays of permissions
        //cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        cameraPermissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE};

        avatarIv = view.findViewById(R.id.avatarIv);
        nameTv = view.findViewById(R.id.nameTv);
        usernameTv = view.findViewById(R.id.usernameTv);
        interestsTv = view.findViewById(R.id.interestsTv);
        fab = view.findViewById(R.id.fab);

        //init progress dialog
        pd = new ProgressDialog(getActivity());


        Query query = reference.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // traverse through database until required account info is found
                for (DataSnapshot ds: snapshot.getChildren()){
                    // retrieve data
                    String name = "" + ds.child("name").getValue();
                    String username = "" + ds.child("username").getValue();
                    String image = "" + ds.child("image").getValue();
                    String interests = "" + ds.child("interests").getValue();

                    // set/upload data to user profile
                    nameTv.setText(name);
                    usernameTv.setText(username);
                    interestsTv.setText(interests);
                    try {
                        // if image is found then set, otherwise set a default image to avatarIv
                        Picasso.get().load(image).into(avatarIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img_grey).into(avatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditProfileDialog();
            }
        });

        return view;
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        // ActivityCompat.requestPermissions(getActivity(), storagePermissions, Storage_REQUEST_CODE);
        requestPermissions( storagePermissions, Storage_REQUEST_CODE);
    }


    private boolean checkCameraPermission(){

        boolean result = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);


        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        // ActivityCompat.requestPermissions(getActivity(), cameraPermissions, CAMERA_REQUEST_CODE);
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        //Options shown
        String options[] = {"Edit Profile Picture", "Edit Name", "Edit Interests"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Choose Action");
        //set irems to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle dialog item clicks
                if (i == 0){
                    //Edit Profile
                    pd.setMessage("Updating Profile Picture");
                    showImagePicDialog();
                } else if (i == 1) {
                    //Edit Name
                    pd.setMessage("Updating Name");
                    showNameInterestsUpdateDialog("name");
                } else if (i == 2) {
                    //Edit interests
                    pd.setMessage("Updating Interests");
                    showNameInterestsUpdateDialog("interests");
                }
            }

            private void showNameInterestsUpdateDialog(String key) {
                //custom dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Update " + key);
                //set layout of dialog
                LinearLayout linearLayout = new LinearLayout(getActivity());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setPadding(10, 10, 10 ,10);
                //add edit text
                EditText editText = new EditText(getActivity());
                editText.setHint("Enter " + key);
                linearLayout.addView(editText);
                builder.setView(linearLayout);

                //add buttons in dialog
                builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = editText.getText().toString().trim();
                        if (!TextUtils.isEmpty(value)){
                            pd.show();
                            HashMap<String, Object> result = new HashMap<>();
                            result.put(key, value);

                            reference.child(firebaseUser.getUid()).updateChildren(result)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Updated...", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "", Toast.LENGTH_SHORT).show();

                                        }
                                    });
                        }
                        else {
                            Toast.makeText(getActivity(), "Please Enter " + key + "", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                //add buttons to cancel
                builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                //create and show dialog
                builder.create().show();
            }








        });
        //create and show dialog
        builder.create().show();
    }

    private void showImagePicDialog() {
        //show dialog containing options camera and gallery to pick the image


        //Options shown
        String options[] = {"Camera", "Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //handle dialog item clicks
                if (i == 0){
                    //Camera clicked
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();
                    }
                } else if (i == 1) {
                    //Gallery Picked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else {
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }
    //first check firebase storage rules


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickFromCamera();
                    }else{
                        Toast.makeText(getActivity(), "Please enable camera & storage permission", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case Storage_REQUEST_CODE:{


                if (grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickFromGallery();
                    }else{
                        Toast.makeText(getActivity(), "Please enable storage permission", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }
        // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //this method wil be called after picking image form camera or gallery
        if (requestCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get uri of image
                image_uri = data.getData();

                uploadProfileCoverPhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image


                uploadProfileCoverPhoto(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverPhoto (Uri uri){
        //show progress
        pd.show();

        //path and name if image to be stored in firebase storage
        String filePathAndName = storagePath + "" + profileOrCoverPhoto + "" + firebaseUser.getUid();

        StorageReference storageReference2 = storageReference.child(filePathAndName);
        storageReference2.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image is uploaded to storage, now it's url and store in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();

                        //check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()){
                            //image uploaded
                            //add/update url in user's database
                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileOrCoverPhoto, downloadUri.toString());

                            reference.child(firebaseUser.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Image Updated...", Toast.LENGTH_SHORT).show();

                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            pd.dismiss();
                                            Toast.makeText(getActivity(), "Error Updating Image...", Toast.LENGTH_SHORT).show();

                                        }
                                    });
                        }
                        else{
                            //error
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void pickFromCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }
    private void checkUserStatus () {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){
        }
        else {
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }


//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
//        inflater.inflate(R.menu.menu_main, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }

        if (id == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}