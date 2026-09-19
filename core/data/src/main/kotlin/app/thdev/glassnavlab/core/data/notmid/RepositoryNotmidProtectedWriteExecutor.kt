package app.thdev.glassnavlab.core.data.notmid

import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteExecutor
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRepository
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteRequest
import app.thdev.glassnavlab.core.domain.notmid.NotmidProtectedWriteResult
import app.thdev.glassnavlab.core.model.notmid.NotmidAuthState

class RepositoryNotmidProtectedWriteExecutor(
    private val repository: NotmidProtectedWriteRepository,
) : NotmidProtectedWriteExecutor {
    override suspend fun execute(
        authState: NotmidAuthState,
        request: NotmidProtectedWriteRequest,
    ): NotmidProtectedWriteResult = when (request) {
        is NotmidProtectedWriteRequest.PublishCapture -> {
            repository.publishCapture(authState, request.request)
            NotmidProtectedWriteResult.Completed
        }
        is NotmidProtectedWriteRequest.SaveClip -> {
            repository.saveClip(authState, request.clipId)
            NotmidProtectedWriteResult.Completed
        }
        is NotmidProtectedWriteRequest.SendThreadMessage -> {
            val receipt = repository.sendThreadMessage(authState, request.threadId, request.request)
            NotmidProtectedWriteResult.MessageSent(receipt.message)
        }
        is NotmidProtectedWriteRequest.StartThread -> {
            val receipt = repository.startThread(authState, request.request)
            NotmidProtectedWriteResult.ThreadStarted(receipt.thread, receipt.message)
        }
        is NotmidProtectedWriteRequest.RespondThreadInvite -> {
            val receipt = repository.respondThreadInvite(authState, request.threadId, request.decision)
            NotmidProtectedWriteResult.ThreadUpdated(receipt.thread)
        }
        is NotmidProtectedWriteRequest.UpdateProfileSettings -> {
            val receipt = repository.updateProfileSettings(authState, request.request)
            NotmidProtectedWriteResult.ProfileUpdated(receipt.settings.user)
        }
    }
}
