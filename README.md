# 🚌 BusCoruña

Aplicación para relojes **Wear OS** que muestra cuánto falta para que llegue el autobús urbano de A Coruña, tanto **para ir al trabajo** como **para volver a casa**, sin sacar el móvil.

Los tiempos se obtienen en directo del servicio de información de iTranvías.

<p align="center">
  <img src="docs/images/pantalla-principal.png" width="220" alt="Pantalla principal con el próximo bus al trabajo">
  &nbsp;&nbsp;
  <img src="docs/images/proximos-buses.png" width="220" alt="Lista de próximos buses para volver a casa">
</p>
<p align="center">
  <img src="docs/images/complicaciones-esfera.png" width="254" alt="Complicaciones en la esfera del reloj: casa 1 minuto línea 3, trabajo 8 minutos línea 3A">
</p>

## Funciones

- **Dos rutas: al trabajo y a casa.** Cada tarjeta muestra el próximo bus que te sirve, los minutos que faltan en grande y la parada donde cogerlo.
- **Varias paradas y líneas por ruta.** Si puedes coger el bus en más de una parada o te valen varias líneas, la app elige el que llega antes.
- **Lista de próximos buses.** Al tocar una tarjeta se ven todos los buses que vienen para esa ruta, ordenados por minutos.
- **Complicaciones para la esfera.** "Bus al trabajo" y "Bus a casa" muestran el tiempo restante y la línea directamente en la esfera, con una cuenta atrás que se actualiza sola.
- **Configuración desde el reloj.** Las paradas se eligen por línea (viendo su recorrido) o tecleando el número. Las líneas se marcan de una lista que solo incluye las que pasan por tus paradas.
- **Nombres reales de las paradas**, obtenidos del catálogo oficial de la red.

## Uso

### Pantalla principal

Al abrir la app aparecen dos tarjetas, **🏢 Al Trabajo** y **🏠 A Casa**. Cada una muestra:

- la **línea** del próximo bus que cumple tu configuración (por ejemplo, *Bus 3A*);
- los **minutos** que faltan para que llegue;
- la **parada** donde pasa: `42 - Emilio González López, Manuel Azaña`.

Los tiempos se actualizan cada vez que abres la app o vuelves a ella.

### Próximos buses

Toca una tarjeta para ver **todos los buses** que vienen para esa ruta, de cualquiera de tus paradas y líneas, ordenados de menor a mayor tiempo de espera. El primero de la lista es el mismo que el de la tarjeta.

Toca cualquier bus de la lista para actualizarla.

### Configuración

Desde **⚙️ Configuración**, al final de la pantalla principal, se ajusta cada ruta por separado.

<p align="center">
  <img src="docs/images/config-paradas.png" width="200" alt="Paradas configuradas para ir al trabajo">
  &nbsp;
  <img src="docs/images/config-lineas.png" width="200" alt="Líneas marcadas para ir al trabajo">
</p>

**Paradas**

- **+ Añadir parada** abre la lista de líneas. Elige una para ver sus paradas en el orden del recorrido y toca la tuya.
- Si ya conoces el número, usa **🔢 Escribir número**: un teclado numérico muestra el nombre de la parada mientras escribes y no deja guardar una que no exista.
- Toca una parada añadida para cambiarla por otra, o pulsa **✕** para quitarla.

<p align="center">
  <img src="docs/images/elegir-linea.png" width="200" alt="Elegir parada por línea o escribiendo el número">
  &nbsp;
  <img src="docs/images/paradas-de-linea.png" width="200" alt="Paradas de la línea 1 en orden de recorrido">
  &nbsp;
  <img src="docs/images/teclado-numerico.png" width="200" alt="Teclado numérico mostrando el nombre de la parada 42">
</p>

**Líneas**

- Solo aparecen las líneas que pasan por las paradas de esa ruta. Marca las que te sirven.
- Con **Todas** marcada, se muestra cualquier bus.

Valores por defecto:

