package com.cloudbudget.app.ui.auth

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.BuildConfig
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CloudCredentialsActivity : AppCompatActivity() {

    private val apiBase = BuildConfig.API_BASE_URL
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: BuildConfig.FIRESTORE_USER_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_credentials)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_cloud_credentials)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        checkConnectedProviders()

        // AWS Connect
        findViewById<TextView>(R.id.btnConnectAws).setOnClickListener {
            val key = findViewById<EditText>(R.id.etAwsKey).text.toString().trim()
            val secret = findViewById<EditText>(R.id.etAwsSecret).text.toString().trim()
            val region = findViewById<EditText>(R.id.etAwsRegion).text.toString().trim().ifEmpty { "us-east-1" }

            if (key.isEmpty() || secret.isEmpty()) {
                showStatus("Please enter AWS Access Key and Secret", false)
                return@setOnClickListener
            }

            postCredentials("aws", JSONObject().apply {
                put("access_key_id", key)
                put("secret_access_key", secret)
                put("region", region)
            })
        }

        // Azure Connect
        findViewById<TextView>(R.id.btnConnectAzure).setOnClickListener {
            val tenant = findViewById<EditText>(R.id.etAzureTenant).text.toString().trim()
            val clientId = findViewById<EditText>(R.id.etAzureClientId).text.toString().trim()
            val clientSecret = findViewById<EditText>(R.id.etAzureClientSecret).text.toString().trim()
            val sub = findViewById<EditText>(R.id.etAzureSubscription).text.toString().trim()

            if (tenant.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty() || sub.isEmpty()) {
                showStatus("Please fill all Azure fields", false)
                return@setOnClickListener
            }

            postCredentials("azure", JSONObject().apply {
                put("tenant_id", tenant)
                put("client_id", clientId)
                put("client_secret", clientSecret)
                put("subscription_id", sub)
            })
        }

        // GCP Connect
        findViewById<TextView>(R.id.btnConnectGcp).setOnClickListener {
            val project = findViewById<EditText>(R.id.etGcpProject).text.toString().trim()
            val saJson = findViewById<EditText>(R.id.etGcpServiceAccount).text.toString().trim()

            if (project.isEmpty() || saJson.isEmpty()) {
                showStatus("Please enter GCP Project ID and Service Account JSON", false)
                return@setOnClickListener
            }

            postCredentials("gcp", JSONObject().apply {
                put("project_id", project)
                put("credentials_json", saJson)
            })
        }

        // Sync All
        findViewById<TextView>(R.id.btnSyncAll).setOnClickListener {
            showStatus("Syncing all connected clouds...", true)
            Thread {
                try {
                    val conn = (URL("$apiBase/api/users/$userId/sync").openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        connectTimeout = 30000; readTimeout = 30000; doOutput = true
                    }
                    conn.outputStream.write("{}".toByteArray())

                    val code = conn.responseCode
                    val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
                    conn.disconnect()

                    val json = JSONObject(resp)
                    runOnUiThread {
                        if (json.optBoolean("ok")) {
                            val total = json.optDouble("totalSpend", 0.0)
                            val providers = json.optJSONObject("providers")
                            val sb = StringBuilder("Sync complete! Total: $${String.format("%.2f", total)}\n")
                            providers?.keys()?.forEach { p ->
                                val info = providers.optJSONObject(p)
                                sb.append("${p.uppercase()}: ${info?.optString("status")} (${info?.optString("source")})\n")
                            }
                            showStatus(sb.toString().trim(), true)
                            checkConnectedProviders()
                        } else {
                            showStatus("Sync failed: ${json.optString("error")}", false)
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { showStatus("Sync error: ${e.message}", false) }
                }
            }.start()
        }

        findViewById<TextView>(R.id.btnContinue).setOnClickListener { goMain() }
        findViewById<TextView>(R.id.btnSkipCredentials).setOnClickListener { goMain() }
    }

    private fun postCredentials(provider: String, body: JSONObject) {
        showStatus("Connecting ${provider.uppercase()}...", true)
        Thread {
            try {
                val conn = (URL("$apiBase/api/users/$userId/credentials/$provider").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 15000; readTimeout = 15000; doOutput = true
                }
                OutputStreamWriter(conn.outputStream).apply { write(body.toString()); flush(); close() }

                val code = conn.responseCode
                val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
                conn.disconnect()

                runOnUiThread {
                    if (code in 200..299) {
                        showStatus("${provider.uppercase()} connected successfully!", true)
                        checkConnectedProviders()
                    } else {
                        showStatus("Failed: $resp", false)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { showStatus("Error: ${e.message}\nIs backend running at $apiBase?", false) }
            }
        }.start()
    }

    private fun checkConnectedProviders() {
        Thread {
            try {
                val conn = (URL("$apiBase/api/users/$userId/credentials").openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 10000; readTimeout = 10000
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    val arr = json.optJSONArray("connectedProviders")
                    val connected = (0 until (arr?.length() ?: 0)).map { arr!!.getString(it) }
                    runOnUiThread {
                        setProviderStatus(R.id.tvAwsStatus, "aws" in connected)
                        setProviderStatus(R.id.tvAzureStatus, "azure" in connected)
                        setProviderStatus(R.id.tvGcpStatus, "gcp" in connected)
                    }
                }
                conn.disconnect()
            } catch (_: Exception) { }
        }.start()
    }

    private fun setProviderStatus(viewId: Int, connected: Boolean) {
        val tv = findViewById<TextView>(viewId)
        tv.text = if (connected) "Connected" else "Not connected"
        tv.setTextColor(if (connected) Color.parseColor("#00FEB1") else Color.parseColor("#8E95A9"))
    }

    private fun showStatus(msg: String, success: Boolean) {
        val tv = findViewById<TextView>(R.id.tvStatus)
        tv.visibility = View.VISIBLE
        tv.text = msg
        tv.setTextColor(if (success) Color.parseColor("#00FEB1") else Color.parseColor("#FF716C"))
    }

    private fun goMain() {
        DemoPreferences.setLoggedIn(this, true)
        DemoPreferences.setCredentialsSetupDone(this, true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
