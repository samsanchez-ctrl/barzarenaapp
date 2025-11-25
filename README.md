# Barzarena App

Esta es una aplicación que desarrollé en Android nativo. Permite a los usuarios comprar items y realizar apuestas.

## Un Vistazo Técnico

Aquí explico un poco sobre cómo está construido el proyecto, las decisiones que tomé y las tecnologías que usé.

### Arquitectura

Para la arquitectura de la app, elegí usar **MVVM (Model-View-ViewModel)**. Me parece que es un enfoque muy sólido y recomendado para el desarrollo moderno en Android, ya que me ayuda a mantener el código organizado y fácil de escalar.

Así es como la organicé:

-   **Model**: Aquí es donde viven los datos y la lógica de negocio. Esto incluye mis entidades de Room (`Item`, `Bet`, `User`), los DAOs y las fuentes de datos.
-   **View**: Para la interfaz de usuario, usé **Jetpack Compose**. La vista simplemente observa los datos que le pasa el ViewModel y se actualiza cuando hay cambios.
-   **ViewModel**: Este es el intermediario. Expone los datos del modelo a la vista y se encarga de la lógica detrás de las acciones del usuario.

### Tecnologías que Utilicé

Para construir la app, me apoyé en varias librerías y tecnologías modernas del ecosistema de Android:

-   **Lenguaje**: [Kotlin](https://kotlinlang.org/), por supuesto.
-   **UI**: Toda la interfaz está hecha con [Jetpack Compose](https://developer.android.com/jetpack/compose).
-   **Inyección de Dependencias**: Uso [Hilt](https://dagger.dev/hilt/) para mantener todo desacoplado y fácil de testear.
-   **Base de Datos**: Para guardar los datos localmente, elegí [Room](https://developer.android.com/training/data-storage/room).
-   **Asincronía**: Las operaciones en segundo plano las manejo con [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html).
-   **Componentes de Arquitectura**: Utilizo componentes como ViewModel, LiveData y Lifecycle.
-   **Networking**: [Retrofit](https://square.github.io/retrofit/) para las llamadas a servicios web.

### ¿Cómo organicé el código?

Traté de mantener una estructura de paquetes clara, que sigue la arquitectura MVVM:

-   `data`: Aquí está todo lo relacionado con la obtención de datos (DAOs, repositorios...).
-   `di`: Mis módulos de Hilt para la inyección de dependencias.
-   `model`: Las clases de datos que uso en la app.
-   `viewmodel`: Los ViewModels con la lógica de UI.
-   `ui`: Donde viven mis Composables y todo lo relacionado con la interfaz.

### Base de Datos

Para la base de datos local, uso **Room**. Mi base de datos, `AppDatabase`, tiene las siguientes tablas:

-   `User`: Para guardar la información de los usuarios.
-   `Bet`: Donde almaceno el historial de apuestas.
-   `Item`: Para llevar un registro de los items de la tienda.

## ¿Cómo puedes probarla?

Si quieres compilar y ejecutar la app, solo sigue estos pasos:

1.  Clona el repositorio.
2.  Abre el proyecto en Android Studio.
3.  Sincroniza el proyecto con Gradle.
4.  ¡Listo! Ya puedes ejecutar la app en un emulador o en tu dispositivo.
