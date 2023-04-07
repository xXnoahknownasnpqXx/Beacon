package com.example.newbeacon;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder> {

    Context context;
    List<ModelUser> userList;

    //constructor
    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate layout(row_user.xml)
        View view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        // get date
        final String hisUid = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        String userEmail = userList.get(position).getEmail();
        String userUsername = userList.get(position).getUsername();

        //set data
        holder.mNameTv.setText(userName);
        // holder.mEmailTv.setText(userEmail);
        holder.mUsernameTv.setText(userUsername);

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_img)
                    .into(holder.mAvatarIv);

        } catch(Exception e){

        }

        // handle item click
        holder.itemView.setOnClickListener((v) -> {
            //@Override
            //public void onClick(View v) {
            //    Toast.makeText(context, ""+ userUsername, Toast.LENGTH_SHORT).show();
            //}
            //show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(new String[]{"Profile"}, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which==0){
                        //profile clicked
                        Intent intent = new Intent(context, ThereProfileActivity.class);
                        intent.putExtra("uid",hisUid);
                        context.startActivity(intent);
                    }
                }
            });
            builder.create().show();
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {


        ImageView mAvatarIv;
        TextView mNameTv, mUsernameTv;
//        TextView mEmailTv;


        public MyHolder(@NonNull View itemView){
            super(itemView);

            mAvatarIv = itemView.findViewById(R.id.avatarIv);
            mNameTv = itemView.findViewById(R.id.nameTv);
//            mEmailTv = itemView.findViewById(R.id.emailtxt);
            mUsernameTv = itemView.findViewById(R.id.usernameTv);
        }
    }
}
