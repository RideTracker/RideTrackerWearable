package com.norasoderlund.ridetrackerapp

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.norasoderlund.ridetrackerapp.presentation.MainActivity
import com.norasoderlund.ridetrackerapp.utils.getDeviceName
import org.json.JSONObject
import java.lang.Exception

class ApiClient {
    private val apiUrl: String = "https://staging.service.ridetracker.app";
    private val userAgent: String = "RideTrackerWearable-0.9.0";

    private val activity: MainActivity;
    private val queue: RequestQueue;

    constructor(activity: MainActivity) {
        this.activity = activity;

        queue = Volley.newRequestQueue(activity);
    }

    fun verifyLoginCode(code: String, callback: (response: ClientLoginResponse) -> Unit) {
        try {
            val body = JSONObject();
            body.put("name", activity.deviceName);
            body.put("code", code);

            val stringRequest =
                object : JsonObjectRequest(Request.Method.POST, "$apiUrl/api/devices/auth/verify", body,
                    { response ->
                        if(response.has("success")) {
                            val success = response.getBoolean("success");

                            if(!success) {
                                if(response.has("message")) {
                                    val message = response.getString("message");

                                    callback(ClientLoginResponse(false, message));
                                }
                                else
                                    callback(ClientLoginResponse(false, "Something went wrong."));

                            }
                            else {
                                if(response.has("token")) {
                                    val token = response.getJSONObject("token");

                                    if(token.has("key")) {
                                        val key = token.getString("key");

                                        callback(ClientLoginResponse(true, null, key));
                                    }
                                    else
                                        callback(ClientLoginResponse(false, "Something went wrong."));
                                }
                                else
                                    callback(ClientLoginResponse(false, "Something went wrong."));
                            }
                        }
                        else
                            callback(ClientLoginResponse(false, "Something went wrong."));
                    },
                    {
                        callback(ClientLoginResponse(false, "Something went wrong."));
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>();
                        headers["User-Agent"] = userAgent;
                        return headers;
                    }
                };

            queue.add(stringRequest);
        }
        catch (exception: Exception) {
            callback(ClientLoginResponse(false, "Something went wrong."));
        }
    }

    fun verifyLoginPassword(email: String, password: String, callback: (response: ClientLoginResponse) -> Unit) {
        try {
            val body = JSONObject();
            body.put("name", activity.deviceName);
            body.put("email", email);
            body.put("password", password);

            val stringRequest =
                object : JsonObjectRequest(Request.Method.POST, "$apiUrl/api/devices/auth/login", body,
                    { response ->
                        if(response.has("success")) {
                            val success = response.getBoolean("success");

                            if(!success) {
                                if(response.has("message")) {
                                    val message = response.getString("message");

                                    callback(ClientLoginResponse(false, message));
                                }
                                else
                                    callback(ClientLoginResponse(false, "Something went wrong."));

                            }
                            else {
                                if(response.has("token")) {
                                    val token = response.getJSONObject("token");

                                    if(token.has("key")) {
                                        val key = token.getString("key");

                                        callback(ClientLoginResponse(true, null, key));
                                    }
                                    else
                                        callback(ClientLoginResponse(false, "Something went wrong."));
                                }
                                else
                                    callback(ClientLoginResponse(false, "Something went wrong."));
                            }
                        }
                        else
                            callback(ClientLoginResponse(false, "Something went wrong."));
                    },
                    {
                        callback(ClientLoginResponse(false, "Something went wrong."));
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>();
                        headers["User-Agent"] = userAgent;
                        return headers;
                    }
                };

            queue.add(stringRequest);
        }
        catch (exception: Exception) {
            callback(ClientLoginResponse(false, "Something went wrong."));
        }
    }

}

data class ClientLoginResponse(val success: Boolean, val message: String? = null, val token: String? = null);