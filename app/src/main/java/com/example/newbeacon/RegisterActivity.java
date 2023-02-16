package com.example.newbeacon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {


    EditText emailtxt, usernametxt, passwordtxt, confirmpasswordtxt;
    Button signUpBtn;
    TextView haveaccounttxt, signintxt;
    ProgressDialog progressDialog;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create Account");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        emailtxt = findViewById(R.id.emailtxt);
        usernametxt = findViewById(R.id.usernametxt);
        passwordtxt = findViewById(R.id.passwordtxt);
        confirmpasswordtxt = findViewById(R.id.confirmpasswordtxt);
        signUpBtn = findViewById(R.id.signupbtn);
        haveaccounttxt = findViewById(R.id.haveaccounttxt);
        signintxt = findViewById(R.id.signintxt);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");


        /*  For team members:
            Username/email/password reqs: username must be longer than 4 chars, can't have a space in it,
            and can't start with a number
            Password can't have a space, must be 8 or more chars long, needs to have 3/4 of the following reqs:
            1 digit, 1 uppercase, 1 lowercase, 1 symbol
            Email is automatically done by firebase and just ensures that string has email format
            // TODO: fill hashmap of user info for each user so that their profiles are unique
         */

        signUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernametxt.getText().toString().trim();
                String email = emailtxt.getText().toString().trim();
                String password = passwordtxt.getText().toString().trim();
                String confirmpassword = confirmpasswordtxt.getText().toString().trim();

                if (username.length() < 5 || username.contains(" ") || isDigit(String.valueOf(username.charAt(0)))) {
                    usernametxt.setError("Username must contain at least 5 characters, no spaces, " +
                            "and cannot start with a digit.");
                }
                else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    // error and focus to email edit text
                    emailtxt.setError("Invalid Email");
                    emailtxt.setFocusable(true);
                }
                else if (!validatePassword(password)) {
                    // set error and focus to password editText
                    passwordtxt.setError("Invalid Password. Password must have at least 8 characters" +
                            "\nand satisfy 3 out of the 4 following requirements:" +
                            "\n\t1. At least one digit"
                            + "\n\t2. One lower case letter \n\t3. One upper case letter\n\t4. One symbol.");
                } else if (!validateequality(password, confirmpassword)){
                    confirmpasswordtxt.setError("The 2 passwords don't match! Password 1: " + password + "\nPassword 2: " + confirmpassword);
                }
                else {
                    registerUser(email, password);
                }
            }
        });

        signintxt.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v){
               startActivity(new Intent(RegisterActivity.this, MainActivity.class));
           }
        });
    }

    private void registerUser(String email, String password) {
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            //TODO switch out email for username on profile info
                            Toast.makeText(RegisterActivity.this,"User Registered... " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private static boolean validatePassword(String str) {
        int count = 0;

        if (str.contains(" ")) {
//            System.out.println("password must not contain spaces!");
            return false;
        }

        if (isGoodLength(str) == false) {
//            System.out.println("Invalid Password");
//            System.out.println("Password length must be between 8 and 16 characters");
            return false;
        } else {
            if (isDigit(str) == true) {
                count++;
            }
            if (isLowerCase(str) == true) {
                count++;
            }
            if (isUpperCase(str) == true) {
                count++;
            }
            if (isSymbol(str) == true) {
                count++;
            }

            if (count >= 3) {
//                System.out.println("Valid password!");
                return true;
            } else {
//                System.out.println("Invalid Password. Password must satisfy 3 out of the 4 following requirements:" +
//                        "\n\t1. At least one digit"
//                        + "\n\t2. One lower case letter \n\t3. One upper case letter\n\t4. One symbol.");
                return false;
            }
        }
    }

    private static boolean validateequality(String password1, String password2){
        if (password1.equals(password2)){
            return true;
        }
        return false;
    }

    private static boolean isGoodLength(String password) {
        if (password.length() < 8){
            return false;
        }
        return true;
    }

    private static boolean isDigit(String password) {
        String regex = ".*[0-9].*";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }

    private static boolean isLowerCase(String password) {
        String regex = ".*[a-z].*";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }

    private static boolean isSymbol(String password) {
        String regex = ".*[~!@#$%^&*()=+_-].*";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }

    private static boolean isUpperCase(String password) {
        String regex = ".*[A-Z].*";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(password);

        return m.matches();
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}