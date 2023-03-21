package com.example.newbeacon.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authroization:key=AAAAfCAVQ2s:APA91bHfuYonjazZtYd_IAdqwBLaQpWAUQnAzUi8v1iCu7iyp0f2Ggs253CgOoZMtltMXx-f8VYzolK-fjolGmRc5rLHp17wX5rboRtYmO9C22NqbZav1m5q_SB1EjlmqXbF4l4m5HA8"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
