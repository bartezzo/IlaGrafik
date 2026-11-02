package pl.bartezzo.ilagrafik

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.launch
import pl.bartezzo.ilagrafik.data.GrafikRepository
import pl.bartezzo.ilagrafik.data.Tile
import pl.bartezzo.ilagrafik.data.TodayTileFinder
import pl.bartezzo.ilagrafik.ui.ZoomState
import pl.bartezzo.ilagrafik.ui.ZoomableImage
import pl.bartezzo.ilagrafik.ui.theme.IlaGrafikTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = GrafikRepository(applicationContext)
        val tileFinder = TodayTileFinder()
        setContent {
            IlaGrafikTheme {
                HomeScreen(repository, tileFinder)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: GrafikRepository, tileFinder: TodayTileFinder) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(true) }
    // Zwiększany po każdej podmianie, żeby ponownie wczytać obrazek.
    var version by remember { mutableIntStateOf(0) }
    // Kafelek dzisiejszego dnia znaleziony na obrazku (null = nie znaleziono).
    var todayTile by remember { mutableStateOf<Tile?>(null) }

    LaunchedEffect(version) {
        loading = true
        todayTile = null
        bitmap = repository.load()
        loading = false
    }

    val current = bitmap
    val image = remember(current) { current?.asImageBitmap() }
    val zoomState = remember(current) { ZoomState() }

    // Po wczytaniu obrazka szukamy dzisiejszego dnia i przybliżamy na niego.
    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        val tile = runCatching { tileFinder.find(current, LocalDate.now().dayOfWeek) }.getOrNull()
        todayTile = tile
        if (tile != null) zoomState.zoomTo(tile)
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            loading = true
            runCatching { repository.replaceWith(uri) }
                .onSuccess {
                    version++
                    Toast.makeText(context, R.string.grafik_updated, Toast.LENGTH_SHORT).show()
                }
                .onFailure {
                    loading = false
                    Toast.makeText(context, R.string.grafik_error, Toast.LENGTH_LONG).show()
                }
        }
    }
    val openPicker = { pickImage.launch("image/*") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        floatingActionButton = {
            if (image != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    todayTile?.let { tile ->
                        SmallFloatingActionButton(
                            onClick = { scope.launch { zoomState.zoomTo(tile) } }
                        ) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = stringResource(R.string.show_today)
                            )
                        }
                    }
                    ExtendedFloatingActionButton(
                        onClick = openPicker,
                        icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        text = { Text(stringResource(R.string.change_grafik)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            when {
                loading -> CircularProgressIndicator()
                image != null -> ZoomableImage(
                    bitmap = image,
                    state = zoomState,
                    contentDescription = stringResource(R.string.grafik_description)
                )
                else -> EmptyState(onAddClick = openPicker)
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_grafik),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_grafik_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Text(stringResource(R.string.add_grafik))
        }
    }
}
