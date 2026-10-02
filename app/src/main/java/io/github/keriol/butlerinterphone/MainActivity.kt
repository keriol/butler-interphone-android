package io.github.keriol.butlerinterphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.keriol.butlerinterphone.client.BifrostEndpointParts
import io.github.keriol.butlerinterphone.client.BifrostHttpClient
import io.github.keriol.butlerinterphone.settings.AndroidConnectionSettingsStore
import io.github.keriol.butlerinterphone.ui.InterphoneRoute
import io.github.keriol.butlerinterphone.ui.theme.InterphoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InterphoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    InterphoneRoute(
                        appVersion = BuildConfig.VERSION_NAME,
                        buildDate = BuildConfig.BUILD_DATE,
                        buildType = BuildConfig.BUILD_TYPE,
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
