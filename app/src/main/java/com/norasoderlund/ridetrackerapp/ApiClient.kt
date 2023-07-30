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

    private val context: Context;
    private val queue: RequestQueue;

    constructor(context: Context) {
        this.context = context;

        queue = Volley.newRequestQueue(context);
    }

    fun verifyLoginCode(name: String, code: String, callback: (response: ClientLoginResponse) -> Unit) {
        try {
            val body = JSONObject();
            body.put("name", name);
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

    fun verifyLoginPassword(name: String, email: String, password: String, callback: (response: ClientLoginResponse) -> Unit) {
        try {
            val body = JSONObject();
            body.put("name", name);
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

    fun uploadRecording(token: String, recording: String, callback: (response: ClientCreateActivityResponse) -> Unit) {
        try {
            val body = JSONObject(recording);

            val stringRequest =
                object : JsonObjectRequest(Request.Method.POST, "$apiUrl/api/activities/create", body,
                    { response ->
                        if(response.has("success")) {
                            val success = response.getBoolean("success");

                            if(!success) {
                                callback(ClientCreateActivityResponse(false));
                            }
                            else {
                                if(response.has("activity")) {
                                    val activity = response.getJSONObject("activity");

                                    if(activity.has("id")) {
                                        val id = activity.getString("id");

                                        callback(ClientCreateActivityResponse(true, id));
                                    }
                                    else
                                        callback(ClientCreateActivityResponse(true));
                                }
                                else
                                    callback(ClientCreateActivityResponse(true));
                            }
                        }
                        else
                            callback(ClientCreateActivityResponse(false));
                    },
                    {
                        callback(ClientCreateActivityResponse(false));
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>();
                        headers["User-Agent"] = userAgent;
                        headers["Authorization"] = "Basic device:$token";
                        return headers;
                    }
                };

            queue.add(stringRequest);
        }
        catch (exception: Exception) {
            callback(ClientCreateActivityResponse(false));
        }
    }

}

data class ClientLoginResponse(val success: Boolean, val message: String? = null, val token: String? = null);
data class ClientCreateActivityResponse(val success: Boolean, val activity: String? = null);