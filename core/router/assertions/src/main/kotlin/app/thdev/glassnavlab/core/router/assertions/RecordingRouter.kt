package app.thdev.glassnavlab.core.router.assertions

import app.thdev.glassnavlab.core.router.runtime.RouteCommand
import app.thdev.glassnavlab.core.router.runtime.RoutePlan
import app.thdev.glassnavlab.core.router.runtime.Router

class RecordingRouter : Router {
    private val recordedCommands = mutableListOf<RouteCommand>()

    val commands: List<RouteCommand>
        get() = recordedCommands.toList()

    val plans: List<RoutePlan>
        get() = recordedCommands.map(RouteCommand::plan)

    val lastCommand: RouteCommand?
        get() = recordedCommands.lastOrNull()

    val lastPlan: RoutePlan?
        get() = plans.lastOrNull()

    override fun navigate(command: RouteCommand) {
        recordedCommands += command
    }

    fun clear() {
        recordedCommands.clear()
    }

    fun assertCommandCount(expected: Int) {
        assertEquals(
            expected = expected,
            actual = commands.size,
            label = "route command count",
        )
    }

    fun assertLastStack(block: RouteStackSubject.() -> Unit) {
        val stack = (lastCommand as? RouteCommand.SetComposeStack)?.stack
            ?: fail("Expected a recorded compose stack route command.")
        RouteStackSubject(stack).block()
    }

    fun assertLastPlan(block: RoutePlanSubject.() -> Unit) {
        val plan = lastPlan ?: fail("Expected a recorded route plan.")
        RoutePlanSubject(plan).block()
    }
}
