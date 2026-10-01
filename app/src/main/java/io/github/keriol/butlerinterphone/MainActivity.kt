package io.github.keriol.butlerinterphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.keriol.butlerinterphone.client.BifrostHttpClient
import io.github.keriol.butlerinterphone.ui.InterphoneRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val client = BifrostHttpClient(
            baseUrl = BuildConfig.BIFROST_URL,
            token = BuildConfig.BIFROST_TOKEN,
        )

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    InterphoneRoute(client = client)
                }
            }
        }
    }
}
