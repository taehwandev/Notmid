package app.thdev.glassnavlab.core.domain.notmid

class GetNotmidDestinationsUseCase(
    private val repository: NotmidContentRepository,
) {
    operator fun invoke() = repository.destinations()
}
