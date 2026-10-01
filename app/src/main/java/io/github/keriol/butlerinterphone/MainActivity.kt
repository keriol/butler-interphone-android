package io.github.keriol.butlerinterphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.BifrostHttpClient
import io.github.keriol.butlerinterphone.settings.AndroidConnectionSettingsStore
import io.github.keriol.butlerinterphone.ui.InterphoneRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    InterphoneRoute(
                        buildIdentity = "v${BuildConfig.VERSION_NAME} • ${BuildConfig.BUILD_DATE}",
                        initialEndpoint = BifrostEndpointParts.fromUrl(
                            BuildConfig.BIFROST_URL
                        ),
                        initialToken = BuildConfig.BIFROST_TOKEN,
                        settingsStore = AndroidConnectionSettingsStore(
                            applicationContext
                        ),
                        clientFactory = { endpoint, token ->
                            BifrostHttpClient(
                                baseUrl = endpoint,
                                token = token,
                            )
                        },
                    )
                }
            }
        }
    }
}