| Ruta | Paradas | Líneas |
|---|---|---|
| 🏢 Al trabajo | 42 y 44 | 3 y 3A |
| 🏠 A casa | 159 y 3 | 3 y 3A |

### Complicaciones en la esfera

1. Mantén pulsada la esfera y entra en **Personalizar**.
2. Ve a **Complicaciones**, toca el hueco que quieras y elige **BusCoruña**.
3. Selecciona **Bus al trabajo** o **Bus a casa**.

| Tamaño | Qué se ve |
|---|---|
| Círculo pequeño | Icono de la ruta (💼 o 🏠) y el tiempo con la línea: `8m 3A` |
| Alargada | `Bus 3A en 8 min` y la parada |

La esfera hace la cuenta atrás por sí sola. Wear OS solo deja actualizar una complicación cada 5 minutos como mínimo, así que la app vuelve a consultar con esa frecuencia y, además, cada vez que la abres. Al tocar la complicación se abre la lista de próximos buses de esa ruta.

El color del icono lo decide la esfera. En muchas esferas se puede cambiar desde **Personalizar → Color**.

## Instalación

No está publicada en Google Play: hay que compilarla e instalarla desde Android Studio.

### Requisitos

- Reloj con **Wear OS** (Android 8.0 o superior). Probada en un Samsung Galaxy Watch4.
- [Android Studio](https://developer.android.com/studio) con el SDK de Android 33.
- **JDK 17**. El proyecto usa Kotlin 1.7, que falla con JDK más recientes. En Android Studio, configúralo en **Settings → Build Tools → Gradle → Gradle JDK**; para la terminal, apunta `JAVA_HOME` a un JDK 17.

### Pasos

1. Clona el repositorio y ábrelo en Android Studio.
2. En el reloj, activa **Ajustes → Opciones para desarrolladores → Depuración inalámbrica** y vincúlalo desde **Device Manager → Pair Devices Using Wi-Fi**.
3. En **Build → Select Build Variant…** elige **release**. La variante debug funciona, pero es bastante más lenta en el reloj.
4. Selecciona el reloj y pulsa **Run ▶**.

Desde la terminal:

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

La build release se firma con la clave de depuración para poder instalarla directamente. Para distribuirla habría que configurar una clave propia en `app/build.gradle`.

## Cómo funciona

- **Datos:** consulta directamente la web de iTranvías (`itranvias.com/queryitr_v3.php`):
  - `func=0`: próximos buses de una parada;
  - `func=7`: catálogo de líneas y paradas con sus recorridos. Se guarda en el reloj y se renueva cada 7 días.
- **Arquitectura:** capas `datasource` → `repository` → `presentation`/`ui`, con Jetpack Compose for Wear OS para la interfaz y una `SuspendingComplicationDataSourceService` para las complicaciones.
- **Rendimiento:**
  - las paradas de las dos rutas se consultan en paralelo con un único cliente HTTP compartido;
  - la build release usa R8 e incluye un Baseline Profile para arrancar más rápido.

```
app/src/main/java/com/esanchon/buscorua/presentation/
├── complication/   Complicaciones de la esfera
├── datasource/     API de iTranvías, catálogo y configuración guardada
├── domain/         Modelos (rutas, paradas, líneas, llegadas)
├── presentation/   Presenter de la pantalla principal
├── repository/     Combina datos y aplica los filtros de cada ruta
├── theme/          Colores y tipografía
└── ui/             Pantallas (principal, detalle, configuración, selectores)
```

## Aviso

Proyecto personal sin relación con la Compañía de Tranvías de La Coruña ni con iTranvías. Usa un servicio web público no documentado que puede cambiar o dejar de funcionar sin aviso. Los tiempos son estimaciones del propio servicio.

## Licencia

El código se distribuye bajo la [licencia MIT](LICENSE). El logotipo "TC" del icono de la app pertenece a la Compañía de Tranvías de La Coruña y no está cubierto por esta licencia.
