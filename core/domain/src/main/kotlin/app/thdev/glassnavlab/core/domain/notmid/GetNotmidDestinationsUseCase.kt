package app.thdev.glassnavlab.core.domain.notmid

class GetNotmidDestinationsUseCase(
    private val repository: NotmidContentRepository,
) {
    suspend operator fun invoke() = repository.destinations()
}
