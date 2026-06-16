package app.thdev.glassnavlab.core.router

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

interface Route {
    val route: String
}

interface ComposeRoute : Route

interface ActivityRoute : Route {
    val activityKey: String
}

interface WebRoute : Route {
    val webPathSegments: List<String>
}

interface RouteSpec<out R : Route> {
    val routePattern: String
}

interface StaticRouteSpec<out R : Route> : RouteSpec<R> {
    val route: R

    override val routePattern: String
        get() = route.route
}

interface TopLevelRouteSpec<out R : ComposeRoute> : StaticRouteSpec<R> {
    val destinationId: String
    val title: String
}

interface ActivityRouteSpec<out R : ActivityRoute> : RouteSpec<R> {
    val activityKey: String
}

interface RouteEvent

data class RouteStack(
    val entries: List<Route>,
) {
    init {
        require(entries.isNotEmpty()) { "RouteStack requires at least one route." }
    }

    val topRoute: Route
        get() = entries.last()

    val webPathSegments: List<String>
        get() = entries.flatMap { route ->
            (route as? WebRoute)?.webPathSegments.orEmpty()
        }

    companion object {
        fun single(route: Route): RouteStack {
            return RouteStack(listOf(route))
        }

        fun of(vararg routes: Route): RouteStack {
            return RouteStack(routes.toList())
        }
    }
}

data class RoutePlan(
    val composeStack: RouteStack? = null,
    val activityRoutes: List<ActivityRoute> = emptyList(),
    val launchSingleTop: Boolean = true,
    val restoreState: Boolean = true,
) {
    init {
        require(composeStack != null || activityRoutes.isNotEmpty()) {
            "RoutePlan requires a compose stack or at least one activity route."
        }
    }

    companion object {
        fun fromStack(
            stack: RouteStack,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            val activityRoutes = stack.entries.filterIsInstance<ActivityRoute>()
            val composeRoutes = stack.entries.filterNot { route -> route is ActivityRoute }

            return RoutePlan(
                composeStack = composeRoutes
                    .takeIf { routes -> routes.isNotEmpty() }
                    ?.let(::RouteStack),
                activityRoutes = activityRoutes,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        fun compose(
            stack: RouteStack,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            return RoutePlan(
                composeStack = stack,
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }

        fun activity(
            route: ActivityRoute,
            launchSingleTop: Boolean = true,
            restoreState: Boolean = true,
        ): RoutePlan {
            return RoutePlan(
                activityRoutes = listOf(route),
                launchSingleTop = launchSingleTop,
                restoreState = restoreState,
            )
        }
    }
}

data class RouteCommand(
    val stack: RouteStack,
    val launchSingleTop: Boolean = true,
    val restoreState: Boolean = true,
) {
    constructor(
        route: Route,
        launchSingleTop: Boolean = true,
        restoreState: Boolean = true,
    ) : this(
        stack = RouteStack.single(route),
        launchSingleTop = launchSingleTop,
        restoreState = restoreState,
    )

    val route: Route
        get() = stack.topRoute

    val plan: RoutePlan
        get() = RoutePlan.fromStack(
            stack = stack,
            launchSingleTop = launchSingleTop,
            restoreState = restoreState,
        )
}

fun interface Router {
    fun navigate(command: RouteCommand)
}

fun interface RouteEventSink {
    fun onRouteEvent(event: RouteEvent)
}

interface DeepLinkSpec {
    val priority: Int
        get() = 0

    fun match(link: WebRouteLink): RouteStack?
}

interface RouteRegistry {
    val defaultStack: RouteStack
    val topLevelRouteSpecs: List<TopLevelRouteSpec<*>>
    val deepLinkSpecs: List<DeepLinkSpec>

    fun stackForDestination(destinationId: String): RouteStack?
    fun resolve(link: WebRouteLink): RouteStack?
}

data class WebRouteLink(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
    val queryParameters: Map<String, List<String>>,
) {
    fun pathSegmentsAfter(prefix: String): List<String>? {
        if (pathSegments.firstOrNull() != prefix) return null
        return pathSegments.drop(1)
    }

    companion object {
        fun parse(uriString: String): WebRouteLink? {
            val uri = runCatching { URI(uriString) }.getOrNull() ?: return null
            val scheme = uri.scheme ?: return null
            val host = uri.host ?: return null
            val pathSegments = uri.path
                .orEmpty()
                .split("/")
                .filter(String::isNotBlank)

            return WebRouteLink(
                scheme = scheme.lowercase(),
                host = host.lowercase(),
                pathSegments = pathSegments,
                queryParameters = uri.query.orEmpty().toQueryParameters(),
            )
        }

        private fun String.toQueryParameters(): Map<String, List<String>> {
            if (isBlank()) return emptyMap()

            return split("&")
                .mapNotNull { pair ->
                    val key = pair.substringBefore("=", missingDelimiterValue = "").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    val value = pair.substringAfter("=", missingDelimiterValue = "")
                    key.urlDecode() to value.urlDecode()
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second },
                )
        }

        private fun String.urlDecode(): String {
            return URLDecoder.decode(this, StandardCharsets.UTF_8.name())
        }
    }
}
