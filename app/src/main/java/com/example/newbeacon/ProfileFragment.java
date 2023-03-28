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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.security.Key;
import java.util.HashMap;


public class ProfileFragment extends Fragment {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase database;
    private DatabaseReference reference;

    //storage
    private StorageReference storageReference;
    //path where images of user profile will be stored
    String storagePath = "Users_Profile_Imgs/";

    private ImageView avatarIv;
    private TextView nameTv, usernameTv, interestsTv;
    FloatingActionButton fab;

    //Progress dialog
    ProgressDialog pd;

    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;
    //arrays of permissions to be requested
    private static final String TAG = "PERMISSION_TAG";

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
        storageReference = FirebaseStorage.getInstance().getReference();

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
                    profileOrCoverPhoto = "image";
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
        });
        //create and show dialog
        builder.create().show();
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
                                    Toast.makeText(getActivity(), "Updated!", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                    Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                                }
                            });
                }
                else {
                    Toast.makeText(getActivity(), "Please Enter " + key + "", Toast.LENGTH_SHORT).show();
                }

            }
        });
        //add buttons to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private ActivityResultLauncher<String> permissionLauncherSingle = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG,"onActivityResult: isGranted: "+isGranted);

                    if (isGranted){
                        pickFromCamera();
                    }
                    else {
                        Log.d(TAG,"onActivityResult: Permission denied...");
                        //Toast.makeText(ProfileFragment.this,"permission denied...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private ActivityResultLauncher<String> permissionLauncherSingle2 = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG,"onActivityResult: isGranted: "+isGranted);

                    if (isGranted){
                        pickFromGallery();
                    }
                    else {
                        Log.d(TAG,"onActivityResult: Permission denied...");
                    }
                }
            }
    );

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

                    String permission = Manifest.permission.CAMERA;
                    permissionLauncherSingle.launch(permission);
                } else if (i == 1) {

                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_GALLERY_CODE);
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //this method wil be called after picking image form camera or gallery
        if (resultCode == RESULT_OK && data != null && data.getData() != null){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get uri of image
                image_uri = data.getData(); //TODO review the last video to see if it can be reused

                Picasso.get().load(image_uri).into(avatarIv);
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
        String filePathAndName = storagePath + "" + profileOrCoverPhoto + "_" + firebaseUser.getUid();

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
                                            Toast.makeText(getActivity(), "Image Updated!", Toast.LENGTH_SHORT).show();

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


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        else if (id == R.id.action_add_post){
            startActivity(new Intent(getActivity(), AddPostActivity.class));
        }
        else if (id==R.id.action_settings) {
            //goes to settings activity
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}