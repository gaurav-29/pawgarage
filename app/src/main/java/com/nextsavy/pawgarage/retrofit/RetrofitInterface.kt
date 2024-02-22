package com.nextsavy.pawgarage.retrofit

import com.google.gson.JsonObject
import com.nextsavy.pawgarage.models.PlacesModel
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface RetrofitInterface {

    @GET("place/nearbysearch/json")
    fun getAllNearbyPlaces(@Query("location") location : String,
                    @Query("radius") radius : String,
                    @Query("type") type : String,
                    @Query("key") apiKey : String
    ) : Call<PlacesModel>

// Below server key (For Notifications) is for client project(Paw Garage App- Prod):
//    @Headers("Content-Type:application/json",
//        "Authorization:key=AAAAxydQea4:APA91bGvQ74d-jVXwDNskj1pUQt5Tu45F1GSjO1o0R9D-018rFQp04wh-UkI5ruTq2ZR9UQEaWMtLQb_19lx2eObaWXsVHzch8PG4sJu8xEqT6Ou4IPNatbnMPFSOGX6P70rAzf9Jua6")
// Below server key (For Notifications) is for debug project(Paw Garage App- Debug):
    @Headers("Content-Type:application/json",
    "Authorization:key=AAAAhDCybS8:APA91bG0IumWzxmhyzPNEwCCffOtWYX3lSGSEv-akKQlLkb3ds_PL-xVKIkMbVa5uUk3f_JhjUMObq0CTSS2Yjk6kTNI9cKx74GFzPE-IrXdJXicYS42Buh78V3Ro5kSulSm5uqBwv2p")
    @POST("fcm/send")
    fun sendNotificationThroughAPI(@Body notificationData: JsonObject): Call<JsonObject>
}