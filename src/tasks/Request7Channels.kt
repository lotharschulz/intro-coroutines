package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import contributors.logRepos
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import samples.log

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
            log("for start")
            val users = service.getRepoContributors(req.org, repo.name)
                .also { log("repo: ${repo.name}") }
                .bodyList()
            log("users: ${users}")
            channel.send(users)
            log("after channel.send(users)")
        }
        repeat(repos.size) {
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.lastIndex)
        }

    }
}
