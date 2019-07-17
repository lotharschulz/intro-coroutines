package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    // this scope inherits the context from the outer scope
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .body() ?: listOf()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        GlobalScope.async {// started in global scope
            service
                .getRepoContributors(req.org, repo.name)
                .also { log("repo: ${repo.name} contained $it contributors") }
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    return deferreds.awaitAll().flatten().aggregate()

}