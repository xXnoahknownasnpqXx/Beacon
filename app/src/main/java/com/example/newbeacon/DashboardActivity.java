package com.example.newbeacon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.newbeacon.notifications.Token;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class DashboardActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference ref;
    private ActionBar actionBar;

    String mUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");

        firebaseAuth = FirebaseAuth.getInstance();

        BottomNavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        if (firebaseAuth.getUid() != null) {
            userRef.child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // Check the value of the "username" attribute
                    String account_type = snapshot.child("Atype").getValue(String.class);
                    if (account_type.equals("USER")) {
                        navigationView.getMenu().removeItem(R.id.nav_notification);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }


        actionBar.setTitle("Feed");
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment1, "");
        ft1.commit();

        // JAVA
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) { return; }
                        String token = task.getResult();
                    }
                });

        //update token
//        @Override
//        public void onNewToken(@NonNull String token) {
//            super.onNewToken(token);
//
//            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//            if (user!=null) {
//                updateToken(token); // TODO FIGURE THIS SHIT OUT
//            }
//        }
        // TODO FIGURE THIS SHIT OUT
    }

//    private void updateToken(String tokenRefresh){
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
//        Token token = new Token(tokenRefresh);
//        ref.child(mUID).setValue(token);
//    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem){
                    // handle item clicks
                    switch(menuItem.getItemId()){
                        case R.id.nav_home:
                            // home fragment transaction
                            actionBar.setTitle("Home");
                            HomeFragment fragment1 = new HomeFragment();
                            FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                            ft1.replace(R.id.content, fragment1, "");
                            ft1.commit();
                            return true;
                        case R.id.nav_profile:
                            // profile fragment transaction
                            actionBar.setTitle("Profile");
                            ref = FirebaseDatabase.getInstance().getReference("Users");
                            ref.child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    // Check the value of the "username" attribute
                                    String account_type = snapshot.child("Atype").getValue(String.class);
                                    if (account_type.equals("USER")) {
                                        // The username matches the desired value
                                        UserProfileFragment fragment2 = new UserProfileFragment();
                                        try {
                                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                                            ft2.replace(R.id.content, fragment2, "");
                                            ft2.commit();
                                        }
                                        catch (IllegalStateException ignored) {
                                            //no solution
                                        }

                                    } else {
//                                        // The username does not match the desired value
                                        ProfileFragment fragment2 = new ProfileFragment();
                                        try {
                                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                                            ft2.replace(R.id.content, fragment2, "");
                                            ft2.commit();
                                        }
                                        catch (IllegalStateException ignored) {
                                            //no solution
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    // Handle the error
                                }
                            });

                            return true;
                        case R.id.nav_users:
                            // users fragment transaction
                            actionBar.setTitle("Users");
                            UsersFragment fragment3 = new UsersFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content, fragment3, "");
                            ft3.commit();
                            return true;

                        case R.id.nav_notification:
                            // users fragment transaction
                            actionBar.setTitle("Notifications");
                            NotificationsFragment fragment4 = new NotificationsFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content, fragment4, "");
                            ft4.commit();
                            return true;
                    }

                    return false;
                }

            };

    private void checkUserStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }
        else {
            mUID = user.getUid();
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }
}