package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview

/** Kept for compatibility and cheap HTML parses; the universal resolver is now the default. */
class OpenGraphResolver(
    override val name: String = "open-graph",
    private val userAgentOverride: String? = null,
) : Resolver {
    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        val ua = userAgentOverride ?: context.userAgent
        val fetched = HtmlFetcher.get(url, context, ua) ?: return null
        val (html, finalUrl) = fetched
        return parse(html, finalUrl, name)
    }

    companion object {
        fun parse(html: String, finalUrl: String, source: String = "open-graph"): Preview =
            MetadataParser.parseHtml(html, finalUrl, source)
    }
}
