package com.example.newbeacon;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotificationsFragment extends Fragment {

    //recycler view
    RecyclerView notificationsRv;

    private FirebaseAuth firebaseAuth;
    private ArrayList<ModelNotification> notificationsList;
    private AdapterNotification adapterNotification;
    public NotificationsFragment(){

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_notifications, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        //init recyclerview
        notificationsRv = view.findViewById(R.id.notificationsRv);

//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
//        userRef.child(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Check the value of the "username" attribute
//                String account_type = snapshot.child("Atype").getValue(String.class);
//                if (account_type.equals("USER")) {
//                    view.setVisibility(View.GONE);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

        getAllNotifications();

        return view;
    }

    private void getAllNotifications() {
        notificationsList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //get data
                            ModelNotification model = ds.getValue(ModelNotification.class);

                            //add to list
                            notificationsList.add(model);
                        }
                        Collections.reverse(notificationsList);
                        //adapter
                        adapterNotification = new AdapterNotification(getActivity(), notificationsList);
                        // set to Recyclerview
                        notificationsRv.setAdapter(adapterNotification);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}