package com.example.newbeacon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

//import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
//import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {


    private EditText emailtxt, passwordtxt;
    private TextView signuptxt, recoverpasstxt, dont_have_accounttxt;
    private Button signinbtn;
    private FirebaseAuth mAuth;
    private GoogleSignInOptions gso;
    private SignInButton googlebtn;
    private GoogleSignInClient gsc;
    private ProgressDialog progressDialog;
    private static final int RC_SIGN_IN = 100;
    private static final String TAG = "GOOGLE_SIGN_IN_TAG";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting up and customizing action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Beacon");
        actionBar.setBackgroundDrawable(new ColorDrawable(getColor(R.color.black)));
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);


        //instantiating the firebase database
        mAuth = FirebaseAuth.getInstance();

        // declaring all the views
        signinbtn = findViewById(R.id.signinbtn);
        signuptxt = findViewById(R.id.signuptxt);
        recoverpasstxt = findViewById(R.id.recoverpasswordtxt);
        emailtxt = findViewById(R.id.emailtxt);
        passwordtxt = findViewById(R.id.passwordtxt);
        googlebtn = findViewById(R.id.googleSignInBtn);

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        gsc = GoogleSignIn.getClient(this, gso);

        googlebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: begin Google SignIn");
                Intent intent = gsc.getSignInIntent();

                startActivityForResult(intent, RC_SIGN_IN);
            }
        });


        signinbtn.setOnClickListener(new View.OnClickListener() {
            //on click of signin button
            @Override
            public void onClick(View v) {
                String email = emailtxt.getText().toString();
                String password = passwordtxt.getText().toString();
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailtxt.setError("This email does not belong to any existing account");
                    emailtxt.setFocusable(true);
                } else{
                    loginUser(email,password);
                }
            }
        });

        // Sign up text click
        signuptxt.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
                finish();
            }
        });

        //recover password textview click
        recoverpasstxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecoverPasswordDialog();
            }
        });


        progressDialog = new ProgressDialog(this);

    }

    private void showRecoverPasswordDialog() {
        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Password");

        //set layout linear layout
        LinearLayout linearLayout = new LinearLayout(this);
        //views to set in dialog
        EditText emailtxt = new EditText(this);
        emailtxt.setHint("Email address");
        emailtxt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        emailtxt.setMinEms(16);

        linearLayout.addView(emailtxt);
        linearLayout.setPadding(10,10,10,10);

        builder.setView(linearLayout);

        //buttons recover
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                //input email
                String email = emailtxt.getText().toString().trim();
                beginRecovery(email);
            }
        });
        //buttons cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                //dismiss dialog
                dialog.dismiss();
            }
        });

        //show dialog
        builder.create().show();
    }

    private void beginRecovery(String email) {
        progressDialog.setMessage("Sending Email...");
        progressDialog.show();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()){
                            Toast.makeText(MainActivity.this, "Email sent", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(MainActivity.this, "Failed to send email. Make sure you are using correct email address", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        //get and show proper error message
                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void loginUser(String email, String password) {
        progressDialog.setMessage("Logging in...");
        progressDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();

                            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){

            Log.d(TAG, "onActivityResult: Google SignIn intent result");

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try{
                //google sign in
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogleAccount(account);
            } catch (ApiException e){
                //failed sign in
                Log.d(TAG, "onActivityResult: " + e.getMessage());
            }
        }
    }

    private void firebaseAuthWithGoogleAccount(GoogleSignInAccount account) {
        Log.d(TAG, "firebaseAuthWithGoogleAccount: begin firebase auth with google account");
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //login success
                        Log.d(TAG, "onSuccess: Logged in");

                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        String uid = firebaseUser.getUid();
                        String email = firebaseUser.getEmail();


//                        Log.d(TAG, "onSuccess: Email: " + email);
//                        Log.d(TAG, "onSuccess: UID: " + uid);

                        // check if account is new or existing
                        if (authResult.getAdditionalUserInfo().isNewUser()){
                            // user is new - Account Created
                            Log.d(TAG, "onSuccess:Account Created...\n" + email);

                            HashMap<Object, String> map = new HashMap<>();
                            //put user info in HashMap

                            map.put("Atype", "USER");
                            map.put("email", email);
                            map.put("uid", uid);
                            map.put("name", "");
                            map.put("interests", "");
                            map.put("image", "");
                            map.put("username", email);
                            map.put("location", "");

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference reference = database.getReference("Users");
                            reference.child(uid).setValue(map);

                            Toast.makeText(MainActivity.this, "Account Created...\n" + email, Toast.LENGTH_SHORT).show();

                        } else {
                            //existing user - Logged in
                            Log.d(TAG, "onSuccess: Existing user...\n" + email);
                            Toast.makeText(MainActivity.this, "Existing user...\n" + email, Toast.LENGTH_SHORT).show();
                        }

                        // start home fragment
                        startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

//    private void registerUser(String email, String password) {
//        progressDialog.show();
//
//        mAuth.createUserWithEmailAndPassword(email,password)
//                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()) {
//                            // Sign in success, update UI with the signed-in user's information
//                            progressDialog.dismiss();
//                            FirebaseUser user = mAuth.getCurrentUser();
//
//                            String email = user.getEmail();
//                            String uid  = user.getUid();
//
//                            HashMap<Object, String> map = new HashMap<>();
//                            //put user info in HashMap
//
//                            map.put("email", email);
//                            map.put("uid", uid);
//                            map.put("name", "");
//                            map.put("interests", "");
//                            map.put("image", "");
//                            map.put("username", usernametxt.getText().toString());
//
//                            FirebaseDatabase database = FirebaseDatabase.getInstance();
//                            DatabaseReference reference = database.getReference("Users");
//                            reference.child(uid).setValue(map);
//
//
//                            Toast.makeText(RegisterActivity.this,"User registered... " + user.getEmail(), Toast.LENGTH_SHORT).show();
//                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
//                            finish();
//                        } else {
//                            // If sign in fails, display a message to the user.
//                            progressDialog.dismiss();
//                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        progressDialog.dismiss();
//                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
}