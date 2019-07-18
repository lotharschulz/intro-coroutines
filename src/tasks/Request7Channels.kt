package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        var allUsers = emptyList<User>()
        val channel = Channel<List<User>>()

        for ((index, repo) in repos.withIndex()) {
            val users = service.getRepoContributors(req.org, repo.name)
                .also { log("repo: ${repo.name} contained $it contributors") }
                .bodyList()
            channel.send(users)
        }
        repeat(repos.size) {
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.lastIndex)
        }

    }
}
