package com.example.newbeacon;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    ActionBar actionBar;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;


    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //image pick constants
    private static final int  IMAGE_PICK_CAMERA_CODE= 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    String[] cameraPermissions;
    String[] storagePermissions;

    private static final String TAG = "PERMISSION_TAG";

    EditText titleEt, descriptionEt, genreEt;
    ImageView imageIv;
    TextView locationTv;
    Spinner locationMenu;
    private String[] locations;
    ArrayAdapter<String> adapter;
    Button uploadBtn;

    //user info
    String name,email, uid, dp;

    //image picked will be saved to this uri
    Uri image_rui = null;

    //progress bar
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // initiate permissions arrays
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        actionBar.setSubtitle(email);

        //get some info of current user to include in post
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    name = ""+ds.child("name").getValue();
                    email = ""+ds.child("email").getValue();
                    dp = ""+ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        titleEt = findViewById(R.id.pTitleEt);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        imageIv = findViewById(R.id.pImageIv);
        uploadBtn = findViewById(R.id.pUploadBtn);
        locationTv = findViewById(R.id.locationTv);
        locationMenu = findViewById(R.id.locationMenu);
        genreEt = findViewById(R.id.genreTv);

        locations = new String[]{"Rochester Hills", "Troy", "Lake Orion", "Pontiac"};
        adapter = new ArrayAdapter<>(AddPostActivity.this, android.R.layout.simple_spinner_dropdown_item, locations);
        locationMenu.setAdapter(adapter);


        locationMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                TextView textView = (TextView) view;
                int width = (int) textView.getPaint().measureText(selectedOption) + textView.getPaddingLeft() + textView.getPaddingRight();
                locationMenu.setLayoutParams(new LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });


        imageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickDialog();
            }
        });

        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleEt.getText().toString().trim();
                String description = descriptionEt.getText().toString().trim();
                String genre = genreEt.getText().toString().trim();
                String location = locationMenu.getSelectedItem().toString().trim();
                if(TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this, "Enter Title", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this, "Enter Description", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(genre)){
                    Toast.makeText(AddPostActivity.this, "Enter Genre", Toast.LENGTH_SHORT).show();
                    return;
                }


                if(image_rui == null){
                    //post without image
                    uploadData(title, description, location, genre);
                    Intent intent = new Intent(AddPostActivity.this, DashboardActivity.class);
                    startActivity(intent);
                }
                else{
                    //post with image
                    uploadData(title, description, location, genre);
                    Intent intent = new Intent(AddPostActivity.this, DashboardActivity.class);
                    startActivity(intent);
                }
            }
        });
    }

    private void uploadData(final String title, final String description, String location, String genre) {
        pd.setMessage("Publishing Post");
        pd.show();

        //for post-image name, post-id, post-publish-time
        final String timeStamp = String.valueOf(System.currentTimeMillis());

        String filePathandName = "Posts/" + "post_" + timeStamp;

        if ((imageIv.getDrawable() != null)){
            Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();


            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathandName);
            ref.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image is uploaded to firebase storage, now get its url
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());

                            String downloadUri = uriTask.getResult().toString();

                            if(uriTask.isSuccessful()) {

                                //url is received upload post to firebase database

                                HashMap<Object, String> hashMap = new HashMap<>();
                                //put post info
                                hashMap.put("uid", uid);
                                hashMap.put("uName", name);
                                hashMap.put("uEmail", email);
                                hashMap.put("uDp", dp);
                                hashMap.put("pId", timeStamp);
                                hashMap.put("pTitle", title);
                                hashMap.put("pDescr", description);
                                hashMap.put("pImage", downloadUri);
                                hashMap.put("pTime", timeStamp);
                                hashMap.put("pLikes", "0");
                                hashMap.put("pLocation", location);
                                hashMap.put("pGenre", genre);



                                //path to store post data
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                //put data in this ref
                                ref.child(timeStamp).setValue(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                //added in database
                                                pd.dismiss();
                                                Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
//                                                titleEt.setText("");
//                                                descriptionEt.setText("");
//                                                imageIv.setImageURI(null);
//                                                image_rui = null;


                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //failed adding post in database
                                                pd.dismiss();
                                                Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                                //reset views
//                                                titleEt.setText("");
//                                                descriptionEt.setText("");
//                                                imageIv.setImageURI(null);
//                                                image_rui = null;

                                                //send notification
//                                                prepareNotification(
//                                                        ""+timeStamp,
//                                                        ""+ name + " added a new post",
//                                                        ""+title+ "\n"+description,
//                                                        "PostNotification",
//                                                        "POST");
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed uploading image
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else {
            //post without image

            HashMap<Object, String> hashMap = new HashMap<>();
            //put post info
            hashMap.put("uid", uid);
            hashMap.put("uName", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescr", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);
            hashMap.put("pLikes", "0");
            hashMap.put("pLocation", location);
            hashMap.put("pGenre", genre);

            //path to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            //put data in this ref
            ref.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //added in database
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this, "Post Published", Toast.LENGTH_SHORT).show();
                            titleEt.setText("");
                            descriptionEt.setText("");
                            imageIv.setImageURI(null);
                            image_rui = null;

//                            prepareNotification(
//                                    ""+timeStamp,
//                                    ""+ name + " added a new post",
//                                    ""+title+ "\n"+description,
//                                    "PostNotification",
//                                    "POST");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed adding post in database
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    //call this whenever you publish a post
//    private void prepareNotification(String pID, String title, String description, String notificationType, String notificationTopic){
//        //prepare data for notification
//
//        String NOTIFICATION_TOPIC = "/topics/" + notificationTopic; //Topic must match with what the receiver subscribed to
//        String NOTIFICATION_TITLE = title; //e.g. Noah Quick added new post
//        String NOTIFICATION_MESSAGE = description; //content of post
//        String NOTIFICATION_TYPE = notificationType; //now there are two notifications types chat and post, so to differentiate in firebasemessageing.java class
//
//        //prepare json what to send, and where to sent
//        JSONObject notificationJo = new JSONObject();
//        JSONObject notificationBodyJo = new JSONObject();
//
//        try {
//            //waht to send
//            notificationBodyJo.put("notificationType", NOTIFICATION_TYPE);
//            notificationBodyJo.put("sender", uid);//uid of current use/sender
//            notificationBodyJo.put("pId", pID);//post id
//            notificationBodyJo.put("pTitle", NOTIFICATION_TITLE);
//            notificationBodyJo.put("pDescription", NOTIFICATION_MESSAGE);
//            //where to send
//            notificationJo.put("to", NOTIFICATION_TOPIC);
//
//            notificationJo.put("data", notificationBodyJo);//combine data to be sent
//        } catch (JSONException e) {
//            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//
//        sendPostNotification(notificationJo);
//
//    }

//    private void sendPostNotification(JSONObject notificationJo) {
//        //send volley object request
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", notificationJo,
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        Log.d("FCM_RESPONSE", "onResponse: " + response.toString());
//                    }
//                },
//                new Response.ErrorListener() {
//                    @Override
//                    public void onErrorResponse(VolleyError error) {
//                        //error occurs
//                        Toast.makeText(AddPostActivity.this, ""+ error.toString(), Toast.LENGTH_SHORT).show();
//                    }
//        }){
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                //put required headers
//                Map<String, String> headers = new HashMap<>();
//                headers.put("Content-Type", "application/json");
//                headers.put("Authorization", "key= AAAAfCAVQ2s:APA91bHfuYonjazZtYd_IAdqwBLaQpWAUQnAzUi8v1iCu7iyp0f2Ggs253CgOoZMtltMXx-f8VYzolK-fjolGmRc5rLHp17wX5rboRtYmO9C22NqbZav1m5q_SB1EjlmqXbF4l4m5HA8");
//
//                return headers;
//            }
//        };
//
//        //enqueue the volley request
//        Volley.newRequestQueue(this).add(jsonObjectRequest);
//    }
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
    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image from");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // item click handle
                if (which == 0) {

                    String permission = Manifest.permission.CAMERA;
                    permissionLauncherSingle.launch(permission);
                }
                if (which == 1) {
                    // gallery closed
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICK_GALLERY_CODE);
                }
            }
        });
        // create and show dialog
        builder.create().show();
    }

    private void pickFromCamera() {
        //intent to pick image from camera
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Descr");
        image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);


        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_rui);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            email = user.getEmail();
            uid = user.getUid();
        }else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);


        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //this method will be called after picking image from camera or gallery
        if(resultCode == RESULT_OK){

            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get uri of image
                image_rui = data.getData(); //TODO review the last video to see if it can be reused

                Picasso.get().load(image_rui).into(imageIv);

            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image

                imageIv.setImageURI(image_rui);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}