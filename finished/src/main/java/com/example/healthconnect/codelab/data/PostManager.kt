package com.example.healthconnect.codelab.data

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class PostManager(private val context: Context) {

    fun performPostRequest(url: String, postData: JSONObject,
                           success: (String) -> Unit, error: (String) -> Unit) {

        val requestQueue = Volley.newRequestQueue(context)

        val stringRequest = object : StringRequest(Request.Method.POST, url,
            Response.Listener<String> {
                response -> success(response)
            },
            Response.ErrorListener {
                volleyError -> error(volleyError.toString())
            })
        {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getBody(): ByteArray {
                return postData.toString().toByteArray(Charsets.UTF_8)
            }
        }

        requestQueue.add(stringRequest)
    }
}