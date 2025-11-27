package com.example.adoptaunamigo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.SubcomposeAsyncImage
import com.example.adoptaunamigo.data.MascotaContract
import com.example.adoptaunamigo.data.MascotaDbHelper
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Modelo de datos para representar una mascota.
 *
 * @param id El identificador único de la mascota.
 * @param nombre El nombre de la mascota.
 * @param raza La raza de la mascota.
 * @param edad La edad de la mascota.
 * @param especie La especie de la mascota (ej. Perro, Gato).
 * @param fotoUri La URI de la foto de la mascota, como String.
 * @param descripcion Una breve descripción de la mascota.
 */
data class Mascota(val id: Long, val nombre: String, val raza: String, val edad: String, val especie: String, val fotoUri: String, val descripcion: String)

// --- 1. ACTIVIDAD PRINCIPAL ---
/**
 * Actividad principal de la aplicación. Se encarga de inicializar la base de datos
 * y de establecer el contenido de la UI con Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: MascotaDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = MascotaDbHelper(this)

        insertarDatosDeEjemploSiEsNecesario()

        setContent {
            MaterialTheme {
                AppMascotas(dbHelper = dbHelper)
            }
        }
    }

    /**
     * Inserta un conjunto de mascotas de ejemplo en la base de datos si la aplicación
     * se ejecuta por primera vez y no hay datos. Utiliza SharedPreferences para controlar
     * si ya se ha ejecutado antes.
     */
    private fun insertarDatosDeEjemploSiEsNecesario() {
        val prefs = getSharedPreferences("com.example.adoptaunamigo.prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ${MascotaContract.MascotaEntry.TABLE_NAME}", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()

            if (count == 0) {
                val writableDb = dbHelper.writableDatabase
                val listaMascotas = listOf(
                    Mascota(1, "Max", "Golden Retriever", "2 años", "Perro", "", "Juguetón y amigable"),
                    Mascota(2, "Luna", "Siamesa", "1 año", "Gato", "", "Curiosa y cariñosa"),
                    Mascota(3, "Rocky", "Bulldog", "3 años", "Perro", "", "Tranquilo y leal"),
                    Mascota(4, "Coco", "Poodle", "5 meses", "Perro", "", "Inteligente y enérgico"),
                    Mascota(5, "Simba", "Persa", "4 años", "Gato", "", "Elegante y mimoso"),
                    Mascota(6, "Nala", "Labrador", "2 años", "Perro", "", "Obediente y juguetona"),
                    Mascota(7, "Bruno", "Beagle", "6 años", "Perro", "", "Aventurero y divertido")
                )

                listaMascotas.forEach { mascota ->
                    val values = ContentValues().apply {
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE, mascota.nombre)
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA, mascota.raza)
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD, mascota.edad)
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE, mascota.especie)
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI, mascota.fotoUri)
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION, mascota.descripcion)
                    }
                    writableDb.insert(MascotaContract.MascotaEntry.TABLE_NAME, null, values)
                }
            }
            prefs.edit().putBoolean("isFirstRun", false).apply()
        }
    }
}

/**
 * Guarda una imagen desde una URI de contenido en el almacenamiento interno de la aplicación.
 * Devuelve la URI del archivo guardado como un String, o null si falla.
 */
fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, "${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        Uri.fromFile(file).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// --- 2. DEFINICIÓN DE RUTAS ---
/**
 * Define las pantallas principales de la aplicación que se muestran en la barra de navegación.
 *
 * @param route La ruta de navegación asociada a la pantalla.
 * @param title El título que se mostrará.
 * @param icon El icono que representará a la pantalla.
 */
sealed class AppScreens(val route: String, val title: String, val icon: ImageVector) {
    object Inicio : AppScreens("inicio", "Inicio", Icons.Default.Pets)
    object Perfil : AppScreens("perfil", "Perfil", Icons.Default.Person)
}

/**
 * Objeto que contiene las constantes para las rutas de navegación que requieren argumentos,
 * y funciones para construir dichas rutas de forma segura.
 */
