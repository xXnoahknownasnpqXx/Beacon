package com.example.newbeacon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

// import org.w3c.dom.Comment;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {


    //to get detail of user and post
    String hisUid, myUid, myEmail, myName, myDp, postId, pLikes, hisDp, hisName, pImage;

    // boolean mProcessComment = false;
    boolean mProcessLike = false;

    //progress bar
    ProgressDialog pd;


    //views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTiv, pTitleTv, pDescriptionTv, pLikesTv; // pCommentsTv
    ImageButton moreBtn;
    Button likeBtn; // shareBtn
    LinearLayout profileLayout;
    // RecyclerView recyclerView; TODO see if this view matters

    //add comments views
    //EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);


        //Actionbar and its purpose
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);


        //get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");


        //init view
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTiv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
//        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
//        shareBtn = findViewById(R.id.shareBtn);

        //profileLayout = findViewById(R.id.profileLayout);

//        commentEt = findViewById(R.id.commentEt);
//        sendBtn =findViewById(R.id.sendBtn);
//        cAvatarIv = findViewById(R.id.cAvatarIv);
        loadPostInfo();
        
        checkUserStatus();
        
        loadUserInfo();

        //set subtitle of actionbar
        actionBar.setSubtitle("SignedIn as: "+myEmail);

        //send comment button click
//        sendBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//
//
//        });

        //like button click handle
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likePost();
            }
        });
    }

    private void addToHisNotifications(String hisUid, String pId, String notification){
        String timestamp = "" + System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //added successfully
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                    }
                });

    }

    private void showMoreOptions() {
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        if (hisUid.equals(myUid)) {
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == 0){
                    beginDelete();
                }
                return false;
            }
        });
        // show pop-up menu
        popupMenu.show();
    }

    private void beginDelete() {
        if (pImage.equals("no Image")) {
            deleteWithoutImage();
        } else {
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()) {
                                    ds.getRef().removeValue();
                                }
                                //deleted
                                Toast.makeText(PostDetailActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(PostDetailActivity.this, ""+ e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteWithoutImage () {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting...");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ds.getRef().removeValue();
                }
                //deleted
                Toast.makeText(PostDetailActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)) {
                    likeBtn.setText("Liked");
                } else {
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void likePost() {
        mProcessLike = true;

        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessLike){
                    if(snapshot.child(postId).hasChild(myUid)) {
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) - 1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    } else {
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes) + 1));
                        likesRef.child(postId).child(myUid).setValue("Liked"); // set any value
                        mProcessLike = false;

                        addToHisNotifications("" + hisUid, "" + postId, "Liked your post");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

//    private void postComment() {
//        pd = new ProgressDialog(this);
//        pd.setMessage("Adding comment...");
//
//        //get data from comment edit
//        String comment = commentEt.getText().toString().trim();
//        //validate
//        if(TextUtils.isEmpty(comment)){
//            //no value is entered
//            Toast.makeText(this, "Comment is empty...", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//
//        String timeStamp = String.valueOf(System.currentTimeMillis());
//
//        //each post will have a child "Comments" that will contain comments of that post
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
//
//        HashMap<String, Object> hashMap = new HashMap<>();
//        //put info in hashmap
//        hashMap.put("cId",timeStamp);
//        hashMap.put("comment",comment);
//        hashMap.put("uid",myUid);
//        hashMap.put("uEmail",myEmail);
//        hashMap.put("uDp",myDp);
//        hashMap.put("uName",myName);
//
//        //put this data in db
//        ref.child(timeStamp).setValue(hashMap)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                    //added
//                        pd.dismiss();
//                        Toast.makeText(PostDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
//                        commentEt.setText("");
//                        updateCommentCount();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                    //failed, not added
//                    pd.dismiss();
//                        Toast.makeText(PostDetailActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//
//    }


//    private void updateCommentCount() {
//        //whenever user adds comment increase the comment count as we did for like count
//        mProcessComment = true;
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if(mProcessComment){
//                    String comments =""+ snapshot.child("pComments").getValue();
//                    int newCommentVal = Integer.parseInt(comments) + 1;
//                    ref.child("pComments").setValue(""+newCommentVal);
//                    mProcessComment = false;
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });
//    }

    private void loadUserInfo() {
        //get current user info
        Query myRef= FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    myName = ""+ds.child("name").getValue();
                    myDp = ""+ds.child("image").getValue();

                    //set data
                    try {
                    //if image is received then set
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv);
                    }
                    catch (Exception e){
                        Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPostInfo() {
        //get post using the id of the post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //keep checking the post until get the required post
                for (DataSnapshot ds : snapshot.getChildren()) {
                    //get data
                    String pTitle = "" + ds.child("pTitle").getValue();
                    String pDescr = "" + ds.child("pDescr").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimeStamp = "" + ds.child("pTime").getValue();
                    String pImage = "" + ds.child("pImage").getValue();
                    hisDp = "" + ds.child("uDp").getValue();
                    String uid = "" + ds.child("uid").getValue();
                    String uEmail = "" + ds.child("uEmail").getValue();
                    hisName = "" + ds.child("uName").getValue();
                    // String commentCount = "" + ds.child("pComments").getValue();


                    //convert timestamp to dd/mm/yyy hh:mm am/pm
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("MM/dd/yyyy hh:mm aa", calendar).toString();

                    //set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes + " Likes");
                    pTimeTiv.setText(pTime);
                    // pCommentsTv.setText(commentCount + " Comments");

                    uNameTv.setText(hisName);

                    //set image of the user who posted
                    //if there is no image i.e. pImage.equals("noImage") then hide ImageView
                    if (pImage.equals("noImage")) {
                        //hide imageview
                        pImageIv.setVisibility(View.GONE);
                    } else {
                        //show imageview
                        pImageIv.setVisibility(View.VISIBLE);
                        try {

                            Picasso.get().load(pImage).into(pImageIv);
                        } catch (Exception e) {

                        }
                    }

                    //set user image in comment part
//                    try {
//                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv);
//                    }catch (Exception e){
//                        Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv);
//                    }

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user!= null){
            //user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();
        }
        else{
            //user not signed in, go to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide some menu items
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if(id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}