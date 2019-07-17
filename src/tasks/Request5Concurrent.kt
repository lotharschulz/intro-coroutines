package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    // this scope inherits the context from the outer scope
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: listOf()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        async(Dispatchers.Default) {// nested coroutine started with the inherited context
            service
                .getRepoContributors(req.org, repo.name)
                .also { log("repo: ${repo.name} contained $it contributors") }
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    deferreds.awaitAll().flatten().aggregate()  // the last expression inside the lambda is the result
}