object Rutas {
    const val DETALLE = "detalle/{mascotaId}"
    const val AGREGAR = "agregar"
    const val EDITAR = "editar/{mascotaId}"
    fun crearRutaDetalle(id: Long) = "detalle/$id"
    fun crearRutaEditar(id: Long) = "editar/$id"
}

// --- 3. ESTRUCTURA PRINCIPAL CON SCAFFOLD ---
/**
 * Composable principal que define la estructura de la UI con `Scaffold`.
 * Gestiona la barra superior (TopAppBar), la barra de navegación inferior (BottomBar),
 * el botón de acción flotante (FAB) y el contenido principal a través de `AppNavigation`.
 *
 * @param dbHelper El helper para interactuar con la base de datos SQLite.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMascotas(dbHelper: MascotaDbHelper) {
    val navController = rememberNavController()
    val view = LocalView.current

    var mascotas by remember { mutableStateOf(emptyList<Mascota>()) }

    // Estado para el filtro por especie
    var filtroEspecie by remember { mutableStateOf("Todos") }
    var showFilterMenu by remember { mutableStateOf(false) }
    val especiesParaFiltrar = listOf("Todos", "Perro", "Gato", "Conejo", "Ave", "Hámster", "Cuyo", "Pez", "Roedor")

    // Filtra la lista de mascotas según la especie seleccionada
    val mascotasFiltradas = if (filtroEspecie == "Todos") mascotas else mascotas.filter { it.especie == filtroEspecie }

    /**
     * Carga todas las mascotas desde la base de datos y actualiza el estado `mascotas`.
     */
    fun cargarMascotas() {
        val db = dbHelper.readableDatabase
        val projection = arrayOf(
            MascotaContract.MascotaEntry.COLUMN_NAME_ID,
            MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE,
            MascotaContract.MascotaEntry.COLUMN_NAME_RAZA,
            MascotaContract.MascotaEntry.COLUMN_NAME_EDAD,
            MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE,
            MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI,
            MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION
        )
        val cursor = db.query(MascotaContract.MascotaEntry.TABLE_NAME, projection, null, null, null, null, "${MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE} ASC")
        val lista = mutableListOf<Mascota>()
        with(cursor) {
            while (moveToNext()) {
                val id = getLong(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_ID))
                val nombre = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE))
                val raza = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA))
                val edad = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD))
                val especie = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE))
                val fotoUri = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI))
                val descripcion = getString(getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION))
                lista.add(Mascota(id, nombre, raza, edad, especie, fotoUri, descripcion))
            }
        }
        cursor.close()
        mascotas = lista
    }

    // Carga las mascotas al iniciar el composable
    LaunchedEffect(Unit) {
        cargarMascotas()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf(AppScreens.Inicio.route, AppScreens.Perfil.route)

    Scaffold(
        topBar = {
            // Decide qué TopAppBar mostrar según la ruta actual
            if (currentRoute?.startsWith("detalle") == true || currentRoute == Rutas.AGREGAR || currentRoute?.startsWith("editar") == true) {
                TopAppBar(
                    title = { Text(if (currentRoute == Rutas.AGREGAR) "Agregar Mascota" else if (currentRoute.startsWith("editar")) "Editar Mascota" else "Detalles") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("AdoptaUnAmigo", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    actions = {
                        // Muestra el botón de filtro solo en la pantalla de inicio
                        if (currentRoute == AppScreens.Inicio.route) {
                            Box {
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filtrar Mascotas")
                                }
                                DropdownMenu(
                                    expanded = showFilterMenu,
                                    onDismissRequest = { showFilterMenu = false }
                                ) {
                                    especiesParaFiltrar.forEach { especie ->
                                        DropdownMenuItem(
                                            text = { Text(especie) },
                                            onClick = {
                                                filtroEspecie = especie
                                                showFilterMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                MenuInferior(navController)
            }
        },
        floatingActionButton = {
            if (currentRoute == AppScreens.Inicio.route) {
                FloatingActionButton(onClick = { 
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    navController.navigate(Rutas.AGREGAR) 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Mascota")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNavigation(navController = navController, dbHelper = dbHelper, mascotas = mascotas, onMascotaAction = { cargarMascotas() })
        }
    }
}

// --- 4. GESTOR DE NAVEGACIÓN (NAVHOST) ---
/**
 * Gestiona la navegación entre las diferentes pantallas de la aplicación utilizando `NavHost`.
 *
 * @param navController El controlador de navegación.
 * @param dbHelper El helper de la base de datos.
 * @param mascotas La lista completa de mascotas para búsquedas.
 * @param onMascotaAction Callback que se ejecuta cuando se realiza una acción sobre una mascota (agregar, editar, eliminar).
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    dbHelper: MascotaDbHelper,
    mascotas: List<Mascota>,
    onMascotaAction: () -> Unit
) {
    NavHost(navController = navController, startDestination = AppScreens.Inicio.route) {
        composable(AppScreens.Inicio.route) {
             val navBackStackEntry by navController.currentBackStackEntryAsState()
            LaunchedEffect(navBackStackEntry) {
                onMascotaAction()
            }
            PantallaInicio(
                mascotas = mascotas,
                onMascotaClick = { id ->
                    navController.navigate(Rutas.crearRutaDetalle(id))
                }
            )
        }
        composable(AppScreens.Perfil.route) { PantallaPerfil() }

        composable(Rutas.AGREGAR) {
            PantallaAgregarMascota(dbHelper = dbHelper, navController = navController)
        }

        composable(
            route = Rutas.EDITAR,
            arguments = listOf(navArgument("mascotaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mascotaId = backStackEntry.arguments?.getLong("mascotaId") ?: -1
            val db = dbHelper.readableDatabase
            val cursor = db.query(MascotaContract.MascotaEntry.TABLE_NAME, null, "${MascotaContract.MascotaEntry.COLUMN_NAME_ID} = ?", arrayOf(mascotaId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE))
                val raza = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA))
                val edad = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD))
                val especie = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE))
                val fotoUri = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION))
                val mascota = Mascota(mascotaId, nombre, raza, edad, especie, fotoUri, descripcion)
                PantallaEditarMascota(mascota = mascota, dbHelper = dbHelper, navController = navController)
            }
            cursor.close()
        }

        composable(
            route = Rutas.DETALLE,
            arguments = listOf(navArgument("mascotaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mascotaId = backStackEntry.arguments?.getLong("mascotaId") ?: -1
            // Carga la mascota directamente desde la DB para asegurar datos frescos
            val db = dbHelper.readableDatabase
            val cursor = db.query(MascotaContract.MascotaEntry.TABLE_NAME, null, "${MascotaContract.MascotaEntry.COLUMN_NAME_ID} = ?", arrayOf(mascotaId.toString()), null, null, null)
            if (cursor.moveToFirst()) {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE))
                val raza = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA))
                val edad = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD))
                val especie = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE))
                val fotoUri = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI))
                val descripcion = cursor.getString(cursor.getColumnIndexOrThrow(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION))
                val mascota = Mascota(mascotaId, nombre, raza, edad, especie, fotoUri, descripcion)
                PantallaDetalle(mascota = mascota, dbHelper = dbHelper, navController = navController, onMascotaAction = onMascotaAction)
            }
            cursor.close()
        }
    }
}


// --- 6. PANTALLAS ---

/**
 * Muestra la lista principal de mascotas. Si la lista está vacía, muestra un mensaje informativo.
 *
 * @param mascotas La lista de mascotas a mostrar.
 * @param onMascotaClick Callback que se invoca al hacer clic en una mascota.
 */
@Composable
fun PantallaInicio(mascotas: List<Mascota>, onMascotaClick: (Long) -> Unit) {
    if (mascotas.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Por el momento no hay mascotas, puedes agregar una desde el botón designado",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F2)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Mascotas esperando un hogar",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            items(mascotas) { mascota ->
                ItemMascota(mascota = mascota, onClick = { onMascotaClick(mascota.id) })
            }
        }
    }
}

/**
 * Formulario para agregar una nueva mascota a la base de datos.
 *
 * @param dbHelper Helper para la base de datos.
 * @param navController Controlador de navegación para volver atrás tras guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAgregarMascota(dbHelper: MascotaDbHelper, navController: NavHostController) {
    var nombre by remember { mutableStateOf("") }
    var raza by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fotoUri by remember { mutableStateOf("") }
    val view = LocalView.current
    val context = LocalContext.current

    val especiesComunes = listOf("Perro", "Gato", "Conejo", "Ave", "Hámster", "Cuyo", "Pez", "Roedor", "Otro")
    var especieSeleccionada by remember { mutableStateOf(especiesComunes[0]) }
    var otraEspecie by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val especieFinal = if (especieSeleccionada == "Otro") otraEspecie else especieSeleccionada

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            val newUri = saveImageToInternalStorage(context, it)
            if (newUri != null) {
                fotoUri = newUri
            }
        }
    }

    val isFormValid by remember(nombre, especieFinal, raza, edad, descripcion) {
        derivedStateOf { nombre.isNotBlank() && especieFinal.isNotBlank() && raza.isNotBlank() && edad.isNotBlank() && descripcion.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = especieSeleccionada,
                onValueChange = { },
                label = { Text("Especie") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                especiesComunes.forEach { especie ->
                    DropdownMenuItem(
                        text = { Text(especie) },
                        onClick = {
                            especieSeleccionada = especie
                            expanded = false
                        }
                    )
                }
            }
        }

        if (especieSeleccionada == "Otro") {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = otraEspecie,
                onValueChange = { otraEspecie = it },
                label = { Text("Especificar otra especie") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = raza, onValueChange = { raza = it }, label = { Text("Raza") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Seleccionar Foto")
        }
        if (fotoUri.isNotBlank()) {
            Text("Foto seleccionada", style = MaterialTheme.typography.bodySmall) 
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE, nombre)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA, raza)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD, edad)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE, especieFinal)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI, fotoUri)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION, descripcion)
                }
                db.insert(MascotaContract.MascotaEntry.TABLE_NAME, null, values)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                navController.popBackStack()
            },
            enabled = isFormValid
        ) {
            Text("Guardar Mascota")
        }
    }
}

/**
 * Formulario para editar los datos de una mascota existente.
 *
 * @param mascota La mascota a editar.
 * @param dbHelper Helper para la base de datos.
 * @param navController Controlador de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarMascota(mascota: Mascota, dbHelper: MascotaDbHelper, navController: NavHostController) {
    var nombre by remember { mutableStateOf(mascota.nombre) }
    var raza by remember { mutableStateOf(mascota.raza) }
    var edad by remember { mutableStateOf(mascota.edad) }
    var descripcion by remember { mutableStateOf(mascota.descripcion) }
    val fotoUri by remember { mutableStateOf(mascota.fotoUri) }

    val especiesComunes = listOf("Perro", "Gato", "Conejo", "Ave", "Hámster", "Cuyo", "Pez", "Roedor", "Otro")
    val esEspecieComun = mascota.especie in especiesComunes.dropLast(1)

    var especieSeleccionada by remember { mutableStateOf(if (esEspecieComun) mascota.especie else "Otro") }
    var otraEspecie by remember { mutableStateOf(if (esEspecieComun) "" else mascota.especie) }
    var expanded by remember { mutableStateOf(false) }

    val especieFinal = if (especieSeleccionada == "Otro") otraEspecie else especieSeleccionada

    val isFormValid by remember(nombre, especieFinal, raza, edad, descripcion) {
        derivedStateOf { nombre.isNotBlank() && especieFinal.isNotBlank() && raza.isNotBlank() && edad.isNotBlank() && descripcion.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = especieSeleccionada,
                onValueChange = { },
                label = { Text("Especie") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                especiesComunes.forEach { especie ->
                    DropdownMenuItem(
                        text = { Text(especie) },
                        onClick = {
                            especieSeleccionada = especie
                            if (especie != "Otro") {
                                otraEspecie = ""
                            }
                            expanded = false
                        }
                    )
                }
            }
        }

        if (especieSeleccionada == "Otro") {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = otraEspecie,
                onValueChange = { otraEspecie = it },
                label = { Text("Especificar otra especie") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = raza, onValueChange = { raza = it }, label = { Text("Raza") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = edad, onValueChange = { edad = it }, label = { Text("Edad") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_NOMBRE, nombre)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_RAZA, raza)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_EDAD, edad)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_ESPECIE, especieFinal)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_DESCRIPCION, descripcion)
                    put(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI, fotoUri)
                }
                db.update(MascotaContract.MascotaEntry.TABLE_NAME, values, "${MascotaContract.MascotaEntry.COLUMN_NAME_ID} = ?", arrayOf(mascota.id.toString()))
                navController.popBackStack()
            },
            enabled = isFormValid
        ) {
            Text("Guardar Cambios")
        }
    }
}

/**
 * Muestra la vista detallada de una mascota, incluyendo su foto, nombre, descripción
 * y botones de acción (Editar, Adoptar).
 *
 * @param mascota La mascota a mostrar.
 * @param dbHelper Helper de la base de datos.
 * @param navController Controlador de navegación.
 * @param onMascotaAction Callback para actualizar la lista de mascotas.
 */
@Composable
fun PantallaDetalle(mascota: Mascota, dbHelper: MascotaDbHelper, navController: NavHostController, onMascotaAction: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val view = LocalView.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val newUri = saveImageToInternalStorage(context, it)
            if (newUri != null) {
                try {
                    val db = dbHelper.writableDatabase
                    val values = ContentValues().apply {
                        put(MascotaContract.MascotaEntry.COLUMN_NAME_FOTO_URI, newUri)
                    }
                    db.update(MascotaContract.MascotaEntry.TABLE_NAME, values, "${MascotaContract.MascotaEntry.COLUMN_NAME_ID} = ?", arrayOf(mascota.id.toString()))
                    onMascotaAction() // Recarga para mostrar la nueva foto
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirmar Adopción") },
            text = { Text("¿Estás seguro de que quieres adoptar a ${mascota.nombre}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        val db = dbHelper.writableDatabase
                        db.delete(MascotaContract.MascotaEntry.TABLE_NAME, "${MascotaContract.MascotaEntry.COLUMN_NAME_ID} = ?", arrayOf(mascota.id.toString()))
                        showDialog = false
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onMascotaAction()
                        navController.popBackStack()
                    }
                ) {
                    Text("Adoptar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SubcomposeAsyncImage(
            model = mascota.fotoUri,
            contentDescription = "Foto de ${mascota.nombre}",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    tint = Color(0xFFFF6F00),
                    modifier = Modifier.size(120.dp)
                )
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Cambiar Foto")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "¡Conoce a ${mascota.nombre}!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = mascota.descripcion,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { navController.navigate(Rutas.crearRutaEditar(mascota.id)) }) {
                Text("Editar")
            }
            OutlinedButton(onClick = { showDialog = true }) {
                Text("Adoptar")
            }
        }
    }
}

/**
 * Barra de navegación inferior que permite moverse entre las pantallas principales.
 */
@Composable
fun MenuInferior(navController: NavHostController) {
    val screens = listOf(AppScreens.Inicio, AppScreens.Perfil)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        screens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = null) },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

/**
 * Composable que representa un único elemento en la lista de mascotas.
 */
@Composable
fun ItemMascota(mascota: Mascota, onClick: () -> Unit) {
    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SubcomposeAsyncImage(
                model = mascota.fotoUri,
                contentDescription = "Foto de ${mascota.nombre}",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = "Mascota",
                        tint = Color(0xFFFF6F00),
                        modifier = Modifier.size(48.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = mascota.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "${mascota.especie} • ${mascota.raza} • ${mascota.edad}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        }
    }
}

/**
 * Pantalla de perfil (actualmente es un placeholder).
 */
@Composable
fun PantallaPerfil() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFCE4EC)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Person, null, Modifier.size(64.dp), tint = Color.Magenta)
            Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
